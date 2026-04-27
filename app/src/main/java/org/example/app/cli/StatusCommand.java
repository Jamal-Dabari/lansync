package org.example.app.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.example.app.manifest.Manifest;
import org.example.app.manifest.ManifestBuilder;
import org.example.app.util.Config;

import picocli.CommandLine.Command;

@Command(name = "status", description = "Show last sync time and local manifest stats")
public class StatusCommand implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    Config c = Config.load();
    if (c == null) {
      System.out.println("Not initialized. Run `lansync init --dir <path>` first.");
      return 1;
    }

    System.out.println("Device ID:   " + c.getDeviceId());
    System.out.println("Sync dir:    " + c.getSyncDir());
    System.out.println("Last sync:   " + (c.getLastSyncAt() == null ? "never" : c.getLastSyncAt()));

    Path root = Paths.get(c.getSyncDir());
    if (!Files.isDirectory(root)) {
      System.out.println("Sync dir missing on disk.");
      return 2;
    }
    Manifest m = ManifestBuilder.build(root, c.getDeviceId());
    long totalBytes = m.getEntries().values().stream().mapToLong(e -> e.getSize()).sum();
    System.out.println("Files:       " + m.getEntries().size());
    System.out.println("Total bytes: " + totalBytes);
    return 0;
  }
}
