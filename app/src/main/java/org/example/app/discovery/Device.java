package org.example.app.discovery;

import java.util.Objects;

/**
 * A peer discovered on the local network, or one addressed directly by IP.
 *
 * <p>This is a JSON-serialized value type, so it keeps a no-argument constructor
 * and setters for Gson.
 *
 * @author Jamal
 * @see DeviceDiscovery
 */
public class Device {

  /** Shortest device-ID prefix {@link #matches(String)} accepts, to avoid ambiguity. */
  private static final int MIN_PREFIX_LENGTH = 4;

  private static final int SHORT_ID_LENGTH = 8;

  private String name;
  private String ip;
  private String deviceId;
  private int port;

  /** Required by Gson for deserialization. */
  public Device() {
  }

  /**
   * Creates a device known only by name and address.
   *
   * @param name the display name
   * @param ip   the IPv4 address
   */
  public Device(String name, String ip) {
    this.name = name;
    this.ip = ip;
  }

  /**
   * Creates a fully identified device.
   *
   * @param name     the display name
   * @param ip       the IPv4 address
   * @param deviceId the peer's device ID, or {@code null} when addressed directly by IP
   * @param port     the peer's TCP port
   */
  public Device(String name, String ip, String deviceId, int port) {
    this.name = name;
    this.ip = ip;
    this.deviceId = deviceId;
    this.port = port;
  }

  /**
   * Returns the display name.
   *
   * @return the name, or {@code null} if unknown
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the display name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the address.
   *
   * @return the IPv4 address, or {@code null} if unresolved
   */
  public String getIp() {
    return ip;
  }

  /**
   * Sets the address.
   *
   * @param ip the IPv4 address
   */
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
   * Returns the device ID.
   *
   * @return the device ID, or {@code null} when the peer was addressed directly by IP
   */
  public String getDeviceId() {
    return deviceId;
  }

  /**
   * Sets the device ID.
   *
   * @param deviceId the device ID
   */
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
   * Returns the TCP port.
   *
   * @return the port the peer listens on
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the TCP port.
   *
   * @param port the port the peer listens on
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Reports whether a user-supplied token names this device.
   *
   * <p>Accepts a full device ID, a device-ID prefix of at least
   * {@value #MIN_PREFIX_LENGTH} characters, the hostname, or the IP address, so
   * that users need not paste a whole UUID.
   *
   * @param token the token typed on the command line
   * @return {@code true} if this device is the one named
   */
  public boolean matches(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    return token.equals(deviceId)
        || (deviceId != null && token.length() >= MIN_PREFIX_LENGTH && deviceId.startsWith(token))
        || token.equalsIgnoreCase(name)
        || token.equals(ip);
  }

  /**
   * Returns an abbreviated device ID for table output.
   *
   * @return the first {@value #SHORT_ID_LENGTH} characters of the device ID,
   *         or {@code "?"} if it is unknown
   */
  public String shortId() {
    if (deviceId == null) {
      return "?";
    }
    return deviceId.length() > SHORT_ID_LENGTH ? deviceId.substring(0, SHORT_ID_LENGTH) : deviceId;
  }

  /**
   * Returns a human-readable description for CLI output.
   *
   * @return the name, short ID and address, or just the address when the device
   *         was addressed directly by IP
   */
  @Override
  public String toString() {
    String address = ip + ":" + port;
    return deviceId == null ? address : name + " (" + shortId() + ") " + address;
  }

  /**
   * Compares devices by identity: device ID when known, address and port otherwise.
   *
   * @param other the object to compare with
   * @return {@code true} if both refer to the same peer
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Device device)) {
      return false;
    }
    if (deviceId != null || device.deviceId != null) {
      return Objects.equals(deviceId, device.deviceId);
    }
    return port == device.port && Objects.equals(ip, device.ip);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return deviceId != null ? deviceId.hashCode() : Objects.hash(ip, port);
  }
}
