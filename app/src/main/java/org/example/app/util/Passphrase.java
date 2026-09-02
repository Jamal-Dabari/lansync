package org.example.app.util;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;

/**
 * Reads the shared passphrase from the terminal, or from the environment for
 * headless runs.
 *
 * <p>The passphrase is never written to disk. It is returned as a {@code char[]}
 * rather than a {@link String} so it can be zeroed after use — strings are
 * immutable and would linger in the heap until garbage collected. Callers pass
 * the array to {@link org.example.app.crypto.KeyDerivation#deriveKey}, which
 * zeroes it.
 *
 * @author Jamal
 */
public final class Passphrase {

  /** Environment variable consulted for non-interactive runs such as daemons or CI. */
  public static final String ENV_VAR = "LANSYNC_PASSPHRASE";

  private Passphrase() {
    throw new AssertionError("No instances of Passphrase");
  }

  /**
   * Reads a passphrase, preferring the environment variable when it is set.
   *
   * @param label the prompt to show, without punctuation
   * @return the passphrase, which the caller owns and should zero after use
   * @throws IOException if there is no console and no environment override, or
   *                     the entry was empty
   */
  public static char[] prompt(String label) throws IOException {
    String fromEnvironment = System.getenv(ENV_VAR);
    if (fromEnvironment != null && !fromEnvironment.isEmpty()) {
      return fromEnvironment.toCharArray();
    }

    Console console = System.console();
    if (console == null) {
      throw new IOException("No console available for a passphrase prompt. Set "
          + ENV_VAR + " instead, or run in an interactive terminal.");
    }
    char[] entered = console.readPassword("%s: ", label);
    if (entered == null || entered.length == 0) {
      throw new IOException("Empty passphrase");
    }
    return entered;
  }

  /**
   * Reads a passphrase twice and requires the entries to match, so a typo cannot
   * silently become the passphrase a user must reproduce on another device.
   *
   * <p>Confirmation is skipped when the environment variable supplies the value,
   * since there is nothing to mistype.
   *
   * @return the confirmed passphrase, which the caller owns and should zero after use
   * @throws IOException if the entries differ, or no passphrase could be read
   */
  public static char[] promptWithConfirmation() throws IOException {
    char[] first = prompt("Passphrase");
    if (suppliedByEnvironment()) {
      return first;
    }

    char[] second = prompt("Confirm passphrase");
    boolean matched = Arrays.equals(first, second);
    Arrays.fill(second, '\0');

    if (!matched) {
      Arrays.fill(first, '\0');
      throw new IOException("Passphrases did not match");
    }
    return first;
  }

  private static boolean suppliedByEnvironment() {
    String value = System.getenv(ENV_VAR);
    return value != null && !value.isEmpty();
  }
}
