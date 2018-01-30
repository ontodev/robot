package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLClass;

public class CURIEViolation extends AbstractCheckViolation implements CheckViolation {

  public CURIEViolation(String axiomLabel, String name, int severity) {
    super(axiomLabel, name, severity);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MetadataViolation [getSeverity()=" + getSeverity() + ", getDesc()=" + getDescription() + "]";
  }
  

  /** @return the type */
  public String getType() {
    return "CURIE violation";
  }
}
