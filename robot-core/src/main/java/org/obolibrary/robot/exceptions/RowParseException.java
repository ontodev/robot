package org.obolibrary.robot.exceptions;

/** Template row cannot be parsed. */
public class RowParseException extends Exception {
  private static final long serialVersionUID = -646778731149993824L;

  /**
   * Throw new RowParseException with message.
   *
   * @param s message
   */
  public RowParseException(String s) {
    super(s);
  }

  /**
   * Throw new RowParseException with message amd cause.
   *
   * @param s message
   * @param e cause
   */
  public RowParseException(String s, Exception e) {
    super(s, e);
  }
}
