package org.example.app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {

  private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".lansync");
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

  private String deviceId;
  private String hostname;
  private String syncDir;
  private int port = 5176;
  private String lastSyncAt;

  public static Config load() throws IOException {
    if (!Files.exists(CONFIG_FILE)) return null;
    return new Gson().fromJson(Files.readString(CONFIG_FILE), Config.class);
  }

  public static Config loadOrInit(String syncDir) throws IOException {
    Config c = load();
    if (c != null) return c;
    c = new Config();
    c.deviceId = UUID.randomUUID().toString();
    c.hostname = java.net.InetAddress.getLocalHost().getHostName();
    c.syncDir = syncDir;
    c.save();
    return c;
  }

  public void save() throws IOException {
    Files.createDirectories(CONFIG_DIR);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Files.writeString(CONFIG_FILE, gson.toJson(this));
  }

  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

  public String getHostname() { return hostname; }
  public void setHostname(String hostname) { this.hostname = hostname; }

  public String getSyncDir() { return syncDir; }
  public void setSyncDir(String syncDir) { this.syncDir = syncDir; }

  public int getPort() { return port; }
  public void setPort(int port) { this.port = port; }

  public String getLastSyncAt() { return lastSyncAt; }
  public void setLastSyncAt(String lastSyncAt) { this.lastSyncAt = lastSyncAt; }
}
