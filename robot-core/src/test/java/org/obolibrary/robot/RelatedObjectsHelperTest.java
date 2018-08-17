package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/** Tests for RelatedObjectsHelper. */
public class RelatedObjectsHelperTest extends CoreTest {

  private final OWLDataFactory df = OWLManager.getOWLDataFactory();

  /**
   * Test getting complete axioms.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testGetCompleteAxioms() throws IOException {
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
    axioms = RelatedObjectsHelper.getCompleteAxioms(ontology, objects, axiomTypes);
    assertEquals(1, axioms.size());

    objects = new HashSet<>();
    objects.add(df.getOWLClass(IRI.create(base + "test1")));
    objects.add(df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")));
    axiomTypes = Sets.newHashSet(OWLAnnotationAssertionAxiom.class);
    axioms = RelatedObjectsHelper.getCompleteAxioms(ontology, objects, axiomTypes);
    assertEquals(2, axioms.size());
  }

  /**
   * Test getting partial axioms.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testGetPartialAxioms() throws IOException {
    IOHelper ioHelper = new IOHelper();
    String base = "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#";
    Set<OWLObject> objects;
    Set<Class<? extends OWLAxiom>> axiomTypes;
    Set<OWLAxiom> axioms;

    OWLOntology ontology = loadOntology("/simple.owl");

    objects = new HashSet<>();
    objects.add(df.getOWLClass(IRI.create(base + "test1")));
    axiomTypes = Sets.newHashSet(OWLSubClassOfAxiom.class);
    axioms = RelatedObjectsHelper.getPartialAxioms(ontology, objects, axiomTypes);
    assertEquals(1, axioms.size());

    objects = new HashSet<>();
    objects.add(df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")));
    axiomTypes = Sets.newHashSet(OWLAnnotationAssertionAxiom.class);
    axioms = RelatedObjectsHelper.getPartialAxioms(ontology, objects, axiomTypes);
    assertEquals(2, axioms.size());
  }
}
