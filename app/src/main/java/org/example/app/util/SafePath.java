package org.example.app.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Resolves peer-supplied relative paths against a sync root without letting them
 * escape it.
 *
 * <p>Paths in a manifest or file request arrive from another machine and must be
 * treated as untrusted input. Resolving {@code "../../.ssh/authorized_keys"}
 * naively would let a hostile peer write anywhere the process can, so every
 * resolution is normalised and confirmed to stay inside the root.
 *
 * @author Jamal
 */
public final class SafePath {

  private SafePath() {
    throw new AssertionError("No instances of SafePath");
  }

  /**
   * Resolves a relative path inside a root directory.
   *
   * @param root    the sync root that must contain the result
   * @param relPath the peer-supplied relative path
   * @return the absolute, normalised path inside {@code root}
   * @throws IOException if the path is empty, or escapes or equals the root
   */
  public static Path resolve(Path root, String relPath) throws IOException {
    if (relPath == null || relPath.isBlank()) {
      throw new IOException("Empty path");
    }
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path target = normalizedRoot.resolve(relPath).normalize();

    if (!target.startsWith(normalizedRoot) || target.equals(normalizedRoot)) {
      throw new IOException("Path escapes sync root: " + relPath);
    }
    return target;
  }
}
