package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ReasonOperation. */
public class ReduceOperationTest extends CoreTest {

  /**
   * Test removing redundant subclass axioms.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testRemoveRedundantSubClassAxiomsPreserveAnnotated()
      throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/redundant_subclasses.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");
    options.put("preserve-annotated-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/without_redundant_subclasses.owl", reasoned);
  }

  @Test
  public void testRemoveRedundantSubClassAxiomsComplete()
      throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/redundant_subclasses.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");
    options.put("preserve-annotated-axioms", "false");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/without_redundant_subclasses2.owl", reasoned);
  }

  /**
   * Test removing redundant subclass expression (existential restriction) axioms.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testRemoveRedundantSubClassExpressionAxioms()
      throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/redundant_expr.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/redundant_expr_reduced.obo", reasoned);
  }

  /**
   * If P is reflexive, and we have "A SubClassOf B" and "A SubClassOf P some B", then the second
   * becomes redundant.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testReduceWithReflexivity() throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/reduce_reflexivity_test.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/reduce_reflexivity_test_reduced.obo", reasoned);
  }

  /**
   * Test removing GCIs.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testReduceGci() throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/reduce_gci_test.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/reduce_gci_reduced.obo", reasoned);
  }

  /**
   * This test ensures that subClassOf axioms are never removed if they lead to loss of information
   * in the subClassOf graph
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testReduceWithEquiv() throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/equiv_reduce_test.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/equiv_reduce_test_reduced.obo", reasoned);
  }

  /**
   * Edge case, taken from GO
   *
   * <p>See: 'central nervous system development' in the test file
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testReduceEdgeCase() throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/reduce-edgecase-cnd.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/reduce-edgecase-cnd-reduced.obo", reasoned);
  }

  /**
   * Domain case, see https://github.com/ontodev/robot/issues/321
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testReduceDomainCase() throws IOException, OWLOntologyCreationException {
    OWLOntology reasoned = loadOntology("/reduce-domain-test.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();

    Map<String, String> options = new HashMap<String, String>();

    ReduceOperation.reduce(reasoned, reasonerFactory, options);
    assertIdentical("/reduce-domain-test.owl", reasoned);
  }
}
