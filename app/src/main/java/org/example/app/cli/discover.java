package org.example.app.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "discover", description = "Discover connections across a network")
public class discover implements Callable<Integer> {

  @Option(names = "-s", description = "Specify an Ip Adress to look for")
  String specify;

  @Override
  public Integer call() {

    System.out.println("discovering... ");

    return 0;
  }
}
