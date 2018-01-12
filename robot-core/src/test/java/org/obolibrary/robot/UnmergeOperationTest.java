package org.obolibrary.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/** Tests for UnmergeOperation. */
public class UnmergeOperationTest extends CoreTest {

  /**
   * Test unmerging two ontologies without imports. Result should equal simpleParts.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testUnmergeTwo() throws IOException, OWLOntologyCreationException {
    OWLOntology simple = loadOntology("/simple.owl");
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    ontologies.add(simpleParts);
    ontologies.add(simple);
    OWLOntology unmerged = UnmergeOperation.unmerge(ontologies);
    assertIdentical("/simple_parts_unmerged.owl", unmerged);
  }
}
