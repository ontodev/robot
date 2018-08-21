package org.obolibrary.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;

/** Tests for MergeOperation. */
public class MergeOperationTest extends CoreTest {
  /**
   * Test merging a single ontology without imports.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testMergeOne() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    OWLOntology merged = MergeOperation.merge(simple);
    assertIdentical("/simple.owl", merged);
  }

  /**
   * Test merging two ontologies without imports. Result should equal simpleParts.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testMergeTwo() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(simple);
    ontologies.add(simpleParts);
    OWLOntology merged = MergeOperation.merge(ontologies);
    assertIdentical("/simple_parts.owl", merged);
  }
}
