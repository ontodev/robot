package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Tests creating local cache.
 *
 * @author cjm
 */
public class MirrorOperationTest extends CoreTest {

  /**
   * Test MIREOT.
   *
   * @throws IOException on IO problems
   * @throws OWLOntologyCreationException on ontology problems
   * @throws OWLOntologyStorageException if error in saving ontology
   */
  @Test
  public void testMirror()
      throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
    testMirror("/import_test.owl");
  }

  /**
   * Mirrors a given ontology plus its import closure, and tests the results are identical when
   * loaded.
   *
   * @param expectedPath resource path for the ontology.
   * @throws IOException for reading/writing errors.
   * @throws OWLOntologyCreationException if ontology cannot be loaded.
   * @throws OWLOntologyStorageException if ontology cannot be saved.
   */
  public void testMirror(String expectedPath)
      throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
    OWLOntology inputOntology = loadOntologyWithCatalog(expectedPath);

    File catalogFile = new File("target/mirror-catalog.xml");
    MirrorOperation.mirror(inputOntology, new File("target"), catalogFile);

    OWLOntology loadedOntology =
        loadOntologyWithCatalog(inputOntology.getOntologyID().getOntologyIRI().get(), catalogFile);
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
