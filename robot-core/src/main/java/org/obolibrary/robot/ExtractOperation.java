package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
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

  /** Shared data factory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** RDFS isDefinedBy annotation property. */
  private static OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

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
    return extract(inputOntology, terms, outputIRI, moduleType, false, null);
  }

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME). The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param terms a set of IRIs for terms to extract
   * @param outputIRI the OntologyIRI of the new ontology
   * @param moduleType determines the type of extraction; defaults to STAR
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   * @param sourceMap map of term IRI to source IRI
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology,
      Set<IRI> terms,
      IRI outputIRI,
      ModuleType moduleType,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap)
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

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology = manager.createOntology(extractor.extract(entities), outputIRI);
    // Maybe annotate entities with rdfs:isDefinedBy
    if (annotateSource) {
      Set<OWLAnnotationAxiom> sourceAxioms = new HashSet<>();
      for (OWLEntity entity : OntologyHelper.getEntities(outputOntology)) {
        // Check if rdfs:isDefinedBy already exists
        Set<OWLAnnotationValue> existingValues =
            OntologyHelper.getAnnotationValues(outputOntology, isDefinedBy, entity.getIRI());
        if (existingValues == null || existingValues.size() == 0) {
          // If not, add it
          sourceAxioms.add(getIsDefinedBy(entity, sourceMap));
        }
      }
      manager.addAxioms(outputOntology, sourceAxioms);
    }

    return outputOntology;
  }

  /**
   * Given an OWLEntity, return an OWLAnnotationAssertionAxiom indicating the source ontology with
   * rdfs:isDefinedBy.
   *
   * @param entity entity to get source of
   * @return OWLAnnotationAssertionAxiom with rdfs:isDefinedBy as the property
   */
  protected static OWLAnnotationAxiom getIsDefinedBy(OWLEntity entity, Map<IRI, IRI> sourceMap) {
    String iri = entity.getIRI().toString();
    IRI base;
    if (sourceMap != null && sourceMap.containsKey(entity.getIRI())) {
      // IRI exists in the prefixes
      base = sourceMap.get(entity.getIRI());
    } else {
      // Brute force edit the IRI string
      // Warning - this may not work with non-OBO Foundry terms, depending on the IRI format!
      if (iri.contains("#")) {
        if (iri.contains(".owl#")) {
          String baseStr = iri.substring(0, iri.lastIndexOf("#")).toLowerCase();
          base = IRI.create(baseStr);
        } else {
          String baseStr = iri.substring(0, iri.lastIndexOf("#")).toLowerCase() + ".owl";
          base = IRI.create(baseStr);
        }
      } else if (iri.contains("_")) {
        String baseStr = iri.substring(0, iri.lastIndexOf("_")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      } else {
        String baseStr = iri.substring(0, iri.lastIndexOf("/")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      }
    }
    return dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), base);
  }
}
