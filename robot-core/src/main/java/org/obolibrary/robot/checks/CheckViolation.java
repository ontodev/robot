package org.obolibrary.robot.checks;

/**
 * Common methods for all objects representing an instance of a violation of an ontology check
 *
 * @author cjm
 */
public interface CheckViolation {

  /** @return number between 0 and 5 */
  public int getSeverity();
  
  public String getType();
  
  public String getDescription();
}
