package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Represents a reference violation
 * 
 * An axiom is in violation if it references an OWL entity that is either
 * 
 *  - deprecated
 *  - non-existent (dangling) within the current import closure
 *  
 * It is important for the ontology maintainer to be aware of these as they
 * can cause incomplete results
 *
 * <p>TODO: we may want to make warnings if a class is slated for deprecation in the future: see
 * https://github.com/information-artifact-ontology/ontology-metadata/issues/22
 *
 * @author cjm
 */
public class InvalidReferenceViolation implements CheckViolation {

  @JsonSerialize(using = ToStringSerializer.class)
  private final OWLAxiom axiom;
  
  @JsonSerialize(using = ToStringSerializer.class)
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
    /**
     * A dangling reference violation - the axiom points to a class/entity
     * for which there are no axioms ABOUT this class/entity in the imports closure
     * 
     * Here ABOUTS means either a logical axiom with the entity as subject or
     * an annotation axiom with the entity IRI as subject
     */
    DANGLING,
    
    /**
     * Axiom points to a class/entity that has an owl:deprecation axiom
     * 
     * (in the OBO universe, "obsolete" classes all have deprecation axioms)
     */
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

  @Override
  public int getSeverity() {
    if (getCategory().equals(Category.DEPRECATED)) {
      return 2;
    } else {
      return 1;
    }
  }

  @Override
  public String getType() {
    return "reference violation";
  }

  @Override
  public String getDescription() {
    return getAxiom().toString();
  }
}
