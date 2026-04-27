package org.example.app.sync;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;

import org.example.app.crypto.AesGcmCipher;
import org.example.app.manifest.DiffResult;
import org.example.app.manifest.Manifest;

public class SyncSession {

  public enum Role { CLIENT, SERVER }

  private final Role role;
  private final Socket socket;
  private final Path syncRoot;
  private final char[] passphrase;
  private final String localDeviceId;

  private AesGcmCipher cipher;
  private String remoteDeviceId;
  private boolean mirror = false;
  private boolean dryRun = false;

  public SyncSession(Role role, Socket socket, Path syncRoot, char[] passphrase, String localDeviceId) {
    this.role = role;
    this.socket = socket;
    this.syncRoot = syncRoot;
    this.passphrase = passphrase;
    this.localDeviceId = localDeviceId;
  }

  public void setMirror(boolean mirror) { this.mirror = mirror; }
  public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

  public void run() throws Exception {
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();

    // 1. handshake: HELLO / CHALLENGE / AUTH / AUTH_OK
    //    - exchange device ids + nonces
    //    - derive shared key via KeyDerivation.deriveKey(passphrase, pairSalt(local, remote))
    //    - verify HMAC challenge; on success assign this.cipher
    handshake(in, out);

    // 2. manifest exchange (MANIFEST frames in both directions)
    Manifest localM = buildLocalManifest();
    Manifest remoteM = exchangeManifests(in, out, localM);

    // 3. compute diff
    DiffResult diff = org.example.app.manifest.Differ.diff(localM, remoteM, mirror);

    if (dryRun) {
      System.out.println("push: " + diff.getPush());
      System.out.println("pull: " + diff.getPull());
      System.out.println("delete: " + diff.getDelete());
      return;
    }

    // 4. transfer loop using TransferEngine
    transferAll(in, out, diff);

    // 5. BYE
    close();
  }

  private void handshake(InputStream in, OutputStream out) throws Exception {
    // TODO
    throw new UnsupportedOperationException("handshake not implemented");
  }

  private Manifest buildLocalManifest() throws Exception {
    return org.example.app.manifest.ManifestBuilder.build(syncRoot, localDeviceId);
  }

  private Manifest exchangeManifests(InputStream in, OutputStream out, Manifest localM) throws Exception {
    // TODO: send MANIFEST frame (gson-serialized), read remote MANIFEST
    throw new UnsupportedOperationException("manifest exchange not implemented");
  }

  private void transferAll(InputStream in, OutputStream out, DiffResult diff) throws Exception {
    // TODO: drive TransferEngine for each entry in diff.push / diff.pull / diff.delete
    throw new UnsupportedOperationException("transfer loop not implemented");
  }

  public void close() {
    try { socket.close(); } catch (Exception ignored) {}
    if (passphrase != null) java.util.Arrays.fill(passphrase, '\0');
  }
}
