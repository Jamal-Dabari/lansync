package org.example.app.net;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MessageProtocol {

  private static BufferedWriter bf;

  // Using bytebuffer to convert to the length to length to big-endian
  public static void sendMessage(OutputStream out, byte[] bytes) {
    // convert bytes.length to 4 bytes (big-endian)
    int length = bytes.length;
    bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(length).array();

    // Write those 4 bytes
    bf = new BufferedWriter(new OutputStreamWriter(out));
    // Write the actual data
    try {

      bf.write(String.valueOf(bytes));

      bf.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void sendMessageLittleEndian(OutputStream out, byte[] bytes) {
    // convert bytes.length to 4 bytes (big-endian)
    int length = bytes.length;
    bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(length).array();

    // Write those 4 bytes
    bf = new BufferedWriter(new OutputStreamWriter(out));
    // Write the actual data
    try {

      bf.write(String.valueOf(bytes));

      bf.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static byte[] recvMessage(InputStream in) {
    byte[] bits = new byte[4];

    try {

      in.read(bits, 0, 4);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Read exactly 4 bytes
    // Convert to integer
    // Read exactly that many bytes
    // return the bytes

    return bits;
  }

}
