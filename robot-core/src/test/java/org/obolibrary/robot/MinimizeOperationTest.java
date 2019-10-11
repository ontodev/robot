package org.obolibrary.robot;

import static junit.framework.TestCase.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/** Tests minimize operation. */
public class MinimizeOperationTest extends CoreTest {

  /**
   * Test minimize operation.
   *
   * @throws IOException on issue loading ontology or running minimize
   * @throws OWLOntologyCreationException on loading ontology
   */
  @Test
  public void testMinimize() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    MinimizeOperation.minimize(ontology, 3, new HashSet<>());
    int after = ontology.getClassesInSignature().size();
    assertEquals(6, after);
  }

  /**
   * Test minimize operation with a precious class.
   *
   * @throws IOException on issue loading ontology or running minimize
   * @throws OWLOntologyCreationException on loading ontology
   */
  @Test
  public void testMinimizeWithPrecious() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/uberon.owl");
    Set<IRI> precious = new HashSet<>();
    // 'skeletal joint' will be kept
    precious.add(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000982"));
    MinimizeOperation.minimize(ontology, 3, precious);
    int after = ontology.getClassesInSignature().size();
    assertEquals(7, after);
  }
}
