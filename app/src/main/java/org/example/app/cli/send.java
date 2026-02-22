package org.example.app.cli;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.example.app.net.*;

@Command(name = "send", description = "Send Files across the network")
public class send implements Callable<Integer> {

  @Option(names = { "-f", "--file" })
  private File file;

  private TransferClient ts = new TransferClient();

  @Override
  public Integer call() {

    System.out.println("sending....");

    try {
      ts.start(file);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }
}
