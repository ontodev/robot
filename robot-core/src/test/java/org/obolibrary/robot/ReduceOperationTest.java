package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests for ReasonOperation.
 */
public class ReduceOperationTest extends CoreTest {

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

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        ReduceOperation.reduce(reasoned, reasonerFactory, options);
        assertIdentical("/without_redundant_subclasses.owl", reasoned);
    }

    /**
     * Test removing redundant subclass expression
     * (existential restriction) axioms.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testRemoveRedundantSubClassExpressionAxioms()
            throws IOException {
        OWLOntology reasoned = loadOntology("/redundant_expr.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        ReduceOperation.reduce(reasoned, reasonerFactory, options);
        assertIdentical("/redundant_expr_reduced.obo", reasoned);
    }
    
    /**
     * If P is reflexive, and we have
     *  "A SubClassOf B" and "A SubClassOf P some B",
     * then the second becomes redundant
     *
     * @throws IOException on file problem
     */
    @Test
    public void testReduceWithReflexivity()
            throws IOException {
        OWLOntology reasoned = loadOntology("/reduce_reflexivity_test.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        ReduceOperation.reduce(reasoned, reasonerFactory, options);
        assertIdentical("/reduce_reflexivity_test_reduced.obo", reasoned);
    }

    /**
     * Test removing GCIs.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testReduceGci() throws IOException {
        OWLOntology reasoned = loadOntology("/reduce_gci_test.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        ReduceOperation.reduce(reasoned, reasonerFactory, options);
        assertIdentical("/reduce_gci_reduced.obo", reasoned);
    }
}
