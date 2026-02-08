package org.example.app.net;

import java.io.*;
import java.net.*;

public class TransferServer {

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader bufferedReader;
  private BufferedWriter bufferedWriter;

  public void start() throws IOException {

    int port = 5176;
    serverSocket = new ServerSocket(port);

    try {
      System.out.println("Server Has Started");
      clientSocket = serverSocket.accept();

      out = new PrintWriter(clientSocket.getOutputStream(), true);
      bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String message;

      while ((message = bufferedReader.readLine()) != null) {
        System.out.println("Client: " + message);
        out.println("Echo " + message);
        if (message.equalsIgnoreCase("bye")) {
          endConnection();
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void endConnection() {
    try {
      serverSocket.close();
      out.close();
      bufferedReader.close();
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
