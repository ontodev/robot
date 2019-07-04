package org.obolibrary.robot.exceptions;

/** Template column cannot be parsed. */
public class ColumnException extends Exception {
  private static final long serialVersionUID = -2799779465303691943L;

  /**
   * Throw new ColumnException with message.
   *
   * @param s message
   */
  public ColumnException(String s) {
    super(s);
  }

  /**
   * Throw new ColumnException with message and exception cause.
   *
   * @param s message
   * @param e cause
   */
  public ColumnException(String s, Exception e) {
    super(s, e);
  }
}
