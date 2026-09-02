package org.example.app.cli;

import java.util.List;
import java.util.concurrent.Callable;

import org.example.app.discovery.Device;
import org.example.app.discovery.DeviceDiscovery;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Lists the lansync peers currently advertising themselves on the LAN.
 *
 * @author Jamal
 * @see InterfacesCommand
 */
@Command(name = "list-peers", description = "Show discovered lansync peers on the LAN")
public class ListPeersCommand implements Callable<Integer> {

  @Option(names = { "-t", "--timeout" }, description = "Discovery window (seconds)", defaultValue = "5")
  private int timeoutSeconds;

  @Option(names = "--bind", description = "Network interface or address for mDNS "
      + "(overrides the configured value; see `lansync interfaces`)")
  private String bind;

  /** Creates the command. picocli instantiates it reflectively. */
  public ListPeersCommand() {
  }

  /**
   * Runs a discovery window and prints what it finds.
   *
   * @return {@code 0} whether or not any peers were discovered
   * @throws Exception if mDNS cannot bind to the selected interface
   */
  @Override
  public Integer call() throws Exception {
    try (DeviceDiscovery d = new DeviceDiscovery(bind)) {
      System.out.println("Searching for peers (" + timeoutSeconds + "s)...");
      List<Device> peers = d.findPeers(timeoutSeconds);

      if (peers.isEmpty()) {
        System.out.println("No peers found. Is `lansync serve` running on the other device?");
        System.out.println("If it is, both devices may be on different interfaces -- "
            + "check `lansync interfaces` and pin one with `lansync config --bind <interface>`.");
        return 0;
      }
      System.out.printf("%-24s %-10s %-15s %s%n", "HOSTNAME", "ID", "ADDRESS", "PORT");
      for (Device p : peers) {
        System.out.printf("%-24s %-10s %-15s %d%n", p.getName(), p.shortId(), p.getIp(), p.getPort());
      }
    }
    return 0;
  }
}
