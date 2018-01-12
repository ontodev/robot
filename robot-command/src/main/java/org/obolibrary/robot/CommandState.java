package org.obolibrary.robot;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A simple state container for communicating between Commands.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandState {
  /** An ontology to pass between commands. */
  private OWLOntology ontology = null;

  /**
   * Get the ontology (not a copy).
   *
   * @return the ontology
   */
  public OWLOntology getOntology() {
    return ontology;
  }

  /**
   * Set the ontology.
   *
   * @param ontology the ontology to store
   */
  public void setOntology(OWLOntology ontology) {
    this.ontology = ontology;
  }
}
