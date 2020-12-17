package org.obolibrary.robot.metrics;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyCycleDetector {

  /*
   * Adapted from Dmitry Tsarkov
   */

  /*
   * Sound, but maybe incomplete (may miss some cycles).
   */

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(OntologyCycleDetector.class);

  /**
   * @param ontology the ontology to be checked for cycles
   * @param includeImports whether imports closure should be considered
   * @return True if there is certainly a cycle, otherwise False.
   */
  public static boolean containsCycle(OWLOntology ontology, Imports includeImports) {
    final Set<OWLClass> Classes = ontology.getClassesInSignature(includeImports);
    for (OWLClass Class : Classes) {
      Queue<OWLClass> Queue = new LinkedList<>();
      Set<OWLClass> Visited = new HashSet<>();
      Queue.add(Class);
      // System.out.println("DeCycler.processOntology() Class " + Class);
      while (Queue.peek() != null) {
        OWLClass current = Queue.poll();
        if (current == Class && !Queue.isEmpty()) {
          LOGGER.warn(
              "Cycle detector encountered a potential problem - results may be unreliable.");
        }
        // no need to check the same entry more than once
        if (Visited.contains(current)) continue;
        Visited.add(current);

        Set<OWLAxiom> Defs = getReferencingAxioms(ontology, current, includeImports);
        Set<OWLClassExpression> ces = getDefinition(current, Defs);

        // no definition for a class -- fine!
        OWLClassExpression C =
            ontology.getOWLOntologyManager().getOWLDataFactory().getOWLObjectIntersectionOf(ces);

        // check the signature of that axiom
        Set<OWLClass> AxSig = C.getClassesInSignature();

        if (AxSig.contains(Class)) {
          return true;
        } else {
          AxSig.remove(current);
          Queue.addAll(AxSig);
        }
      }
    }
    return false;
  }

  private static Set<OWLAxiom> getReferencingAxioms(
      OWLOntology ontology, OWLClass current, Imports includeImports) {
    Set<OWLAxiom> refs = new HashSet<>();
    if (includeImports == Imports.INCLUDED) {
      for (OWLOntology o : ontology.getImportsClosure()) {
        refs.addAll(o.getReferencingAxioms(current));
      }
    } else {
      refs.addAll(ontology.getReferencingAxioms(current));
    }
    return refs;
  }

  private static Set<OWLClassExpression> getDescription(OWLClass current, OWLAxiom ax) {
    Set<OWLClassExpression> ces = new HashSet<>();
    if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
      OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) ax;
      return Collections.singleton(axiom.getSuperClass());
    } else if (ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
      OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) ax;
      ces.addAll(axiom.getClassExpressions());
      ces.remove(current);
    }
    return ces;
  }

  private static Set<OWLClassExpression> getDefinition(OWLClass current, Set<OWLAxiom> defs) {
    Set<OWLClassExpression> sce = new HashSet<>();
    for (OWLAxiom ax : defs) {
      if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
        OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) ax;
        if (current.equals(axiom.getSubClass())) {
          sce.addAll(getDescription(current, ax));
        }
      } else if (ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
        OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) ax;
        for (OWLClassExpression cl : axiom.getClassExpressions()) {
          if (!cl.isAnonymous() && current.equals(cl.asOWLClass())) {
            sce.addAll(getDescription(current, ax));
          }
        }
      }
    }
    return sce;
  }
}
