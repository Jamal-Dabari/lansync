package org.example.app.sync;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import org.example.app.crypto.AesGcmCipher;
import org.example.app.crypto.HmacUtil;
import org.example.app.crypto.KeyDerivation;
import org.example.app.manifest.DiffResult;
import org.example.app.manifest.Differ;
import org.example.app.manifest.Manifest;
import org.example.app.manifest.ManifestBuilder;
import org.example.app.net.AuthMessage;
import org.example.app.net.ErrorMessage;
import org.example.app.net.FileHeaderMessage;
import org.example.app.net.FilePathMessage;
import org.example.app.net.Frame;
import org.example.app.net.FrameIO;
import org.example.app.net.HelloMessage;
import org.example.app.net.MessageType;
import org.example.app.net.SecureChannel;
import org.example.app.net.Wire;

/**
 * One end of a sync conversation between two peers.
 *
 * <p>Both peers run this same class; {@link Role} decides which half of each
 * exchange it performs. The client drives — it computes the diff and issues
 * pushes, pulls and deletes — while the server responds to whatever it is asked
 * for until the client says goodbye. Concentrating the decisions on one side
 * keeps the protocol free of the deadlocks and tie-breaks a symmetric design
 * would need.
 *
 * <p>A session runs once and is not reusable. It owns its socket and closes it
 * when {@link #run()} returns or fails.
 *
 * @author Jamal
 * @see TransferEngine
 * @see SecureChannel
 */
public class SyncSession implements Closeable {

  /** Which half of the conversation this end performs. */
  public enum Role {
    /** Connects out, computes the diff, and issues every request. */
    CLIENT,
    /** Accepts inbound connections and answers the client's requests. */
    SERVER
  }

  private static final int NONCE_LENGTH = 32;
  private static final String CLIENT_PROOF_LABEL = "lansync-client";
  private static final String SERVER_PROOF_LABEL = "lansync-server";

  private final Role role;
  private final Socket socket;
  private final Path syncRoot;
  private final char[] passphrase;
  private final String localDeviceId;

  private SecureChannel channel;
  private String remoteDeviceId;
  private boolean mirror;
  private boolean dryRun;
  private boolean closed;

  /**
   * Creates a session over an already-connected socket.
   *
   * <p>The session takes ownership of {@code passphrase} and zeroes it once the
   * key has been derived, so callers sharing a passphrase across sessions must
   * pass a copy.
   *
   * @param role          whether this end drives or responds
   * @param socket        the connected socket to the peer
   * @param syncRoot      the directory being synchronised
   * @param passphrase    the shared passphrase, consumed and zeroed by this session
   * @param localDeviceId this device's ID
   * @throws NullPointerException if any argument is {@code null}
   */
  public SyncSession(Role role, Socket socket, Path syncRoot, char[] passphrase, String localDeviceId) {
    this.role = Objects.requireNonNull(role, "role");
    this.socket = Objects.requireNonNull(socket, "socket");
    this.syncRoot = Objects.requireNonNull(syncRoot, "syncRoot");
    this.passphrase = Objects.requireNonNull(passphrase, "passphrase");
    this.localDeviceId = Objects.requireNonNull(localDeviceId, "localDeviceId");
  }

  /**
   * Sets whether remote files missing locally should be deleted instead of pulled.
   * Only meaningful for {@link Role#CLIENT}.
   *
   * @param mirror {@code true} to delete remote-only files
   */
  public void setMirror(boolean mirror) {
    this.mirror = mirror;
  }

  /**
   * Sets whether to report the planned work and stop without transferring anything.
   * Only meaningful for {@link Role#CLIENT}.
   *
   * @param dryRun {@code true} to preview only
   */
  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  /**
   * Returns the peer's device ID, known once the handshake has completed.
   *
   * @return the remote device ID, or {@code null} before the handshake
   */
  public String getRemoteDeviceId() {
    return remoteDeviceId;
  }

  /**
   * Runs the session to completion, then closes the socket.
   *
   * @throws IOException if authentication fails, the peer speaks another protocol
   *                     version, or the connection breaks
   * @throws Exception   if encryption or file I/O fails
   */
  public void run() throws Exception {
    try {
      channel = handshake();
      Manifest localManifest = buildLocalManifest();
      Manifest remoteManifest = exchangeManifests(localManifest);

      if (role == Role.SERVER) {
        serveRequests();
      } else {
        driveSync(localManifest, remoteManifest);
      }
    } finally {
      close();
    }
  }

  // ---------------------------------------------------------------- handshake

  /**
   * Performs the HELLO / CHALLENGE / AUTH / AUTH_OK exchange.
   *
   * <p>Both sides derive the same key from the passphrase and a salt built from
   * the sorted device-ID pair, then prove possession of it with an HMAC over the
   * whole transcript. The nonces make each transcript unique, so a recorded
   * handshake cannot be replayed; the two distinct labels stop a proof being
   * reflected back at the peer that produced it.
   *
   * @return the encrypted channel to use for the rest of the session
   * @throws IOException if the peer's version, identity or proof is unacceptable
   */
  private SecureChannel handshake() throws Exception {
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();

    String localNonce = Base64.getEncoder().encodeToString(HmacUtil.nonce(NONCE_LENGTH));
    HelloMessage peer = exchangeGreetings(in, out, localNonce);
    validatePeer(peer);
    remoteDeviceId = peer.getDeviceId();

    Transcript transcript = Transcript.of(role, localDeviceId, remoteDeviceId, localNonce, peer.getNonce());
    byte[] key = KeyDerivation.deriveKey(passphrase,
        KeyDerivation.pairSalt(transcript.clientId(), transcript.serverId()));
    byte[] expectedClientProof = transcript.proof(key, CLIENT_PROOF_LABEL);
    byte[] expectedServerProof = transcript.proof(key, SERVER_PROOF_LABEL);

    if (role == Role.CLIENT) {
      writePlain(out, MessageType.AUTH, new AuthMessage(encode(expectedClientProof)));
      AuthMessage accepted = readPlain(in, MessageType.AUTH_OK, AuthMessage.class);
      requireProof(expectedServerProof, accepted.getProof(), out);
    } else {
      AuthMessage offered = readPlain(in, MessageType.AUTH, AuthMessage.class);
      requireProof(expectedClientProof, offered.getProof(), out);
      writePlain(out, MessageType.AUTH_OK, new AuthMessage(encode(expectedServerProof)));
    }

    return new SecureChannel(in, out, new AesGcmCipher(key));
  }

  /** Sends our greeting and reads the peer's, in the order this role requires. */
  private HelloMessage exchangeGreetings(InputStream in, OutputStream out, String localNonce)
      throws IOException {
    HelloMessage ours = new HelloMessage(localDeviceId, localNonce);
    if (role == Role.CLIENT) {
      writePlain(out, MessageType.HELLO, ours);
      return readPlain(in, MessageType.CHALLENGE, HelloMessage.class);
    }
    HelloMessage theirs = readPlain(in, MessageType.HELLO, HelloMessage.class);
    writePlain(out, MessageType.CHALLENGE, ours);
    return theirs;
  }

  private void validatePeer(HelloMessage peer) throws IOException {
    if (peer.getVersion() != Wire.PROTOCOL_VERSION) {
      throw new IOException("Protocol version mismatch: peer speaks v" + peer.getVersion()
          + ", this build speaks v" + Wire.PROTOCOL_VERSION);
    }
    if (peer.getDeviceId() == null || peer.getDeviceId().equals(localDeviceId)) {
      throw new IOException("Refusing to sync with a peer reporting our own device ID");
    }
    if (peer.getNonce() == null) {
      throw new IOException("Peer omitted its handshake nonce");
    }
  }

  /** Compares proofs in constant time and tells the peer before failing. */
  private void requireProof(byte[] expected, String offered, OutputStream out) throws IOException {
    if (!HmacUtil.constantTimeEq(expected, decode(offered))) {
      try {
        writePlain(out, MessageType.ERROR, new ErrorMessage("AUTH_FAILED", "passphrase mismatch"));
      } catch (IOException ignored) {
        // Best effort: the peer may already have hung up.
      }
      throw new IOException("Authentication failed: the peer's passphrase does not match");
    }
  }

  // ------------------------------------------------------- manifests and work

  private Manifest buildLocalManifest() throws IOException {
    return ManifestBuilder.build(syncRoot, localDeviceId);
  }

  /**
   * Swaps manifests with the peer.
   *
   * <p>The client always writes first. If both sides wrote before reading, a
   * manifest larger than the socket buffer would deadlock the pair.
   *
   * @param localManifest this device's manifest
   * @return the peer's manifest
   */
  private Manifest exchangeManifests(Manifest localManifest) throws Exception {
    if (role == Role.CLIENT) {
      channel.sendJson(MessageType.MANIFEST, localManifest);
      return channel.receiveJson(MessageType.MANIFEST, Manifest.class);
    }
    Manifest remote = channel.receiveJson(MessageType.MANIFEST, Manifest.class);
    channel.sendJson(MessageType.MANIFEST, localManifest);
    return remote;
  }

  /** CLIENT side: diff the manifests, then push, pull and delete accordingly. */
  private void driveSync(Manifest localManifest, Manifest remoteManifest) throws Exception {
    DiffResult diff = Differ.diff(localManifest, remoteManifest, mirror);
    report(diff);

    if (!dryRun) {
      TransferEngine engine = new TransferEngine(channel);
      for (String path : diff.getPush()) {
        engine.sendFile(syncRoot, path);
      }
      for (String path : diff.getPull()) {
        pull(engine, path);
      }
      for (String path : diff.getDelete()) {
        channel.sendJson(MessageType.FILE_DELETE, new FilePathMessage(path));
      }
    }
    channel.sendEmpty(MessageType.BYE);
  }

  private void pull(TransferEngine engine, String path) throws Exception {
    channel.sendJson(MessageType.FILE_REQUEST, new FilePathMessage(path));
    FileHeaderMessage header =
        SecureChannel.bodyOf(channel.expect(MessageType.FILE_HEADER), FileHeaderMessage.class);
    engine.receiveFile(syncRoot, header);
  }

  /** SERVER side: answer requests until the client sends {@link MessageType#BYE}. */
  private void serveRequests() throws Exception {
    TransferEngine engine = new TransferEngine(channel);

    while (true) {
      Frame frame = channel.receive();
      switch (frame.getType()) {
        case FILE_HEADER -> {
          FileHeaderMessage header = SecureChannel.bodyOf(frame, FileHeaderMessage.class);
          System.out.println("recv   " + header.getPath());
          engine.receiveFile(syncRoot, header);
        }
        case FILE_REQUEST -> {
          String path = SecureChannel.bodyOf(frame, FilePathMessage.class).getPath();
          System.out.println("send   " + path);
          try {
            engine.sendFile(syncRoot, path);
          } catch (NoSuchFileException e) {
            // The client's manifest is stale; report it and let the sync continue.
            channel.sendError("NOT_FOUND", path);
          }
        }
        case FILE_DELETE -> {
          String path = SecureChannel.bodyOf(frame, FilePathMessage.class).getPath();
          if (engine.deleteFile(syncRoot, path)) {
            System.out.println("delete " + path);
          }
        }
        case BYE -> {
          return;
        }
        case ERROR -> throw new IOException("Peer error: "
            + SecureChannel.bodyOf(frame, ErrorMessage.class).getMessage());
        default -> throw new IOException("Unexpected frame from client: " + frame.getType());
      }
    }
  }

  private void report(DiffResult diff) {
    if (diff.isEmpty()) {
      System.out.println("Already in sync.");
      return;
    }
    diff.getPush().forEach(path -> System.out.println("push   " + path));
    diff.getPull().forEach(path -> System.out.println("pull   " + path));
    diff.getDelete().forEach(path -> System.out.println("delete " + path + " (remote)"));
  }

  // ------------------------------------------------------------------ helpers

  /** Handshake frames precede key agreement, so they are written unencrypted. */
  private static void writePlain(OutputStream out, MessageType type, Object body) throws IOException {
    FrameIO.write(out, new Frame(type, Wire.toBytes(body)));
  }

  private static <T> T readPlain(InputStream in, MessageType expected, Class<T> bodyType)
      throws IOException {
    Frame frame = FrameIO.read(in);
    if (frame.getType() == MessageType.ERROR) {
      throw new IOException("Peer refused the handshake: "
          + Wire.fromBytes(frame.getPayload(), ErrorMessage.class).getMessage());
    }
    if (frame.getType() != expected) {
      throw new IOException("Expected " + expected + " but got " + frame.getType());
    }
    return Wire.fromBytes(frame.getPayload(), bodyType);
  }

  private static String encode(byte[] raw) {
    return Base64.getEncoder().encodeToString(raw);
  }

  /** Decodes a peer-supplied proof, treating malformed input as a failed match. */
  private static byte[] decode(String base64) {
    if (base64 == null) {
      return new byte[0];
    }
    try {
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      return new byte[0];
    }
  }

  /**
   * Closes the socket and zeroes the passphrase. Idempotent, so it is safe to
   * call from both {@link #run()} and a caller's error path.
   */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      socket.close();
    } catch (IOException ignored) {
      // The session is over either way.
    }
    Arrays.fill(passphrase, '\0');
  }

  /**
   * The ordered handshake inputs both peers must agree on, in client-then-server
   * order regardless of which side is computing them.
   *
   * @param clientId    the connecting peer's device ID
   * @param serverId    the listening peer's device ID
   * @param clientNonce the connecting peer's nonce
   * @param serverNonce the listening peer's nonce
   */
  private record Transcript(String clientId, String serverId, String clientNonce, String serverNonce) {

    static Transcript of(Role role, String localId, String remoteId, String localNonce, String remoteNonce) {
      boolean isClient = role == Role.CLIENT;
      return new Transcript(
          isClient ? localId : remoteId,
          isClient ? remoteId : localId,
          isClient ? localNonce : remoteNonce,
          isClient ? remoteNonce : localNonce);
    }

    /** Computes one side's proof over this transcript, distinguished by {@code label}. */
    byte[] proof(byte[] key, String label) {
      String material = label + "|" + Wire.PROTOCOL_VERSION + "|" + clientId + "|" + serverId
          + "|" + clientNonce + "|" + serverNonce;
      return HmacUtil.hmac(key, material.getBytes(StandardCharsets.UTF_8));
    }
  }
}
