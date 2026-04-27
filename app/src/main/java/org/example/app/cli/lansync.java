package org.example.app.cli;

import picocli.CommandLine.Command;

@Command(name = "lansync", version = "0.0.1", subcommands = {
    listen.class,
    send.class,
    discover.class,
    watchcommand.class,
    InitCommand.class,
    ServeCommand.class,
    ListPeersCommand.class,
    StatusCommand.class
})
public class lansync {

}
