package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.Test;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ReasonOperation. */
public class MaterializeOperationTest extends CoreTest {

  /**
   * Test reasoning with Expression Materializing Reasoner.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic problem
   */
  @Test
  public void testMaterialize()
      throws IOException, OWLOntologyCreationException, OntologyLogicException {
    OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    Map<String, String> opts = ReasonOperation.getDefaultOptions();
    // opts.put("exclude-owl-thing", "true");
    MaterializeOperation.materialize(reasoned, coreReasonerFactory, null, opts);
    assertIdentical("/relax_equivalence_axioms_expressions_materialized.obo", reasoned);
  }

  /**
   * Test reasoning with Expression Materializing Reasoner.
   *
   * <p>This test ensures that o trivial "C SubClassOf R some C" axioms are materialized for case
   * where R is reflexive
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic problem
   */
  @Test
  public void testMaterializeWithReflexivity()
      throws IOException, OWLOntologyCreationException, OntologyLogicException {
    OWLOntology reasoned = loadOntology("/mat_reflexivity_test.obo");
    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    Map<String, String> opts = ReasonOperation.getDefaultOptions();
    // opts.put("exclude-owl-thing", "true");
    MaterializeOperation.materialize(reasoned, coreReasonerFactory, null, opts);
    assertIdentical("/mat_reflexivity_test_materialized.obo", reasoned);
  }

  /**
   * Test reasoning with Expression Materializing Reasoner.
   *
   * <p>This test effectively relaxes an equivalence axiom
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic problem
   */
  @Test
  public void testMaterializeGCIs()
      throws IOException, OWLOntologyCreationException, OntologyLogicException {
    OWLOntology reasoned = loadOntology("/gci_example.obo");
    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    Map<String, String> opts = ReasonOperation.getDefaultOptions();
    // opts.put("exclude-owl-thing", "true");
    MaterializeOperation.materialize(reasoned, coreReasonerFactory, null, opts);
    assertIdentical("/gci_example_materialized.obo", reasoned);
  }

  /**
   * Test reasoning with imports.
   *
   * <p>For motivation, see https://github.com/ontodev/robot/issues/119
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws URISyntaxException if URI incorrectly formatted
   * @throws OntologyLogicException on logic problem
   */
  @Test
  public void testMaterializeWithImports()
      throws IOException, OWLOntologyCreationException, OntologyLogicException, URISyntaxException {

    // TODO: minor, simplify this once
    // https://github.com/ontodev/robot/issues/121 implemeted

    File f = new File(getClass().getResource("/import-non-reasoned.owl").toURI());
    IOHelper ioh = new IOHelper();
    OWLOntology reasoned = ioh.loadOntology(f, true);
    OWLOntology original = ioh.loadOntology(f, true);

    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    Map<String, String> opts = ReasonOperation.getDefaultOptions();
    MaterializeOperation.materialize(reasoned, coreReasonerFactory, null, opts);
    assertIdentical(original, reasoned);
  }
}
