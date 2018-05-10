package org.obolibrary.robot;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * Tests non-MIREOT extraction operations.
 *
 * @author cjm
 */
public class ExtractOperationTest extends CoreTest {

  /**
   * Tests STAR.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyCreationException on ontology error
   */
  @Test
  public void testExtractStar() throws IOException, OWLOntologyCreationException {

    testExtract(ModuleType.STAR, "/star.owl");
  }

  /**
   * Tests BOT.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyCreationException on ontology error
   */
  @Test
  public void testExtractBot() throws IOException, OWLOntologyCreationException {

    testExtract(ModuleType.BOT, "/bot.owl");
  }

  /**
   * Tests TOP.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyCreationException on ontology error
   */
  @Test
  public void testExtractTop() throws IOException, OWLOntologyCreationException {

    testExtract(ModuleType.TOP, "/top.owl");
  }

  /**
   * Tests a generic non-MIREOT (i.e. SLME) extraction operation using a custom module type and a
   * pre-generated OWL file to compare against.
   *
   * @param moduleType type for the extraction
   * @param expectedPath path to the known-good file for comparison
   * @throws IOException on IO error
   * @throws OWLOntologyCreationException on ontology error
   */
  public void testExtract(ModuleType moduleType, String expectedPath)
      throws IOException, OWLOntologyCreationException {
    OWLOntology simple = loadOntology("/filtered.owl");

    IRI outputIRI = IRI.create("http://purl.obolibrary.org/obo/uberon.owl");

    Set<IRI> terms =
        Collections.singleton(IRI.create("http://purl.obolibrary.org/obo/UBERON_0001235"));
    OWLOntology module = ExtractOperation.extract(simple, terms, outputIRI, moduleType);

    OWLOntology expected = loadOntology(expectedPath);
    removeDeclarations(expected);
    removeDeclarations(module);
    assertIdentical(expected, module);
  }
}
