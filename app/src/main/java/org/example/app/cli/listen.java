package org.example.app.cli;

import java.util.concurrent.Callable;

import org.example.app.net.TransferClient;

import picocli.CommandLine.Command;

@Command(name = "listen", description = "Listen For incoming files")
public class listen implements Callable<Integer> {

  private TransferClient ts = new TransferClient();

  // Listen to recieve files
  @Override
  public Integer call() {
    System.out.println("listening...");

    ts.start();

    return 0;
  }

}
