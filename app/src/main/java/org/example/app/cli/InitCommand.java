package org.example.app.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.example.app.crypto.KeyDerivation;
import org.example.app.util.Config;
import org.example.app.util.NetworkPicker;
import org.example.app.util.Passphrase;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Prepares this device for syncing: creates the sync directory, generates a
 * device ID, and confirms the shared passphrase.
 *
 * <p>The passphrase is confirmed but never stored. It is derived once here purely
 * to prove the entry works, then discarded; every later command asks for it again.
 *
 * @author Jamal
 */
@Command(name = "init", description = "Initialize lansync with a sync directory and passphrase")
public class InitCommand implements Callable<Integer> {

  @Option(names = { "-d", "--dir" }, required = true, description = "Directory to sync")
  private String dir;

  @Option(names = { "-p", "--port" }, description = "TCP port for the sync listener")
  private Integer port;

  @Option(names = "--bind", description = "Network interface or address for mDNS "
      + "(default: auto-select; see `lansync interfaces`)")
  private String bind;

  @Option(names = "--name", description = "Display name other peers see (default: this hostname)")
  private String name;

  /** Creates the command. picocli instantiates it reflectively. */
  public InitCommand() {
  }

  /**
   * Creates the config and sync directory, then confirms the passphrase.
   *
   * @return {@code 0} once the device is initialised
   * @throws Exception if the directory or config cannot be written, the bind
   *                   specification is invalid, or the passphrases do not match
   */
  @Override
  public Integer call() throws Exception {
    Path root = Paths.get(dir).toAbsolutePath().normalize();
    Files.createDirectories(root);

    Config c = Config.loadOrInit(root.toString());
    c.setSyncDir(root.toString());
    if (port != null) c.setPort(port);
    if (name != null) c.setHostname(name);
    if (bind != null) {
      String value = NetworkPicker.AUTO.equalsIgnoreCase(bind) ? null : bind;
      if (value != null) NetworkPicker.resolve(value); // fail early, with the interface table
      c.setBindAddress(value);
    }
    c.save();

    System.out.println("Device ID: " + c.getDeviceId());
    System.out.println("Name:      " + c.getHostname());
    System.out.println("Sync dir:  " + c.getSyncDir());
    System.out.println("Port:      " + c.getPort());
    System.out.println("mDNS on:   " + describeBind(c));
    System.out.println();
    System.out.println("Set the shared passphrase. Every peer you sync with must enter the same one.");
    System.out.println("It is never stored or transmitted -- you will be asked for it each session.");

    // Derived once here purely to confirm the entry works; the key is discarded
    // immediately and the passphrase itself is never persisted.
    char[] passphrase = Passphrase.promptWithConfirmation();
    byte[] probe = KeyDerivation.deriveKey(passphrase, KeyDerivation.pairSalt(c.getDeviceId(), c.getDeviceId()));
    Arrays.fill(probe, (byte) 0);

    System.out.println("Passphrase accepted. Run `lansync serve` on both devices to begin.");
    return 0;
  }

  private static String describeBind(Config c) {
    String configured = c.getBindAddress() == null ? "auto" : c.getBindAddress();
    try {
      return NetworkPicker.resolve(c.getBindAddress()).getHostAddress() + " (" + configured + ")";
    } catch (Exception e) {
      return configured + " -- unresolved: " + e.getMessage();
    }
  }
}
