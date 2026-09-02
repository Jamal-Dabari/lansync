package org.example.app.net;

/**
 * The message types of the lansync wire protocol.
 *
 * <p>Each constant owns the single byte that identifies it on the wire. Codes are
 * grouped by phase: {@code 0x0*} for the handshake, {@code 0x1*} for manifest
 * exchange, {@code 0x2*} for file transfer, and {@code 0xF*} for termination.
 *
 * @author Jamal
 */
public enum MessageType {

  /** Client opens the handshake with its device ID, protocol version and nonce. */
  HELLO((byte) 0x01),
  /** Server answers with its own device ID, protocol version and nonce. */
  CHALLENGE((byte) 0x02),
  /** Client proves possession of the shared passphrase. */
  AUTH((byte) 0x03),
  /** Server accepts and proves possession in return; the session is now encrypted. */
  AUTH_OK((byte) 0x04),
  /** A JSON-serialized {@link org.example.app.manifest.Manifest}. */
  MANIFEST((byte) 0x10),
  /** Asks the peer to send one file by relative path. */
  FILE_REQUEST((byte) 0x20),
  /** Announces the file that the following chunks belong to. */
  FILE_HEADER((byte) 0x21),
  /** One chunk of file data. */
  FILE_CHUNK((byte) 0x22),
  /** Marks the end of the current file's chunk stream. */
  FILE_END((byte) 0x23),
  /** Asks the peer to delete one file by relative path (mirror mode). */
  FILE_DELETE((byte) 0x24),
  /** Reports that the peer refused or could not complete the last request. */
  ERROR((byte) 0xF0),
  /** Graceful close; the sender will write nothing further. */
  BYE((byte) 0xFF);

  private final byte code;

  MessageType(byte code) {
    this.code = code;
  }

  /**
   * Returns the byte that identifies this type on the wire.
   *
   * @return the protocol code
   */
  public byte getCode() {
    return code;
  }

  /**
   * Looks up the type carried by a wire byte.
   *
   * @param code the byte read from a frame header
   * @return the matching message type
   * @throws IllegalArgumentException if no type uses that code
   */
  public static MessageType fromCode(byte code) {
    for (MessageType type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown message type: 0x" + Integer.toHexString(code & 0xFF));
  }
}
