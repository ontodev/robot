package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/** Tests for FilterOperation. */
public class FilterOperationTest extends CoreTest {
  /**
   * Filter all object properties from an ontology that has no object properties. Result is
   * identical.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testFilterNothing() throws IOException {
    Set<OWLObjectProperty> properties = new HashSet<>();
    OWLOntology filtered = loadOntology("/simple.owl");
    FilterOperation.filter(filtered, properties);
    assertIdentical("/simple.owl", filtered);
  }

  /**
   * Filter all object properties from an ontology that has just one object property. Result matches
   * the simple.owl ontology.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testRemoveParts() throws IOException {
    Set<OWLObjectProperty> properties = new HashSet<>();
    OWLOntology filtered = loadOntology("/simple_parts.owl");
    FilterOperation.filter(filtered, properties);
    assertIdentical("/simple.owl", filtered);
  }

  /**
   * Filter for one object property from an ontology that has just one object property. Result is
   * identical.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testKeepParts() throws IOException {
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");

    OWLOntologyManager manager = simpleParts.getOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    Set<OWLObjectProperty> properties = new HashSet<>();
    properties.add(df.getOWLObjectProperty(IRI.create(base + "simple.owl#part_of")));

    OWLOntology filtered = loadOntology("/simple_parts.owl");
    FilterOperation.filter(filtered, properties);

    assertIdentical("/simple_parts.owl", filtered);
  }
}
