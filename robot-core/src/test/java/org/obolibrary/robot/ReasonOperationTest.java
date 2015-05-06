package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests for {@link ReasonOperation}.
 */
public class ReasonOperationTest extends CoreTest {
    /**
     * Test reasoning with StructuralReasoner.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testStructural() throws IOException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .owlapi.reasoner.structural.StructuralReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertIdentical("/simple_structural.owl", reasoned);
    }

    /**
     * Test reasoning with ELK.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testELK() throws IOException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertIdentical("/simple_elk.owl", reasoned);
    }

    /**
     * Test reasoning with HermiT.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testHermit() throws IOException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .HermiT.Reasoner.ReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test removing redundant subclass axioms.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testRemoveRedundantSubClassAxioms() throws IOException {
        OWLOntology reasoned = loadOntology("/redundant_subclasses.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory, null);
        assertIdentical("/redundant_subclasses.owl", reasoned);

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        reasoned = loadOntology("/redundant_subclasses.owl");
        ReasonOperation.reason(reasoned, reasonerFactory, options);
        assertIdentical("/without_redundant_subclasses.owl", reasoned);
    }

}
