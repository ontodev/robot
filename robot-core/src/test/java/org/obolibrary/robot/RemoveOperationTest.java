package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

public class RemoveOperationTest extends CoreTest {

  private static final String INPUT = "/remove.owl";
  private static final String CLASS = "class";
  private static final String INDIV = "individual";
  private static final String OBJ = "object-property";
  private static final String ANN = "annotation-property";
  private static final String DATA = "datatype-property";

  private static Map<String, String> nullEntities;

  static {
    nullEntities = new HashMap<>();
    nullEntities.put(CLASS, null);
    nullEntities.put(INDIV, null);
    nullEntities.put(OBJ, null);
    nullEntities.put(ANN, null);
    nullEntities.put(DATA, null);
  }

  /**
   * Test removal of OWLClass (articular system) from remove.owl. Result is identical to
   * remove_class.owl.
   *
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testRemoveClass() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.remove(ontology, IRI.create("http://purl.obolibrary.org/obo/UBERON_0004770"));
    assertIdentical("/remove_class.owl", ontology);
  }

  /**
   * Test removeal of anonymous superclasses of OWLClass (skeletal system). Result is identical to
   * remove_anonymous.owl.
   *
   * @throws IOException
   */
  @Test
  public void testRemoveAnonymousSuperClasses() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.removeAnonymousClasses(
        ontology, IRI.create("http://purl.obolibrary.org/obo/UBERON_0001434"));
    assertIdentical("/remove_anonymous.owl", ontology);
  }

  /**
   * Test removal of descendant classes of OWLClass (anatomical cluster) from removal.owl. Result is
   * identical to remove_descendants.owl.
   *
   * @throws IOException
   */
  @Test
  public void testRemoveDescendantClasses() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.removeDescendants(
        ontology, IRI.create("http://purl.obolibrary.org/obo/UBERON_0000477"));
    assertIdentical("/remove_descendants.owl", ontology);
  }

  /**
   * Test removal of OWLNamedIndividual (skeleton) from remove.owl. Result is identical to
   * remove_individual.owl.
   *
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testRemoveIndividual() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.remove(ontology, IRI.create("http://purl.obolibrary.org/obo/UBERON_9999999"));
    assertIdentical("/remove_individual.owl", ontology);
  }

  /**
   * Test removal of all OWLNamedIndividuals from remove.owl. Result is identical to
   * remove_individual.owl (as above).
   *
   * @throws IOException
   */
  @Test
  public void testRemoveAllIndividuals() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);
    RemoveOperation.removeIndividuals(ontology);
    assertIdentical("/remove_individual.owl", ontology);
  }

  /**
   * Test removal of OWLAnnotationProperty (label) from remove.owl. Result is identical to
   * remove_annotation.owl.
   *
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testRemoveAnnProp() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.remove(ontology, IRI.create("http://www.w3.org/2000/01/rdf-schema#label"));
    assertIdentical("/remove_annotation.owl", ontology);
  }

  /**
   * Test removal of OWLObjectProperty (has_part) from remove.owl. Result is identical to
   * remove_obj_prop.owl.
   *
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testRemoveObjProp() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.remove(ontology, IRI.create("http://purl.obolibrary.org/obo/BFO_0000051"));
    assertIdentical("/remove_obj_prop.owl", ontology);
  }

  /**
   * Test removal of OWLDataProperty (height) from remove.owl. Result is identical to
   * remove_data_prop.owl.
   *
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testRemoveDataProp() throws IOException {
    OWLOntology ontology = loadOntology(INPUT);

    RemoveOperation.remove(ontology, IRI.create("http://purl.obolibrary.org/obo/UBERON_8888888"));
    assertIdentical("/remove_data_prop.owl", ontology);
  }
}
