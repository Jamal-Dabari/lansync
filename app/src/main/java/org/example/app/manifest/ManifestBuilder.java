package org.example.app.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;

/**
 * Walks a sync directory and hashes what it finds into a {@link Manifest}.
 *
 * @author Jamal
 */
public final class ManifestBuilder {

  private static final int BUF_SIZE = 64 * 1024;
  private static final String DIGEST_ALGORITHM = "SHA-256";
  private static final String TMP_SUFFIX = ".lansync.tmp";
  private static final String MACOS_METADATA = ".DS_Store";

  private ManifestBuilder() {
    throw new AssertionError("No instances of ManifestBuilder");
  }

  /**
   * Builds a manifest of every regular file beneath {@code root}.
   *
   * <p>Paths are stored relative to {@code root} with {@code /} separators so that
   * manifests compare correctly between platforms.
   *
   * @param root     the sync directory to walk
   * @param deviceId the device ID to stamp on the manifest
   * @return the manifest
   * @throws IOException if the tree cannot be walked or a file cannot be read
   */
  public static Manifest build(Path root, String deviceId) throws IOException {
    Manifest manifest = new Manifest();
    manifest.setDeviceId(deviceId);

    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile).forEach(file -> {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (isTransient(relative)) {
          return;
        }
        try {
          manifest.getEntries().put(relative, new ManifestEntry(
              sha256(file),
              Files.size(file),
              Files.getLastModifiedTime(file).toMillis() / 1000));
        } catch (IOException e) {
          // forEach cannot throw a checked exception; unwrapped below.
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    return manifest;
  }

  /**
   * Computes a file's SHA-256, streaming it so that memory use stays constant
   * regardless of file size.
   *
   * @param file the file to hash
   * @return the hex-encoded digest
   * @throws IOException           if the file cannot be read
   * @throws IllegalStateException if the platform lacks SHA-256
   */
  public static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
      try (InputStream in = Files.newInputStream(file)) {
        byte[] buffer = new byte[BUF_SIZE];
        int read;
        while ((read = in.read(buffer)) > 0) {
          digest.update(buffer, 0, read);
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(DIGEST_ALGORITHM + " unavailable", e);
    }
  }

  /**
   * Reports whether a path is lansync's own scratch state or OS metadata, neither
   * of which should be offered to a peer.
   *
   * @param relative the path relative to the sync root
   * @return {@code true} if the file should be left out of the manifest
   */
  private static boolean isTransient(String relative) {
    return relative.endsWith(TMP_SUFFIX)
        || relative.equals(MACOS_METADATA)
        || relative.endsWith("/" + MACOS_METADATA);
  }
}
