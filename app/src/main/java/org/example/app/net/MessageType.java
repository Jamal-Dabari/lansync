package org.example.app.net;

public enum MessageType {
  HELLO((byte) 0x01),
  CHALLENGE((byte) 0x02),
  AUTH((byte) 0x03),
  AUTH_OK((byte) 0x04),
  MANIFEST((byte) 0x10),
  FILE_REQUEST((byte) 0x20),
  FILE_HEADER((byte) 0x21),
  FILE_CHUNK((byte) 0x22),
  FILE_END((byte) 0x23),
  ERROR((byte) 0xF0),
  BYE((byte) 0xFF);

  public final byte code;

  MessageType(byte code) {
    this.code = code;
  }

  public static MessageType fromCode(byte code) {
    for (MessageType t : values()) if (t.code == code) return t;
    throw new IllegalArgumentException("Unknown message type: 0x" + Integer.toHexString(code & 0xFF));
  }
}
