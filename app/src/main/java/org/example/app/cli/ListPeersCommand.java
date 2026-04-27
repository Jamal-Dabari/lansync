package org.example.app.cli;

import java.util.concurrent.Callable;

import javax.jmdns.ServiceInfo;

import org.example.app.discovery.DeviceDiscovery;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list-peers", description = "Show discovered lansync peers on the LAN")
public class ListPeersCommand implements Callable<Integer> {

  @Option(names = { "-t", "--timeout" }, description = "Discovery window (seconds)", defaultValue = "5")
  private int timeoutSeconds;

  @Override
  public Integer call() throws Exception {
    DeviceDiscovery d = new DeviceDiscovery();
    d.registering();
    d.discovering();
    Thread.sleep(timeoutSeconds * 1000L);

    if (d.getDevices().isEmpty()) {
      System.out.println("No peers found.");
      return 0;
    }
    for (ServiceInfo info : d.getDevices()) {
      System.out.printf("%-32s %-15s :%d%n",
          info.getName(),
          info.getHostAddresses().length > 0 ? info.getHostAddresses()[0] : "?",
          info.getPort());
    }
    return 0;
  }
}
