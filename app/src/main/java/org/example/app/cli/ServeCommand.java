package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "serve", description = "Start the sync listener daemon (foreground)")
public class ServeCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    // TODO:
    //  1. Config.load() — abort if not initialized.
    //  2. Register mDNS service (DeviceDiscovery.registering()).
    //  3. Open ServerSocket on Config.port.
    //  4. Accept loop -> per-connection thread -> new SyncSession(SERVER, socket, ...).run()
    System.out.println("serve: not yet implemented");
    return 0;
  }
}
