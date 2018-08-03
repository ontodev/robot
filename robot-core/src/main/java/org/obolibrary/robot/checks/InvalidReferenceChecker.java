package org.obolibrary.robot.checks;

import java.util.HashSet;
import java.util.Set;
import org.obolibrary.robot.checks.InvalidReferenceViolation.Category;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Checks to determine if axioms contain invalid references: to dangling classes or deprecated
 * classes.
 *
 * <p>This is a structural check. To determine the *logical* validity of an axiom reference, robot
 * uses OWL reasoning.
 *
 * <p>See: https://github.com/ontodev/robot/issues/1
 *
 * @author cjm
 */
public class InvalidReferenceChecker {

  /**
   * Checks if entity is dangling.
   *
   * @param ontology the OWLOntology to check
   * @param entity the OWLEntity to check
   * @return true if owlClass has no logical or non-logical axioms
   */
  public static boolean isDangling(OWLOntology ontology, OWLEntity entity) {
    if (entity instanceof OWLClass) {
      OWLClass owlClass = (OWLClass) entity;
      if (ontology.getAxioms(owlClass, Imports.INCLUDED).size() > 0) return false;
      if (ontology.getAnnotationAssertionAxioms(owlClass.getIRI()).size() > 0) return false;
      return true;
    }
    if (entity instanceof OWLObjectProperty) {
      OWLObjectProperty owlProperty = (OWLObjectProperty) entity;
      if (ontology.getAxioms(owlProperty, Imports.INCLUDED).size() > 0) return false;
      if (ontology.getAnnotationAssertionAxioms(owlProperty.getIRI()).size() > 0) return false;
      return true;
    }
    return false;
  }

  /**
   * Checks if entity is deprecated.
   *
   * @param ontology the OWLOntology to check
   * @param entity the OWLEntity to check
   * @return true if entity is deprecated
   */
  public static boolean isDeprecated(OWLOntology ontology, OWLEntity entity) {
    for (OWLOntology o : ontology.getImportsClosure()) {
      for (OWLAnnotationAssertionAxiom a : o.getAnnotationAssertionAxioms(entity.getIRI())) {
        if (a.isDeprecatedIRIAssertion()) {
          if (a.getValue().asLiteral().get().parseBoolean()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Finds axioms that reference a deprecated or dangling entity.
   *
   * <p>Declaration axioms that reference a deprecated class do not count.
   *
   * <p>Note that this does not count the value field of an annotation assertion, since these
   * reference IRIs and not entities
   *
   * @param ontology the OWLOntology to check
   * @param axioms set of OWLAxioms to check
   * @param ignoreDangling if true, ignore dangling entities
   * @return all violations
   */
  public static Set<InvalidReferenceViolation> getInvalidReferenceViolations(
      OWLOntology ontology, Set<OWLAxiom> axioms, boolean ignoreDangling) {
    Set<InvalidReferenceViolation> violations = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      for (OWLEntity e : axiom.getSignature()) {
        if (!ignoreDangling && isDangling(ontology, e)) {
          violations.add(InvalidReferenceViolation.create(axiom, e, Category.DANGLING));
        }
        if (isDeprecated(ontology, e)) {
          if (!(axiom instanceof OWLDeclarationAxiom)) {
            violations.add(InvalidReferenceViolation.create(axiom, e, Category.DEPRECATED));
          }
        }
      }
    }
    return violations;
  }

  /**
   * @param ontology the OWLOntology to check
   * @param ignoreDangling boolean to ignore dangling classes
   * @return all violations in ontology
   */
  public static Set<InvalidReferenceViolation> getInvalidReferenceViolations(
      OWLOntology ontology, boolean ignoreDangling) {
    return getInvalidReferenceViolations(
        ontology, ontology.getAxioms(Imports.INCLUDED), ignoreDangling);
  }

  /**
   * Checks an import module. If the base ontology refers to deprecated entities in the import
   * module this constitutes a violation
   *
   * @param importModule the OWLOntology import to check
   * @param baseOntology the base OWLOntology to check
   * @return all deprecation-reference violations in baseOntology with respect to import module
   */
  public static Set<InvalidReferenceViolation> checkImportModule(
      OWLOntology importModule, OWLOntology baseOntology) {
    return getInvalidReferenceViolations(
        importModule, baseOntology.getAxioms(Imports.INCLUDED), true);
  }
}
