package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import org.example.app.manifest.DiffResult;
import org.example.app.manifest.Differ;
import org.example.app.manifest.Manifest;
import org.example.app.manifest.ManifestEntry;
import org.junit.jupiter.api.Test;

class DifferTest {

  private static Manifest of(String... pathHashPairs) {
    Manifest m = new Manifest();
    for (int i = 0; i < pathHashPairs.length; i += 2) {
      m.getEntries().put(pathHashPairs[i], new ManifestEntry(pathHashPairs[i + 1], 1, 0));
    }
    return m;
  }

  @Test
  void identicalManifestsProduceNoWork() {
    assertTrue(Differ.diff(of("a.txt", "h1"), of("a.txt", "h1"), false).isEmpty());
  }

  @Test
  void localOnlyFilesArePushed() {
    DiffResult r = Differ.diff(of("a.txt", "h1"), of(), false);
    assertEquals(java.util.Set.of("a.txt"), r.getPush());
    assertTrue(r.getPull().isEmpty());
  }

  @Test
  void remoteOnlyFilesArePulled() {
    DiffResult r = Differ.diff(of(), of("b.txt", "h2"), false);
    assertEquals(java.util.Set.of("b.txt"), r.getPull());
  }

  @Test
  void changedContentIsPushed() {
    DiffResult r = Differ.diff(of("a.txt", "local"), of("a.txt", "remote"), false);
    assertEquals(java.util.Set.of("a.txt"), r.getPush());
  }

  @Test
  void mirrorDeletesRemoteOnlyFilesInsteadOfPulling() {
    DiffResult r = Differ.diff(of("a.txt", "h1"), of("b.txt", "h2"), true);
    assertEquals(java.util.Set.of("a.txt"), r.getPush());
    assertEquals(java.util.Set.of("b.txt"), r.getDelete());
    assertTrue(r.getPull().isEmpty());
  }
}
