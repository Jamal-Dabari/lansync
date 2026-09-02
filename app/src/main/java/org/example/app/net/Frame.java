package org.example.app.net;

import java.util.Objects;

/**
 * An immutable single unit of the lansync wire protocol: a {@link MessageType}
 * tag paired with its payload bytes.
 *
 * <p>The payload array is held by reference rather than copied. Frames carry file
 * chunks of up to {@value org.example.app.sync.TransferEngine#CHUNK_SIZE} bytes,
 * and defensively copying on both construction and access would double the memory
 * traffic of every transfer. Callers must therefore treat a payload handed to or
 * received from a frame as owned by that frame and never mutate it in place.
 *
 * @author Jamal
 */
public final class Frame {

  private final MessageType type;
  private final byte[] payload;

  /**
   * Creates a frame.
   *
   * @param type    the message type tag; must not be {@code null}
   * @param payload the payload bytes, which become owned by this frame; must not be {@code null}
   * @throws NullPointerException if either argument is {@code null}
   */
  public Frame(MessageType type, byte[] payload) {
    this.type = Objects.requireNonNull(type, "type");
    this.payload = Objects.requireNonNull(payload, "payload");
  }

  /**
   * Returns this frame's message type.
   *
   * @return the message type, never {@code null}
   */
  public MessageType getType() {
    return type;
  }

  /**
   * Returns this frame's payload.
   *
   * @return the payload bytes, never {@code null}; must not be mutated by the caller
   */
  public byte[] getPayload() {
    return payload;
  }

  /**
   * Returns the payload length in bytes.
   *
   * @return the payload length
   */
  public int getPayloadLength() {
    return payload.length;
  }

  /**
   * Returns a diagnostic description. The payload contents are deliberately omitted:
   * frames routinely carry file data and, before decryption, ciphertext.
   *
   * @return a string naming the type and payload length
   */
  @Override
  public String toString() {
    return "Frame[" + type + ", " + payload.length + " bytes]";
  }
}
