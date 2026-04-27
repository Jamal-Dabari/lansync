package org.example.app.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.example.app.crypto.AesGcmCipher;
import org.example.app.manifest.ManifestBuilder;
import org.example.app.net.Frame;
import org.example.app.net.FrameIO;
import org.example.app.net.MessageType;

public class TransferEngine {

  public static final int CHUNK_SIZE = 64 * 1024;

  private final AesGcmCipher cipher;

  public TransferEngine(AesGcmCipher cipher) {
    this.cipher = cipher;
  }

  public void sendFile(OutputStream out, Path root, String relPath) throws Exception {
    Path p = root.resolve(relPath);
    long size = Files.size(p);
    // TODO: build FILE_HEADER payload (relPath, size, iv-per-chunk policy) and write frame
    FrameIO.write(out, new Frame(MessageType.FILE_HEADER, new byte[0]));

    try (InputStream in = Files.newInputStream(p)) {
      byte[] buf = new byte[CHUNK_SIZE];
      int n;
      while ((n = in.read(buf)) > 0) {
        byte[] iv = cipher.newIv();
        byte[] plain = (n == buf.length) ? buf : java.util.Arrays.copyOf(buf, n);
        byte[] ct = cipher.encrypt(iv, plain, null);
        // TODO: frame = [iv || ct]; fix layout when you define the FILE_CHUNK schema
        byte[] payload = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ct, 0, payload, iv.length, ct.length);
        FrameIO.write(out, new Frame(MessageType.FILE_CHUNK, payload));
      }
    }

    String sha = ManifestBuilder.sha256(p);
    FrameIO.write(out, new Frame(MessageType.FILE_END, sha.getBytes()));
  }

  public void receiveFile(InputStream in, Path root, String relPath, String expectedSha) throws Exception {
    Path target = root.resolve(relPath);
    Path tmp = target.resolveSibling(target.getFileName() + ".lansync.tmp");
    Files.createDirectories(target.getParent());

    try (OutputStream fout = Files.newOutputStream(tmp)) {
      while (true) {
        Frame f = FrameIO.read(in);
        if (f.getType() == MessageType.FILE_END) break;
        if (f.getType() != MessageType.FILE_CHUNK) throw new IOException("Unexpected frame: " + f.getType());

        byte[] payload = f.getPayload();
        byte[] iv = java.util.Arrays.copyOfRange(payload, 0, AesGcmCipher.IV_LEN);
        byte[] ct = java.util.Arrays.copyOfRange(payload, AesGcmCipher.IV_LEN, payload.length);
        byte[] plain = cipher.decrypt(iv, ct, null);
        fout.write(plain);
      }
    }

    String actual = ManifestBuilder.sha256(tmp);
    if (!actual.equals(expectedSha)) {
      Files.deleteIfExists(tmp);
      throw new IOException("Hash mismatch for " + relPath);
    }
    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
