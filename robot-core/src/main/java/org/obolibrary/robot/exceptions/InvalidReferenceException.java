package org.obolibrary.robot.exceptions;

import java.util.Set;
import org.obolibrary.robot.checks.InvalidReferenceViolation;

/**
 * Ontology contains unsatisfiable classes.
 *
 * @author cjm
 */
public class InvalidReferenceException extends OntologyStructureException {

  /** */
  private static final long serialVersionUID = -6157565029337883652L;

  /** @param violations set of invalid reference violations */
  public InvalidReferenceException(Set<InvalidReferenceViolation> violations) {
    // TODO Auto-generated constructor stub
  }
}
