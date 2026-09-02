package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * The {@code lansync} root command, which groups every subcommand.
 *
 * <p>The class holds no behaviour of its own beyond printing usage when invoked
 * without a subcommand; each operation lives in its own {@link Callable} command
 * class so that commands stay independently testable.
 *
 * @author Jamal
 */
@Command(
    name = "lansync",
    version = "0.0.1",
    mixinStandardHelpOptions = true,
    description = "Peer-to-peer encrypted file sync for your local network.",
    subcommands = {
        InitCommand.class,
        ServeCommand.class,
        ListPeersCommand.class,
        StatusCommand.class,
        WatchCommand.class,
        ListenCommand.class,
        SendCommand.class,
        DiscoverCommand.class
    })
public class LansyncCommand implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  /** Creates the command. picocli instantiates it reflectively. */
  public LansyncCommand() {
  }

  /**
   * Prints usage, because a bare {@code lansync} names no operation.
   *
   * @return {@link CommandLine.ExitCode#USAGE}
   */
  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return CommandLine.ExitCode.USAGE;
  }
}
