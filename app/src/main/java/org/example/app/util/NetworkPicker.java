package org.example.app.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Chooses which network interface mDNS binds to.
 *
 * A host usually has several: the real LAN adapter, plus VPN tunnels, container
 * bridges and (on macOS) Apple's awdl/llw peer-to-peer links. Multicast sent on
 * the wrong one never reaches the LAN, so this both guesses well by default and
 * lets the user name an interface explicitly when the guess is wrong.
 *
 * @author Jamal
 */
public final class NetworkPicker {

  /** Headless override, mirroring {@link Passphrase#ENV_VAR}. */
  public static final String ENV_VAR = "LANSYNC_BIND";

  /** Bind specification meaning "choose the most likely LAN interface". */
  public static final String AUTO = "auto";

  /** Bind specification meaning "bind the wildcard address on every interface". */
  public static final String ANY = "any";

  /**
   * Interface-name prefixes that carry an IPv4 address but are not the LAN.
   * These are ranked last rather than excluded, so a user on an unusual setup
   * can still select one by name.
   */
  private static final List<String> DEPRIORITIZED = List.of(
      "awdl",   // macOS Apple Wireless Direct Link
      "llw",    // macOS low-latency WLAN
      "utun",   // macOS/VPN tunnels
      "tun", "tap", "ppp", "wg",   // VPNs
      "bridge", "vmnet", "vboxnet", "virbr", "docker", "veth", // virtual switches
      "anpi", "ap"  // macOS internal/AP interfaces
  );

  private NetworkPicker() {
  }

  /**
   * One bindable IPv4 address on a live interface.
   *
   * @param interfaceName the operating system's name for the interface, such as {@code en0}
   * @param address       the IPv4 address bound to it
   * @param loopback      whether the address is a loopback address
   * @param siteLocal     whether the address is in a private LAN range
   * @param multicast     whether the interface supports multicast, which mDNS requires
   * @param pointToPoint  whether the interface is a point-to-point link such as a VPN tunnel
   */
  public record Candidate(String interfaceName, InetAddress address, boolean loopback,
                          boolean siteLocal, boolean multicast, boolean pointToPoint) {

    /**
     * Summarises this candidate's properties for display.
     *
     * @return a comma-separated list of notable flags, or {@code "-"} if none apply
     */
    public String describe() {
      List<String> flags = new ArrayList<>();
      if (loopback) flags.add("loopback");
      if (siteLocal) flags.add("site-local");
      if (pointToPoint) flags.add("point-to-point");
      if (!multicast) flags.add("no-multicast");
      if (isDeprioritized(interfaceName)) flags.add("virtual");
      return flags.isEmpty() ? "-" : String.join(", ", flags);
    }

    /**
     * Ranks this candidate's suitability for LAN multicast; higher is better.
     *
     * @return the score used to order candidates
     */
    int score() {
      int s = 0;
      // Loopback is never the LAN, so it outranks nothing.
      if (!loopback) s += 1000;
      if (multicast) s += 50;
      if (siteLocal) s += 25;
      if (!pointToPoint) s += 10;
      if (!isDeprioritized(interfaceName)) s += 200;
      return s;
    }
  }

  /**
   * Lists every usable IPv4 address on this host, best candidate first.
   *
   * @return the ranked candidates, never {@code null}
   * @throws IOException if the interfaces cannot be enumerated
   */
  public static List<Candidate> candidates() throws IOException {
    List<Candidate> found = new ArrayList<>();
    try {
      for (NetworkInterface nic : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!nic.isUp()) continue;
        for (InetAddress addr : java.util.Collections.list(nic.getInetAddresses())) {
          if (!(addr instanceof Inet4Address)) continue;
          found.add(new Candidate(nic.getName(), addr, addr.isLoopbackAddress(),
              addr.isSiteLocalAddress(), nic.supportsMulticast(), nic.isPointToPoint()));
        }
      }
    } catch (SocketException e) {
      throw new IOException("Could not enumerate network interfaces: " + e.getMessage(), e);
    }
    found.sort(Comparator.comparingInt(Candidate::score).reversed()
        .thenComparing(Candidate::interfaceName));
    return found;
  }

  /**
   * Returns the candidate auto-selection would choose.
   *
   * @return the best candidate, or {@code null} if this host has no usable interface
   * @throws IOException if the interfaces cannot be enumerated
   */
  public static Candidate best() throws IOException {
    List<Candidate> all = candidates();
    for (Candidate c : all) {
      if (!c.loopback()) return c;
    }
    return all.isEmpty() ? null : all.get(0);
  }

  /**
   * Resolves a bind specification to an address.
   *
   * <p>Resolution order is the supplied {@code spec}, then {@link #ENV_VAR}, then
   * auto-selection, so a single run can be redirected without editing the config.
   *
   * @param spec an interface name ({@code en0}), an IPv4 literal ({@code 192.168.1.10}),
   *             {@code auto} or {@code null} to auto-select, or {@code any} for the
   *             wildcard address
   * @return the address to bind mDNS to
   * @throws IOException if {@code spec} names no local interface; the message lists
   *                     the valid options
   */
  public static InetAddress resolve(String spec) throws IOException {
    String effective = spec;
    if (isBlank(effective)) effective = System.getenv(ENV_VAR);
    if (isBlank(effective) || AUTO.equalsIgnoreCase(effective)) return auto();
    if (ANY.equalsIgnoreCase(effective)) return InetAddress.getByName("0.0.0.0");

    List<Candidate> all = candidates();

    for (Candidate c : all) {
      if (c.interfaceName().equalsIgnoreCase(effective)) return c.address();
    }
    for (Candidate c : all) {
      if (c.address().getHostAddress().equals(effective)) return c.address();
    }
    // Not one of ours, but it may still be a resolvable name the user meant.
    try {
      InetAddress byName = InetAddress.getByName(effective);
      if (NetworkInterface.getByInetAddress(byName) != null) return byName;
    } catch (UnknownHostException | SocketException ignored) {
      // fall through to the error below
    }
    throw new IOException("No local interface matches '" + effective + "'.\n" + optionsTable(all)
        + "\nSet one with `lansync config --bind <interface|address>`, "
        + "or use `auto` to let lansync choose.");
  }

  /**
   * Selects the most likely LAN interface without user input.
   *
   * @return the chosen address
   * @throws IOException if this host exposes no usable interface at all
   */
  public static InetAddress auto() throws IOException {
    Candidate best = best();
    if (best != null && !best.loopback()) return best.address();
    if (best != null) return best.address();
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new IOException("No usable network interface found. Connect to a network, "
          + "or pin one with `lansync config --bind <interface>`.", e);
    }
  }

  /**
   * Renders a candidate list as an aligned table, used for CLI output and to make
   * bind failures actionable.
   *
   * @param all the candidates to render
   * @return the formatted table, without a trailing newline
   */
  public static String optionsTable(List<Candidate> all) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%-10s %-16s %s%n", "INTERFACE", "ADDRESS", "NOTES"));
    for (Candidate c : all) {
      sb.append(String.format("%-10s %-16s %s%n", c.interfaceName(), c.address().getHostAddress(), c.describe()));
    }
    return sb.toString().stripTrailing();
  }

  /**
   * Reports whether an interface name looks like a tunnel, bridge or other
   * virtual device rather than a physical LAN adapter.
   *
   * @param interfaceName the operating system's interface name
   * @return {@code true} if the interface should be ranked last
   */
  static boolean isDeprioritized(String interfaceName) {
    String n = interfaceName.toLowerCase(Locale.ROOT);
    return DEPRIORITIZED.stream().anyMatch(n::startsWith);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
