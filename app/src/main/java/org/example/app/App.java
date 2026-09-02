package org.example.app;

import org.example.app.cli.LansyncCommand;

import picocli.CommandLine;

/**
 * Entry point for the {@code lansync} command-line application.
 *
 * <p>Its only responsibility is to hand argument parsing and dispatch to picocli;
 * every operation lives in its own command class under
 * {@link org.example.app.cli}.
 *
 * @author Jamal
 */
public final class App {

  private App() {
    throw new AssertionError("No instances of App");
  }

  /**
   * Parses the arguments, runs the selected subcommand, and exits with its status.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new LansyncCommand()).execute(args);
    System.exit(exitCode);
  }
}
