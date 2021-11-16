package org.obolibrary.robot.reason;

import java.util.Set;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredObjectPropertyAxiomGenerator;

/**
 * An InferredAxiomGenerator which returns both direct and indirect inferred object property range
 * axioms.
 */
public class InferredObjectPropertyRangeAxiomGenerator
    extends InferredObjectPropertyAxiomGenerator<OWLObjectPropertyRangeAxiom> {

  @Override
  protected void addAxioms(
      @Nonnull OWLObjectProperty entity,
      @Nonnull OWLReasoner reasoner,
      @Nonnull OWLDataFactory dataFactory,
      @Nonnull Set<OWLObjectPropertyRangeAxiom> result,
      @Nonnull Set<OWLObjectPropertyExpression> nonSimpleProperties) {
    for (OWLClass range : reasoner.getObjectPropertyRanges(entity, true).getFlattened()) {
      if (!range.equals(dataFactory.getOWLThing())) {
        result.add(dataFactory.getOWLObjectPropertyRangeAxiom(entity, range));
      }
    }
  }

  @Override
  public String getLabel() {
    return "Inferred object property range axioms";
  }
}
