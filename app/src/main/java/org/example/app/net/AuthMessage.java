package org.example.app.net;

/**
 * Body of the {@link MessageType#AUTH} and {@link MessageType#AUTH_OK} frames:
 * an HMAC proving possession of the shared passphrase.
 *
 * @author Jamal
 */
public class AuthMessage {

  private String proof;

  /** Required by Gson for deserialization. */
  public AuthMessage() {
  }

  /**
   * Creates an auth body.
   *
   * @param proof the base64-encoded HMAC-SHA256 over the handshake transcript
   */
  public AuthMessage(String proof) {
    this.proof = proof;
  }

  /**
   * Returns the sender's proof.
   *
   * @return the base64-encoded HMAC, or {@code null} if the peer omitted it
   */
  public String getProof() {
    return proof;
  }
}
