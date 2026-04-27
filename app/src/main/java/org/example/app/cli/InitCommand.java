package org.example.app.cli;

import java.util.concurrent.Callable;

import org.example.app.util.Config;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "init", description = "Initialize lansync with a sync directory and passphrase")
public class InitCommand implements Callable<Integer> {

  @Option(names = { "-d", "--dir" }, required = true, description = "Directory to sync")
  private String dir;

  @Override
  public Integer call() throws Exception {
    Config c = Config.loadOrInit(dir);
    c.setSyncDir(dir);
    c.save();
    // TODO: prompt for passphrase via System.console().readPassword() and
    //       verify by deriving + confirming it matches on re-entry.
    //       Do NOT persist the passphrase.
    System.out.println("Device ID: " + c.getDeviceId());
    System.out.println("Sync dir:  " + c.getSyncDir());
    System.out.println("Set a passphrase; it will be requested per-session.");
    return 0;
  }
}
