package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests for ReasonOperation.
 */
public class MaterializeOperationTest extends CoreTest {
    
 
    /**
     * Test reasoning with Expression Materializing Reasoner.
     *
     * This test effectively relaxes an equivalence axiom
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testMaterialize()
            throws IOException, OWLOntologyCreationException {
        OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
        OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
        Map<String, String> opts = ReasonOperation.getDefaultOptions();
        //opts.put("exclude-owl-thing", "true");
        MaterializeOperation.materialize(reasoned, coreReasonerFactory, null, opts);
        assertIdentical("/relax_equivalence_axioms_expressions_materialized.obo", reasoned);
    }
    

}
