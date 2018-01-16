package org.obolibrary.robot.checks;

public interface Checker {

  /** @return name of the checker */
  public String getName();

  /** @return number of instances of the check being violated */
  public int getNumberOfViolations();
}
