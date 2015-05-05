package org.obolibrary.robot;

import java.io.IOException;

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

}
