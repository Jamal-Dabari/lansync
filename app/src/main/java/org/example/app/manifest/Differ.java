package org.example.app.manifest;

import java.util.Map;

/**
 * Compares two manifests to decide what a sync must move.
 *
 * @author Jamal
 * @see DiffResult
 */
public final class Differ {

  private Differ() {
    throw new AssertionError("No instances of Differ");
  }

  /**
   * Diffs the local manifest against a peer's.
   *
   * <p>Comparison is by content digest, not timestamp, so clock skew between
   * peers cannot cause a spurious transfer. Where both sides hold the same path
   * with different content, the local copy wins and is pushed: the initiating
   * side is the one the user is acting from, which makes the outcome predictable
   * without a conflict-resolution prompt.
   *
   * @param local  this device's manifest
   * @param remote the peer's manifest
   * @param mirror {@code true} to delete remote-only files instead of pulling them
   * @return the resulting work set
   */
  public static DiffResult diff(Manifest local, Manifest remote, boolean mirror) {
    DiffResult result = new DiffResult();
    Map<String, ManifestEntry> localEntries = local.getEntries();
    Map<String, ManifestEntry> remoteEntries = remote.getEntries();

    for (Map.Entry<String, ManifestEntry> entry : localEntries.entrySet()) {
      ManifestEntry counterpart = remoteEntries.get(entry.getKey());
      if (counterpart == null || !counterpart.getSha256().equals(entry.getValue().getSha256())) {
        result.getPush().add(entry.getKey());
      }
    }

    for (String remotePath : remoteEntries.keySet()) {
      if (!localEntries.containsKey(remotePath)) {
        if (mirror) {
          result.getDelete().add(remotePath);
        } else {
          result.getPull().add(remotePath);
        }
      }
    }
    return result;
  }
}
