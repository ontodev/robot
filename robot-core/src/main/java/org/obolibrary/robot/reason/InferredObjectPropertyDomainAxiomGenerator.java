package org.obolibrary.robot.reason;

import java.util.Set;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredObjectPropertyAxiomGenerator;

/**
 * An InferredAxiomGenerator which returns both direct and indirect inferred object property domain
 * axioms.
 */
public class InferredObjectPropertyDomainAxiomGenerator
    extends InferredObjectPropertyAxiomGenerator<OWLObjectPropertyDomainAxiom> {

  @Override
  protected void addAxioms(
      @Nonnull OWLObjectProperty entity,
      @Nonnull OWLReasoner reasoner,
      @Nonnull OWLDataFactory dataFactory,
      @Nonnull Set<OWLObjectPropertyDomainAxiom> result,
      @Nonnull Set<OWLObjectPropertyExpression> nonSimpleProperties) {
    for (OWLClass domain : reasoner.getObjectPropertyRanges(entity, false).getFlattened()) {
      result.add(dataFactory.getOWLObjectPropertyDomainAxiom(entity, domain));
    }
  }

  @Override
  public String getLabel() {
    return "Inferred Object Property Domain axioms";
  }
}
