package org.example.app.cli;

import java.util.List;
import java.util.concurrent.Callable;

import org.example.app.util.Config;
import org.example.app.util.NetworkPicker;

import picocli.CommandLine.Command;

/**
 * Lists the network interfaces available for mDNS, marking the one in use.
 *
 * <p>This is the first thing to run when two devices on the same LAN cannot see
 * each other: it shows which interface was chosen and what else could be picked.
 *
 * @author Jamal
 * @see ConfigCommand
 */
@Command(name = "interfaces", description = "List network interfaces lansync can bind mDNS to")
public class InterfacesCommand implements Callable<Integer> {

  /** Creates the command. picocli instantiates it reflectively. */
  public InterfacesCommand() {
  }

  /**
   * Prints every bindable interface, flagging the configured or auto-selected one.
   *
   * @return {@code 0} on success, {@code 1} if no usable interface exists
   * @throws Exception if the interfaces cannot be enumerated
   */
  @Override
  public Integer call() throws Exception {
    List<NetworkPicker.Candidate> all = NetworkPicker.candidates();
    if (all.isEmpty()) {
      System.out.println("No usable IPv4 interfaces found.");
      return 1;
    }

    Config c = Config.load();
    String configured = c == null ? null : c.getBindAddress();
    NetworkPicker.Candidate best = NetworkPicker.best();

    System.out.printf("%-10s %-16s %-28s %s%n", "INTERFACE", "ADDRESS", "NOTES", "");
    for (NetworkPicker.Candidate cand : all) {
      String marker = "";
      if (configured != null
          && (configured.equalsIgnoreCase(cand.interfaceName())
              || configured.equals(cand.address().getHostAddress()))) {
        marker = "<- configured";
      } else if (configured == null && cand.equals(best)) {
        marker = "<- auto-selected";
      }
      System.out.printf("%-10s %-16s %-28s %s%n",
          cand.interfaceName(), cand.address().getHostAddress(), cand.describe(), marker);
    }

    System.out.println();
    if (configured == null) {
      System.out.println("Bind address: auto. Pin one with `lansync config --bind <interface|address>`");
      System.out.println("if peers on the same LAN cannot see each other.");
    } else {
      System.out.println("Bind address: " + configured + " (from ~/.lansync/config.json)");
      System.out.println("Return to automatic selection with `lansync config --bind auto`.");
    }
    return 0;
  }
}
