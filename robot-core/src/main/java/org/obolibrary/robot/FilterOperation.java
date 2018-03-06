package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
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
   * Remove axioms from the input ontology based on entities to retain.
   *
   * @param ontology the ontology to filter
   * @param properties a set of OWLObjectProperties to retain
   * @param classes a set of OWLClasses to retain (plus their descendants)
   * @param individuals a set of OWLNamedIndividuals to retain (plus their types)
   * @param annotations a set of OWLAnnotations to retain classes with
   */
  public static void filter(
      OWLOntology ontology,
      Set<OWLObjectProperty> properties,
      Set<OWLClass> classes,
      Set<OWLNamedIndividual> individuals,
      Set<OWLAnnotation> annotations) {
    if (!properties.isEmpty()) {
      filterProperties(ontology, properties);
    }
    if (!classes.isEmpty()) {
      filterClasses(ontology, classes);
    }
    if (!individuals.isEmpty()) {
      filterIndividuals(ontology, individuals);
    }
    if (!annotations.isEmpty()) {
      filterAnnotations(ontology, annotations);
    }
  }

  /**
   * Remove axioms from the input ontology that do not include the given object property or
   * properties.
   *
   * @param ontology the ontology to filter
   * @param properties a set of OWLObjectProperties to retain
   */
  private static void filterProperties(OWLOntology ontology, Set<OWLObjectProperty> properties) {
    logger.debug("Filtering ontology for axioms with ObjectProperties " + properties);

    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLAxiom> axioms = ontology.getAxioms();
    logger.debug("Ontology has {} axioms before filtering", axioms.size());

    // For each axiom, get all its object properties,
    // then remove the properties that we're looking for.
    // If there are no object properties left, then we keep this axiom.
    // All annotation axioms, declarations, and subClass relations remains.
    for (OWLAxiom axiom : axioms) {
      Set<OWLObjectProperty> ps = axiom.getObjectPropertiesInSignature();
      ps.removeAll(properties);
      if (ps.size() > 0) {
        manager.removeAxiom(ontology, axiom);
      }
    }

    logger.debug("Ontology has {} axioms after filtering", ontology.getAxioms().size());
  }

  /**
   * Given an ontology and a set of classes, remove any classes not in the set as well as any
   * individuals that are not types of one of the classes.
   *
   * @param ontology the ontology to filter
   * @param classes the set of classes to filter on
   */
  private static void filterClasses(OWLOntology ontology, Set<OWLClass> classes) {
    logger.debug("Filtering ontology for Classes matching " + classes);
    Set<OWLEntity> entities = new HashSet<>();
    Set<OWLEntity> filteredEntities = new HashSet<>();
    filteredEntities.addAll(classes);
    entities.addAll(ontology.getIndividualsInSignature());
    for (OWLEntity e : entities) {
      for (OWLClassAssertionAxiom axiom :
          ontology.getClassAssertionAxioms(e.asOWLNamedIndividual())) {
        for (OWLClass c : axiom.getClassesInSignature()) {
          if (filteredEntities.contains(c)) {
            filteredEntities.add(e);
          }
        }
      }
    }
    Set<OWLClass> allClasses = ontology.getClassesInSignature();
    entities.addAll(allClasses);
    logger.debug("Ontology has {} classes before filtering", allClasses.size());
    entities.removeAll(filteredEntities);
    RemoveOperation.remove(ontology, entities);
    logger.debug(
        "Ontology has {} classes after filtering", ontology.getClassesInSignature().size());
  }

  /**
   * Given an ontology and a set of named individuals, remove any individuals not in the set as well
   * as any classes that are not in the class assertion axioms. Does not filter any properties.
   *
   * @param ontology the ontology to filter
   * @param individuals the set of named individuals to filter on
   */
  private static void filterIndividuals(OWLOntology ontology, Set<OWLNamedIndividual> individuals) {
    logger.debug("Filtering ontology for NamedIndividuals matching " + individuals);
    Set<OWLEntity> entities = new HashSet<>();
    Set<OWLEntity> filteredEntities = new HashSet<>();
    entities.addAll(ontology.getIndividualsInSignature());
    logger.debug("Ontology has {} individuals before filtering", entities.size());
    entities.addAll(ontology.getClassesInSignature());
    for (OWLNamedIndividual individual : individuals) {
      filteredEntities.add(individual);
      for (OWLClassAssertionAxiom axiom : ontology.getClassAssertionAxioms(individual)) {
        filteredEntities.addAll(axiom.getClassesInSignature());
      }
    }
    entities.removeAll(filteredEntities);
    RemoveOperation.remove(ontology, entities);
    logger.debug(
        "Ontology has {} individuals after filtering", ontology.getIndividualsInSignature().size());
  }

  /**
   * Given an ontology and a set of OWLAnnotations, remove any classes and individuals that do not
   * have at least one of the annotations. Does not filter any properties.
   *
   * @param ontology the ontology to filter
   * @param annotations the set of annotations to filter on
   */
  private static void filterAnnotations(OWLOntology ontology, Set<OWLAnnotation> annotations) {
    logger.debug("Filtering ontology for entities with annotations " + annotations);
    Set<OWLEntity> entities = new HashSet<>();
    Set<OWLEntity> filteredEntities = new HashSet<>();
    entities.addAll(ontology.getClassesInSignature());
    logger.debug("Ontology has {} classes before filtering", entities.size());
    entities.addAll(ontology.getIndividualsInSignature());
    for (OWLEntity e : entities) {
      for (OWLAnnotation compare : EntitySearcher.getAnnotations(e, ontology)) {
        if (annotations.contains(compare)) {
          filteredEntities.add(e);
        }
      }
    }
    entities.removeAll(filteredEntities);
    RemoveOperation.remove(ontology, entities);
    logger.debug(
        "Ontology has {} classes after filtering", ontology.getClassesInSignature().size());
  }
}
