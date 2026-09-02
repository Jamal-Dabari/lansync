package org.example.app.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.example.app.util.Config;

/**
 * Accepts a single inbound file from a {@link TransferClient} and writes it to
 * the working directory.
 *
 * <p>This is a debugging aid from an early milestone: it is unauthenticated,
 * unencrypted, and handles exactly one transfer before returning. Real
 * synchronisation goes through {@link org.example.app.sync.SyncSession}.
 *
 * @author Jamal
 * @see TransferClient
 * @see org.example.app.sync.SyncSession
 */
public final class TransferServer {

  private final int port;
  private final Path destinationDir;

  /** Creates a server listening on the default lansync port, writing to the working directory. */
  public TransferServer() {
    this(Config.DEFAULT_PORT, Paths.get("."));
  }

  /**
   * Creates a server listening on an explicit port.
   *
   * @param port           the TCP port to listen on
   * @param destinationDir the directory received files are written to
   * @throws IllegalArgumentException if {@code port} is outside 1-65535
   */
  public TransferServer(int port, Path destinationDir) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port out of range: " + port);
    }
    this.port = port;
    this.destinationDir = java.util.Objects.requireNonNull(destinationDir, "destinationDir");
  }

  /**
   * Listens for one connection, receives one file, then returns.
   *
   * @return the path the received file was written to
   * @throws IOException if binding, accepting, or the transfer itself fails
   */
  public Path receiveOnce() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Listening on port " + port);

      try (Socket clientSocket = serverSocket.accept()) {
        InputStream in = clientSocket.getInputStream();

        String declaredName = new String(MessageProtocol.recvMessage(in), StandardCharsets.UTF_8);
        long declaredSize = parseSize(new String(MessageProtocol.recvMessage(in), StandardCharsets.UTF_8));

        // The name comes from the peer, so strip any directory part: an
        // unsanitised "../../x" would otherwise escape the destination.
        Path fileName = Paths.get(declaredName).getFileName();
        if (fileName == null) {
          throw new IOException("Peer sent an unusable file name: " + declaredName);
        }
        Path target = destinationDir.resolve(fileName);

        Files.createDirectories(destinationDir);
        try (OutputStream fileOut = Files.newOutputStream(target)) {
          long received = 0;
          while (received < declaredSize) {
            byte[] chunk = MessageProtocol.recvMessage(in);
            fileOut.write(chunk);
            received += chunk.length;
          }
        }
        return target;
      }
    }
  }

  private static long parseSize(String raw) throws IOException {
    try {
      long size = Long.parseLong(raw.trim());
      if (size < 0) {
        throw new IOException("Peer declared a negative file size: " + size);
      }
      return size;
    } catch (NumberFormatException e) {
      throw new IOException("Peer declared a malformed file size: " + raw, e);
    }
  }

  /**
   * Returns the port this server listens on.
   *
   * @return the TCP port
   */
  public int getPort() {
    return port;
  }
}
