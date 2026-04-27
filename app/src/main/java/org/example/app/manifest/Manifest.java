package org.example.app.manifest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class Manifest {

  private int version = 1;
  private String generatedAt;
  private String deviceId;
  private Map<String, ManifestEntry> entries = new LinkedHashMap<>();

  public Manifest() {
    this.generatedAt = Instant.now().toString();
  }

  public int getVersion() { return version; }
  public void setVersion(int version) { this.version = version; }

  public String getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

  public Map<String, ManifestEntry> getEntries() { return entries; }
  public void setEntries(Map<String, ManifestEntry> entries) { this.entries = entries; }
}
