package org.obolibrary.robot.reason;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;
import org.obolibrary.robot.CoreTest;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Created by edouglass on 5/9/17. */
public class EquivalentClassReasoningTest extends CoreTest {

  /**
   * Test all equivalences allowed with inferred equiv axioms. Pass if true.
   *
   * @throws IOException on error
   */
  @Test
  public void testReasonAllEquivalencesAllowed() throws IOException {
    OWLOntology inferred = loadOntology("/inferred-equiv.owl");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
    EquivalentClassReasoning allAllowedReasoning =
        new EquivalentClassReasoning(inferred, reasoner, EquivalentClassReasoningMode.ALL);

    assertTrue(allAllowedReasoning.reason());
  }

  /**
   * Test no equivalences allowed with inferred equiv axioms. Pass if false.
   *
   * @throws IOException on error
   */
  @Test
  public void testReasonNoEquivalencesAllowed() throws IOException {
    OWLOntology inferred = loadOntology("/inferred-equiv.owl");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
    EquivalentClassReasoning noneAllowedReasoning =
        new EquivalentClassReasoning(inferred, reasoner, EquivalentClassReasoningMode.NONE);

    assertFalse(noneAllowedReasoning.reason());
  }

  /**
   * Test asserted only with inferred equiv axioms. Pass if false.
   *
   * @throws IOException on error
   */
  @Test
  public void testReasonOnlyAssertedAllowedOnInferred() throws IOException {
    OWLOntology inferred = loadOntology("/inferred-equiv.owl");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
    EquivalentClassReasoning assertedOnlyReasoning =
        new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.ASSERTED_ONLY);

    assertFalse(assertedOnlyReasoning.reason());
  }

  /**
   * Test asserted only with asserted equiv axioms. Pass if true.
   *
   * @throws IOException on error
   */
  @Test
  public void testReasonOnlyAssertedAllowedOnAsserted() throws IOException {
    OWLOntology inferred = loadOntology("/asserted-equiv.owl");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inferred);
    EquivalentClassReasoning assertedOnlyReasoning =
        new EquivalentClassReasoning(
            inferred, reasoner, EquivalentClassReasoningMode.ASSERTED_ONLY);

    assertTrue(assertedOnlyReasoning.reason());
  }
}
