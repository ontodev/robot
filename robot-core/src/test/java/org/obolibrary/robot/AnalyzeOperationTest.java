package org.obolibrary.robot;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for AnalyzeOperation. */
public class AnalyzeOperationTest extends CoreTest {
  /**
   * Test reasoning with StructuralReasoner.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testBasic() throws Exception {
    OWLOntology ontology = loadOntology("/analyze_test.omn");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.ReasonerFactory();

    // OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    double avgPower = AnalyzeOperation.analyze(ontology, reasonerFactory);
    assertTrue(Math.abs(avgPower - 0.5) < 0.0001);
  }
}
