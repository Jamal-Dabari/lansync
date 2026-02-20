package org.example.app.net;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class Messageprotocal {

  public Messageprotocal() {
    super();
  }

  public static void sendMessage(OutputStream out, byte[] bytes) {
    try {
      // convert bytes.length to 4 bytes (big-endian)
      byte[] messageHeader = new byte[4];
      // Write those 4 bytes
      // Write the actual data
      // flush
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static byte[] recvMessage(InputStream in) {
    byte[] data = new byte[1024];

    try {
      // Read exactly 4 bytes
      // Read exactly that many bytes
      // return the bytes
      // return data;

      while (in.available() != -1) {
        data = in.readNBytes(4);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return data;
  }

}
