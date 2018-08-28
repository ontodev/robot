package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests creating local cache.
 *
 * @author cjm
 */
public class MirrorOperationTest extends CoreTest {

  /**
   * Test mirroring.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testMirror() throws Exception {
    testMirror("/import_test.owl");
  }

  /**
   * Mirrors a given ontology plus its import closure, and tests the results are identical when
   * loaded.
   *
   * @param expectedPath resource path for the ontology.
   * @throws Exception on any problem
   */
  public void testMirror(String expectedPath) throws Exception {
    OWLOntology inputOntology = loadOntologyWithCatalog(expectedPath);

    File catalogFile = new File("target/mirror-catalog.xml");
    MirrorOperation.mirror(inputOntology, new File("target"), catalogFile);

    IRI iri = inputOntology.getOntologyID().getOntologyIRI().orNull();
    OWLOntology loadedOntology;
    if (iri != null) {
      loadedOntology = loadOntologyWithCatalog(iri, catalogFile);
    } else {
      throw new Exception(String.format("Test ontology %s does not have an IRI", expectedPath));
    }
    assertIdentical(loadedOntology, inputOntology);
    int n = 0;
    for (OWLOntology importedOntology : inputOntology.getImportsClosure()) {
      n++;
      boolean isMatched = false;
      for (OWLOntology loadedImportedOntology : loadedOntology.getImportsClosure()) {
        if (loadedImportedOntology.getOntologyID().equals(importedOntology.getOntologyID())) {
          assertIdentical(loadedImportedOntology, importedOntology);
          isMatched = true;
        }
      }
      assertTrue(isMatched);
    }

    assertEquals(2, n);
    assertEquals(2, loadedOntology.getImportsClosure().size());
  }
}
