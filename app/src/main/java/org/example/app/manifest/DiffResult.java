package org.example.app.manifest;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The work a sync must perform, as three disjoint sets of relative paths.
 *
 * <p>The sets are insertion-ordered so a dry run lists files in the same order
 * the transfer will process them.
 *
 * @author Jamal
 * @see Differ
 */
public class DiffResult {

  private final Set<String> push = new LinkedHashSet<>();
  private final Set<String> pull = new LinkedHashSet<>();
  private final Set<String> delete = new LinkedHashSet<>();

  /** Creates an empty diff. */
  public DiffResult() {
  }

  /**
   * Returns the files to send to the peer, because they are missing there or
   * differ in content.
   *
   * @return the mutable push set
   */
  public Set<String> getPush() {
    return push;
  }

  /**
   * Returns the files to fetch from the peer, because they are missing locally.
   *
   * @return the mutable pull set
   */
  public Set<String> getPull() {
    return pull;
  }

  /**
   * Returns the files to delete on the peer, populated only in mirror mode.
   *
   * @return the mutable delete set
   */
  public Set<String> getDelete() {
    return delete;
  }

  /**
   * Returns an unmodifiable view of every path this diff touches, for reporting.
   *
   * @return the union of the push, pull and delete sets
   */
  public Set<String> getAffectedPaths() {
    Set<String> all = new LinkedHashSet<>(push);
    all.addAll(pull);
    all.addAll(delete);
    return Collections.unmodifiableSet(all);
  }

  /**
   * Reports whether the two sides already agree.
   *
   * @return {@code true} if there is nothing to push, pull or delete
   */
  public boolean isEmpty() {
    return push.isEmpty() && pull.isEmpty() && delete.isEmpty();
  }

  /**
   * Returns a short summary of the pending work.
   *
   * @return a string giving the size of each set
   */
  @Override
  public String toString() {
    return "DiffResult[push=" + push.size() + ", pull=" + pull.size()
        + ", delete=" + delete.size() + "]";
  }
}
