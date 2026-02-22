package org.example.app.cli;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.example.app.discovery.DeviceDiscovery;
import org.example.app.net.*;

@Command(name = "send", description = "Send Files across the network")
public class send implements Callable<Integer> {

  @Option(names = { "-f", "--file" })
  private File file;

  private DeviceDiscovery ds = new DeviceDiscovery();

  String ip = ds.getDevices().get(0).getHostAddresses()[0];
  private TransferClient ts = new TransferClient(ip);

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
