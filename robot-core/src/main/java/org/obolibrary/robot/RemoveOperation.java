package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOperation {

  /** Data factory. */
  private static final OWLDataFactory factory =
      OWLManager.createOWLOntologyManager().getOWLDataFactory();

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterOperation.class);
  
  /** Namespace for error messages. */
  private static final String NS = "remove#";
  
  /** Error message when an import ontology does not have an IRI. */
  private static final String nullIRIError = NS + "NULL IRI ERROR import ontology does not have an IRI";
  
  /** Error message when the ontology does not import the provided IRI. Expects: IRI as string. */
  private static final String missingImportError = NS + "MISSING IMPORT ERROR ontology does not contain import: %s";

  /**
   * Given an OWLOntology and a set of IRIs, remove the entities represented by the IRIs from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param IRIs set of IRIs to remove
   */
  public static void remove(OWLOntology ontology, Set<IRI> IRIs) {
    for (IRI iri : IRIs) {
      remove(ontology, iri);
    }
  }

  /**
   * Given an OWLOntology and an IRI, remove the entity represented by the IRI from the ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param iri IRI of the entity to remove
   */
  public static void remove(OWLOntology ontology, IRI iri) {
    // Try for each OWLEntity type
    remove(ontology, factory.getOWLClass(iri));
    remove(ontology, factory.getOWLNamedIndividual(iri));
    remove(ontology, factory.getOWLObjectProperty(iri));
    remove(ontology, factory.getOWLAnnotationProperty(iri));
    remove(ontology, factory.getOWLDataProperty(iri));
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

  /**
   * Given an OWLOntology and a string classID, remove all anonymous superclasses of the class from
   * the ontology. If classID = all, remove ALL anonymous superclasses asserted in the ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param classID CURIE of class to remove anon superclasses, or all
   */
  public static void removeAnonymousClasses(OWLOntology ontology, IRI iri) {
    OWLClass cls = factory.getOWLClass(iri);
    removeAnonymousClasses(ontology, cls);
  }

  /**
   * Given an OWLOntology and a set of entity IDs, remove the descendants of all the entities. Does
   * not remove the entity itself.
   *
   * @param ontology OWLOntology to remove from
   * @param IRIs set of IRIs to remove
   */
  public static void removeDescendants(OWLOntology ontology, Set<IRI> IRIs) {
    for (IRI iri : IRIs) {
      removeDescendants(ontology, iri);
    }
  }

  /**
   * Given an OWLOntology and an entityID, remove all descendants of the entity. Retains the entity
   * itself.
   *
   * @param ontology OWLOntology to remove from
   * @param iri IRI of entity to remove
   */
  public static void removeDescendants(OWLOntology ontology, IRI iri) {
    Set<OWLObject> descendants =
        RelatedEntitiesHelper.getRelatedEntities(ontology, iri, "descendants", true);
    for (OWLObject descendant : descendants) {
      for (OWLEntity e : descendant.getSignature()) {
        remove(ontology, e);
      }
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
    entities.addAll(ontology.getClassesInSignature());
    entities.addAll(ontology.getAnnotationPropertiesInSignature());
    entities.addAll(ontology.getObjectPropertiesInSignature());
    entities.addAll(ontology.getDataPropertiesInSignature());
    entities.addAll(ontology.getIndividualsInSignature());
    for (OWLEntity e : entities) {
      for (OWLAnnotation compare : EntitySearcher.getAnnotations(e, ontology)) {
        if (annotations.contains(compare)) {
          remove(ontology, e);
        }
      }
    }
  }

  /**
   * Given an OWLOntology and an OWLEntity, remove the entity and associated axioms from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param entity OWLEntity to remove
   */
  private static void remove(OWLOntology ontology, OWLEntity entity) {
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
   * Given an OWLOntology and an OWLClass, remove any anonymous superclasses of the class from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param cls OWLClass to remove anonymous superclasses of
   */
  private static void removeAnonymousClasses(OWLOntology ontology, OWLClass cls) {
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
}
