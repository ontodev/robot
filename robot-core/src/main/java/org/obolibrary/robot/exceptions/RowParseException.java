package org.obolibrary.robot.exceptions;

/** Template row cannot be parsed. */
public class RowParseException extends Exception {
  private static final long serialVersionUID = -646778731149993824L;

  public int rowNum = -1;
  public int colNum = -1;
  public String ruleID;
  public String ruleName;
  public String cellValue;

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

  /**
   * Throw a new RowParseException with message and location.
   *
   * @param s message
   * @param rowNum row number
   * @param colNum column number
   * @param cellValue value of cell with exception
   */
  public RowParseException(String s, int rowNum, int colNum, String cellValue) {
    super(s);
    this.rowNum = rowNum;
    this.colNum = colNum;
    this.cellValue = cellValue;

    try {
      this.ruleName = s.substring(s.indexOf("#") + 1, s.indexOf("ERROR") + 5).trim().toLowerCase();
    } catch (Exception e) {
      this.ruleName = "";
    }

    this.ruleID = "ROBOT-template:" + this.ruleName.replace(" ", "-");
  }
}
