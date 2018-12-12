package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Extract a set of OWLEntities from the input ontology to an output ontology. Uses the OWLAPI's
 * SyntacticLocalityModuleExtractor (SLME).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExtractOperation.class);

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME). The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param terms a set of IRIs for terms to extract
   * @param outputIRI the OntologyIRI of the new ontology
   * @param moduleType determines the type of extraction; defaults to STAR
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology, Set<IRI> terms, IRI outputIRI, ModuleType moduleType)
      throws OWLOntologyCreationException {
    logger.debug("Extracting...");

    Set<OWLEntity> entities = new HashSet<>();
    for (IRI term : terms) {
      entities.addAll(inputOntology.getEntitiesInSignature(term, true));
    }

    ModuleType type = moduleType;
    if (type == null) {
      type = ModuleType.STAR;
    }

    SyntacticLocalityModuleExtractor extractor =
        new SyntacticLocalityModuleExtractor(
            inputOntology.getOWLOntologyManager(), inputOntology, type);

    return OWLManager.createOWLOntologyManager()
        .createOntology(extractor.extract(entities), outputIRI);
  }
}
