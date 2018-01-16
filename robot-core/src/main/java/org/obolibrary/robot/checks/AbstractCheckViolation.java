package org.obolibrary.robot.checks;

public abstract class AbstractCheckViolation implements CheckViolation {
  private final String description;
  private final int severity;

  public AbstractCheckViolation(String desc, int severity) {
    super();
    this.description = desc;
    this.severity = severity;
  }
  /** @return the type */
  public String getType() {
    return "TBD";
  }

  /** @return the description */
  public String getDescription() {
    return description;
  }
  /** @return the severity */
  public int getSeverity() {
    return severity;
  }
}
