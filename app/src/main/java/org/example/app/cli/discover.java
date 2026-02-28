package org.example.app.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.List;

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
  private Gson gson = new Gson();
  private String bar = "[";
  private String bar2 = "]";
  private String comma = ",";

  @Override
  public Integer call() throws IOException {

    System.out.println("discovering... ");
    FileOutputStream ot = new FileOutputStream("devices.json", true);
    ds = new DeviceDiscovery();
    ds.registering();
    ds.discovering();

    try {
      Thread.sleep(5000);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try{
      ot.write(bar.getBytes());
      List<ServiceInfo> devices = ds.getDevices();

      try {
        for (int i = 0; i < ds.getDevices().size(); i++) {
         Device device = new Device();
         device.setName(devices.get(i).getName());
         device.setIp(devices.get(i).getHostAddresses()[0]);
         String json = gson.toJson(device);
         ot.write(json.getBytes());
         if(i < devices.size() -1){
          ot.write(comma.getBytes());
          }
        }
        ot.write(bar2.getBytes());
        if (ds.getDevices().isEmpty()) {
         System.out.println("No devices found");
       }
      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (IOException e){
      e.printStackTrace();
    } finally {
      ot.close();
    }
    return 0;
  }
}
