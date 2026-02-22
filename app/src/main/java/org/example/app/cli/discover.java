package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.example.app.discovery.*;

@Command(name = "discover", description = "Discover connections across a network")
public class discover implements Callable<Integer> {
  DeviceDiscovery ds;

  // @Option(names = "-s", description = "Specify an Ip Adress to look for",)
  // String specify;

  @Override
  public Integer call() {

    System.out.println("discovering... ");
    ds = new DeviceDiscovery();
    ds.registering();
    ds.discovering();

    try {
      Thread.sleep(5000);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return 0;
  }
}
