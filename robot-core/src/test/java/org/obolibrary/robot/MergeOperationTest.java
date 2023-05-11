package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
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

  /**
   * Test merging a single ontology without imports and annotating with derived from provenance.
   * Expect old axioms are cleaned and a new provenance property is declared.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testMergeDerivedFrom() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(simple);

    assertEquals(5, simple.getAxiomCount());
    OWLOntology merged = MergeOperation.merge(ontologies, false, false, false, true);

    assertEquals(6, merged.getAxiomCount());
    // old axioms should be cleaned
    OWLOntology expected = loadOntology("/simple_derived_from.owl");
    assertEquals(6, expected.getAxiomCount());
    assertIdentical(expected, merged);
  }

  /**
   * Test merging a single ontology with imports and annotating with derived from provenance. Expect
   * imported axioms are merged without redundancy.
   *
   * @throws IOException on file problem
   * @throws URISyntaxException on converting path to URI
   */
  @Test
  public void testMergeImportDerivedFrom() throws IOException, URISyntaxException {
    OWLOntology simple = loadOntologyWithCatalog("/import_test.owl");
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(simple);

    assertEquals(0, simple.getAxiomCount());

    OWLOntology merged = MergeOperation.merge(ontologies, false, true, false, true);
    assertEquals(6, merged.getAxiomCount());

    OWLOntology expected = loadOntology("/simple_derived_from.owl");
    for (OWLAxiom axiom : expected.getAxioms()) {
      assertTrue(merged.containsAxiom(axiom));
    }
  }

  /**
   * Test merging a single ontology without imports and annotating with defined by provenance.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testMergeDefinedBy() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(simple);

    assertEquals(5, simple.getAxiomCount());
    OWLOntology merged = MergeOperation.merge(ontologies, false, false, true, false);

    assertEquals(9, merged.getAxiomCount());
    OWLOntology expected = loadOntology("/simple_defined_by.owl");
    assertEquals(9, expected.getAxiomCount());
    assertIdentical(expected, merged);
  }
}
