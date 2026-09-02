package org.example.app.net;

/**
 * Body of the {@link MessageType#HELLO} and {@link MessageType#CHALLENGE} frames:
 * the sender's identity plus a fresh nonce.
 *
 * <p>The nonce makes each handshake transcript unique, so a recorded handshake
 * cannot be replayed against either peer.
 *
 * @author Jamal
 */
public class HelloMessage {

  private int version = Wire.PROTOCOL_VERSION;
  private String deviceId;
  private String nonce;

  /** Required by Gson for deserialization. */
  public HelloMessage() {
  }

  /**
   * Creates a hello body for this build's protocol version.
   *
   * @param deviceId the sender's device ID
   * @param nonce    a base64-encoded random nonce
   */
  public HelloMessage(String deviceId, String nonce) {
    this.deviceId = deviceId;
    this.nonce = nonce;
  }

  /**
   * Returns the protocol version the sender speaks.
   *
   * @return the protocol version
   */
  public int getVersion() {
    return version;
  }

  /**
   * Returns the sender's device ID.
   *
   * @return the device ID, or {@code null} if the peer omitted it
   */
  public String getDeviceId() {
    return deviceId;
  }

  /**
   * Returns the sender's handshake nonce.
   *
   * @return the base64-encoded nonce, or {@code null} if the peer omitted it
   */
  public String getNonce() {
    return nonce;
  }
}
