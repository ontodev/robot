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
public class RelaxOperationTest extends CoreTest {
  

   
  

    /**
     * Test removing redundant subclass axioms.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testRelax() throws IOException {
        OWLOntology ont = loadOntology("/relax_equivalence_axioms_test.obo");
 
        Map<String, String> options = new HashMap<String, String>();
        options.put("remove-redundant-subclass-axioms", "true");

        RelaxOperation.relax(ont, options);
        assertIdentical("/relax_equivalence_axioms_relaxed.obo", ont);
    }

 
}
