package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimize a class hierarchy.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Rebecca Jackson</a>
 */
public class MinimizeOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MinimizeOperation.class);

  /**
   * Given an ontology, a threshold, and a set of precious IRIs (or empty set), minimize the input
   * ontology's class hierarchy based on the threshold. The threshold is the minimum number of child
   * classes that an intermediate class should have. Any intermediate class that has less than the
   * threshold number of children will be removed and its children will become children of the next
   * level up. Bottom-level and top-level classes are not removed. Any class with an IRI in the
   * precious set is not removed.
   *
   * @param ontology OWLOntology to minimize
   * @param threshold minimum number of child classes
   * @param precious set of IRIs to keep
   */
  public static void minimize(OWLOntology ontology, int threshold, Set<IRI> precious)
      throws OWLOntologyCreationException {
    OWLOntology copy =
        OWLManager.createOWLOntologyManager().copyOntology(ontology, OntologyCopy.DEEP);
    logger.debug("Classes before minimizing: " + ontology.getClassesInSignature().size());

    Set<OWLObject> removeClasses = getClassesToRemove(ontology, threshold, precious);

    // Remove axioms based on classes
    // Get all axioms that involve these classes
    // Continue to get remove classes until there's no more to remove
    while (!removeClasses.isEmpty()) {
      Set<OWLAxiom> axiomsToRemove =
          RelatedObjectsHelper.getPartialAxioms(ontology, removeClasses, null);
      OWLOntologyManager manager = ontology.getOWLOntologyManager();
      manager.removeAxioms(ontology, axiomsToRemove);
      // Span gaps to maintain hierarchy
      manager.addAxioms(
          ontology, RelatedObjectsHelper.spanGaps(copy, OntologyHelper.getObjects(ontology)));
      // Repeat until there's no more to remove
      removeClasses = getClassesToRemove(ontology, threshold, precious);
    }
    logger.debug("Classes after minimizing: " + ontology.getClassesInSignature().size());
  }

  /**
   * Given an ontology, a threshold, and a set of precious IRIs (or empty set), return the classes
   * to remove to minimize the class hierarchy. Top-level and bottom-level classes are not removed.
   * Any class with a precious IRI is not removed. Any class with a number of named subclasses that
   * is less than the threshold will be removed.
   *
   * @param ontology OWLOntology to minimize
   * @param threshold minimum number of child classes
   * @param precious set of IRIs to keep
   */
  private static Set<OWLObject> getClassesToRemove(
      OWLOntology ontology, int threshold, Set<IRI> precious) {
    Set<OWLClass> classes = ontology.getClassesInSignature();
    Set<OWLObject> remove = new HashSet<>();

    for (OWLClass cls : classes) {
      if (cls.isOWLThing() || precious.contains(cls.getIRI())) {
        // Ignore if the IRI is in precious or is OWL Thing
        System.out.println(cls.getIRI());
        continue;
      }

      // Check for superclasses
      Set<OWLSubClassOfAxiom> superAxioms = ontology.getSubClassAxiomsForSubClass(cls);
      boolean hasNamedSuperclass = false;
      for (OWLSubClassOfAxiom superAx : superAxioms) {
        OWLClassExpression expr = superAx.getSuperClass();
        if (!expr.isAnonymous() && !expr.asOWLClass().isOWLThing()) {
          hasNamedSuperclass = true;
          break;
        }
      }

      if (!hasNamedSuperclass) {
        // Also ignore if there are no named superclasses
        // Or just no superclasses in general
        // This means it is directly placed under owl:Thing
        continue;
      }

      Set<OWLSubClassOfAxiom> subAxioms = ontology.getSubClassAxiomsForSuperClass(cls);
      int scCount = 0;
      for (OWLSubClassOfAxiom subAx : subAxioms) {
        OWLClassExpression expr = subAx.getSubClass();
        if (!expr.isAnonymous()) {
          // Only count the named subclasses
          scCount++;
        }
      }

      if (scCount != 0 && scCount < threshold) {
        // If the class has subclasses, but LESS subclasses than the threshold,
        // add it to the set of classes to be removed
        remove.add(cls);
      }
    }

    return remove;
  }
}
