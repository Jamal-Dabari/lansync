package org.example.app.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads and writes {@link Frame}s in their length-prefixed wire form:
 * {@code [4-byte big-endian length][1-byte type][payload]}, where the length
 * counts the type byte plus the payload.
 *
 * <p>This class is stateless and thread-safe; concurrency on a given stream is
 * the caller's responsibility.
 *
 * @author Jamal
 */
public final class FrameIO {

  /**
   * The largest frame this reader will accept, guarding against a corrupt or
   * hostile length prefix causing a huge allocation.
   */
  public static final int MAX_FRAME = 16 * 1024 * 1024;

  private FrameIO() {
    throw new AssertionError("No instances of FrameIO");
  }

  /**
   * Writes one frame and flushes it.
   *
   * @param out   the stream to write to
   * @param frame the frame to write
   * @throws IOException if the underlying stream fails
   */
  public static void write(OutputStream out, Frame frame) throws IOException {
    DataOutputStream sink = new DataOutputStream(out);
    byte[] payload = frame.getPayload();
    sink.writeInt(payload.length + 1);
    sink.writeByte(frame.getType().getCode());
    sink.write(payload);
    sink.flush();
  }

  /**
   * Reads one frame, blocking until it is complete.
   *
   * @param in the stream to read from
   * @return the frame that was read
   * @throws EOFException if the stream ends before a whole frame arrives
   * @throws IOException  if the length prefix is out of range, the payload is
   *                      truncated, or the underlying stream fails
   */
  public static Frame read(InputStream in) throws IOException {
    DataInputStream source = new DataInputStream(in);
    int length = source.readInt();
    if (length <= 0 || length > MAX_FRAME) {
      throw new IOException("Invalid frame length: " + length);
    }
    byte type = source.readByte();
    byte[] payload = source.readNBytes(length - 1);
    if (payload.length != length - 1) {
      throw new EOFException("Truncated frame payload: expected " + (length - 1)
          + " bytes, got " + payload.length);
    }
    return new Frame(MessageType.fromCode(type), payload);
  }
}
