package org.example.app.cli;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.gson.GsonBuilder;

import org.example.app.discovery.Device;
import org.example.app.discovery.DeviceDiscovery;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Discovers peers and writes them to a JSON file, for inspecting what mDNS sees.
 *
 * <p>Kept from an early milestone; {@link ListPeersCommand} is the command to use
 * day to day.
 *
 * @author Jamal
 * @see ListPeersCommand
 */
@Command(name = "discover", description = "Discover peers and dump them to devices.json (debug)")
public class DiscoverCommand implements Callable<Integer> {

  @Option(names = { "-t", "--timeout" }, description = "Discovery window (seconds)", defaultValue = "5")
  private int timeoutSeconds;

  @Option(names = { "-o", "--out" }, description = "Output file", defaultValue = "devices.json")
  private String out;

  @Option(names = "--bind", description = "Network interface or address for mDNS "
      + "(overrides the configured value; see `lansync interfaces`)")
  private String bind;

  /** Creates the command. picocli instantiates it reflectively. */
  public DiscoverCommand() {
  }

  /**
   * Runs a discovery window and writes the result as a JSON array.
   *
   * @return {@code 0} once the file has been written
   * @throws Exception if discovery or writing the file fails
   */
  @Override
  public Integer call() throws Exception {
    try (DeviceDiscovery discovery = new DeviceDiscovery(bind)) {
      System.out.println("Discovering...");
      List<Device> peers = discovery.findPeers(timeoutSeconds);

      peers.forEach(peer -> System.out.println("Device: " + peer));
      if (peers.isEmpty()) {
        System.out.println("No devices found.");
      }

      // Overwrite rather than append: appending produced a file of concatenated
      // JSON objects that no parser accepts.
      Files.writeString(Paths.get(out), new GsonBuilder().setPrettyPrinting().create().toJson(peers));
      System.out.println("Wrote " + peers.size() + " device(s) to " + out);
      return 0;
    }
  }
}
