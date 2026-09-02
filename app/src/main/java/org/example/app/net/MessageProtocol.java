package org.example.app.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A minimal length-prefixed message codec: {@code [4-byte big-endian length][payload]}.
 *
 * <p>This is the untyped predecessor of {@link FrameIO}, retained only for the
 * {@code listen} and {@code send} debugging commands. New protocol work should use
 * {@link FrameIO} with {@link SecureChannel}, which adds message typing and
 * authenticated encryption.
 *
 * @author Jamal
 * @see FrameIO
 */
public final class MessageProtocol {

  /** Matches {@link FrameIO#MAX_FRAME} so both codecs reject the same oversized input. */
  public static final int MAX_MESSAGE = FrameIO.MAX_FRAME;

  private MessageProtocol() {
    throw new AssertionError("No instances of MessageProtocol");
  }

  /**
   * Writes one length-prefixed message and flushes it.
   *
   * @param out   the stream to write to
   * @param bytes the payload to send
   * @throws IOException if the underlying stream fails
   */
  public static void sendMessage(OutputStream out, byte[] bytes) throws IOException {
    DataOutputStream sink = new DataOutputStream(out);
    sink.writeInt(bytes.length);
    sink.write(bytes);
    sink.flush();
  }

  /**
   * Reads one length-prefixed message, blocking until it is complete.
   *
   * @param in the stream to read from
   * @return the payload that was read
   * @throws EOFException if the stream ends before a whole message arrives
   * @throws IOException  if the length prefix is out of range or the stream fails
   */
  public static byte[] recvMessage(InputStream in) throws IOException {
    DataInputStream source = new DataInputStream(in);
    int length = source.readInt();
    if (length < 0 || length > MAX_MESSAGE) {
      throw new IOException("Invalid message length: " + length);
    }
    byte[] message = source.readNBytes(length);
    if (message.length != length) {
      throw new EOFException("Truncated message: expected " + length
          + " bytes, got " + message.length);
    }
    return message;
  }
}
