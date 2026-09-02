package org.example.app.manifest;

/**
 * One file's identity within a {@link Manifest}: its content digest, size and
 * modification time.
 *
 * <p>Comparison during a sync uses the digest alone. Size and modification time
 * are carried for reporting and for future resume support, not to decide whether
 * a file changed — clock skew between peers makes timestamps unreliable for that.
 *
 * <p>This is a JSON-serialized value type, so it keeps a no-argument constructor
 * and setters for Gson.
 *
 * @author Jamal
 */
public class ManifestEntry {

  private String sha256;
  private long size;
  private long mtime;

  /** Required by Gson for deserialization. */
  public ManifestEntry() {
  }

  /**
   * Creates an entry.
   *
   * @param sha256 the hex-encoded SHA-256 of the file's contents
   * @param size   the file size in bytes
   * @param mtime  the last-modified time in seconds since the epoch
   */
  public ManifestEntry(String sha256, long size, long mtime) {
    this.sha256 = sha256;
    this.size = size;
    this.mtime = mtime;
  }

  /**
   * Returns the content digest.
   *
   * @return the hex-encoded SHA-256
   */
  public String getSha256() {
    return sha256;
  }

  /**
   * Sets the content digest.
   *
   * @param sha256 the hex-encoded SHA-256
   */
  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  /**
   * Returns the file size.
   *
   * @return the size in bytes
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets the file size.
   *
   * @param size the size in bytes
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * Returns the last-modified time.
   *
   * @return seconds since the epoch
   */
  public long getMtime() {
    return mtime;
  }

  /**
   * Sets the last-modified time.
   *
   * @param mtime seconds since the epoch
   */
  public void setMtime(long mtime) {
    this.mtime = mtime;
  }
}
