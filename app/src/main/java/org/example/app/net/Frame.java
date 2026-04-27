package org.example.app.net;

public class Frame {

  private final MessageType type;
  private final byte[] payload;

  public Frame(MessageType type, byte[] payload) {
    this.type = type;
    this.payload = payload;
  }

  public MessageType getType() { return type; }
  public byte[] getPayload() { return payload; }
}
