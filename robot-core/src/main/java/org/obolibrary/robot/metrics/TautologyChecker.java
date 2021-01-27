package org.obolibrary.robot.metrics;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

@SuppressWarnings("unused")
public class TautologyChecker {

  /**
   * @param ax the axiom to be checked
   * @return true if the axiom is a tautology
   */
  public static boolean isTautology(OWLAxiom ax) {
    OWLOntologyManager emptyman = OWLManager.createOWLOntologyManager();
    OWLOntology emptyo;
    try {
      emptyo = emptyman.createOntology();
      OWLReasonerFactory fac = new org.semanticweb.HermiT.ReasonerFactory();
      OWLReasoner reasoner = fac.createReasoner(emptyo);
      return reasoner.isEntailed(ax);
    } catch (OWLOntologyCreationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return false;
  }

  /**
   * @param axioms the axioms to be checked
   * @return all those axioms that are tautologies
   * @throws OWLOntologyCreationException on problem creating reasoner
   */
  public static Set<OWLAxiom> getTautologies(Set<OWLAxiom> axioms)
      throws OWLOntologyCreationException {
    Set<OWLAxiom> delSet = new HashSet<>();
    OWLReasoner r =
        new org.semanticweb.HermiT.ReasonerFactory()
            .createReasoner(OWLManager.createOWLOntologyManager().createOntology());
    for (OWLAxiom ax : axioms) {
      if (r.isEntailed(ax)) {
        delSet.add(ax);
      }
    }
    return delSet;
  }

  // Filtering of axioms - remove declarations and tautologies. At the moment
  // tautologies are
  // just defined w.r.t classification. Later versions will require something
  // more complex.

  /**
   * @param axioms the axioms to be checked
   * @return all those axioms that are tautologies (syntactic check, incomplete!)
   */
  public static Set<OWLAxiom> getTautologySyntactic(Set<OWLAxiom> axioms) {
    Set<OWLAxiom> filter = new HashSet<>();
    for (OWLAxiom ax : axioms) {
      if (ax.isOfType(AxiomType.DECLARATION)) {
        filter.add(ax);
      } else {
        // Sanity check
        if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
          if (isTautology((OWLSubClassOfAxiom) ax)) {
            filter.add(ax);
          }
        }
      }
    }
    axioms.removeAll(filter);
    return axioms;
  }

  private static Boolean isTautology(OWLSubClassOfAxiom ax) {
    return ax.getSubClass().isOWLNothing() || ax.getSuperClass().isOWLThing();
  }
}
