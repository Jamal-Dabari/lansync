package org.example.app.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM authenticated encryption for one session key.
 *
 * <p>GCM provides confidentiality and integrity together: the 128-bit tag appended
 * to every ciphertext is verified on decryption, so tampering surfaces as an
 * exception rather than as corrupt plaintext.
 *
 * <p><strong>A GCM key must never encrypt twice under the same IV.</strong> Callers
 * must take a fresh IV from {@link #newIv()} for every message and never reuse one.
 *
 * <p>Instances are thread-safe: a new {@link Cipher} is created per call rather
 * than shared, since {@code Cipher} objects are stateful.
 *
 * @author Jamal
 */
public final class AesGcmCipher {

  /** GCM's recommended IV length in bytes; 96 bits is the size GCM is optimised for. */
  public static final int IV_LEN = 12;

  /** Authentication tag length in bits. */
  public static final int TAG_LEN_BITS = 128;

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final SecureRandom RNG = new SecureRandom();

  private final SecretKeySpec key;

  /**
   * Wraps a raw session key.
   *
   * @param keyBytes the key, which must be 16, 24 or 32 bytes; lansync derives 32
   * @throws NullPointerException     if {@code keyBytes} is {@code null}
   * @throws IllegalArgumentException if the key length is not valid for AES
   */
  public AesGcmCipher(byte[] keyBytes) {
    Objects.requireNonNull(keyBytes, "keyBytes");
    if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
      throw new IllegalArgumentException("Invalid AES key length: " + keyBytes.length + " bytes");
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  /**
   * Generates a fresh random IV.
   *
   * @return a new {@value #IV_LEN}-byte IV, never reused by this instance
   */
  public byte[] newIv() {
    byte[] iv = new byte[IV_LEN];
    RNG.nextBytes(iv);
    return iv;
  }

  /**
   * Encrypts and authenticates a message.
   *
   * @param iv        a fresh IV from {@link #newIv()}, never previously used with this key
   * @param plaintext the data to encrypt
   * @param aad       additional data to authenticate but not encrypt, or {@code null};
   *                  the same value must be supplied to {@link #decrypt}
   * @return the ciphertext with the authentication tag appended
   * @throws GeneralSecurityException if the platform rejects the key or parameters
   */
  public byte[] encrypt(byte[] iv, byte[] plaintext, byte[] aad) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
    if (aad != null) {
      cipher.updateAAD(aad);
    }
    return cipher.doFinal(plaintext);
  }

  /**
   * Verifies and decrypts a message.
   *
   * @param iv         the IV used to encrypt it
   * @param ciphertext the ciphertext with its trailing authentication tag
   * @param aad        the same additional data supplied to {@link #encrypt}, or {@code null}
   * @return the recovered plaintext
   * @throws javax.crypto.AEADBadTagException if authentication fails, meaning the
   *                                          data was altered or the key differs
   * @throws GeneralSecurityException         if the platform rejects the key or parameters
   */
  public byte[] decrypt(byte[] iv, byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
    if (aad != null) {
      cipher.updateAAD(aad);
    }
    return cipher.doFinal(ciphertext);
  }
}
