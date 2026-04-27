package org.example.app.crypto;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class KeyDerivation {

  public static final int ITERATIONS = 600_000;
  public static final int KEY_LEN_BITS = 256;
  private static final String ALGO = "PBKDF2WithHmacSHA256";

  private KeyDerivation() {
  }

  public static byte[] deriveKey(char[] passphrase, byte[] salt) {
    try {
      KeySpec spec = new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LEN_BITS);
      SecretKeyFactory f = SecretKeyFactory.getInstance(ALGO);
      return f.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("PBKDF2 unavailable", e);
    } finally {
      Arrays.fill(passphrase, '\0');
    }
  }

  public static byte[] pairSalt(String deviceIdA, String deviceIdB) {
    String lo = deviceIdA.compareTo(deviceIdB) < 0 ? deviceIdA : deviceIdB;
    String hi = deviceIdA.compareTo(deviceIdB) < 0 ? deviceIdB : deviceIdA;
    return (lo + ":" + hi).getBytes(StandardCharsets.UTF_8);
  }
}
