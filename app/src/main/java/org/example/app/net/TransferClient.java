package org.example.app.net;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TransferClient {

  private Socket clientSocket;
  private BufferedReader bufferedReader;
  private BufferedWriter bufferedWriter;
  private PrintWriter out;
  private MessageProtocol ms;

  public TransferClient(Socket clientSocket, BufferedReader bufferedReader, BufferedWriter bufferedWriter,
      PrintWriter out, MessageProtocol ms) {
    this.clientSocket = clientSocket;
    this.bufferedReader = bufferedReader;
    this.bufferedWriter = bufferedWriter;
    this.out = out;
    this.ms = ms;

  }

  public TransferClient() {
    super();
  }

  public void start() {

    int port = 5176;

    try {
      clientSocket = new Socket("localhost", port);

      bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      out = new PrintWriter(clientSocket.getOutputStream(), true);

      Scanner sc = new Scanner(System.in);

      while (true) {

        String msgToSend = sc.nextLine();
        byte[] bytes = msgToSend.getBytes();
        ms.sendMessage(clientSocket.getOutputStream(), bytes);
        out.println(msgToSend);

        String response = bufferedReader.readLine();

        System.out.println("Server: " + response);
        System.out.println("Server: " + bufferedReader.readLine());

        if (msgToSend.equalsIgnoreCase("bye")) {
          break;
        }

      }

      sc.close();

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
