package org.example.app.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.example.app.util.Config;
import org.example.app.util.NetworkPicker;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Shows or changes the stored settings, so users need not edit the JSON by hand.
 *
 * <p>With no options it prints the current settings, including the address the
 * bind specification currently resolves to.
 *
 * @author Jamal
 * @see InterfacesCommand
 */
@Command(name = "config", description = "Show or change settings in ~/.lansync/config.json")
public class ConfigCommand implements Callable<Integer> {

  @Option(names = { "-d", "--dir" }, description = "Set the sync directory")
  private String dir;

  @Option(names = { "-p", "--port" }, description = "Set the TCP listen port")
  private Integer port;

  @Option(names = "--bind", description = "Set the mDNS interface: an interface name, an IPv4 "
      + "address, 'auto', or 'any'")
  private String bind;

  @Option(names = "--name", description = "Set the display name other peers see")
  private String name;

  /** Creates the command. picocli instantiates it reflectively. */
  public ConfigCommand() {
  }

  /**
   * Applies any requested changes, then prints the resulting settings.
   *
   * @return {@code 0} on success, {@code 2} if an option value was rejected
   * @throws Exception if the config cannot be read or written
   */
  @Override
  public Integer call() throws Exception {
    Config c = Config.requireLoaded();
    boolean changed = false;

    if (dir != null) {
      Path root = Paths.get(dir).toAbsolutePath().normalize();
      Files.createDirectories(root);
      c.setSyncDir(root.toString());
      changed = true;
    }
    if (port != null) {
      if (port < 1 || port > 65535) {
        System.err.println("Port must be between 1 and 65535.");
        return 2;
      }
      c.setPort(port);
      changed = true;
    }
    if (name != null) {
      c.setHostname(name);
      changed = true;
    }
    if (bind != null) {
      String value = NetworkPicker.AUTO.equalsIgnoreCase(bind) ? null : bind;
      if (value != null) {
        // Fail now with the interface table rather than at the next `serve`.
        try {
          NetworkPicker.resolve(value);
        } catch (Exception e) {
          System.err.println(e.getMessage());
          return 2;
        }
      }
      c.setBindAddress(value);
      changed = true;
    }

    if (changed) {
      c.save();
      System.out.println("Saved.");
      System.out.println();
    }
    show(c);
    return 0;
  }

  /**
   * Prints the stored settings and how the bind specification resolves.
   *
   * @param c the config to display
   */
  private void show(Config c) {
    System.out.println("Device ID:    " + c.getDeviceId());
    System.out.println("Name:         " + c.getHostname());
    System.out.println("Sync dir:     " + c.getSyncDir());
    System.out.println("Port:         " + c.getPort());
    System.out.println("Bind address: " + (c.getBindAddress() == null ? "auto" : c.getBindAddress()));
    System.out.println("Last sync:    " + (c.getLastSyncAt() == null ? "never" : c.getLastSyncAt()));

    try {
      System.out.println("Resolves to:  " + NetworkPicker.resolve(c.getBindAddress()).getHostAddress()
          + "  (see `lansync interfaces`)");
    } catch (Exception e) {
      System.out.println("Resolves to:  UNRESOLVED -- " + e.getMessage());
    }
  }
}
