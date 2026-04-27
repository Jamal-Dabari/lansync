package org.example.app.crypto;

import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacUtil {

  private static final String ALGO = "HmacSHA256";
  private static final SecureRandom RNG = new SecureRandom();

  private HmacUtil() {
  }

  public static byte[] nonce(int len) {
    byte[] n = new byte[len];
    RNG.nextBytes(n);
    return n;
  }

  public static byte[] hmac(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance(ALGO);
      mac.init(new SecretKeySpec(key, ALGO));
      return mac.doFinal(data);
    } catch (Exception e) {
      throw new IllegalStateException("HMAC unavailable", e);
    }
  }

  public static boolean constantTimeEq(byte[] a, byte[] b) {
    if (a.length != b.length) return false;
    int r = 0;
    for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
    return r == 0;
  }
}
