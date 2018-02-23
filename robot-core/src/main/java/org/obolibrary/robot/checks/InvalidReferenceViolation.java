package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * Represents a reference violation
 *
 * <p>TODO: we may want to make warnings if a class is slated for deprecation in the future: see
 * https://github.com/information-artifact-ontology/ontology-metadata/issues/22
 *
 * @author cjm
 */
public class InvalidReferenceViolation {

  private final OWLAxiom axiom;
  private final OWLEntity referencedObject;
  private final Category category;

  private InvalidReferenceViolation(OWLAxiom axiom, OWLEntity referencedObject, Category category) {
    super();
    this.axiom = axiom;
    this.referencedObject = referencedObject;
    this.category = category;
  }

  /**
   * @param axiom the OWLAxiom containing invalid reference
   * @param referencedObject the OWLEntity that is dangling or deprecated
   * @param category the Category of violation (deprecated or dangling)
   * @return new instance
   */
  public static InvalidReferenceViolation create(
      OWLAxiom axiom, OWLEntity referencedObject, Category category) {
    return new InvalidReferenceViolation(axiom, referencedObject, category);
  }

  /** @return the axiom that contains the invalid reference */
  public OWLAxiom getAxiom() {
    return axiom;
  }

  /** @return the referencedObject that is either deprecated or dangling */
  public OWLEntity getReferencedObject() {
    return referencedObject;
  }

  /** @return the category of violation (deprecated or dangling) */
  public Category getCategory() {
    return category;
  }

  /**
   * A reference may be invalid for different reasons
   *
   * <p>- the referenced object may be deprecated - the referenced object may be dangling
   *
   * @author cjm
   */
  public enum Category {
    DANGLING,
    DEPRECATED
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    // TODO: pretty-printer
    return "InvalidReferenceViolation [axiom="
        + axiom
        + ", referencedObject="
        + referencedObject
        + ", category="
        + category
        + "]";
  }
}
