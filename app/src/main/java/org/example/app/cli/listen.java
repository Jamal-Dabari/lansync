package org.example.app.cli;

import java.io.IOException;
import java.util.concurrent.Callable;

//import org.example.app.net.TransferClient;
import org.example.app.net.TransferServer;

import picocli.CommandLine.Command;

@Command(name = "listen", description = "Listen For incoming files")
public class listen implements Callable<Integer> {

  private TransferServer ts = new TransferServer();

  // Listen to recieve files
  @Override
  public Integer call() {
    System.out.println("listening...");

    try {

      ts.start();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }

}
