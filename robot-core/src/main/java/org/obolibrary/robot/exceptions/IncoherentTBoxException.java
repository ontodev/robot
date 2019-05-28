package org.obolibrary.robot.exceptions;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLClass;

/**
 * Ontology contains unsatisfiable classes.
 *
 * @author cjm
 */
public class IncoherentTBoxException extends OntologyLogicException {

  /** */
  private static final long serialVersionUID = -6157565029337883652L;

  /** @param unsatisfiableClasses list of unsat classes */
  public IncoherentTBoxException(Set<OWLClass> unsatisfiableClasses) {
    // TODO Auto-generated constructor stub
    super(unsatisfiableClasses);
  }
}
