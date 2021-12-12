package org.obolibrary.robot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ReasonOperation. */
public class MigrateOperationTest extends CoreTest {

  /**
   * Test removing redundant subclass axioms.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException if ontology cannot be created
   */
  @Test
  public void testMigrate() throws IOException, OWLOntologyCreationException {
    OWLOntology mondo_small = loadOntology("/mondo_small.owl");
    OWLOntology ordo_small = loadOntology("/ordo_small.owl");

    IOHelper ioHelper = new IOHelper();

    Map<String, String> mappings = new HashMap<>();
    mappings.put(
        "http://www.orpha.net/ORDO/Orphanet_309144",
        "http://purl.obolibrary.org/obo/MONDO_0017719");
    mappings.put(
        "http://www.orpha.net/ORDO/Orphanet_93575", "http://purl.obolibrary.org/obo/MONDO_0013043");
    mappings.put(
        "http://www.orpha.net/ORDO/Orphanet_100034",
        "http://purl.obolibrary.org/obo/MONDO_0007093");
    mappings.put(
        "http://www.orpha.net/ORDO/Orphanet_2130", "http://purl.obolibrary.org/obo/MONDO_0016240");
    mappings.put(
        "http://www.orpha.net/ORDO/Orphanet_156162",
        "http://purl.obolibrary.org/obo/MONDO_0022409");

    String source_id = "ordo";

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    List<String> axiomSelectors = new ArrayList<>();

    Set<OWLEntity> excludeObjects = new HashSet<>();

    Set<OWLEntity> migrateTerms = new HashSet<>();

    OWLOntology migrated =
        MigrateOperation.migrate(
            mondo_small,
            ordo_small,
            migrateTerms,
            source_id,
            mappings,
            axiomSelectors,
            excludeObjects,
            OWLManager.createOWLOntologyManager().createOntology(),
            reasonerFactory,
            true,
            true,
            true,
            ioHelper);
    //assertIdentical("/mondo_small.owl", migrated);

    OWLOntologyManager man = migrated.getOWLOntologyManager();

    try {
      for(OWLAxiom ax:migrated.getAxioms()) {
        System.out.println(ax);
      }
      //man.saveOntology(migrated,new FileOutputStream(new File("mondo_test_out.owl")));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
