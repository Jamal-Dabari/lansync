package org.example.app.util;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This device's persisted settings, stored as JSON at
 * {@code ~/.lansync/config.json}.
 *
 * <p>Deliberately excludes the passphrase, which is never written to disk; see
 * {@link Passphrase}.
 *
 * <p>This is a JSON-serialized value type, so it keeps a no-argument constructor
 * and setters for Gson.
 *
 * @author Jamal
 */
public class Config {

  /** The port used when none has been configured. */
  public static final int DEFAULT_PORT = 5176;

  private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".lansync");
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

  private String deviceId;
  private String hostname;
  private String syncDir;
  private int port = DEFAULT_PORT;
  private String bindAddress;
  private String lastSyncAt;

  /** Creates a config with default values, as required by Gson. */
  public Config() {
  }

  /**
   * Loads the stored config.
   *
   * @return the config, or {@code null} if this device has not been initialised
   * @throws IOException if the file exists but cannot be read
   */
  public static Config load() throws IOException {
    if (!Files.exists(CONFIG_FILE)) {
      return null;
    }
    return new Gson().fromJson(Files.readString(CONFIG_FILE), Config.class);
  }

  /**
   * Loads the stored config, failing with an actionable message when absent.
   *
   * @return the config
   * @throws IOException if this device has not been initialised, or the file cannot be read
   */
  public static Config requireLoaded() throws IOException {
    Config config = load();
    if (config == null) {
      throw new IOException("Not initialized. Run `lansync init --dir <path>` first.");
    }
    return config;
  }

  /**
   * Loads the stored config, creating one with a fresh device ID if none exists.
   *
   * @param syncDir the directory to sync when creating a new config
   * @return the existing or newly created config
   * @throws IOException if the config cannot be read or written
   */
  public static Config loadOrInit(String syncDir) throws IOException {
    Config existing = load();
    if (existing != null) {
      return existing;
    }

    Config created = new Config();
    // A random UUID, so device identity survives hostname and address changes.
    created.deviceId = UUID.randomUUID().toString();
    created.hostname = InetAddress.getLocalHost().getHostName();
    created.syncDir = syncDir;
    created.save();
    return created;
  }

  /**
   * Writes this config to disk, creating the config directory if needed.
   *
   * @throws IOException if the file cannot be written
   */
  public void save() throws IOException {
    Files.createDirectories(CONFIG_DIR);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Files.writeString(CONFIG_FILE, gson.toJson(this));
  }

  /**
   * Records the current time as the last successful sync and persists it.
   *
   * @throws IOException if the file cannot be written
   */
  public void touchLastSync() throws IOException {
    this.lastSyncAt = Instant.now().toString();
    save();
  }

  /**
   * Returns the sync directory, creating it if it does not exist.
   *
   * @return the sync root
   * @throws IOException if no directory is configured, or it cannot be created
   */
  public Path syncRoot() throws IOException {
    if (syncDir == null) {
      throw new IOException("No sync directory configured. Run `lansync init --dir <path>`.");
    }
    Path root = Paths.get(syncDir);
    Files.createDirectories(root);
    return root;
  }

  /**
   * Returns this device's stable identifier.
   *
   * @return the device ID
   */
  public String getDeviceId() {
    return deviceId;
  }

  /**
   * Sets this device's identifier.
   *
   * @param deviceId the device ID
   */
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
   * Returns the display name advertised to peers.
   *
   * @return the hostname or user-chosen name
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Sets the display name advertised to peers.
   *
   * @param hostname the name
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Returns the configured sync directory.
   *
   * @return the directory path as text, or {@code null} if unset
   */
  public String getSyncDir() {
    return syncDir;
  }

  /**
   * Sets the sync directory.
   *
   * @param syncDir the directory path
   */
  public void setSyncDir(String syncDir) {
    this.syncDir = syncDir;
  }

  /**
   * Returns the TCP port the sync listener uses.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the TCP port the sync listener uses.
   *
   * @param port the port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Returns the interface mDNS should bind to.
   *
   * @return an interface name, IPv4 literal or {@code "any"}; {@code null} means
   *         auto-select
   * @see NetworkPicker#resolve(String)
   */
  public String getBindAddress() {
    return bindAddress;
  }

  /**
   * Sets the interface mDNS should bind to.
   *
   * @param bindAddress an interface name, IPv4 literal or {@code "any"};
   *                    {@code null} to auto-select
   */
  public void setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
  }

  /**
   * Returns when this device last completed a sync.
   *
   * @return an ISO-8601 timestamp, or {@code null} if it has never synced
   */
  public String getLastSyncAt() {
    return lastSyncAt;
  }

  /**
   * Sets when this device last completed a sync.
   *
   * @param lastSyncAt an ISO-8601 timestamp
   */
  public void setLastSyncAt(String lastSyncAt) {
    this.lastSyncAt = lastSyncAt;
  }
}
