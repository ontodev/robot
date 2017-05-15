package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.junit.Test;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * Tests for ReasonOperation.
 */
public class ReasonOperationTest extends CoreTest {
    /**
     * Test reasoning with StructuralReasoner.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testStructural()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
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
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testELK() throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertIdentical("/simple_elk.owl", reasoned);
    }

    /**
     * Test reasoning with HermiT.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testHermit() throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .HermiT.Reasoner.ReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertEquals(6, reasoned.getAxiomCount());
        assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test reasoning with JFact.
     *
     * @throws IOException on file problem
      * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
    */
    @Test
    public void testJFact() throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertEquals(6, reasoned.getAxiomCount());
        assertIdentical("/simple_jfact.owl", reasoned);
    }

    /**
     * Test inferring into new ontology.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testInferIntoNewOntology()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .HermiT.Reasoner.ReasonerFactory();
        Map<String, String> opts = new HashMap<>();
        // see https://github.com/ontodev/robot/issues/80
        opts.put("create-new-ontology", "true");
        // see https://github.com/ontodev/robot/issues/80
        opts.put("annotate-inferred-axioms", "true");
        ReasonOperation.reason(reasoned, reasonerFactory, opts);
        assertEquals(2, reasoned.getAxiomCount());
        //assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test inferring into new ontology.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testInferIntoNewOntologyNonTrivial()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned =
                loadOntology("/relax_equivalence_axioms_test.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .HermiT.Reasoner.ReasonerFactory();
        Map<String, String> opts = new HashMap<>();

        // see https://github.com/ontodev/robot/issues/80
        opts.put("create-new-ontology", "true");
        opts.put("annotate-inferred-axioms", "true");

        ReasonOperation.reason(reasoned, reasonerFactory, opts);

        // note that some of the inferred axioms are trivial
        // involving owl:Thing
        assertEquals(15, reasoned.getAxiomCount());
        //assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test inferring into new ontology, excluding duplicates.
     *
     * @throws IOException on file problem
      * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
    */
    @Test
    public void testInferIntoNewOntologyNoDupes()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned =
                loadOntology("/relax_equivalence_axioms_test.obo");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .HermiT.Reasoner.ReasonerFactory();
        Map<String, String> opts = new HashMap<>();
        opts.put("create-new-ontology", "true");
        opts.put("annotate-inferred-axioms", "true");
        opts.put("exclude-duplicate-axioms", "true");
        ReasonOperation.reason(reasoned, reasonerFactory, opts);
        assertEquals(5, reasoned.getAxiomCount());
        //assertIdentical("/simple_hermit.owl", reasoned);
    }

    /**
     * Test removing redundant subclass axioms.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testRemoveRedundantSubClassAxioms()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/redundant_subclasses.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        ReasonOperation.reason(reasoned, reasonerFactory, Collections.emptyMap());
        assertIdentical("/redundant_subclasses.owl", reasoned);

        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        reasoned = loadOntology("/redundant_subclasses.owl");
        ReasonOperation.reason(reasoned, reasonerFactory, options);
        assertIdentical("/without_redundant_subclasses.owl", reasoned);
    }

    /**
     * Test reasoning with Expression Materializing Reasoner.
     *
     * This test should return the same results as running any other reasoner
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testEMRBasic()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned = loadOntology("/simple.owl");
        OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
        OWLReasonerFactory reasonerFactory =
                new ExpressionMaterializingReasonerFactory(coreReasonerFactory);
        ReasonOperation.reason(reasoned, reasonerFactory);
        assertIdentical("/simple_elk.owl", reasoned);
    }

    /**
     * Test reasoning with Expression Materializing Reasoner.
     *
     * This test effectively relaxes an equivalence axiom
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     * @throws OntologyLogicException on logic error
     */
    @Test
    public void testEMRRelax()
            throws IOException, OWLOntologyCreationException,
            OntologyLogicException {
        OWLOntology reasoned =
                loadOntology("/relax_equivalence_axioms_test.obo");
        OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
        OWLReasonerFactory reasonerFactory =
                new ExpressionMaterializingReasonerFactory(coreReasonerFactory);
        Map<String, String> opts = ReasonOperation.getDefaultOptions();
        opts.put("exclude-owl-thing", "true");
        ReasonOperation.reason(reasoned, reasonerFactory, opts);
        assertIdentical(
                "/relax_equivalence_axioms_expressions_materialized.obo",
                reasoned);
    }


}
