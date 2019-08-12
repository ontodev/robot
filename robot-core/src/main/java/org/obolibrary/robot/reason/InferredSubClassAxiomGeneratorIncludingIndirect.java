package org.obolibrary.robot.reason;

import java.util.Set;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredClassAxiomGenerator;

/** An InferredAxiomGenerator which returns both direct and indirect inferred subclass axioms. */
public class InferredSubClassAxiomGeneratorIncludingIndirect
    extends InferredClassAxiomGenerator<OWLSubClassOfAxiom> {

  @Override
  protected void addAxioms(
      @Nonnull OWLClass entity,
      @Nonnull OWLReasoner reasoner,
      @Nonnull OWLDataFactory dataFactory,
      @Nonnull Set<OWLSubClassOfAxiom> result) {
    if (reasoner.isSatisfiable(entity)) {
      for (OWLClass superclass : reasoner.getSuperClasses(entity, false).getFlattened()) {
        result.add(dataFactory.getOWLSubClassOfAxiom(entity, superclass));
      }
    } else {
      result.add(dataFactory.getOWLSubClassOfAxiom(entity, dataFactory.getOWLNothing()));
    }
  }

  @Override
  public String getLabel() {
    return "Subclasses including indirect";
  }
}
