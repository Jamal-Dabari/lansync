package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.example.app.sync.SyncSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Two in-process peers over loopback, asserting the directories converge. */
class SyncSessionIT {

  private static final String CLIENT_ID = "11111111-client";
  private static final String SERVER_ID = "22222222-server";

  @Test
  void pushesPullsAndConverges(@TempDir Path clientDir, @TempDir Path serverDir) throws Exception {
    write(clientDir.resolve("only-local.txt"), "hello from the client");
    write(clientDir.resolve("nested/deep.txt"), "a file in a subdirectory");
    write(clientDir.resolve("shared.txt"), "same on both sides");
    write(serverDir.resolve("shared.txt"), "same on both sides");
    write(serverDir.resolve("only-remote.txt"), "hello from the server");

    runSync(clientDir, serverDir, "correct horse battery", false);

    // Pushed to the server.
    assertEquals("hello from the client", read(serverDir.resolve("only-local.txt")));
    assertEquals("a file in a subdirectory", read(serverDir.resolve("nested/deep.txt")));
    // Pulled to the client.
    assertEquals("hello from the server", read(clientDir.resolve("only-remote.txt")));
    // Unchanged on both.
    assertEquals("same on both sides", read(clientDir.resolve("shared.txt")));
    assertEquals(0, Files.walk(clientDir).filter(p -> p.toString().endsWith(".lansync.tmp")).count());
  }

  @Test
  void transfersAFileLargerThanOneChunk() throws Exception {
    Path clientDir = Files.createTempDirectory("lansync-c");
    Path serverDir = Files.createTempDirectory("lansync-s");

    StringBuilder sb = new StringBuilder();
    while (sb.length() < 300_000) sb.append("chunked payload line\n");
    write(clientDir.resolve("big.txt"), sb.toString());

    runSync(clientDir, serverDir, "correct horse battery", false);

    assertEquals(sb.toString(), read(serverDir.resolve("big.txt")));
  }

  @Test
  void mirrorDeletesRemoteFilesMissingLocally() throws Exception {
    Path clientDir = Files.createTempDirectory("lansync-c");
    Path serverDir = Files.createTempDirectory("lansync-s");

    write(clientDir.resolve("keep.txt"), "keep me");
    write(serverDir.resolve("keep.txt"), "keep me");
    write(serverDir.resolve("stale.txt"), "delete me");

    runSync(clientDir, serverDir, "correct horse battery", true);

    assertTrue(Files.exists(serverDir.resolve("keep.txt")));
    assertFalse(Files.exists(serverDir.resolve("stale.txt")), "mirror should have removed the orphan");
    assertFalse(Files.exists(clientDir.resolve("stale.txt")), "mirror should not pull the orphan back");
  }

  @Test
  void mismatchedPassphraseFailsTheHandshake() throws Exception {
    Path clientDir = Files.createTempDirectory("lansync-c");
    Path serverDir = Files.createTempDirectory("lansync-s");
    write(clientDir.resolve("secret.txt"), "should never arrive");

    Exception failure = assertThrows(Exception.class,
        () -> runSync(clientDir, serverDir, "right passphrase", "wrong passphrase", false));

    assertTrue(failure.getMessage().toLowerCase().contains("auth")
        || failure.getMessage().toLowerCase().contains("passphrase"),
        "expected an auth failure, got: " + failure.getMessage());
    assertFalse(Files.exists(serverDir.resolve("secret.txt")));
  }

  private void runSync(Path clientDir, Path serverDir, String passphrase, boolean mirror) throws Exception {
    runSync(clientDir, serverDir, passphrase, passphrase, mirror);
  }

  private void runSync(Path clientDir, Path serverDir, String clientPass, String serverPass, boolean mirror)
      throws Exception {
    AtomicReference<Exception> serverFailure = new AtomicReference<>();

    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      Thread server = new Thread(() -> {
        try (Socket s = listener.accept()) {
          new SyncSession(SyncSession.Role.SERVER, s, serverDir, serverPass.toCharArray(), SERVER_ID).run();
        } catch (Exception e) {
          serverFailure.set(e);
        }
      });
      server.start();

      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), listener.getLocalPort()), 5_000);
        SyncSession session = new SyncSession(SyncSession.Role.CLIENT, socket, clientDir,
            clientPass.toCharArray(), CLIENT_ID);
        session.setMirror(mirror);
        session.run();
      } finally {
        server.join(30_000);
      }
    }
    if (serverFailure.get() != null) throw serverFailure.get();
  }

  private static void write(Path p, String content) throws IOException {
    Files.createDirectories(p.getParent());
    Files.writeString(p, content, StandardCharsets.UTF_8);
  }

  private static String read(Path p) throws IOException {
    assertTrue(Files.exists(p), "expected " + p + " to exist");
    return Files.readString(p, StandardCharsets.UTF_8);
  }
}
