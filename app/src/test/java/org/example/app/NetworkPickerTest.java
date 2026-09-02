package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.util.List;

import org.example.app.util.NetworkPicker;
import org.junit.jupiter.api.Test;

class NetworkPickerTest {

  @Test
  void findsAtLeastLoopback() throws Exception {
    List<NetworkPicker.Candidate> all = NetworkPicker.candidates();
    assertFalse(all.isEmpty(), "every host has at least a loopback interface");
  }

  @Test
  void ranksLoopbackLast() throws Exception {
    List<NetworkPicker.Candidate> all = NetworkPicker.candidates();
    int firstLoopback = -1;
    for (int i = 0; i < all.size(); i++) {
      if (all.get(i).loopback()) { firstLoopback = i; break; }
    }
    if (firstLoopback < 0) return; // no loopback exposed on this host
    for (int i = firstLoopback; i < all.size(); i++) {
      assertTrue(all.get(i).loopback(),
          "a non-loopback interface must not rank below loopback: " + all.get(i).interfaceName());
    }
  }

  @Test
  void ranksVirtualInterfacesBelowRealOnes() throws Exception {
    List<NetworkPicker.Candidate> all = NetworkPicker.candidates();
    boolean seenVirtual = false;
    for (NetworkPicker.Candidate c : all) {
      boolean virtual = c.describe().contains("virtual");
      if (virtual) seenVirtual = true;
      else if (seenVirtual && !c.loopback()) {
        fail("real interface " + c.interfaceName() + " ranked below a virtual one");
      }
    }
  }

  @Test
  void resolvesAutoAndAny() throws Exception {
    assertNotNull(NetworkPicker.resolve(null));
    assertNotNull(NetworkPicker.resolve("auto"));
    assertNotNull(NetworkPicker.resolve("AUTO"));
    assertEquals("0.0.0.0", NetworkPicker.resolve("any").getHostAddress());
  }

  @Test
  void resolvesEveryInterfaceByNameAndByAddress() throws Exception {
    for (NetworkPicker.Candidate c : NetworkPicker.candidates()) {
      assertEquals(c.address(), NetworkPicker.resolve(c.address().getHostAddress()),
          "resolving by literal address should round-trip");
      InetAddress byName = NetworkPicker.resolve(c.interfaceName());
      assertNotNull(byName, "resolving by interface name should work for " + c.interfaceName());
    }
  }

  @Test
  void unknownInterfaceFailsWithTheAvailableOptions() {
    Exception e = assertThrows(Exception.class, () -> NetworkPicker.resolve("definitely-not-a-nic0"));
    assertTrue(e.getMessage().contains("definitely-not-a-nic0"));
    // The error must be actionable: it lists what the user could have picked instead.
    assertTrue(e.getMessage().contains("INTERFACE") && e.getMessage().contains("--bind"),
        "expected the interface table and a hint, got: " + e.getMessage());
  }

  @Test
  void autoNeverPicksLoopbackWhenARealInterfaceExists() throws Exception {
    boolean hasReal = NetworkPicker.candidates().stream().anyMatch(c -> !c.loopback());
    if (!hasReal) return;
    assertFalse(NetworkPicker.auto().isLoopbackAddress(),
        "auto-selection must prefer a real LAN interface over loopback");
  }
}
