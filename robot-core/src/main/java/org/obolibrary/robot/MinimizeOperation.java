package org.obolibrary.robot;

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

    boolean cont = true;
    while (cont) {
      cont = continueMinimizing(ontology, threshold, precious);
    }

    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.addAxioms(
        ontology, RelatedObjectsHelper.spanGaps(copy, OntologyHelper.getObjects(ontology)));
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
      if (subClasses.size() > threshold && !cannotDelete && !precious.contains(c.getIRI())) {
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
