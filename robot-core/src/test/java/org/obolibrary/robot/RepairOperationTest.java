package org.obolibrary.robot;

import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/** Tests for MergeOperation. */
public class RepairOperationTest extends CoreTest {

  /**
   * Test repair an ontology
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testRepair() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/need-of-repair.owl");
    IOHelper iohelper = new IOHelper();
    RepairOperation.repair(ontology, iohelper, true);
    iohelper.saveOntology(ontology, "target/foo.owl");
    assertIdentical("/repaired.owl", ontology);
  }

  @Test
  public void testXrefRepair() throws IOException {
    OWLAnnotationProperty hasDbXref =
        OWLManager.getOWLDataFactory()
            .getOWLAnnotationProperty(
                IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));
    OWLOntology ontology = loadOntology("/xref-need-of-repair.obo");
    IOHelper iohelper = new IOHelper();
    RepairOperation.repair(ontology, iohelper, true, Collections.singleton(hasDbXref));
    assertIdentical("/xref-repaired.obo", ontology);
  }
}
