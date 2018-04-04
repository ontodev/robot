package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
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
      OWLOntology inputOntology, Set<OWLEntity> entities, Set<Class<? extends OWLAxiom>> axiomTypes)
      throws OWLOntologyCreationException {
    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // TODO: should this automatically copy the ID?
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLEntity entity : entities) {
      for (OWLAxiom axiom : inputOntology.getAxioms()) {
        if (axiom.getSignature().contains(entity)
            && OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
          axioms.add(axiom);
        }
      }
      // Entities that are the subject of annotation assertions aren't caught by getSignature()
      if (OntologyHelper.extendsAxiomTypes(OWLAnnotationAssertionAxiom.class, axiomTypes)) {
        for (OWLAxiom axiom : EntitySearcher.getAnnotationAssertionAxioms(entity, inputOntology)) {
          axioms.add(axiom);
          // Add declarations for the annotation properties used so they don't get trimmed
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

  /**
   * Given an input ontology, a set of entities, and a set of axiom types, create a new ontology
   * consisting of the complement set to the given entities.
   *
   * @param inputOntology ontology to filter
   * @param entities entities to get the complement set of
   * @param axiomTypes types of axioms to keep
   * @return filtered ontology
   * @throws OWLOntologyCreationException on issue creating output
   */
  public static OWLOntology filterComplement(
      OWLOntology inputOntology, Set<OWLEntity> entities, Set<Class<? extends OWLAxiom>> axiomTypes)
      throws OWLOntologyCreationException {
    Set<OWLEntity> complements = RelatedEntitiesHelper.getComplements(inputOntology, entities);
    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // TODO: should this automatically copy the ID?
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom axiom : inputOntology.getAxioms()) {
      // Only add the axiom to filter if the entity is in the signature
      // AND it's of a specified axiom type
      if (complements.containsAll(axiom.getSignature())
          && OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        // Handle erroneous annotations being added
        if (axiom.isOfType(AxiomType.ANNOTATION_ASSERTION)) {
          if (complements.contains(((OWLAnnotationAssertionAxiom) axiom).getSubject())) {
            axioms.add(axiom);
          }
        } else {
          axioms.add(axiom);
        }
      }
      // Entities that are the subject of annotation assertions aren't caught by getSignature()
      if (OntologyHelper.extendsAxiomTypes(OWLAnnotationAssertionAxiom.class, axiomTypes)) {
        for (OWLEntity entity : complements) {
          axioms.addAll(EntitySearcher.getAnnotationAssertionAxioms(entity, inputOntology));
        }
      }
    }
    outputManager.addAxioms(outputOntology, axioms);
    return outputOntology;
  }
}
