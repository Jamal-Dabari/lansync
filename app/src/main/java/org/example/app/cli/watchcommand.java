package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "watch", description = "watching")
public class watchcommand implements Callable<Integer> {

  @Override
  public Integer call() {

    System.out.println("Watching");

    return 0;
  }

}
