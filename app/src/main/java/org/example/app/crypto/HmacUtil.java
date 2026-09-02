package org.example.app.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 and the random nonces used by the lansync handshake.
 *
 * @author Jamal
 */
public final class HmacUtil {

  private static final String ALGORITHM = "HmacSHA256";
  private static final SecureRandom RNG = new SecureRandom();

  private HmacUtil() {
    throw new AssertionError("No instances of HmacUtil");
  }

  /**
   * Generates a cryptographically random nonce.
   *
   * @param length the nonce length in bytes
   * @return a fresh random nonce
   */
  public static byte[] nonce(int length) {
    byte[] nonce = new byte[length];
    RNG.nextBytes(nonce);
    return nonce;
  }

  /**
   * Computes HMAC-SHA256 over {@code data}.
   *
   * @param key  the MAC key
   * @param data the message to authenticate
   * @return the 32-byte MAC
   * @throws IllegalStateException if the platform lacks HMAC-SHA256, which every
   *                               conformant JRE is required to provide
   */
  public static byte[] hmac(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal(data);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 unavailable", e);
    }
  }

  /**
   * Compares two byte arrays in time independent of their contents.
   *
   * <p>Authentication comparisons must not short-circuit on the first differing
   * byte: the time taken would leak how much of a guessed value was correct,
   * letting an attacker recover a MAC byte by byte.
   *
   * @param a the first array
   * @param b the second array
   * @return {@code true} if both arrays have the same length and contents
   */
  public static boolean constantTimeEq(byte[] a, byte[] b) {
    if (a.length != b.length) {
      return false;
    }
    int difference = 0;
    for (int i = 0; i < a.length; i++) {
      difference |= a[i] ^ b[i];
    }
    return difference == 0;
  }
}
