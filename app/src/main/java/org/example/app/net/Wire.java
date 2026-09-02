package org.example.app.net;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Protocol-wide constants and JSON codec for lansync message bodies.
 *
 * <p>The {@link Gson} instance is kept private so every caller goes through
 * {@link #toBytes(Object)} and {@link #fromBytes(byte[], Class)}, which fix the
 * charset in one place rather than at each call site.
 *
 * @author Jamal
 */
public final class Wire {

  /**
   * The protocol version this build speaks. Peers reporting a different version
   * are rejected during the handshake rather than allowed to fail later.
   */
  public static final int PROTOCOL_VERSION = 1;

  /** Gson is immutable after construction and safe to share across threads. */
  private static final Gson GSON = new Gson();

  private Wire() {
    throw new AssertionError("No instances of Wire");
  }

  /**
   * Serializes a message body to UTF-8 JSON bytes.
   *
   * @param body the message body
   * @return the encoded payload
   */
  public static byte[] toBytes(Object body) {
    return GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Deserializes a message body from UTF-8 JSON bytes.
   *
   * @param <T>      the body type
   * @param payload  the encoded payload
   * @param bodyType the class to deserialize into
   * @return the decoded body
   * @throws JsonSyntaxException if the payload is not valid JSON for that type
   */
  public static <T> T fromBytes(byte[] payload, Class<T> bodyType) {
    return GSON.fromJson(new String(payload, StandardCharsets.UTF_8), bodyType);
  }

  /**
   * Serializes a message body to a JSON string.
   *
   * @param body the message body
   * @return the JSON text
   */
  public static String toJson(Object body) {
    return GSON.toJson(body);
  }
}
