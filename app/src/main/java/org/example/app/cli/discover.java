package org.example.app.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import javax.jmdns.ServiceInfo;

import com.google.gson.Gson;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.example.app.discovery.*;
import org.example.app.net.Messageprotocal;

@Command(name = "discover", description = "Discover connections across a network")
public class discover implements Callable<Integer> {
  private DeviceDiscovery ds;
  private Device device;
  private FileOutputStream out;

  // @Option(names = "-s", description = "Specify an Ip Adress to look for",)
  // String specify;

  @Override
  public Integer call() {

    System.out.println("discovering... ");
    ds = new DeviceDiscovery();
    ds.registering();
    ds.discovering();

    try {
      Thread.sleep(5000);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    for (ServiceInfo info : ds.getDevices()) {
      System.out.println("Device: " + info);
      Device device = new Device();
      device.setName(info.getName());
      device.setIp(info.getHostAddresses()[0]);
      System.out.println("Name: " + info.getName());
      System.out.println("IP: " + info.getHostAddresses()[0]);
      System.out.println("device found: " + device);
      Gson gson = new Gson();

      Device test = new Device("test", "192.168.1.1");
      System.out.println(gson.toJson(test));

      System.out.println("About to serialize - name: " + device.getName() + " ip: " + device.getIp());
      String json = gson.toJson(device);
      try (FileOutputStream ot = new FileOutputStream("devices.json", true)) {
        ot.write(json.getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    if (ds.getDevices().isEmpty()) {
      System.out.println("No devices found");
    }

    return 0;
  }
}
