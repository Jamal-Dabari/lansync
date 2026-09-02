package org.example.app.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.example.app.util.Config;
import org.example.app.util.NetworkPicker;

/**
 * Advertises this device and finds peers over mDNS, so that syncing needs no
 * manually configured addresses.
 *
 * <p>An instance owns one {@link JmDNS} handle bound to a single interface and
 * must be {@link #close() closed} to withdraw the advertisement; it is designed
 * for use in a try-with-resources block.
 *
 * <p>The discovered-service list is synchronised because jmDNS delivers
 * resolution callbacks on its own threads while the caller reads the list.
 *
 * @author Jamal
 * @see NetworkPicker
 */
public class DeviceDiscovery implements Closeable {

  /** The DNS-SD service type lansync advertises and browses for. */
  public static final String SERVICE_TYPE = "_lansync._tcp.local.";

  private static final String PROP_DEVICE_ID = "deviceId";
  private static final String PROP_HOSTNAME = "hostname";
  private static final int MILLIS_PER_SECOND = 1000;

  private final List<ServiceInfo> devices = Collections.synchronizedList(new ArrayList<>());

  private JmDNS jmdns;
  private ServiceInfo serviceInfo;
  private String localDeviceId;
  private String bindSpec;
  private InetAddress boundTo;

  /** Creates a discovery handle that binds to the configured or auto-selected interface. */
  public DeviceDiscovery() {
  }

  /**
   * Creates a discovery handle bound to a specific interface.
   *
   * @param bindSpec an interface name, an IPv4 literal, {@code "auto"} or
   *                 {@code "any"}; {@code null} falls back to the configured
   *                 value and then to auto-selection
   */
  public DeviceDiscovery(String bindSpec) {
    this.bindSpec = bindSpec;
  }

  /**
   * Returns the interface address mDNS bound to.
   *
   * @return the bound address, or {@code null} before {@link #register} has run
   */
  public InetAddress getBoundAddress() {
    return boundTo;
  }

  /**
   * Returns the services resolved so far.
   *
   * @return a snapshot copy, safe to iterate while discovery continues
   */
  public List<ServiceInfo> getDevices() {
    synchronized (devices) {
      return new ArrayList<>(devices);
    }
  }

  /**
   * Advertises this device using the identity stored in the config file.
   *
   * @throws IOException if the config cannot be read or mDNS cannot bind
   */
  public void registering() throws IOException {
    Config config = Config.load();
    if (config == null) {
      register(null, InetAddress.getLocalHost().getHostName(), Config.DEFAULT_PORT);
      return;
    }
    if (bindSpec == null) {
      bindSpec = config.getBindAddress();
    }
    register(config.getDeviceId(), config.getHostname(), config.getPort());
  }

  /**
   * Advertises this device under an explicit identity.
   *
   * <p>The device ID travels in a TXT record so peers can be addressed by ID
   * rather than by an address that may change between sessions.
   *
   * @param deviceId this device's ID, or {@code null} if not yet initialised
   * @param hostname the display name peers will see
   * @param port     the TCP port the sync listener accepts on
   * @throws IOException if the bind specification names no local interface, or
   *                     mDNS cannot bind to it
   */
  public void register(String deviceId, String hostname, int port) throws IOException {
    this.localDeviceId = deviceId;
    this.boundTo = NetworkPicker.resolve(bindSpec);
    this.jmdns = JmDNS.create(boundTo);

    Map<String, String> properties = new HashMap<>();
    if (deviceId != null) {
      properties.put(PROP_DEVICE_ID, deviceId);
    }
    if (hostname != null) {
      properties.put(PROP_HOSTNAME, hostname);
    }

    serviceInfo = ServiceInfo.create(SERVICE_TYPE,
        hostname != null ? hostname : "lansync", port, 0, 0, properties);
    jmdns.registerService(serviceInfo);
  }

  /**
   * Starts listening for peer advertisements. Resolution is asynchronous, so
   * callers must allow a discovery window before reading {@link #getDevices()}.
   *
   * @throws IllegalStateException if called before {@link #register}
   */
  public void discovering() {
    if (jmdns == null) {
      throw new IllegalStateException("register() must run before discovering()");
    }
    jmdns.addServiceListener(SERVICE_TYPE, new PeerListener());
  }

  /**
   * Runs a discovery window and returns the peers found, excluding this device.
   *
   * @param timeoutSeconds how long to listen before returning
   * @return the peers discovered, possibly empty
   * @throws IOException          if registration or binding fails
   * @throws InterruptedException if the wait is interrupted
   */
  public List<Device> findPeers(int timeoutSeconds) throws IOException, InterruptedException {
    registering();
    discovering();
    Thread.sleep((long) timeoutSeconds * MILLIS_PER_SECOND);

    List<Device> peers = new ArrayList<>();
    for (ServiceInfo info : getDevices()) {
      Device device = toDevice(info);
      // Skip our own advertisement, which we receive along with everyone else's.
      if (localDeviceId != null && localDeviceId.equals(device.getDeviceId())) {
        continue;
      }
      peers.add(device);
    }
    return peers;
  }

  /**
   * Resolves a peer token to an addressable device.
   *
   * @param token          a device ID, ID prefix, hostname or IP address
   * @param timeoutSeconds how long to listen before giving up
   * @return the matching peer, or {@code null} if none was found
   * @throws IOException          if registration or binding fails
   * @throws InterruptedException if the wait is interrupted
   * @see Device#matches(String)
   */
  public Device findPeer(String token, int timeoutSeconds) throws IOException, InterruptedException {
    for (Device peer : findPeers(timeoutSeconds)) {
      if (peer.matches(token)) {
        return peer;
      }
    }
    return null;
  }

  /**
   * Converts a resolved mDNS service record into a {@link Device}.
   *
   * @param info the resolved service record
   * @return the device it describes
   */
  public static Device toDevice(ServiceInfo info) {
    Device device = new Device();
    device.setName(info.getName());
    device.setIp(info.getHostAddresses().length > 0 ? info.getHostAddresses()[0] : null);
    device.setPort(info.getPort());
    device.setDeviceId(info.getPropertyString(PROP_DEVICE_ID));

    String advertisedHostname = info.getPropertyString(PROP_HOSTNAME);
    if (advertisedHostname != null) {
      device.setName(advertisedHostname);
    }
    return device;
  }

  /**
   * Withdraws the advertisement and releases the mDNS handle. Safe to call more
   * than once.
   */
  @Override
  public void close() {
    if (jmdns == null) {
      return;
    }
    try {
      jmdns.unregisterAllServices();
      jmdns.close();
    } catch (IOException ignored) {
      // Shutting down regardless; nothing useful to report.
    } finally {
      jmdns = null;
      serviceInfo = null;
    }
  }

  /** Keeps {@link #devices} in step with what jmDNS resolves, one entry per service name. */
  private final class PeerListener implements ServiceListener {

    @Override
    public void serviceAdded(ServiceEvent event) {
      // Announcements carry only a name; ask for the address and TXT records.
      jmdns.requestServiceInfo(event.getType(), event.getName());
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
      synchronized (devices) {
        devices.removeIf(known -> known.getName().equals(event.getName()));
      }
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
      ServiceInfo info = event.getInfo();
      if (info.getHostAddresses().length == 0) {
        return;
      }
      synchronized (devices) {
        // Replace rather than append: a peer may re-announce during the window.
        devices.removeIf(known -> known.getName().equals(info.getName()));
        devices.add(info);
      }
    }
  }
}
