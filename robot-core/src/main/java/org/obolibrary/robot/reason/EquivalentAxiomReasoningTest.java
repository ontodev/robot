package org.obolibrary.robot.reason;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Created by edouglass on 5/9/17.
 *
 * <p>Performs a test on an OWLAxiom and states if it "fails". This will be up to the implementation
 */
public interface EquivalentAxiomReasoningTest {

  /**
   * Returns true if the given axiom fails the type of Test this Reasoning Test is. For example, if
   * no equivalent axioms are allowed, any axiom here should return false.
   *
   * @param axiom OWLAxiom
   * @return boolean
   */
  boolean fails(OWLAxiom axiom);
}
