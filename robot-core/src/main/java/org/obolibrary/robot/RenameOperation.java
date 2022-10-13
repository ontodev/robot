package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Rename entity IRIs in an ontology.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RenameOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RenameOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "rename#";

  /** Error message when an entity to rename does not exist. */
  private static final String missingEntityError =
      NS + "MISSING ENTITY ERROR entity to rename ('%s') does not exist.";

  /** Error message when the new IRI is null. */
  private static final String newIRIError =
      NS + "NEW IRI ERROR failed to generate an IRI for '%s'.";

  /** Shared DataFactory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /**
   * Given an ontology, an IOHelper, and a map of old IRIs to new IRIs, rename each old IRI with the
   * new IRI.
   *
   * @param ontology OWLOntology to rename entities in
   * @param ioHelper IOHelper to create IRIs
   * @param mappings map of old IRI to new IRI
   * @param allowMissingEntities if true, command will fail when an entity is in the rename map, but
   *     does not appear in ontology
   * @throws Exception if the old IRI in a mapping does not exist
   */
  public static void renameFull(
      OWLOntology ontology,
      IOHelper ioHelper,
      Map<String, String> mappings,
      boolean allowMissingEntities)
      throws Exception {
    renameFull(ontology, ioHelper, mappings, new HashMap<>(), allowMissingEntities);
  }

  /**
   * Given an ontology, an IOHelper, and a map of old IRIs to new IRIs, rename each old IRI with the
   * new IRI.
   *
   * @param ontology OWLOntology to rename entities in
   * @param ioHelper IOHelper to create IRIs
   * @param mappings map of old IRI to new IRI
   * @param labels map of new IRI to new label
   * @param allowMissingEntities if true, command will fail when an entity is in the rename map, but
   *     does not appear in ontology
   * @throws Exception if the old IRI in a mapping does not exist
   */
  public static void renameFull(
      OWLOntology ontology,
      IOHelper ioHelper,
      Map<String, String> mappings,
      Map<IRI, String> labels,
      boolean allowMissingEntities)
      throws Exception {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLEntityRenamer entityRenamer = new OWLEntityRenamer(manager, Sets.newHashSet(ontology));
    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
      IRI oldIRI = ioHelper.createIRI(mapping.getKey());
      IRI newIRI = ioHelper.createIRI(mapping.getValue());
      if (!ontology.containsEntityInSignature(oldIRI)) {
        if (allowMissingEntities) {
          logger.info("Entity " + oldIRI + " is in map, but does not exist in ontology.");
        } else {
          throw new Exception(String.format(missingEntityError, oldIRI));
        }
      }
      if (newIRI == null) {
        throw new Exception(String.format(newIRIError, mapping.getValue()));
      }
      // Update the IRI first
      manager.applyChanges(entityRenamer.changeIRI(oldIRI, newIRI));
      if (labels.containsKey(newIRI)) {
        // Remove old label annotation(s)
        for (OWLAnnotationAssertionAxiom ax :
            EntitySearcher.getAnnotationAssertionAxioms(
                OntologyHelper.getEntity(ontology, newIRI), ontology)) {
          if (ax.getProperty()
              .getIRI()
              .toString()
              .equals(dataFactory.getRDFSLabel().getIRI().toString())) {
            manager.removeAxiom(ontology, ax);
          }
        }
        // Add the new label
        OWLAnnotation newLabel =
            dataFactory.getOWLAnnotation(
                dataFactory.getRDFSLabel(), dataFactory.getOWLLiteral(labels.get(newIRI)));
        manager.addAxiom(ontology, dataFactory.getOWLAnnotationAssertionAxiom(newIRI, newLabel));
      }
    }
  }

  /**
   * Given an ontology, an IOHelper, and a map of old IRIs to new IRIs, rename each old IRI with the
   * new IRI.
   *
   * @param ontology OWLOntology to rename entities in
   * @param ioHelper IOHelper to create IRIs
   * @param mappings map of old IRI to new IRI
   * @throws Exception if the old IRI in a mapping does not exist
   */
  public static void renameFull(
      OWLOntology ontology, IOHelper ioHelper, Map<String, String> mappings) throws Exception {
    renameFull(ontology, ioHelper, mappings, false);
  }

  /**
   * Given an ontology, an IOHelper, and a map of old IRI bases to new IRI bases, rename each IRI
   * with the 'old base' as a prefix, replacing it with the 'new base'.
   *
   * @param ontology OWLOntology to rename base prefixes in
   * @param ioHelper IOHelper to create IRIs
   * @param mappings map of old base to new base
   */
  public static void renamePrefixes(
      OWLOntology ontology, IOHelper ioHelper, Map<String, String> mappings) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLEntityRenamer entityRenamer = new OWLEntityRenamer(manager, Sets.newHashSet(ontology));
    Set<IRI> allIRIs = OntologyHelper.getIRIs(ontology);
    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
      String oldBase = mapping.getKey();
      String newBase = mapping.getValue();
      Set<IRI> matchIRIs =
          allIRIs.stream()
              .filter(iri -> iri.toString().startsWith(oldBase))
              .collect(Collectors.toSet());
      if (matchIRIs.isEmpty()) {
        logger.warn(String.format("No entities with prefix '%s' to rename", oldBase));
        continue;
      }
      for (IRI iri : matchIRIs) {
        IRI newIRI = ioHelper.createIRI(iri.toString().replace(oldBase, newBase));
        manager.applyChanges(entityRenamer.changeIRI(iri, newIRI));
      }
    }
  }
}
