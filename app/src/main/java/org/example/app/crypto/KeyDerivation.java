package org.example.app.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Turns the user's shared passphrase into an AES session key.
 *
 * @author Jamal
 */
public final class KeyDerivation {

  /**
   * PBKDF2 iteration count, at the OWASP 2023 recommendation for HMAC-SHA256.
   * A high count is what makes a guessed passphrase expensive to test offline.
   */
  public static final int ITERATIONS = 600_000;

  /** Derived key length in bits, matching AES-256. */
  public static final int KEY_LEN_BITS = 256;

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

  private KeyDerivation() {
    throw new AssertionError("No instances of KeyDerivation");
  }

  /**
   * Derives a session key from a passphrase.
   *
   * <p>The passphrase array is zeroed before returning, whether or not derivation
   * succeeded, so the secret does not linger on the heap waiting for a garbage
   * collection that may never come. Callers needing the passphrase afterwards
   * must pass a copy.
   *
   * @param passphrase the shared passphrase; zeroed by this call
   * @param salt       the salt, normally from {@link #pairSalt(String, String)}
   * @return the derived {@value #KEY_LEN_BITS}-bit key
   * @throws IllegalStateException if the platform lacks PBKDF2-HMAC-SHA256
   */
  public static byte[] deriveKey(char[] passphrase, byte[] salt) {
    try {
      KeySpec spec = new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LEN_BITS);
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("PBKDF2-HMAC-SHA256 unavailable", e);
    } finally {
      Arrays.fill(passphrase, '\0');
    }
  }

  /**
   * Builds the salt shared by a pair of devices.
   *
   * <p>The two IDs are sorted so both peers compute the same salt regardless of
   * which one initiated, which lets them agree on a key without either sending a
   * salt over the wire. Because the salt is per-pair, the same passphrase yields
   * a different key for every pairing.
   *
   * @param deviceIdA one device's ID
   * @param deviceIdB the other device's ID
   * @return the deterministic salt for that pair
   */
  public static byte[] pairSalt(String deviceIdA, String deviceIdB) {
    boolean aFirst = deviceIdA.compareTo(deviceIdB) < 0;
    String low = aFirst ? deviceIdA : deviceIdB;
    String high = aFirst ? deviceIdB : deviceIdA;
    return (low + ":" + high).getBytes(StandardCharsets.UTF_8);
  }
}
