package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/** Tests for RemoveOperation. */
public class RemoveOperationTest extends CoreTest {

  private final IOHelper ioHelper = new IOHelper();

  private final OWLDataFactory df = OWLManager.getOWLDataFactory();

  private final Set<Class<? extends OWLAxiom>> allAxioms = Sets.newHashSet(OWLAxiom.class);

  /**
   * Test removing all entities.
   *
   * @throws IOException on any problem
   */
  public void testRemoveAll() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");

    RemoveOperation.remove(ontology, OntologyHelper.getEntities(ontology), allAxioms);
    assertIdentical("/empty.owl", ontology);
  }

  /**
   * Test removing a class.
   *
   * @throws IOException on any problem
   */
  public void testRemoveClass() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    OWLEntity cls = df.getOWLClass(ioHelper.createIRI("UBERON:0004770"));

    RemoveOperation.remove(ontology, cls, allAxioms);
    assertIdentical("/remove_class.owl", ontology);
  }

  /**
   * Test removing descendants of a class (without removing the class itself).
   *
   * @throws IOException on any problem
   */
  @Test
  public void testRemoveDescendants() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<OWLEntity> relatedEntities =
        RelatedEntitiesHelper.getRelated(
            ontology,
            Sets.newHashSet(df.getOWLClass(ioHelper.createIRI("UBERON:0004905"))),
            RelationType.DESCENDANTS);

    RemoveOperation.remove(ontology, relatedEntities, allAxioms);
    assertIdentical("/remove_descendants.owl", ontology);
  }

  /**
   * Test removing all subclass statements.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testRemoveSubClassOfs() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<Class<? extends OWLAxiom>> axiomTypes = new HashSet<>();
    axiomTypes.add(OWLSubClassOfAxiom.class);

    RemoveOperation.remove(ontology, OntologyHelper.getEntities(ontology), axiomTypes);
    assertIdentical("/remove_subclass_ofs.owl", ontology);
  }

  /**
   * Test removing all individuals.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testRemoveIndividuals() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<RelationType> relationTypes = new HashSet<>();
    relationTypes.add(RelationType.INDIVIDUALS);

    Set<OWLEntity> individuals =
        RelatedEntitiesHelper.getRelated(
            ontology, OntologyHelper.getEntities(ontology), relationTypes);

    RemoveOperation.remove(ontology, individuals, allAxioms);
    assertIdentical("/remove_individual.owl", ontology);
  }

  /**
   * Test removing all anonymous entities.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testRemoveAnonymous() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<RelationType> relationTypes = new HashSet<>();
    relationTypes.add(RelationType.ANCESTORS);
    relationTypes.add(RelationType.EQUIVALENTS);

    RemoveOperation.removeAnonymous(
        ontology, OntologyHelper.getEntities(ontology), relationTypes, allAxioms);
    assertIdentical("/remove_anonymous.owl", ontology);
  }

  /**
   * Test removing a class based on an annotation.
   *
   * @throws IOException on any problem
   */
  @Test
  public void testRemoveByAnnotation() throws IOException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<OWLAnnotation> annotations = new HashSet<>();
    annotations.add(
        df.getOWLAnnotation(
            df.getOWLAnnotationProperty(ioHelper.createIRI("rdfs:label")),
            df.getOWLLiteral("articular system")));
    Set<OWLEntity> relatedEntities = RelatedEntitiesHelper.getAnnotated(ontology, annotations);

    RemoveOperation.remove(ontology, relatedEntities, allAxioms);
    assertIdentical("/remove_class.owl", ontology);
  }
}
