package org.example.app.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;

public final class ManifestBuilder {

  private static final int BUF_SIZE = 64 * 1024;

  private ManifestBuilder() {
  }

  public static Manifest build(Path root, String deviceId) throws IOException {
    Manifest m = new Manifest();
    m.setDeviceId(deviceId);
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile).forEach(p -> {
        try {
          String rel = root.relativize(p).toString().replace('\\', '/');
          ManifestEntry e = new ManifestEntry(
              sha256(p),
              Files.size(p),
              Files.getLastModifiedTime(p).toMillis() / 1000
          );
          m.getEntries().put(rel, e);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
    return m;
  }

  public static String sha256(Path p) throws IOException {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      try (InputStream in = Files.newInputStream(p)) {
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
      }
      return HexFormat.of().formatHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
