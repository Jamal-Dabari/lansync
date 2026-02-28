package org.example.app.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Messageprotocal {

  public Messageprotocal() {
    super();
  }

  public static void sendMessage(OutputStream out, byte[] bytes) {
    try {
      // convert bytes.length to 4 bytes (big-endian)
      byte[] messageHeader = new byte[4];
      int length = bytes.length;
      messageHeader[0] = (byte) (length >> 24);
      messageHeader[1] = (byte) (length >> 16);
      messageHeader[2] = (byte) (length >> 8);
      messageHeader[3] = (byte) (length);

      // Write those 4 bytes
      out.write(messageHeader);
      // Write the actual data
      out.write(bytes);
      // flush
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static byte[] recvMessage(InputStream in) {
    byte[] data = new byte[4];

    try {
      // Read exactly 4 bytes
      data = in.readNBytes(4);
      // Read exactly that many bytes
      int length = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8)
          | (data[3] & 0xFF);

      byte[] message = in.readNBytes(length);
      return message;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return data;
  }

}
