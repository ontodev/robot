package org.obolibrary.robot.exceptions;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * Ontology contains unsatisfiable classes, properties or inconsistencies.
 *
 * @author cjm
 */
public class OntologyLogicException extends Exception {

  /** */
  private static final long serialVersionUID = 8211835056287159708L;

  /**
   * Throw a new OntologyLogicException with string message.
   *
   * @param s message
   */
  public OntologyLogicException(String s) {
    super(s);
  }

  /**
   * Throw a new OntologyLogicException with a set of unsatisfiable classes or properties.
   *
   * @param unsatisfiable set of unsatisfiable classes or properties
   */
  public OntologyLogicException(Set<? extends OWLEntity> unsatisfiable) {}

  /** Default constructor */
  public OntologyLogicException() {}
}
