package org.example.app.net;

import java.io.*;
import java.net.*;

public class TransferServer {

  private ServerSocket serverSocket;
  private Socket clientSocket;

  public void start() throws IOException {

    int port = 5176;
    serverSocket = new ServerSocket(port);

    try {
      System.out.println("Server Has Started");
      clientSocket = serverSocket.accept();
      byte[] name = Messageprotocal.recvMessage(clientSocket.getInputStream());
      byte[] size = Messageprotocal.recvMessage(clientSocket.getInputStream());

      String realname = new String(name);
      long realSize = Long.parseLong(new String(size));
      FileOutputStream fos = new FileOutputStream(realname);
      long i = 0;

      while (i < realSize) {
        byte[] chunk = Messageprotocal.recvMessage(clientSocket.getInputStream());
        fos.write(chunk);
        i += chunk.length;
      }
      fos.close();

      /*
       * while (true) {
       * byte[] data = Messageprotocal.recvMessage(clientSocket.getInputStream());
       * String message = new String(data);
       * System.out.println("Client: " + message);
       * Messageprotocal.sendMessage(clientSocket.getOutputStream(), ("Echo: " +
       * message).getBytes());
       * 
       * if (message.equalsIgnoreCase("bye")) {
       * endConnection();
       * }
       * }
       */

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void endConnection() {
    try {
      serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
