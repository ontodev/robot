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
   * Represents an error in the previous chained command. If there was an error, this should be
   * true.
   */
  private boolean hadAnError;

  public CommandState(OWLOntology ontology, boolean hadAnError) {
    this.ontology = ontology;
    this.hadAnError = hadAnError;
  }

  public CommandState() {
    this(null, false);
  }

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

  public boolean hadAnError() {
    return hadAnError;
  }

  /**
   * Sets the flag that indicates the previous command had an error while running.
   *
   * @param hadAnError true if there was an error
   */
  public void setHadError(boolean hadAnError) {
    this.hadAnError = hadAnError;
  }
}
