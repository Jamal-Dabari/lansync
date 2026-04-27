package org.example.app.manifest;

import java.util.Map;

public final class Differ {

  private Differ() {
  }

  public static DiffResult diff(Manifest local, Manifest remote, boolean mirror) {
    DiffResult r = new DiffResult();
    Map<String, ManifestEntry> l = local.getEntries();
    Map<String, ManifestEntry> rm = remote.getEntries();

    for (Map.Entry<String, ManifestEntry> e : l.entrySet()) {
      ManifestEntry other = rm.get(e.getKey());
      if (other == null || !other.getSha256().equals(e.getValue().getSha256())) {
        r.getPush().add(e.getKey());
      }
    }
    for (Map.Entry<String, ManifestEntry> e : rm.entrySet()) {
      if (!l.containsKey(e.getKey())) {
        if (mirror) r.getDelete().add(e.getKey());
        else r.getPull().add(e.getKey());
      }
    }
    return r;
  }
}
