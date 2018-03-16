package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.io.IOException;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

/** Tests for FilterOperation. */
public class FilterOperationTest extends CoreTest {

  private final IOHelper ioHelper = new IOHelper();

  private final OWLDataFactory df = OWLManager.getOWLDataFactory();

  /**
   * Test filtering for entities that have a given annotation.
   *
   * @throws Exception
   */
  public void testFilterAnnotation() throws Exception {
    OWLOntology simple = loadOntology("/simple.owl");
    OWLAnnotation annotation =
        df.getOWLAnnotation(
            df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")),
            df.getOWLLiteral("Test 1", df.getOWLDatatype(ioHelper.createIRI("rdf:PlainLiteral"))));
    OWLOntology filtered = FilterOperation.filterAnnotations(simple, Sets.newHashSet(annotation));
    assertIdentical("/simple_filtered.owl", filtered);
  }

  /**
   * Test filtering for a class.
   *
   * @throws IOException
   */
  @Test
  public void testFilterClass() throws IOException {
    OWLOntology filtered = loadOntology("/simple.owl");
    OWLClass cls = df.getOWLClass(IRI.create(base + "simple.owl#test1"));
    FilterOperation.filterClasses(filtered, Sets.newHashSet(cls));
    assertIdentical("/simple_filtered.owl", filtered);
  }

  /**
   * Test filtering for an object property.
   *
   * @throws IOException
   */
  @Test
  public void testFilterProperty() throws IOException {
    OWLOntology filtered = loadOntology("/simple_parts.owl");
    OWLObjectProperty property = df.getOWLObjectProperty(IRI.create(base + "simple.owl#part_of"));
    FilterOperation.filterProperties(filtered, Sets.newHashSet(property));
    assertIdentical("/simple_parts.owl", filtered);
  }
}
