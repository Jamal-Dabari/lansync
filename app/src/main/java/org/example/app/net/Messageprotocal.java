package org.example.app.net;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class Messageprotocal {

  private static BufferedWriter bf;
  private static PrintWriter p;

  public static void sendMessage(OutputStream out, byte[] bytes) {
    try {
      // convert bytes.length to 4 bytes (big-endian)
      byte[] j = new byte[bytes.length * 4];
      // Write those 4 bytes
      p = new PrintWriter(out);

      // Write the actual data
      bf = new BufferedWriter(p, j.length);
      // flush
      bf.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static byte[] recvMessage(InputStream in) {

    try {
      // Read exactly 4 bytes
      if (in.available() > 4) {
      }
      // Convert to integer
      // Read exactly that many bytes
      // return the bytes
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
