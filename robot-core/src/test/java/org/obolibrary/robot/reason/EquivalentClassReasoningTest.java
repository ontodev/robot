package org.obolibrary.robot.reason;

import org.junit.Test;
import org.obolibrary.robot.CoreTest;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by edouglass on 5/9/17.
 *
 */
public class EquivalentClassReasoningTest extends CoreTest{

    @Test
    public void testReasonAllEquivalencesAllowed() throws IOException {
        OWLOntology inferred = loadOntology("/inferred-equiv.owl");
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
        EquivalentClassReasoning allAllowedReasoning = new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.ALL);

        assertTrue(allAllowedReasoning.reason());
    }

    @Test
    public void testReasonNoEquivalencesAllowed() throws IOException {
        OWLOntology inferred = loadOntology("/inferred-equiv.owl");
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
        EquivalentClassReasoning noneAllowedReasoning = new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.NONE);

        assertFalse(noneAllowedReasoning.reason());
    }

    @Test
    public void testReasonOnlyAssertedAllowedOnInferred() throws IOException {
        OWLOntology inferred = loadOntology("/inferred-equiv.owl");
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
        EquivalentClassReasoning assertedOnlyReasoning = new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.ASSERTED_ONLY);

        assertFalse(assertedOnlyReasoning.reason());
    }

    @Test
    public void testReasonOnlyAssertedAllowedOnAsserted() throws IOException {
        OWLOntology inferred = loadOntology("/asserted-equiv.owl");
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
        EquivalentClassReasoning assertedOnlyReasoning = new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.ASSERTED_ONLY);

        assertTrue(assertedOnlyReasoning.reason());
    }

}
