package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
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
