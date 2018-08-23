package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests MIREOT extraction.
 *
 * <p>This is a very minimal test with a dumb example
 *
 * @author cjm
 */
public class MireotOperationTest extends CoreTest {

  /**
   * Test MIREOT.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testMireot() throws Exception {
    testMireot("/mireot.owl");
  }

  /**
   * Test MIREOT.
   *
   * @param expectedPath the path to a known-good file for comparison
   * @throws Exception on any problem
   */
  public void testMireot(String expectedPath) throws Exception {
    OWLOntology inputOntology = loadOntology("/filtered.owl");

    IRI outputIRI = IRI.create("http://purl.obolibrary.org/obo/uberon.owl");

    Set<IRI> upperIRIs =
        Collections.singleton(IRI.create("http://purl.obolibrary.org/obo/UBERON_0001235"));
    // Set<IRI> branchIRIs = upperIRIs;

    List<OWLOntology> outputOntologies = new ArrayList<>();

    outputOntologies.add(MireotOperation.getAncestors(inputOntology, upperIRIs, upperIRIs, null));

    /*
    outputOntologies.add(
            MireotOperation.getDescendants(inputOntology,
                    branchIRIs, null));
    */

    OWLOntology outputOntology = MergeOperation.merge(outputOntologies);

    OntologyHelper.setOntologyIRI(outputOntology, outputIRI, null);

    OWLOntology expected = loadOntology(expectedPath);
    removeDeclarations(expected);
    removeDeclarations(outputOntology);
    assertIdentical(expected, outputOntology);
  }
}
