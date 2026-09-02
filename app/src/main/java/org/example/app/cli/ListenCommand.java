package org.example.app.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.example.app.net.TransferServer;
import org.example.app.util.Config;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Receives a single unencrypted file, for debugging the transport in isolation.
 *
 * <p>Kept from an early milestone. Day-to-day use should prefer
 * {@link ServeCommand}, which authenticates and encrypts.
 *
 * @author Jamal
 * @see ServeCommand
 */
@Command(name = "listen", description = "Receive one file over a plain socket (debug)")
public class ListenCommand implements Callable<Integer> {

  @Option(names = { "-p", "--port" }, description = "Port to listen on")
  private Integer port;

  @Option(names = { "-o", "--out-dir" }, description = "Directory to write the file into",
      defaultValue = ".")
  private String outDir;

  /** Creates the command. picocli instantiates it reflectively. */
  public ListenCommand() {
  }

  /**
   * Listens for one connection and writes the file it receives.
   *
   * @return {@code 0} on success, {@code 1} if the transfer failed
   */
  @Override
  public Integer call() {
    int listenPort = port != null ? port : Config.DEFAULT_PORT;
    try {
      Path received = new TransferServer(listenPort, Paths.get(outDir)).receiveOnce();
      System.out.println("Received " + received.toAbsolutePath().normalize());
      return 0;
    } catch (Exception e) {
      System.err.println("listen failed: " + describe(e));
      return 1;
    }
  }

  private static String describe(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
