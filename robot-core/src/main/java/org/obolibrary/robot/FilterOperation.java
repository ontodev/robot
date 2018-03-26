package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter the axioms of an ontology by given criteria.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterOperation.class);

  /**
   * Given an input ontology, a set of entities, and a set of axiom types, create a new ontology
   * consisting of the entities in the set with only the given axiom types.
   *
   * @param inputOntology ontology to filter
   * @param entities entities to keep
   * @param axiomTypes types of axioms to keep
   * @return filtered ontology
   * @throws OWLOntologyCreationException on issue creating output
   */
  public static OWLOntology filter(
      OWLOntology inputOntology, Set<OWLEntity> entities, Set<AxiomType<?>> axiomTypes)
      throws OWLOntologyCreationException {
    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // TODO: should this automatically copy the ID?
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLEntity entity : entities) {
      for (OWLAxiom axiom : inputOntology.getAxioms()) {
        // Only add the axiom to filter if the entity is in the signature
        // AND it's of a specified axiom type
        if (axiom.getSignature().contains(entity) && axiomTypes.contains(axiom.getAxiomType())) {
          logger.debug("Filtering axiom: " + axiom.toString());
          axioms.add(axiom);
        }
      }
    }
    outputManager.addAxioms(outputOntology, axioms);
    return outputOntology;
  }

  /**
   * Given an input ontology, a set of entities, a set of relation types, and a set of axiom types,
   * create a new ontology consisting of related entities with only the given axiom types.
   *
   * @param inputOntology ontology to filter
   * @param entities entities to start with
   * @param relationTypes related entities to keep
   * @param axiomTypes types of axioms to keep
   * @return filtered ontology
   * @throws OWLOntologyCreationException
   */
  public static OWLOntology filterAnonymous(
      OWLOntology inputOntology,
      Set<OWLEntity> entities,
      Set<RelationType> relationTypes,
      Set<AxiomType<?>> axiomTypes)
      throws OWLOntologyCreationException {
    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // TODO: should this automatically copy the ID?
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());
    Set<OWLAxiom> anonAxioms = new HashSet<>();
    for (RelationType rt : relationTypes) {
      if (rt.equals(RelationType.ANCESTORS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousAncestorAxioms(inputOntology, entity));
        }
      } else if (rt.equals(RelationType.DESCENDANTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousDescendantAxioms(inputOntology, entity));
        }
      } else if (rt.equals(RelationType.PARENTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousParentAxioms(inputOntology, entity));
        }
      } else if (rt.equals(RelationType.EQUIVALENTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousEquivalentAxioms(inputOntology, entity));
        }
      }
    }
    // Filter axioms by type
    for (OWLAxiom axiom : anonAxioms) {
      if (axiomTypes.contains(axiom.getAxiomType())) {
        logger.debug("Filtering axiom: " + axiom.toString());
        outputManager.addAxiom(outputOntology, axiom);
      }
    }
    return outputOntology;
  }
}
