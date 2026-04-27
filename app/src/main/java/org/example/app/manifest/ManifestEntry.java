package org.example.app.manifest;

public class ManifestEntry {

  private String sha256;
  private long size;
  private long mtime;

  public ManifestEntry() {
  }

  public ManifestEntry(String sha256, long size, long mtime) {
    this.sha256 = sha256;
    this.size = size;
    this.mtime = mtime;
  }

  public String getSha256() { return sha256; }
  public void setSha256(String sha256) { this.sha256 = sha256; }

  public long getSize() { return size; }
  public void setSize(long size) { this.size = size; }

  public long getMtime() { return mtime; }
  public void setMtime(long mtime) { this.mtime = mtime; }
}
