package org.example.app.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;

import org.example.app.manifest.ManifestBuilder;
import org.example.app.net.FileHeaderMessage;
import org.example.app.net.Frame;
import org.example.app.net.MessageType;
import org.example.app.net.SecureChannel;
import org.example.app.util.SafePath;

/**
 * Moves one file at a time over a {@link SecureChannel}, as a header frame
 * followed by chunk frames and a terminator.
 *
 * <p>Receiving is deliberately transactional: data lands in a temporary file, is
 * checked against the size and digest the sender declared, and only then is moved
 * into place atomically. A failed, truncated or tampered transfer therefore never
 * leaves a partial or corrupt file in the sync directory.
 *
 * <p>Instances are not thread-safe; each session owns one engine.
 *
 * @author Jamal
 * @see SyncSession
 */
public class TransferEngine {

  /** Plaintext bytes per {@link MessageType#FILE_CHUNK} frame. */
  public static final int CHUNK_SIZE = 64 * 1024;

  /** Suffix of the temporary file used while a transfer is in flight. */
  public static final String TMP_SUFFIX = ".lansync.tmp";

  private final SecureChannel channel;

  /**
   * Creates an engine bound to one session's channel.
   *
   * @param channel the encrypted channel to transfer over
   * @throws NullPointerException if {@code channel} is {@code null}
   */
  public TransferEngine(SecureChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  /**
   * Sends one file: a {@link MessageType#FILE_HEADER}, then one
   * {@link MessageType#FILE_CHUNK} per {@value #CHUNK_SIZE} bytes, then a
   * {@link MessageType#FILE_END}.
   *
   * @param root    the sync root the path is relative to
   * @param relPath the file's path relative to {@code root}
   * @throws java.nio.file.NoSuchFileException if the file does not exist
   * @throws IOException                       if {@code relPath} escapes the root or reading fails
   * @throws Exception                         if encryption or the channel fails
   */
  public void sendFile(Path root, String relPath) throws Exception {
    Path source = SafePath.resolve(root, relPath);
    String digest = ManifestBuilder.sha256(source);
    channel.sendJson(MessageType.FILE_HEADER,
        new FileHeaderMessage(relPath, Files.size(source), digest));

    try (InputStream in = Files.newInputStream(source)) {
      byte[] buffer = new byte[CHUNK_SIZE];
      int read;
      while ((read = in.read(buffer)) > 0) {
        channel.send(MessageType.FILE_CHUNK,
            read == buffer.length ? buffer : Arrays.copyOf(buffer, read));
      }
    }
    channel.sendEmpty(MessageType.FILE_END);
  }

  /**
   * Consumes the chunk stream following an already-read header and commits the file.
   *
   * @param root   the sync root the header's path is relative to
   * @param header the header frame's body, describing the incoming file
   * @throws IOException if the path escapes the root, an unexpected frame arrives,
   *                     or the received size or digest does not match the header
   * @throws Exception   if decryption fails, which means the data was tampered with
   */
  public void receiveFile(Path root, FileHeaderMessage header) throws Exception {
    Path target = SafePath.resolve(root, header.getPath());
    Path tmp = target.resolveSibling(target.getFileName() + TMP_SUFFIX);
    Files.createDirectories(target.getParent());

    try {
      long received = writeChunks(tmp);
      verify(header, tmp, received);
      Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      // Never leave a partial file behind for the next manifest scan to pick up.
      Files.deleteIfExists(tmp);
      throw e;
    }
  }

  /** Streams chunk frames into {@code tmp} until FILE_END, returning the byte count. */
  private long writeChunks(Path tmp) throws Exception {
    long received = 0;
    try (OutputStream fileOut = Files.newOutputStream(tmp)) {
      while (true) {
        Frame frame = channel.receive();
        if (frame.getType() == MessageType.FILE_END) {
          return received;
        }
        if (frame.getType() != MessageType.FILE_CHUNK) {
          throw new IOException("Unexpected frame during transfer: " + frame.getType());
        }
        fileOut.write(frame.getPayload());
        received += frame.getPayloadLength();
      }
    }
  }

  /** Checks the received bytes against what the sender declared. */
  private static void verify(FileHeaderMessage header, Path tmp, long received) throws IOException {
    if (received != header.getSize()) {
      throw new IOException("Size mismatch for " + header.getPath()
          + ": expected " + header.getSize() + ", got " + received);
    }
    String actual = ManifestBuilder.sha256(tmp);
    if (!actual.equals(header.getSha256())) {
      throw new IOException("Hash mismatch for " + header.getPath());
    }
  }

  /**
   * Removes a file the peer no longer has, as requested in mirror mode.
   *
   * @param root    the sync root the path is relative to
   * @param relPath the file's path relative to {@code root}
   * @return {@code true} if a file was deleted, {@code false} if it was already gone
   * @throws IOException if the path escapes the root or deletion fails
   */
  public boolean deleteFile(Path root, String relPath) throws IOException {
    return Files.deleteIfExists(SafePath.resolve(root, relPath));
  }
}
