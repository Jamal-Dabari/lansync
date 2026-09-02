package org.example.app.cli;

import java.io.File;
import java.util.concurrent.Callable;

import org.example.app.net.TransferClient;
import org.example.app.util.Config;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Sends a single unencrypted file, for debugging the transport in isolation.
 *
 * <p>Kept from an early milestone. Day-to-day use should prefer
 * {@link SyncCommand}, which authenticates, encrypts and transfers only deltas.
 *
 * @author Jamal
 * @see SyncCommand
 */
@Command(name = "send", description = "Send one file over a plain socket (debug)")
public class SendCommand implements Callable<Integer> {

  @Option(names = { "-f", "--file" }, required = true, description = "File to send")
  private File file;

  @Option(names = { "-p", "--port" }, description = "Port to connect to")
  private Integer port;

  @Parameters(index = "0", paramLabel = "<ip>", description = "Address of the listening peer")
  private String ip;

  /** Creates the command. picocli instantiates it reflectively. */
  public SendCommand() {
  }

  /**
   * Connects to the peer and sends the file.
   *
   * <p>The client is constructed here rather than in a field initializer: picocli
   * injects {@code ip} after building the command object, so a field initializer
   * would capture {@code null}.
   *
   * @return {@code 0} on success, {@code 1} if the file is unusable or the send failed
   */
  @Override
  public Integer call() {
    if (!file.isFile()) {
      System.err.println("Not a file: " + file);
      return 1;
    }
    System.out.println("Sending " + file.getName() + " to " + ip + "...");

    TransferClient client = new TransferClient(ip, port != null ? port : Config.DEFAULT_PORT);
    try {
      client.sendFile(file);
      System.out.println("Sent " + file.length() + " bytes.");
      return 0;
    } catch (Exception e) {
      System.err.println("send failed: " + describe(e));
      return 1;
    }
  }

  private static String describe(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
