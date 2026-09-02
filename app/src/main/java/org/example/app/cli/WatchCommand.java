package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

/**
 * Placeholder for continuous sync driven by a filesystem watcher.
 *
 * <p>Not yet implemented. The intended design is a
 * {@link java.nio.file.WatchService} over the sync root that debounces change
 * events and triggers a {@link SyncCommand} run per peer.
 *
 * @author Jamal
 */
@Command(name = "watch", description = "Continuously sync on file changes (not yet implemented)")
public class WatchCommand implements Callable<Integer> {

  /** Creates the command. picocli instantiates it reflectively. */
  public WatchCommand() {
  }

  /**
   * Reports that the command is not implemented.
   *
   * @return {@code 1}, since no work was performed
   */
  @Override
  public Integer call() {
    System.out.println("watch: not yet implemented.");
    System.out.println("Run `lansync sync <peer>` manually, or see the roadmap in README.md.");
    return 1;
  }
}
