package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.example.app.crypto.AesGcmCipher;
import org.example.app.crypto.HmacUtil;
import org.example.app.crypto.KeyDerivation;
import org.junit.jupiter.api.Test;

class CryptoTest {

  private static final byte[] KEY = new byte[32];

  @Test
  void roundTripsPlaintext() throws Exception {
    AesGcmCipher c = new AesGcmCipher(KEY);
    byte[] iv = c.newIv();
    byte[] plain = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

    byte[] ct = c.encrypt(iv, plain, new byte[] { 0x22 });
    assertArrayEquals(plain, c.decrypt(iv, ct, new byte[] { 0x22 }));
  }

  @Test
  void rejectsTamperedCiphertext() throws Exception {
    AesGcmCipher c = new AesGcmCipher(KEY);
    byte[] iv = c.newIv();
    byte[] ct = c.encrypt(iv, "payload".getBytes(StandardCharsets.UTF_8), null);
    ct[0] ^= 0x01;

    assertThrows(Exception.class, () -> c.decrypt(iv, ct, null));
  }

  @Test
  void rejectsMismatchedAad() throws Exception {
    AesGcmCipher c = new AesGcmCipher(KEY);
    byte[] iv = c.newIv();
    byte[] ct = c.encrypt(iv, "payload".getBytes(StandardCharsets.UTF_8), new byte[] { 0x22 });

    // A FILE_CHUNK frame must not be replayable as any other frame type.
    assertThrows(Exception.class, () -> c.decrypt(iv, ct, new byte[] { 0x10 }));
  }

  @Test
  void pairSaltIsOrderIndependent() {
    assertArrayEquals(KeyDerivation.pairSalt("aaa", "bbb"), KeyDerivation.pairSalt("bbb", "aaa"));
  }

  @Test
  void constantTimeEqComparesContentAndLength() {
    assertTrue(HmacUtil.constantTimeEq(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 }));
    assertFalse(HmacUtil.constantTimeEq(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 4 }));
    assertFalse(HmacUtil.constantTimeEq(new byte[] { 1, 2 }, new byte[] { 1, 2, 3 }));
  }
}
