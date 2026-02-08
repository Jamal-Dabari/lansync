package org.example.app.net;

import java.io.*;
import java.net.*;

public class Messageprotocal {

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;

  public void start() {

    int port = 5176;

    try {
      serverSocket = new ServerSocket(port);
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
