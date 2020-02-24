package org.obolibrary.robot.reason;

import java.util.Set;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredObjectPropertyAxiomGenerator;

/**
 * An InferredAxiomGenerator which returns both direct and indirect inferred subobjectproperty
 * axioms.
 */
public class InferredSubObjectPropertyAxiomGeneratorIncludingIndirect
    extends InferredObjectPropertyAxiomGenerator<OWLSubObjectPropertyOfAxiom> {

  @Override
  protected void addAxioms(
      @Nonnull OWLObjectProperty entity,
      @Nonnull OWLReasoner reasoner,
      @Nonnull OWLDataFactory dataFactory,
      @Nonnull Set<OWLSubObjectPropertyOfAxiom> result,
      @Nonnull Set<OWLObjectPropertyExpression> nonSimpleProperties) {
    for (OWLObjectPropertyExpression prop :
        reasoner.getSuperObjectProperties(entity, false).getFlattened()) {
      result.add(dataFactory.getOWLSubObjectPropertyOfAxiom(entity, prop));
    }
  }

  @Override
  public String getLabel() {
    return "Sub object properties including indirect";
  }
}
