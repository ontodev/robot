package org.obolibrary.robot.exceptions;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * Ontology contains unsatisfiable properties.
 *
 * @author cjm
 */
public class IncoherentRBoxException extends OntologyLogicException {

  /** */
  private static final long serialVersionUID = 2608045757804483323L;

  /** @param unsatisfiableProperties The unsatisfiable properties. */
  public IncoherentRBoxException(Set<OWLObjectProperty> unsatisfiableProperties) {
    super(unsatisfiableProperties);
  }
}
