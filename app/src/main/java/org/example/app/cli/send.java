package org.example.app.cli;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import org.example.app.net.*;

@Command(name = "send", description = "Send Files across the network")
public class send implements Callable<Integer> {

  @Option(names = { "-f", "--file" })
  private File file;

  @Parameters
  String ip;

  /*
   * @Parameters
   * int port;
   */

  private TransferClient ts = new TransferClient(ip);

  @Override
  public Integer call() {

    System.out.println("sending....");
    System.out.println(ip);
    try {
      ts.start(file);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }
}
