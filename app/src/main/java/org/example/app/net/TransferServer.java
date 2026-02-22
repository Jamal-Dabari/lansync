package org.example.app.net;

import java.io.*;
import java.net.*;

public class TransferServer {

  private ServerSocket serverSocket;
  private Socket clientSocket;

  public void start() throws IOException {

    int port = 5176;
    serverSocket = new ServerSocket(port);
    String filename = "test";

    try {
      System.out.println("Server Has Started");
      clientSocket = serverSocket.accept();
      FileOutputStream fos = new FileOutputStream(filename);
      byte[] chunk = Messageprotocal.recvMessage(clientSocket.getInputStream());
      fos.write(chunk);
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
