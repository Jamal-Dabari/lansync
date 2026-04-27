package org.example.app.manifest;

import java.util.LinkedHashSet;
import java.util.Set;

public class DiffResult {

  private final Set<String> push = new LinkedHashSet<>();
  private final Set<String> pull = new LinkedHashSet<>();
  private final Set<String> delete = new LinkedHashSet<>();

  public Set<String> getPush() { return push; }
  public Set<String> getPull() { return pull; }
  public Set<String> getDelete() { return delete; }

  public boolean isEmpty() {
    return push.isEmpty() && pull.isEmpty() && delete.isEmpty();
  }
}
