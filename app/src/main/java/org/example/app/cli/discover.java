package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "discover")
public class discover implements Callable<Integer> {

  @Override
  public Integer call() {

    System.out.println("discovering... ");

    return 0;
  }
}
