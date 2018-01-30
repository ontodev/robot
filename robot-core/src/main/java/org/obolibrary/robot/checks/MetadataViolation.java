package org.obolibrary.robot.checks;

public abstract class MetadataViolation extends AbstractCheckViolation implements CheckViolation {

  public MetadataViolation(String desc, int severity) {
    super(desc, severity);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MetadataViolation [getSeverity()=" + getSeverity() + ", getDesc()=" + getDescription() + "]";
  }
}
