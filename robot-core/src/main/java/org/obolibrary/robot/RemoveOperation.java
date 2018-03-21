package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
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

  /** Error message when the entity type for an operation is not valid. */
  private static final String entityTypeError =
      NS + "ENTITY TYPE ERROR %s is not a valid type to retrieve %s";

  /**
   * @param ontology
   * @param entities
   * @param axiomTypes
   */
  public static void remove(
      OWLOntology ontology, Set<OWLEntity> entities, Set<AxiomType<?>> axiomTypes) {
    for (OWLEntity entity : entities) {
      remove(ontology, entity, axiomTypes);
    }
  }

  /**
   * Given an OWLOntology and an OWLEntity, remove the entity and associated axioms from the
   * ontology.
   *
   * @param ontology OWLOntology to remove from
   * @param entity OWLEntity to remove
   */
  public static void remove(OWLOntology ontology, OWLEntity entity, Set<AxiomType<?>> axiomTypes) {
    logger.debug("Removing from ontology: " + entity);
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      // Only add the axiom to remove if the entity is in the signature
      // AND it's of a specified axiom type
      if (axiom.getSignature().contains(entity) && axiomTypes.contains(axiom.getAxiomType())) {
        logger.debug("Removing axiom: " + axiom.toString());
        axioms.add(axiom);
      }
    }
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

  public static void removeAxioms(OWLOntology ontology, Set<AxiomType<?>> axiomTypes) {
    if (axiomTypes.isEmpty()) {
      return;
    }
    logger.debug("REMOVING AXIOMS OF TYPE(S):");
    axiomTypes.forEach(at -> logger.debug(at.toString()));
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (axiomTypes.contains(axiom.getAxiomType())) {
        manager.removeAxiom(ontology, axiom);
      }
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
}
