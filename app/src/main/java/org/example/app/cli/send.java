package org.example.app.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.example.app.net.*;

@Command(name = "send", description = "Send Files across the network")
public class send implements Callable<Integer> {

  // @Option(names = { "-f", "--file" })
  // private File file;
  private TransferServer ts = new TransferServer();

  @Override
  public Integer call() {

    System.out.println("sending....");

    try {

      ts.start();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }
}
