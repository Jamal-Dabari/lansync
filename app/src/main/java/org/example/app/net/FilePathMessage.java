package org.example.app.net;

/**
 * Body of the {@link MessageType#FILE_REQUEST} and {@link MessageType#FILE_DELETE}
 * frames: a single path relative to the sync root.
 *
 * @author Jamal
 */
public class FilePathMessage {

  private String path;

  /** Required by Gson for deserialization. */
  public FilePathMessage() {
  }

  /**
   * Creates a path reference.
   *
   * @param path the file's path relative to the sync root, using {@code /} separators
   */
  public FilePathMessage(String path) {
    this.path = path;
  }

  /**
   * Returns the referenced path.
   *
   * @return the relative path, or {@code null} if the peer omitted it
   */
  public String getPath() {
    return path;
  }
}
