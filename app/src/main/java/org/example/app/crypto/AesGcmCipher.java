package org.example.app.crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AesGcmCipher {

  public static final int IV_LEN = 12;
  public static final int TAG_LEN_BITS = 128;
  private static final SecureRandom RNG = new SecureRandom();

  private final SecretKeySpec key;

  public AesGcmCipher(byte[] keyBytes) {
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  public byte[] newIv() {
    byte[] iv = new byte[IV_LEN];
    RNG.nextBytes(iv);
    return iv;
  }

  public byte[] encrypt(byte[] iv, byte[] plaintext, byte[] aad) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
    if (aad != null) c.updateAAD(aad);
    return c.doFinal(plaintext);
  }

  public byte[] decrypt(byte[] iv, byte[] ciphertext, byte[] aad) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
    if (aad != null) c.updateAAD(aad);
    return c.doFinal(ciphertext);
  }
}
