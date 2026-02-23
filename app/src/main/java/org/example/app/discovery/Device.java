package org.example.app.discovery;

public class Device {

  private String name;
  private String ip;

  public Device(String name, String ip) {
    this.name = name;
    this.ip = ip;
  }

  public Device() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

}
