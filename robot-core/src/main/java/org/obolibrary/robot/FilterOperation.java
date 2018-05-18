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
import org.semanticweb.owlapi.search.EntitySearcher;
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

  public static OWLOntology filter(
      OWLOntology inputOntology, Set<OWLEntity> entities, Set<Class<? extends OWLAxiom>> axiomTypes)
      throws OWLOntologyCreationException {
    // TODO: By default, return with annotations?
    return filter(inputOntology, entities, axiomTypes, true);
  }

  /**
   * Given an input ontology, a set of entities, and a set of axiom types, return a new ontology
   * containing only the desired entities and the desired axiom types.
   *
   * @param inputOntology ontology to filter
   * @param entities entities to keep
   * @param axiomTypes axiom types to keep
   * @return filtered ontology
   * @throws OWLOntologyCreationException on issue creating new ontology
   */
  public static OWLOntology filter(
      OWLOntology inputOntology,
      Set<OWLEntity> entities,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean includeAnnotations)
      throws OWLOntologyCreationException {
    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // TODO: should this automatically copy the ID?
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLEntity entity : entities) {
      // Add referencing axioms ONLY if all entities are in the entity set
      for (OWLAxiom axiom : EntitySearcher.getReferencingAxioms(entity, inputOntology)) {
        if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)
            && entities.containsAll(axiom.getSignature())) {
          axioms.add(axiom);
        }
      }
      // Add annotations for the entities to filter for
      if (includeAnnotations) {
        for (OWLAxiom axiom : EntitySearcher.getAnnotationAssertionAxioms(entity, inputOntology)) {
          axioms.add(axiom);
          // Add declarations for the annotation properties
          for (OWLEntity ap : axiom.getAnnotationPropertiesInSignature()) {
            for (OWLAxiom ax : EntitySearcher.getReferencingAxioms(ap, inputOntology)) {
              if (ax.isOfType(AxiomType.DECLARATION)) {
                axioms.add(ax);
              }
            }
          }
        }
      }
    }
    // Add these axioms to the new output ontology
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
      Set<Class<? extends OWLAxiom>> axiomTypes)
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
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        logger.debug("Filtering axiom: " + axiom.toString());
        outputManager.addAxiom(outputOntology, axiom);
      }
    }
    return outputOntology;
  }
}
