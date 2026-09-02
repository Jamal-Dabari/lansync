package org.example.app.cli;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.example.app.discovery.DeviceDiscovery;
import org.example.app.sync.SyncSession;
import org.example.app.util.Config;
import org.example.app.util.Passphrase;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Runs the sync listener in the foreground, accepting inbound sessions until
 * interrupted.
 *
 * <p>Each connection is handled on its own virtual thread, so a slow transfer
 * cannot block other peers. mDNS advertising is best-effort: if registration
 * fails the listener keeps running, since peers can still connect by address.
 *
 * @author Jamal
 * @see SyncCommand
 */
@Command(name = "serve", description = "Start the sync listener daemon (foreground)")
public class ServeCommand implements Callable<Integer> {

  @Option(names = { "-p", "--port" }, description = "Override the configured listen port")
  private Integer port;

  @Option(names = "--no-mdns", description = "Skip mDNS registration (listen only)")
  private boolean noMdns;

  @Option(names = "--bind", description = "Network interface or address for mDNS "
      + "(overrides the configured value; see `lansync interfaces`)")
  private String bind;

  /** Creates the command. picocli instantiates it reflectively. */
  public ServeCommand() {
  }

  /**
   * Advertises this device and serves inbound sync sessions until interrupted.
   *
   * @return {@code 0} when the listener is closed
   * @throws Exception if the config is missing, the passphrase cannot be read,
   *                   or the port cannot be bound
   */
  @Override
  public Integer call() throws Exception {
    Config c = Config.requireLoaded();
    Path root = c.syncRoot();
    int listenPort = port != null ? port : c.getPort();

    char[] passphrase = Passphrase.prompt("Passphrase");

    DeviceDiscovery discovery = null;
    if (!noMdns) {
      discovery = new DeviceDiscovery(bind != null ? bind : c.getBindAddress());
      try {
        discovery.register(c.getDeviceId(), c.getHostname(), listenPort);
      } catch (java.io.IOException e) {
        System.err.println("mDNS registration failed: " + e.getMessage());
        System.err.println("Peers will not discover this device automatically. "
            + "They can still reach it directly at <ip>:" + listenPort + ".");
        discovery = null;
      }
      if (discovery != null) {
        System.out.println("Advertising " + c.getHostname() + " on " + DeviceDiscovery.SERVICE_TYPE
            + " via " + discovery.getBoundAddress().getHostAddress());
      }
    }

    try (ServerSocket server = new ServerSocket(listenPort)) {
      final DeviceDiscovery d = discovery;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (d != null) d.close();
        try { server.close(); } catch (Exception ignored) {}
      }));

      System.out.println("Device ID: " + c.getDeviceId());
      System.out.println("Sync dir:  " + root);
      System.out.println("Listening on all interfaces, port " + listenPort + ". Ctrl-C to stop.");

      while (!server.isClosed()) {
        Socket socket = server.accept();
        // Each session consumes (and zeroes) its own copy of the passphrase.
        char[] sessionPass = passphrase.clone();
        Thread.ofVirtual().start(() -> handle(socket, root, sessionPass, c.getDeviceId()));
      }
    }
    return 0;
  }

  /**
   * Runs one inbound session to completion, reporting failures without ending
   * the daemon.
   *
   * @param socket     the accepted connection
   * @param root       the sync directory
   * @param passphrase this session's own copy of the passphrase, zeroed when it ends
   * @param deviceId   this device's ID
   */
  private void handle(Socket socket, Path root, char[] passphrase, String deviceId) {
    String peer = socket.getInetAddress().getHostAddress();
    System.out.println("Session opened from " + peer);
    SyncSession session = new SyncSession(SyncSession.Role.SERVER, socket, root, passphrase, deviceId);
    try {
      session.run();
      System.out.println("Session with " + peer + " complete.");
    } catch (Exception e) {
      System.err.println("Session with " + peer + " failed: " + describe(e));
      session.close();
    }
  }

  /** Some failures (a port probe that hangs up mid-handshake) carry no message. */
  private static String describe(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
