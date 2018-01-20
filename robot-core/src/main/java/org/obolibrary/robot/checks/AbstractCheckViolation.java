package org.obolibrary.robot.checks;

public abstract class AbstractCheckViolation implements CheckViolation {
  private final String description;
  private final int severity;
  private final String axiomLabel;

  public AbstractCheckViolation(String desc, int severity) {
    super();
    this.description = desc;
    this.severity = severity;
    axiomLabel = null;
  }
  
  
 

  public AbstractCheckViolation(String axiomLabel, String description, int severity) {
    super();
    this.description = description;
    this.severity = severity;
    this.axiomLabel = axiomLabel;
  }





  /** @return the description */
  public String getDescription() {
    return description;
  }
  /** @return the severity */
  public int getSeverity() {
    return severity;
  }




  /**
   * @return the axiomLabel
   */
  public String getAxiomLabel() {
  return axiomLabel;}
  
  
}
