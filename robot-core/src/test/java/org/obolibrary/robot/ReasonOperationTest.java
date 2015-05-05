package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests for {@link ReasonOperation}.
 */
public class ReasonOperationTest extends CoreTest {
    /**
     * Test reasoning with StructuralReasoner.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testStructural()
            throws IOException, OWLOntologyCreationException {
        OWLOntology simple = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .owlapi.reasoner.structural.StructuralReasonerFactory();
        OWLOntology actual = ReasonOperation.reason(
                simple, reasonerFactory, simpleIRI);
        assertIdentical("/simple_structural.owl", actual);
    }

    /**
     * Test reasoning with ELK.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testELK() throws IOException, OWLOntologyCreationException {
        OWLOntology simple = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();
        OWLOntology actual = ReasonOperation.reason(
                simple, reasonerFactory, simpleIRI);
        assertIdentical("/simple_elk.owl", actual);
    }

    /**
     * Test reasoning with HermiT.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testHermit() throws IOException, OWLOntologyCreationException {
        OWLOntology simple = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .HermiT.Reasoner.ReasonerFactory();
        OWLOntology actual = ReasonOperation.reason(
                simple, reasonerFactory, simpleIRI);
        assertIdentical("/simple_hermit.owl", actual);
    }

    /**
     * Test removing redundant subclass axioms.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testRemoveRedundantSubClassAxioms()
            throws IOException, OWLOntologyCreationException {
        OWLOntology input = loadOntology("/redundant_subclasses.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .elk.owlapi.ElkReasonerFactory();
        OWLOntology redundant = ReasonOperation.reason(
                input, reasonerFactory, simpleIRI, null);
        assertIdentical("/redundant_subclasses.owl", redundant);

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        OWLOntology withoutRedundant = ReasonOperation.reason(
                input, reasonerFactory, simpleIRI, options);
        assertIdentical("/without_redundant_subclasses.owl", withoutRedundant);
    }

}
