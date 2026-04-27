package org.example.app.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FrameIO {

  public static final int MAX_FRAME = 16 * 1024 * 1024;

  private FrameIO() {
  }

  public static void write(OutputStream out, Frame f) throws IOException {
    DataOutputStream d = new DataOutputStream(out);
    byte[] p = f.getPayload();
    d.writeInt(p.length + 1);
    d.writeByte(f.getType().code);
    d.write(p);
    d.flush();
  }

  public static Frame read(InputStream in) throws IOException {
    DataInputStream d = new DataInputStream(in);
    int len = d.readInt();
    if (len <= 0 || len > MAX_FRAME) throw new IOException("Invalid frame length: " + len);
    byte type = d.readByte();
    byte[] payload = d.readNBytes(len - 1);
    if (payload.length != len - 1) throw new IOException("Truncated frame payload");
    return new Frame(MessageType.fromCode(type), payload);
  }
}
