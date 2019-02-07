package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

/**
 * Tests non-MIREOT extraction operations.
 *
 * @author cjm
 */
public class ExtractOperationTest extends CoreTest {

  /**
   * Tests STAR.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testExtractStar() throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put("method", "star");
    testExtract("/star.owl", options);
  }

  /**
   * Tests minimal intermediates with STAR.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testExtractMinimal() throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put("intermediates", "minimal");
    testExtract("/star-minimal.owl", options);
  }

  /**
   * Tests no intermediates with STAR.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testExtractNone() throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put("intermediates", "none");
    testExtract("/none.owl", options);
  }

  /**
   * Tests BOT.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testExtractBot() throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put("method", "bot");
    testExtract("/bot.owl", options);
  }

  /**
   * Tests TOP.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testExtractTop() throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put("method", "top");
    testExtract("/top.owl", options);
  }

  /** Tests getting the source annotation based on IRI of the entity. */
  @Test
  public void testGetIsDefinedBy() {
    OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();
    IRI source = IRI.create("http://purl.obolibrary.org/obo/go.owl");
    IRI bfoSource = IRI.create("http://purl.obolibrary.org/obo/bfo.owl");

    // Test with normal OBO Foundry IRI
    OWLEntity entity =
        dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/GO_0000001"));
    OWLAnnotationAxiom axiom =
        dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), source);
    assertEquals(axiom, ExtractOperation.getIsDefinedBy(entity, null));

    // Test with source replacement
    Map<IRI, IRI> sourceMap = new HashMap<>();
    sourceMap.put(IRI.create("http://purl.obolibrary.org/obo/GO_0000001"), bfoSource);
    axiom = dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), bfoSource);
    assertEquals(axiom, ExtractOperation.getIsDefinedBy(entity, sourceMap));

    // Test with "base" IRI
    entity = dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/go.owl#0000001"));
    axiom = dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), source);
    ExtractOperation.getIsDefinedBy(entity, null);
    assertEquals(axiom, ExtractOperation.getIsDefinedBy(entity, null));

    // Test with "base" IRI without '.owl'
    entity = dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/go#0000001"));
    axiom = dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), source);
    ExtractOperation.getIsDefinedBy(entity, null);
    assertEquals(axiom, ExtractOperation.getIsDefinedBy(entity, null));

    // Test with unusual IRI
    entity = dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/go/0000001"));
    axiom = dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), source);
    ExtractOperation.getIsDefinedBy(entity, null);
    assertEquals(axiom, ExtractOperation.getIsDefinedBy(entity, null));
  }

  /**
   * Tests a generic non-MIREOT (i.e. SLME) extraction operation using a custom module type and a
   * pre-generated OWL file to compare against.
   *
   * @param expectedPath path to the known-good file for comparison
   * @throws IOException on IO error
   * @throws OWLOntologyCreationException on ontology error
   */
  public void testExtract(String expectedPath, Map<String, String> options) throws Exception {
    OWLOntology simple = loadOntology("/filtered.owl");

    IRI outputIRI = IRI.create("http://purl.obolibrary.org/obo/uberon.owl");

    Set<IRI> terms =
        Collections.singleton(IRI.create("http://purl.obolibrary.org/obo/UBERON_0001235"));
    OWLOntology module =
        ExtractOperation.extract(simple, new IOHelper(), terms, outputIRI, options);

    OWLOntology expected = loadOntology(expectedPath);
    removeDeclarations(expected);
    removeDeclarations(module);
    assertIdentical(expected, module);
  }
}
