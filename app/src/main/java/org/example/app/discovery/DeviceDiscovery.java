package org.example.app.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class DeviceDiscovery {

  private JmDNS jmdns;
  private ServiceInfo serviceInfo;
  private List<ServiceInfo> devices = new ArrayList<>();
  private List<InetAddress> adresses = new ArrayList<>();

  public DeviceDiscovery() {
    super();
  }

  public List<ServiceInfo> getDevices() {
    return devices;
  }


  public void registering() {
    try {
//      InetAddress addr = NetworkInterface.getNetworkInterfaces().asIterator().next().getInetAddresses().nextElement();
//      InetAddress addr = InetAddress.getByName("192.168.5.0");
//      InetAddress addr = InetAddress.getLocalHost();

      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
      NetworkInterface ni = interfaces.nextElement();
      if (ni.isLoopback() || !ni.isUp()) continue;
      Enumeration<InetAddress> addresses = ni.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress addr = addresses.nextElement();
        if (addr instanceof Inet4Address) {
            jmdns = JmDNS.create(addr);
            serviceInfo = ServiceInfo.create("_lansync._tcp.local.", "Jamals PC", 5176, "Lansync Device");
            jmdns.registerService(serviceInfo);
            return;
        }
    }
}
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void discovering() {
    try {
      jmdns.addServiceListener("_lansync._tcp.local.", new ServiceListener() {
        public void serviceAdded(ServiceEvent event) {
          System.out.println("Service Found: " + event.getName());
          jmdns.requestServiceInfo(event.getType(), event.getName());

        }

        public void serviceRemoved(ServiceEvent event) {
        }

        public void serviceResolved(ServiceEvent event) {
          ServiceInfo info = event.getInfo();
          String ip = info.getHostAddresses()[0];
          int port = info.getPort();
          String name = info.getName();

          boolean alreadyExists = devices.stream().anyMatch(d -> d.getName().equals(name));
          if(!alreadyExists){
          devices.add(info);
          }
          System.out.println("Device: " + name + " Connected on port: " + port + " With ip: " + ip);
        }
      });
    } catch (NullPointerException e) {
      e.printStackTrace();
    }

  }

}
