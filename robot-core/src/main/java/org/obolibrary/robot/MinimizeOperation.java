package org.obolibrary.robot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class MinimizeOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MinimizeOperation.class);

  /**
   * @param ontology
   * @param threshold
   */
  public static void minimize(OWLOntology ontology, int threshold, Set<IRI> precious)
      throws OWLOntologyCreationException {
    OWLOntology copy =
        OWLManager.createOWLOntologyManager().copyOntology(ontology, OntologyCopy.DEEP);
    logger.debug("Classes before minimizing: " + ontology.getClassesInSignature().size());

    minimizeAll(ontology, threshold, precious);

    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.addAxioms(
        ontology, RelatedObjectsHelper.spanGaps(copy, OntologyHelper.getObjects(ontology)));
    logger.debug("Classes after minimizing: " + ontology.getClassesInSignature().size());
  }

  /**
   * @param ontology
   * @param threshold
   * @param precious
   */
  private static void minimizeAll(OWLOntology ontology, int threshold, Set<IRI> precious) {
    Set<OWLClass> classes = ontology.getClassesInSignature();
    Set<OWLObject> remove = new HashSet<>();

    for (OWLClass cls : classes) {
      Collection<OWLClassExpression> subclasses = EntitySearcher.getSubClasses(cls, ontology);
      for (OWLClassExpression expr : subclasses) {
        if (expr.isAnonymous()) {
          continue;
        }
        if (!subclasses.isEmpty()
            && subclasses.size() <= threshold
            && !precious.contains(cls.getIRI())) {
          // If the class has subclasses, but less (or equal to) subclasses
          // than the threshold add it to the set of classes to be removed
          remove.add(cls);
        }
      }
    }

    if (!remove.isEmpty()) {
      Set<OWLAxiom> axiomsToRemove = RelatedObjectsHelper.getPartialAxioms(ontology, remove, null);
      OWLOntologyManager manager = ontology.getOWLOntologyManager();
      manager.removeAxioms(ontology, axiomsToRemove);
    }
  }

  /**
   * @param ontology
   * @param threshold
   * @return
   */
  private static boolean continueMinimizing(
      OWLOntology ontology, int threshold, Set<IRI> precious) {
    // Find any classes that have threshold number of children
    Set<OWLObject> classes = new HashSet<>();

    // For each class in the ontology, check if it has subclasses
    // If it has less than the threshold number of subclasses
    // And the subclasses have NO children
    // *THAT* class can be dissolved
    for (OWLClass c : ontology.getClassesInSignature()) {
      boolean cannotDelete = false;
      Set<OWLClass> subClasses = new HashSet<>();
      for (OWLClassExpression expr : EntitySearcher.getSubClasses(c, ontology)) {
        if (!expr.isAnonymous()) {
          OWLClass subSubClass = expr.asOWLClass();
          subClasses.add(subSubClass);
        }
      }
      for (OWLClass sc : subClasses) {
        if (!EntitySearcher.getSubClasses(sc, ontology).isEmpty()) {
          cannotDelete = true;
        }
      }
      if (subClasses.size() <= threshold && !cannotDelete && !precious.contains(c.getIRI())) {
        classes.add(c);
      }
    }

    Set<Class<? extends OWLAxiom>> axTypes = new HashSet<>();
    axTypes.add(OWLAxiom.class);
    Set<OWLAxiom> axiomsToRemove =
        RelatedObjectsHelper.getPartialAxioms(ontology, classes, axTypes);

    logger.debug(String.format("Removing %d classes", classes.size()));

    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxioms(ontology, axiomsToRemove);

    return !axiomsToRemove.isEmpty();
  }
}
