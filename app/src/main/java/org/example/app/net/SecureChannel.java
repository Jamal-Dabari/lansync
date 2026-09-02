package org.example.app.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

import org.example.app.crypto.AesGcmCipher;

/**
 * The encrypted half of a sync session.
 *
 * <p>Once the handshake has established a shared key, every frame body is sealed
 * with AES-256-GCM as {@code [12-byte IV][ciphertext][16-byte tag]}. The frame's
 * type byte is bound in as additional authenticated data, so a captured frame
 * cannot be replayed under a different type — a {@link MessageType#FILE_CHUNK}
 * will not decrypt as a {@link MessageType#MANIFEST}.
 *
 * <p>Instances are not thread-safe: a channel wraps one socket's streams and is
 * driven by a single session thread.
 *
 * @author Jamal
 * @see org.example.app.sync.SyncSession
 */
public final class SecureChannel {

  private final InputStream in;
  private final OutputStream out;
  private final AesGcmCipher cipher;

  /**
   * Wraps a connected socket's streams.
   *
   * @param in     the stream to read frames from
   * @param out    the stream to write frames to
   * @param cipher the cipher holding the session key
   * @throws NullPointerException if any argument is {@code null}
   */
  public SecureChannel(InputStream in, OutputStream out, AesGcmCipher cipher) {
    this.in = Objects.requireNonNull(in, "in");
    this.out = Objects.requireNonNull(out, "out");
    this.cipher = Objects.requireNonNull(cipher, "cipher");
  }

  /**
   * Seals and writes one frame.
   *
   * @param type      the message type
   * @param plaintext the body to encrypt
   * @throws Exception if encryption or the underlying stream fails
   */
  public void send(MessageType type, byte[] plaintext) throws Exception {
    byte[] iv = cipher.newIv();
    byte[] ciphertext = cipher.encrypt(iv, plaintext, aad(type));
    byte[] payload = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, payload, 0, iv.length);
    System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
    FrameIO.write(out, new Frame(type, payload));
  }

  /**
   * Serializes a body to JSON, then seals and writes it.
   *
   * @param type the message type
   * @param body the body to serialize
   * @throws Exception if encryption or the underlying stream fails
   */
  public void sendJson(MessageType type, Object body) throws Exception {
    send(type, Wire.toBytes(body));
  }

  /**
   * Sends a frame that carries no body, such as {@link MessageType#BYE}.
   *
   * @param type the message type
   * @throws Exception if encryption or the underlying stream fails
   */
  public void sendEmpty(MessageType type) throws Exception {
    send(type, new byte[0]);
  }

  /**
   * Reads the next frame and returns it with its body decrypted.
   *
   * @return the decrypted frame
   * @throws IOException if the frame is too short to be sealed or the stream fails
   * @throws Exception   if authentication fails, meaning the frame was tampered
   *                     with or the peer holds a different key
   */
  public Frame receive() throws Exception {
    Frame sealed = FrameIO.read(in);
    byte[] payload = sealed.getPayload();
    if (payload.length < AesGcmCipher.IV_LEN) {
      throw new IOException("Frame too short to be sealed: " + sealed.getType());
    }
    byte[] iv = Arrays.copyOfRange(payload, 0, AesGcmCipher.IV_LEN);
    byte[] ciphertext = Arrays.copyOfRange(payload, AesGcmCipher.IV_LEN, payload.length);
    return new Frame(sealed.getType(), cipher.decrypt(iv, ciphertext, aad(sealed.getType())));
  }

  /**
   * Reads the next frame, requiring it to be of the expected type.
   *
   * <p>A {@link MessageType#ERROR} frame is translated into an exception carrying
   * the peer's explanation, so callers need not check for it separately.
   *
   * @param expected the type the protocol requires at this point
   * @return the decrypted frame
   * @throws IOException if the peer sent an error or a frame of the wrong type
   * @throws Exception   if decryption fails
   */
  public Frame expect(MessageType expected) throws Exception {
    Frame frame = receive();
    if (frame.getType() == MessageType.ERROR) {
      ErrorMessage error = bodyOf(frame, ErrorMessage.class);
      throw new IOException("Peer error " + error.getCode() + ": " + error.getMessage());
    }
    if (frame.getType() != expected) {
      throw new IOException("Expected " + expected + " but got " + frame.getType());
    }
    return frame;
  }

  /**
   * Reads the next frame of the expected type and deserializes its body.
   *
   * @param <T>      the body type
   * @param expected the type the protocol requires at this point
   * @param bodyType the class to deserialize into
   * @return the decoded body
   * @throws Exception if the frame is of the wrong type or decryption fails
   */
  public <T> T receiveJson(MessageType expected, Class<T> bodyType) throws Exception {
    return bodyOf(expect(expected), bodyType);
  }

  /**
   * Deserializes an already-decrypted frame's body.
   *
   * @param <T>      the body type
   * @param frame    a frame returned by {@link #receive()} or {@link #expect}
   * @param bodyType the class to deserialize into
   * @return the decoded body
   */
  public static <T> T bodyOf(Frame frame, Class<T> bodyType) {
    return Wire.fromBytes(frame.getPayload(), bodyType);
  }

  /**
   * Reports a failure to the peer on a best-effort basis.
   *
   * <p>Errors are sent while something has already gone wrong, so a failure to
   * deliver one is swallowed: the peer may have hung up already, and the original
   * problem is what the caller should surface.
   *
   * @param code    a short stable code
   * @param message a human-readable explanation
   */
  public void sendError(String code, String message) {
    try {
      sendJson(MessageType.ERROR, new ErrorMessage(code, message));
    } catch (Exception ignored) {
      // Nothing useful to do: the peer is already unreachable or mid-failure.
    }
  }

  /** The type byte doubles as additional authenticated data, binding body to type. */
  private static byte[] aad(MessageType type) {
    return new byte[] { type.getCode() };
  }
}
