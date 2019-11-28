package org.obolibrary.robot;

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

/** Tests for RelatedObjectsHelper. */
public class RelatedObjectsHelperTest extends CoreTest {

  private final OWLDataFactory df = OWLManager.getOWLDataFactory();
  private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  private final OWLAnnotation annotation1 =
      df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("English label", "en"));
  private final OWLAnnotation annotation2 =
      df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("French label", "fr"));
  private final OWLAnnotation annotation3 =
      df.getOWLAnnotation(
          df.getRDFSComment(),
          df.getOWLLiteral(
              "comment 1", df.getOWLDatatype(IRI.create("http://example.com/test-datatype"))));
  private final OWLAnnotation annotation4 =
      df.getOWLAnnotation(
          df.getRDFSComment(),
          df.getOWLLiteral(
              "comment 2", df.getOWLDatatype(IRI.create("http://example.com/test-datatype-2"))));

  /**
   * Test filtering for complete axioms.
   *
   * @throws IOException on any problem
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testFilterCompleteAxioms() throws IOException {
    IOHelper ioHelper = new IOHelper();
    String base = "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#";
    Set<OWLObject> objects;
    Set<Class<? extends OWLAxiom>> axiomTypes;
    Set<OWLAxiom> axioms;

    OWLOntology ontology = loadOntology("/simple.owl");

    objects = new HashSet<>();
    objects.add(df.getOWLClass(IRI.create(base + "test1")));
    objects.add(df.getOWLClass(IRI.create(base + "test2")));
    axiomTypes = Sets.newHashSet(OWLSubClassOfAxiom.class);
    axioms =
        RelatedObjectsHelper.filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, false);
    assertEquals(1, axioms.size());

    objects = new HashSet<>();
    objects.add(df.getOWLClass(IRI.create(base + "test1")));
    objects.add(df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")));
    axiomTypes = Sets.newHashSet(OWLAnnotationAssertionAxiom.class);
    axioms =
        RelatedObjectsHelper.filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, false);
    assertEquals(2, axioms.size());
  }

  /**
   * Test filtering for partial axioms.
   *
   * @throws IOException on any problem
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testFilterPartialAxioms() throws IOException {
    IOHelper ioHelper = new IOHelper();
    String base = "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#";
    Set<OWLObject> objects;
    Set<Class<? extends OWLAxiom>> axiomTypes;
    Set<OWLAxiom> axioms;

    OWLOntology ontology = loadOntology("/simple.owl");

    objects = new HashSet<>();
    objects.add(df.getOWLClass(IRI.create(base + "test1")));
    axiomTypes = Sets.newHashSet(OWLSubClassOfAxiom.class);
    axioms =
        RelatedObjectsHelper.filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, false);
    assertEquals(1, axioms.size());

    objects = new HashSet<>();
    objects.add(df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")));
    axiomTypes = Sets.newHashSet(OWLAnnotationAssertionAxiom.class);
    axioms =
        RelatedObjectsHelper.filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, false);
    assertEquals(2, axioms.size());
  }

  @Test
  public void testGetAnnotationByExactLangValue() throws Exception {
    IOHelper ioHelper = new IOHelper();
    OWLOntology ontology = getOntology();
    String select = "rdfs:label='English label'@en";
    Set<OWLAnnotation> annotations =
        RelatedObjectsHelper.getAnnotations(ontology, ioHelper, select);
    assertEquals(1, annotations.size());
    assertEquals(annotations.iterator().next(), annotation1);
  }

  @Test
  public void testGetAnnotationByLangTag() throws Exception {
    IOHelper ioHelper = new IOHelper();
    OWLOntology ontology = getOntology();
    String select = "rdfs:label=@en";
    Set<OWLAnnotation> annotations =
        RelatedObjectsHelper.getAnnotations(ontology, ioHelper, select);
    assertEquals(1, annotations.size());
    assertEquals(annotations.iterator().next(), annotation1);
  }

  @Test
  public void testGetAnnotationByExactDatatypeValue() throws Exception {
    IOHelper ioHelper = new IOHelper();
    OWLOntology ontology = getOntology();
    String select = "rdfs:comment='comment 1'^^<http://example.com/test-datatype>";
    Set<OWLAnnotation> annotations =
        RelatedObjectsHelper.getAnnotations(ontology, ioHelper, select);
    assertEquals(1, annotations.size());
    assertEquals(annotations.iterator().next(), annotation3);
  }

  @Test
  public void testGetAnnotationByDatatype() throws Exception {
    IOHelper ioHelper = new IOHelper();
    OWLOntology ontology = getOntology();
    String select = "rdfs:comment=^^<http://example.com/test-datatype>";
    Set<OWLAnnotation> annotations =
        RelatedObjectsHelper.getAnnotations(ontology, ioHelper, select);
    assertEquals(1, annotations.size());
    assertEquals(annotations.iterator().next(), annotation3);
  }

  @Test
  public void testGetAnnotationByRegex() throws Exception {
    IOHelper ioHelper = new IOHelper();
    OWLOntology ontology = getOntology();
    String select = "rdfs:label=~'^.*label$'";
    Set<OWLAnnotation> annotations =
        RelatedObjectsHelper.getAnnotations(ontology, ioHelper, select);
    assertEquals(2, annotations.size());
    assertTrue(annotations.contains(annotation1));
    assertTrue(annotations.contains(annotation2));
  }

  /** @throws Exception */
  private OWLOntology getOntology() throws Exception {
    OWLOntology ontology = manager.createOntology();
    OWLAnnotationAssertionAxiom ax =
        df.getOWLAnnotationAssertionAxiom(IRI.create("http://example.com/test-class"), annotation1);
    manager.addAxiom(ontology, ax);
    ax =
        df.getOWLAnnotationAssertionAxiom(
            IRI.create("http://example.com/test-class-2"), annotation2);
    manager.addAxiom(ontology, ax);
    ax =
        df.getOWLAnnotationAssertionAxiom(
            IRI.create("http://example.com/test-class-3"), annotation3);
    manager.addAxiom(ontology, ax);
    ax =
        df.getOWLAnnotationAssertionAxiom(
            IRI.create("http://example.com/test-class-4"), annotation4);
    manager.addAxiom(ontology, ax);
    return ontology;
  }
}
