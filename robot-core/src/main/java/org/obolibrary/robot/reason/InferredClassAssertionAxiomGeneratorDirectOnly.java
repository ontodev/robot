package org.obolibrary.robot.reason;

import java.util.Set;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredIndividualAxiomGenerator;

/** An InferredAxiomGenerator which returns only direct class assettion axioms. */
public class InferredClassAssertionAxiomGeneratorDirectOnly
    extends InferredIndividualAxiomGenerator<OWLClassAssertionAxiom> {

  @Override
  protected void addAxioms(
      @Nonnull OWLNamedIndividual entity,
      @Nonnull OWLReasoner reasoner,
      @Nonnull OWLDataFactory dataFactory,
      @Nonnull Set<OWLClassAssertionAxiom> result) {
    for (OWLClass type : reasoner.getTypes(entity, true).getFlattened()) {
      result.add(dataFactory.getOWLClassAssertionAxiom(type, entity));
    }
  }

  @Override
  public String getLabel() {
    return "Class assertions (individual direct types)";
  }
}
