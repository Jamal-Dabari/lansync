package org.example.app.net;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import org.example.app.net.Messageprotocal;

public class TransferClient {

  private Socket clientSocket;
  private BufferedReader bufferedReader;
  private BufferedWriter bufferedWriter;
  private PrintWriter out;
  private Messageprotocal ms;

  public TransferClient(Socket clientSocket, BufferedReader bufferedReader, BufferedWriter bufferedWriter,
      PrintWriter out, Messageprotocal ms) {
    this.clientSocket = clientSocket;
    this.bufferedReader = bufferedReader;
    this.bufferedWriter = bufferedWriter;
    this.out = out;
    this.ms = ms;

  }

  public TransferClient() {
    super();
  }

  public void start(File file) {

    int port = 5176;

    try {
      clientSocket = new Socket("localhost", port);
      // Scanner sc = new Scanner(System.in);
      // Scanner sc = new Scanner(file);
      FileInputStream fis = new FileInputStream(file);
      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = fis.read(buffer)) != -1) {
        byte[] chunk = Arrays.copyOf(buffer, bytesRead);
        Messageprotocal.sendMessage(clientSocket.getOutputStream(), chunk);

      }

      /*
       * while (true) {
       * 
       * String msgToSend = sc.nextLine();
       * byte[] bytes = msgToSend.getBytes();
       * 
       * Messageprotocal.sendMessage(clientSocket.getOutputStream(), bytes);
       * 
       * byte[] response = Messageprotocal.recvMessage(clientSocket.getInputStream());
       * 
       * System.out.println("Server: " + new String(response));
       * 
       * if (msgToSend.equalsIgnoreCase("bye")) {
       * break;
       * }
       * 
       * }
       * 
       * sc.close();
       */

      fis.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() throws IOException {
    clientSocket.close();
    bufferedReader.close();
    bufferedWriter.close();
    out.close();
  }

}
