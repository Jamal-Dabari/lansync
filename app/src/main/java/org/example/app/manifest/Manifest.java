package org.example.app.manifest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A snapshot of every file in a sync directory, keyed by path relative to the
 * sync root.
 *
 * <p>Peers exchange manifests and diff them to decide what needs to move, so a
 * whole directory tree is compared in one round trip rather than file by file.
 * A {@link LinkedHashMap} preserves walk order, keeping the JSON stable and
 * diffs readable.
 *
 * <p>This is a JSON-serialized value type, so it keeps a no-argument constructor
 * and setters for Gson.
 *
 * @author Jamal
 * @see ManifestBuilder
 * @see Differ
 */
public class Manifest {

  private int version = 1;
  private String generatedAt;
  private String deviceId;
  private Map<String, ManifestEntry> entries = new LinkedHashMap<>();

  /** Creates an empty manifest stamped with the current time. */
  public Manifest() {
    this.generatedAt = Instant.now().toString();
  }

  /**
   * Returns the manifest format version.
   *
   * @return the format version
   */
  public int getVersion() {
    return version;
  }

  /**
   * Sets the manifest format version.
   *
   * @param version the format version
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * Returns when this manifest was built.
   *
   * @return an ISO-8601 timestamp
   */
  public String getGeneratedAt() {
    return generatedAt;
  }

  /**
   * Sets when this manifest was built.
   *
   * @param generatedAt an ISO-8601 timestamp
   */
  public void setGeneratedAt(String generatedAt) {
    this.generatedAt = generatedAt;
  }

  /**
   * Returns the device this manifest describes.
   *
   * @return the device ID
   */
  public String getDeviceId() {
    return deviceId;
  }

  /**
   * Sets the device this manifest describes.
   *
   * @param deviceId the device ID
   */
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
   * Returns the entries, keyed by path relative to the sync root using
   * {@code /} separators.
   *
   * @return the live entry map, never {@code null}
   */
  public Map<String, ManifestEntry> getEntries() {
    return entries;
  }

  /**
   * Replaces the entries.
   *
   * @param entries the new entry map
   */
  public void setEntries(Map<String, ManifestEntry> entries) {
    this.entries = entries;
  }
}
