package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "remove#";

  /** Error message when an import ontology does not have an IRI. */
  private static final String nullIRIError =
      NS + "NULL IRI ERROR import ontology does not have an IRI";

  /** Error message when the ontology does not import the provided IRI. Expects: IRI as string. */
  private static final String missingImportError =
      NS + "MISSING IMPORT ERROR ontology does not contain import: %s";

  /** Error message when the entity type for an operation is not valid. */
  private static final String entityTypeError =
      NS + "ENTITY TYPE ERROR %s is not a valid type to retrieve %s";

  /**
   * @param ontology
   * @param iri
   * @throws Exception
   */
  public static void remove(OWLOntology ontology, IRI iri) throws Exception {
    OWLEntity entity = OntologyHelper.getEntity(ontology, iri);
    remove(ontology, entity);
  }

  /**
   * @param ontology
   * @param entities
   */
  public static void remove(OWLOntology ontology, Set<OWLEntity> entities) {
    for (OWLEntity entity : entities) {
      remove(ontology, entity);
    }
  }

  /**
   * Given an OWLOntology and an OWLEntity, remove the entity and associated axioms from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param entity OWLEntity to remove
   */
  public static void remove(OWLOntology ontology, OWLEntity entity) {
    logger.debug("Removing from ontology: " + entity);

    Set<OWLAxiom> axioms = new HashSet<>();
    // Add any logical axioms using the class entity

    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (axiom.getSignature().contains(entity)) {
        axioms.add(axiom);
      }
    }
    // Add any assertions on the class entity
    axioms.addAll(EntitySearcher.getAnnotationAssertionAxioms(entity.getIRI(), ontology));
    axioms.addAll(EntitySearcher.getReferencingAxioms(entity, ontology));
    // Remove all
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxioms(ontology, axioms);
  }

  /**
   * Given an OWLOntology, remove ALL anonymous superclasses asserted in the ontology.
   *
   * @param ontology OWLOntology to remove from
   */
  public static void removeAnonymousClasses(OWLOntology ontology) {
    Set<OWLClass> classes = ontology.getClassesInSignature();
    for (OWLClass cls : classes) {
      removeAnonymousClasses(ontology, cls);
    }
  }

  public static void removeAnonymousClasses(OWLOntology ontology, IRI iri) throws Exception {
    OWLEntity entity = OntologyHelper.getEntity(ontology, iri);
    if (entity.isOWLClass()) {
      removeAnonymousClasses(ontology, entity.asOWLClass());
    } else {
      throw new Exception(
          String.format(entityTypeError, entity.getEntityType().toString(), "anonymous classes"));
    }
  }

  /**
   * Given an OWLOntology and an OWLClass, remove any anonymous superclasses of the class from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param cls OWLClass to remove anonymous superclasses of
   */
  public static void removeAnonymousClasses(OWLOntology ontology, OWLClass cls) {
    // Get set of anonymous superclass axioms
    Set<OWLAxiom> anons = new HashSet<>();
    for (OWLAxiom ax : ontology.getSubClassAxiomsForSubClass(cls)) {
      for (OWLClassExpression ex : ax.getNestedClassExpressions()) {
        if (ex.isAnonymous()) {
          anons.add(ax);
        }
      }
    }
    for (OWLAxiom ax : ontology.getEquivalentClassesAxioms(cls)) {
      for (OWLClassExpression ex : ax.getNestedClassExpressions()) {
        if (ex.isAnonymous()) {
          anons.add(ax);
        }
      }
    }
    // Remove axioms
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxioms(ontology, anons);
  }

  /**
   * @param ontology
   * @param iri
   * @throws Exception
   */
  public static void removeDescendants(OWLOntology ontology, IRI iri) throws Exception {
    removeDescendants(ontology, OntologyHelper.getEntity(ontology, iri));
  }

  /**
   * @param ontology
   * @param entity
   * @throws Exception
   */
  public static void removeDescendants(OWLOntology ontology, OWLEntity entity) throws Exception {
    if (entity.isOWLClass()) {
      removeDescendants(ontology, entity.asOWLClass());
    } else if (entity.isOWLAnnotationProperty()) {
      removeDescendants(ontology, entity.asOWLAnnotationProperty());
    } else if (entity.isOWLDataProperty()) {
      removeDescendants(ontology, entity.asOWLDataProperty());
    } else if (entity.isOWLObjectProperty()) {
      removeDescendants(ontology, entity.asOWLObjectProperty());
    } else {
      throw new Exception(
          String.format(entityTypeError, entity.getEntityType().toString(), "descendants"));
    }
  }

  /**
   * @param ontology
   * @param cls
   */
  public static void removeDescendants(OWLOntology ontology, OWLClass cls) {
    Set<OWLClass> descendants = new HashSet<>();
    RelatedEntitiesHelper.getDescendants(ontology, cls, descendants);
    for (OWLClass descendant : descendants) {
      remove(ontology, descendant);
    }
  }

  /**
   * @param ontology
   * @param annotationProperty
   */
  public static void removeDescendants(
      OWLOntology ontology, OWLAnnotationProperty annotationProperty) {
    Set<OWLAnnotationProperty> descendants = new HashSet<>();
    RelatedEntitiesHelper.getDescendants(ontology, annotationProperty, descendants);
    for (OWLAnnotationProperty descendant : descendants) {
      remove(ontology, descendant);
    }
  }

  /**
   * @param ontology
   * @param dataProperty
   */
  public static void removeDescendants(OWLOntology ontology, OWLDataProperty dataProperty) {
    Set<OWLDataProperty> descendants = new HashSet<>();
    RelatedEntitiesHelper.getDescendants(ontology, dataProperty, descendants);
    for (OWLDataProperty descendant : descendants) {
      remove(ontology, descendant);
    }
  }

  /**
   * @param ontology
   * @param objectProperty
   */
  public static void removeDescendants(OWLOntology ontology, OWLObjectProperty objectProperty) {
    Set<OWLObjectProperty> descendants = new HashSet<>();
    RelatedEntitiesHelper.getDescendants(ontology, objectProperty, descendants);
    for (OWLObjectProperty descendant : descendants) {
      remove(ontology, descendant);
    }
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

  /**
   * Given an OWLOntology, remove all named individuals and associated axioms from the ontology.
   *
   * @param ontology OWLOntology to remove from
   */
  public static void removeIndividuals(OWLOntology ontology) {
    Set<OWLNamedIndividual> indivs = ontology.getIndividualsInSignature();
    for (OWLNamedIndividual i : indivs) {
      remove(ontology, i);
    }
  }

  /**
   * Given an OWLOntology and a set of OWLAnnotations, remove any entities that have one of the
   * annotations in the set.
   *
   * @param ontology OWLOntology to remove from
   * @param annotations OWLAnnotations to remove entities with
   */
  public static void removeWithAnnotations(OWLOntology ontology, Set<OWLAnnotation> annotations) {
    Set<OWLEntity> entities = new HashSet<>();
    entities.addAll(OntologyHelper.getEntities(ontology));
    for (OWLEntity e : entities) {
      for (OWLAnnotation compare : EntitySearcher.getAnnotations(e, ontology)) {
        if (annotations.contains(compare)) {
          remove(ontology, e);
        }
      }
    }
  }
}
