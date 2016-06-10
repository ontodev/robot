package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests for ReasonOperation.
 */
public class ReasonOperationTest extends CoreTest {
    /**
     * Test reasoning with StructuralReasoner.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void testStructural() throws IOException, OWLOntologyCreationException {
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
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void testELK() throws IOException, OWLOntologyCreationException {
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
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void testHermit() throws IOException, OWLOntologyCreationException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .HermiT.Reasoner.ReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertEquals(6, reasoned.getAxiomCount());
        assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test inferring into new ontology
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void testInferIntoNewOntology() throws IOException, OWLOntologyCreationException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .HermiT.Reasoner.ReasonerFactory();
        Map<String, String> opts = new HashMap<>();
        opts.put("create-new-ontology", "true");  // see https://github.com/ontodev/robot/issues/80
        opts.put("annotate-inferred-axioms", "true");  // see https://github.com/ontodev/robot/issues/80
        ReasonOperation.reason(reasoned, reasonerFactory, opts);
        assertEquals(2, reasoned.getAxiomCount());
        //assertIdentical("/simple_hermit.owl", reasoned);
    }
    
    @Test
    public void testInferIntoNewOntologyNonTrivial() throws IOException, OWLOntologyCreationException {
        OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
            .HermiT.Reasoner.ReasonerFactory();
        Map<String, String> opts = new HashMap<>();
        opts.put("create-new-ontology", "true");  // see https://github.com/ontodev/robot/issues/80
        opts.put("annotate-inferred-axioms", "true");  // see https://github.com/ontodev/robot/issues/80
        ReasonOperation.reason(reasoned, reasonerFactory, opts);
        assertEquals(3, reasoned.getAxiomCount());
        //assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test removing redundant subclass axioms.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void testRemoveRedundantSubClassAxioms() throws IOException, OWLOntologyCreationException {
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
