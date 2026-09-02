package org.example.app.net;

/**
 * Body of the {@link MessageType#ERROR} frame: a machine-readable code and a
 * human-readable explanation of why the peer refused the last request.
 *
 * @author Jamal
 */
public class ErrorMessage {

  private String code;
  private String message;

  /** Required by Gson for deserialization. */
  public ErrorMessage() {
  }

  /**
   * Creates an error body.
   *
   * @param code    a short stable code such as {@code NOT_FOUND} or {@code AUTH_FAILED}
   * @param message a human-readable explanation
   */
  public ErrorMessage(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * Returns the error code.
   *
   * @return the code, or {@code null} if the peer omitted it
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns the explanation.
   *
   * @return the message, or {@code null} if the peer omitted it
   */
  public String getMessage() {
    return message;
  }
}
