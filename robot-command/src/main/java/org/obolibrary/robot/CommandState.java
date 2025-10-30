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

  /** The path to the ontology. */
  private String ontologyPath = null;

  /** The path to the catalog. */
  private String catalogPath = null;

  /**
   * Get the catalog path.
   *
   * @return the catalog path
   */
  public String getCatalogPath() {
    return catalogPath;
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
   * Get the ontology path.
   *
   * @return the ontology path
   */
  public String getOntologyPath() {
    return ontologyPath;
  }

  /**
   * Set the catalog path.
   *
   * @param catalogPath the catalog to use
   */
  public void setCatalogPath(String catalogPath) {
    this.catalogPath = catalogPath;
  }

  /**
   * Set the ontology.
   *
   * @param ontology the ontology to store
   */
  public void setOntology(OWLOntology ontology) {
    this.ontology = ontology;
  }

  /**
   * Set the ontology path.
   *
   * @param ontologyPath the path to the ontology
   */
  public void setOntologyPath(String ontologyPath) {
    this.ontologyPath = ontologyPath;
  }
}
