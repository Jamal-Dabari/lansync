package org.example.app.net;

/**
 * Body of the {@link MessageType#FILE_HEADER} frame: describes the file that the
 * following {@link MessageType#FILE_CHUNK} frames carry.
 *
 * <p>The size and digest let the receiver verify the transfer before the file is
 * moved into place.
 *
 * @author Jamal
 */
public class FileHeaderMessage {

  private String path;
  private long size;
  private String sha256;

  /** Required by Gson for deserialization. */
  public FileHeaderMessage() {
  }

  /**
   * Creates a file header.
   *
   * @param path   the file's path relative to the sync root, using {@code /} separators
   * @param size   the plaintext length in bytes
   * @param sha256 the hex-encoded SHA-256 of the plaintext
   */
  public FileHeaderMessage(String path, long size, String sha256) {
    this.path = path;
    this.size = size;
    this.sha256 = sha256;
  }

  /**
   * Returns the file's path relative to the sync root.
   *
   * @return the relative path, or {@code null} if the peer omitted it
   */
  public String getPath() {
    return path;
  }

  /**
   * Returns the declared plaintext length.
   *
   * @return the size in bytes
   */
  public long getSize() {
    return size;
  }

  /**
   * Returns the declared plaintext digest.
   *
   * @return the hex-encoded SHA-256, or {@code null} if the peer omitted it
   */
  public String getSha256() {
    return sha256;
  }
}
