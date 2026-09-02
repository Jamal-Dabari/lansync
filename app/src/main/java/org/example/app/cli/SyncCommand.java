package org.example.app.cli;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.example.app.discovery.Device;
import org.example.app.discovery.DeviceDiscovery;
import org.example.app.sync.SyncSession;
import org.example.app.util.Config;
import org.example.app.util.Passphrase;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Connects to a peer and reconciles the two sync directories.
 *
 * <p>Only differing files cross the wire: the peers exchange manifests, diff them
 * by content digest, and transfer just the gaps.
 *
 * @author Jamal
 * @see ServeCommand
 */
@Command(name = "sync", description = "Perform a delta sync with a peer")
public class SyncCommand implements Callable<Integer> {

  @Parameters(index = "0", paramLabel = "<peer>", description = "Device ID, ID prefix, hostname, or IP address")
  private String peer;

  @Option(names = "--dry-run", description = "Show what would transfer without moving any data")
  private boolean dryRun;

  @Option(names = "--mirror", description = "Delete remote files that are missing locally")
  private boolean mirror;

  @Option(names = { "-t", "--timeout" }, description = "Discovery window in seconds", defaultValue = "5")
  private int timeoutSeconds;

  @Option(names = { "-p", "--port" }, description = "Port to connect to when addressing a peer directly")
  private Integer port;

  @Option(names = "--bind", description = "Network interface or address for mDNS "
      + "(overrides the configured value; see `lansync interfaces`)")
  private String bind;

  /** Creates the command. picocli instantiates it reflectively. */
  public SyncCommand() {
  }

  /**
   * Resolves the peer, authenticates, and performs the sync.
   *
   * @return {@code 0} on success, {@code 1} if the peer could not be found
   * @throws Exception if the config is missing, authentication fails, or the
   *                   transfer breaks down
   */
  @Override
  public Integer call() throws Exception {
    Config c = Config.requireLoaded();
    Path root = c.syncRoot();

    Device target = resolve();
    if (target == null) {
      System.err.println("No peer matching '" + peer + "' found on the LAN.");
      System.err.println("Run `lansync list-peers` to see what is reachable, or pass an IP address directly.");
      return 1;
    }
    System.out.println("Syncing with " + target);

    char[] passphrase = Passphrase.prompt("Passphrase");

    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(target.getIp(), target.getPort()), 10_000);
      SyncSession session = new SyncSession(SyncSession.Role.CLIENT, socket, root, passphrase, c.getDeviceId());
      session.setDryRun(dryRun);
      session.setMirror(mirror);
      session.run();
    }

    if (!dryRun) c.touchLastSync();
    return 0;
  }

  /**
   * Resolves the peer token to an address.
   *
   * <p>A literal address skips discovery entirely, which is the dependable path
   * on networks that filter multicast.
   *
   * @return the peer to connect to, or {@code null} if discovery found no match
   * @throws Exception if discovery fails
   */
  private Device resolve() throws Exception {
    if (looksLikeAddress(peer)) {
      return new Device(peer, peer, null, port != null ? port : Config.DEFAULT_PORT);
    }
    try (DeviceDiscovery discovery = new DeviceDiscovery(bind)) {
      Device found = discovery.findPeer(peer, timeoutSeconds);
      if (found != null && port != null) found.setPort(port);
      return found;
    }
  }

  /**
   * Reports whether a token is an address rather than a name to look up.
   *
   * @param token the peer token typed on the command line
   * @return {@code true} for an IPv4 literal or anything containing a colon
   */
  private static boolean looksLikeAddress(String token) {
    return token.matches("\\d{1,3}(\\.\\d{1,3}){3}") || token.contains(":");
  }
}
