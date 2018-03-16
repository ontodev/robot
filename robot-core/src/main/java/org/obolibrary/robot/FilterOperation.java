package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
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
      OWLOntology ontology, Set<IRI> entityIRIs, Set<IRI> descendantIRIs, Set<IRI> nodeIRIs) {
    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    Set<OWLClass> classes = new HashSet<>();
    Set<OWLObjectProperty> properties = new HashSet<>();
    Set<OWLNamedIndividual> individuals = new HashSet<>();
    if (!entityIRIs.isEmpty()) {
      for (IRI iri : entityIRIs) {
        classes.add(dataFactory.getOWLClass(iri));
        properties.add(dataFactory.getOWLObjectProperty(iri));
        individuals.add(dataFactory.getOWLNamedIndividual(iri));
      }
      filterClasses(ontology, classes);
      filterProperties(ontology, properties);
      filterIndividuals(ontology, individuals);
    }

    if (!descendantIRIs.isEmpty()) {
      classes.clear();
      properties.clear();
      for (IRI iri : descendantIRIs) {
        RelatedEntitiesHelper.getDescendants(ontology, dataFactory.getOWLClass(iri), classes);
        RelatedEntitiesHelper.getDescendants(
            ontology, dataFactory.getOWLObjectProperty(iri), properties);
      }
      filterClasses(ontology, classes);
      filterProperties(ontology, properties);
    }

    if (!nodeIRIs.isEmpty()) {
      classes.clear();
      properties.clear();
      for (IRI iri : nodeIRIs) {
        OWLClass cls = dataFactory.getOWLClass(iri);
        RelatedEntitiesHelper.getDescendants(ontology, cls, classes);
        classes.add(cls);
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(iri);
        RelatedEntitiesHelper.getDescendants(ontology, property, properties);
        properties.add(property);
      }
      filterClasses(ontology, classes);
      filterProperties(ontology, properties);
    }
  }

  /**
   * Given an ontology and a set of OWLAnnotations, remove any classes and individuals that do not
   * have at least one of the annotations. Does not filter any properties.
   *
   * @param ontology the ontology to filter
   * @param annotations the set of annotations to filter on
   * @throws Exception
   */
  public static OWLOntology filterAnnotations(
      OWLOntology inputOntology, Set<OWLAnnotation> annotations) throws Exception {
    logger.debug("Filtering ontology for entities with annotations " + annotations);
    Set<OWLEntity> entities = new HashSet<>();
    Set<OWLEntity> filteredEntities = new HashSet<>();
    entities.addAll(inputOntology.getClassesInSignature());
    logger.debug("Ontology has {} classes before filtering", entities.size());
    entities.addAll(inputOntology.getIndividualsInSignature());
    for (OWLEntity e : entities) {
      for (OWLAnnotation compare : EntitySearcher.getAnnotations(e, inputOntology)) {
        if (annotations.contains(compare)) {
          logger.debug("Found match: " + e.toStringID());
          filteredEntities.add(e);
        }
      }
    }
    OWLOntology filteredOntology =
        OWLManager.createOWLOntologyManager().createOntology(inputOntology.getOntologyID());
    for (OWLEntity e : filteredEntities) {
      OntologyHelper.copy(inputOntology, filteredOntology, e, null);
    }
    logger.debug(
        "Ontology has {} classes after filtering", filteredOntology.getClassesInSignature().size());
    return filteredOntology;
  }

  /**
   * Given an ontology and a set of classes, remove any classes not in the set as well as any
   * individuals that are not types of one of the classes.
   *
   * @param ontology the ontology to filter
   * @param classes the set of classes to filter on
   */
  public static void filterClasses(OWLOntology ontology, Set<OWLClass> classes) {
	// TODO: change to return a new, copied ontology
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
  public static void filterIndividuals(OWLOntology ontology, Set<OWLNamedIndividual> individuals) {
	// TODO: change to return a new, copied ontology
	logger.debug("Filtering ontology for NamedIndividuals matching " + individuals);
    Set<OWLNamedIndividual> removeIndividuals = ontology.getIndividualsInSignature();
    Set<OWLClass> removeClasses = ontology.getClassesInSignature();
    logger.debug("Ontology has {} individuals before filtering", removeIndividuals.size());
    removeIndividuals.removeAll(individuals);
    for (OWLNamedIndividual individual : individuals) {
      for (OWLClassExpression type : RelatedEntitiesHelper.getTypes(ontology, individual)) {
        if (!type.isAnonymous()) {
          removeClasses.remove(type.asOWLClass());
        }
      }
    }
    Set<OWLEntity> remove = new HashSet<>();
    remove.addAll(removeIndividuals);
    remove.addAll(removeClasses);
    RemoveOperation.remove(ontology, remove);
    logger.debug(
        "Ontology has {} individuals after filtering", ontology.getIndividualsInSignature().size());
  }

  /**
   * Remove axioms from the input ontology that do not include the given object property or
   * properties.
   *
   * @param ontology the ontology to filter
   * @param properties a set of OWLObjectProperties to retain
   */
  public static void filterProperties(OWLOntology ontology, Set<OWLObjectProperty> properties) {
	// TODO: change to return a new, copied ontology
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
}
