package org.example.app.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import org.example.app.util.Config;

/**
 * Sends a single file to a {@link TransferServer} over a plain TCP socket.
 *
 * <p>This is a debugging aid from an early milestone and performs no
 * authentication or encryption. Real synchronisation goes through
 * {@link org.example.app.sync.SyncSession}, which authenticates the peer and
 * encrypts every frame.
 *
 * @author Jamal
 * @see TransferServer
 * @see org.example.app.sync.SyncSession
 */
public final class TransferClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int BUFFER_SIZE = 64 * 1024;

  private final String host;
  private final int port;

  /**
   * Creates a client targeting the default lansync port.
   *
   * @param host the peer's hostname or IP address; must not be {@code null}
   */
  public TransferClient(String host) {
    this(host, Config.DEFAULT_PORT);
  }

  /**
   * Creates a client targeting an explicit port.
   *
   * @param host the peer's hostname or IP address; must not be {@code null}
   * @param port the peer's TCP port
   * @throws NullPointerException     if {@code host} is {@code null}
   * @throws IllegalArgumentException if {@code port} is outside 1-65535
   */
  public TransferClient(String host, int port) {
    this.host = Objects.requireNonNull(host, "host");
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port out of range: " + port);
    }
    this.port = port;
  }

  /**
   * Connects to the peer, sends the file, and closes the connection.
   *
   * <p>The wire form is three parts: the file name, the decimal file size, then
   * the contents as a sequence of length-prefixed chunks.
   *
   * @param file the file to send; must be an existing regular file
   * @throws IOException              if the connection or transfer fails
   * @throws IllegalArgumentException if {@code file} is not a regular file
   * @throws NullPointerException     if {@code file} is {@code null}
   */
  public void sendFile(File file) throws IOException {
    Objects.requireNonNull(file, "file");
    if (!file.isFile()) {
      throw new IllegalArgumentException("Not a regular file: " + file);
    }

    // try-with-resources so the socket closes even when the transfer fails midway.
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
      OutputStream out = socket.getOutputStream();

      MessageProtocol.sendMessage(out, file.getName().getBytes(StandardCharsets.UTF_8));
      MessageProtocol.sendMessage(out,
          String.valueOf(file.length()).getBytes(StandardCharsets.UTF_8));

      try (InputStream in = Files.newInputStream(file.toPath())) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) > 0) {
          MessageProtocol.sendMessage(out,
              read == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, read));
        }
      }
    }
  }

  /**
   * Returns the target host.
   *
   * @return the peer hostname or IP address
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns the target port.
   *
   * @return the peer TCP port
   */
  public int getPort() {
    return port;
  }
}
