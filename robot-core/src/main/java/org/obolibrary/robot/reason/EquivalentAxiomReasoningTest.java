package org.obolibrary.robot.reason;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Created by edouglass on 5/9/17.
 */
public interface EquivalentAxiomReasoningTest {

    /**
     * Returns true if the given axiom fails the type of Test this Reasoning Test is. For example,
     * if no equivalent axioms are allowed, any axiom here should return false.
     * @return
     */
    boolean fails(OWLAxiom axiom);

}
