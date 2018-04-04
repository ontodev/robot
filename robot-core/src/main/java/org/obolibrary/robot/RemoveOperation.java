package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RemoveOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "remove#";

  /** Error message when an import ontology does not have an IRI. */
  private static final String nullIRIError =
      NS + "NULL IRI ERROR import ontology does not have an IRI";

  /** Error message when the ontology does not import the provided IRI. Expects: IRI as string. */
  private static final String missingImportError =
      NS + "MISSING IMPORT ERROR ontology does not contain import: %s";

  /**
   * Given an OWLOntology, a set of entities, and a set of axiom types, remove the entities and
   * associated axioms (of the given types) from the ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param entity OWLEntity to remove
   * @param axiomTypes types of axioms to remove
   */
  public static void remove(
      OWLOntology ontology, Set<OWLEntity> entities, Set<Class<? extends OWLAxiom>> axiomTypes) {
    for (OWLEntity entity : entities) {
      remove(ontology, entity, axiomTypes);
    }
  }

  /**
   * Given an OWLOntology, an OWLEntity, and a set of axiom types, remove the entity and associated
   * axioms (of the given types) from the ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param entity OWLEntity to remove
   * @param axiomTypes types of axioms to remove
   */
  public static void remove(
      OWLOntology ontology, OWLEntity entity, Set<Class<? extends OWLAxiom>> axiomTypes) {
    logger.debug("Removing from ontology: " + entity);
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      // Add the axiom to remove if the entity is in the signature
      // and the axiom is of the correct type
      if (axiom.getSignature().contains(entity)
          && OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        axioms.add(axiom);
      }
    }
    // Entities that are the subject of annotation assertions aren't caught by getSignature()
    if (OntologyHelper.extendsAxiomTypes(OWLAnnotationAssertionAxiom.class, axiomTypes)) {
      axioms.addAll(EntitySearcher.getAnnotationAssertionAxioms(entity, ontology));
    }
    // Remove all
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxioms(ontology, axioms);
  }

  /**
   * Given an ontology, a set of entities, and a set of relation types, remove axioms containing
   * references to anonymous entities based on their relation to the entities in the set.
   *
   * @param ontology the OWLOntology to remove from
   * @param entities set of entities to search
   * @param relationTypes set of relation types to search on
   */
  public static void removeAnonymous(
      OWLOntology ontology,
      Set<OWLEntity> entities,
      Set<RelationType> relationTypes,
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLAxiom> anonAxioms = new HashSet<>();
    for (RelationType rt : relationTypes) {
      if (rt.equals(RelationType.ANCESTORS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousAncestorAxioms(ontology, entity));
        }
      } else if (rt.equals(RelationType.DESCENDANTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousDescendantAxioms(ontology, entity));
        }
      } else if (rt.equals(RelationType.PARENTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousParentAxioms(ontology, entity));
        }
      } else if (rt.equals(RelationType.EQUIVALENTS)) {
        for (OWLEntity entity : entities) {
          anonAxioms.addAll(OntologyHelper.getAnonymousEquivalentAxioms(ontology, entity));
        }
      }
    }
    // Remove axioms by type
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    for (OWLAxiom axiom : anonAxioms) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        logger.debug("Removing axiom: " + axiom.toString());
        manager.removeAxiom(ontology, axiom);
      }
    }
  }

  /**
   * Given an ontology, a set of entities, and a set of axiom types, remove the complement of that
   * set.
   *
   * @param ontology OWLOntology to remove from
   * @param entities set of OWLEntities to remove complement of
   * @param axiomTypes types of axioms to remove
   */
  public static void removeComplement(
      OWLOntology ontology, Set<OWLEntity> entities, Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLEntity> complements = RelatedEntitiesHelper.getComplements(ontology, entities);
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      // Add the axiom to remove if all entities in the signature are being removed
      // and it is of the correct axiom type
      if (complements.containsAll(axiom.getSignature())
          && OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        axioms.add(axiom);
      }
    }
    // Entities that are the subject of annotation assertions aren't caught by getSignature()
    if (OntologyHelper.extendsAxiomTypes(OWLAnnotationAssertionAxiom.class, axiomTypes)) {
      for (OWLEntity entity : entities) {
        // Make sure we aren't removing annotations on the entity we want to keep
        for (OWLAxiom axiom : EntitySearcher.getAnnotationAssertionAxioms(entity, ontology)) {
          axioms.remove(axiom);
          // Also keep the declarations of those annotation properties
          for (OWLEntity ap : axiom.getAnnotationPropertiesInSignature()) {
            for (OWLAxiom ax : EntitySearcher.getReferencingAxioms(ap, ontology)) {
              if (ax.isOfType(AxiomType.DECLARATION)) {
                axioms.remove(ax);
              }
            }
          }
        }
      }
    }
    // Remove all
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxioms(ontology, axioms);
  }

  /**
   * Given an OWLOntology, remove all import axioms.
   *
   * @param ontology OWLOntology to remove from
   * @throws Exception if any import does not have an IRI
   */
  public static void removeImports(OWLOntology ontology) throws Exception {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    for (OWLOntology i : ontology.getImports()) {
      IRI iri = i.getOntologyID().getOntologyIRI().orNull();
      if (iri == null) {
        throw new Exception(nullIRIError);
      }
      manager.applyChange(
          new RemoveImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(iri)));
    }
  }

  /**
   * Given an OWLOntology and the IRI of an import ontology, remove the import axiom.
   *
   * @param ontology OWLOntology to remove from
   * @param importIRI import IRI to remove
   * @throws Exception if the ontology does not contain the import
   */
  public static void removeImports(OWLOntology ontology, IRI importIRI) throws Exception {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLOntology importOntology = null;
    for (OWLOntology i : ontology.getImports()) {
      IRI iri = i.getOntologyID().getOntologyIRI().orNull();
      if (iri == importIRI) {
        manager.applyChange(
            new RemoveImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(iri)));
        break;
      }
    }
    if (importOntology == null) {
      throw new Exception(String.format(missingImportError, importIRI.toString()));
    }
  }
}
