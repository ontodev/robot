package org.obolibrary.robot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Implements several variations on MIREOT, as first described in "MIREOT: The minimum information
 * to reference an external ontology term" (<a
 * href="http://dx.doi.org/10.3233/AO-2011-0087">link</a>).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MireotOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MireotOperation.class);

  /** Shared data factory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** RDFS isDefinedBy annotation property. */
  private static OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

  /**
   * Get a set of default annotation properties. Currenly includes only RDFS label.
   *
   * @return a set of annotation properties
   */
  public static Set<OWLAnnotationProperty> getDefaultAnnotationProperties() {
    Set<OWLAnnotationProperty> annotationProperties = new HashSet<>();
    annotationProperties.add(dataFactory.getRDFSLabel());
    return annotationProperties;
  }

  /**
   * Given an ontology, a set of upper-level IRIs, a set of lower-level IRIs, and a set of
   * annotation properties, return a new ontology with just the named ancestors of those terms,
   * their subclass relations, and the selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs ancestors will be copied up to and including these terms
   * @param lowerIRIs copy these terms and their superclasses
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getAncestors(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<IRI> lowerIRIs,
      Set<OWLAnnotationProperty> annotationProperties)
      throws OWLOntologyCreationException {
    return getAncestors(inputOntology, upperIRIs, lowerIRIs, annotationProperties, false, null);
  }

  /**
   * Given an ontology, a set of upper-level IRIs, a set of lower-level IRIs, a set of annotation
   * properties, and a boolean indiciating if rdfs:isDefinedBy should be added to copied classes,
   * return a new ontology with just the named ancestors of those terms, their subclass relations,
   * and the selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs ancestors will be copied up to and including these terms
   * @param lowerIRIs copy these terms and their superclasses
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   * @param sourceMap map of term IRI to source IRI
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getAncestors(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<IRI> lowerIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inputOntology);

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // The other OWLAPI extract methods use the source ontology IRI
    // so we'll use it here too.
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());

    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
    }

    Set<OWLEntity> lowerEntities = OntologyHelper.getEntities(inputOntology, lowerIRIs);
    for (OWLEntity entity : lowerEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
      copyAncestors(
          reasoner,
          inputOntology,
          outputOntology,
          upperEntities,
          entity,
          annotationProperties,
          annotateSource,
          sourceMap);
    }
    return outputOntology;
  }

  /**
   * Given a reasoner, input and output ontologies, a set of upper entitities, a target entity, and
   * a set of annotation properties, copy the target entity and all its named ancestors
   * (recursively) from the input ontology to the output ontology, along with the specified
   * annotations. The input ontology is not changed.
   *
   * @param reasoner use to find superclasses
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param upperEntities the top level of entities, or null
   * @param entity the target entity that will have its ancestors copied
   * @param annotationProperties the annotations to copy, or null for all
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   */
  private static void copyAncestors(
      OWLReasoner reasoner,
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      Set<OWLEntity> upperEntities,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    // If this is an upperEntity, copy it and return.
    if (upperEntities != null && upperEntities.contains(entity)) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      return;
    }

    // Otherwise copy ancestors recursively.
    if (entity.isOWLClass()) {
      Set<OWLClass> superclasses =
          reasoner.getSuperClasses(entity.asOWLClass(), true).getFlattened();
      for (OWLClass superclass : superclasses) {
        OntologyHelper.copy(inputOntology, outputOntology, superclass, annotationProperties);
        outputManager.addAxiom(
            outputOntology, dataFactory.getOWLSubClassOfAxiom(entity.asOWLClass(), superclass));
        copyAncestors(
            reasoner,
            inputOntology,
            outputOntology,
            upperEntities,
            superclass,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> superproperies =
          EntitySearcher.getSuperProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty superproperty : superproperies) {
        OntologyHelper.copy(inputOntology, outputOntology, superproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                entity.asOWLAnnotationProperty(), superproperty));
        copyAncestors(
            reasoner,
            inputOntology,
            outputOntology,
            upperEntities,
            superproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLObjectProperty()) {
      Set<OWLObjectPropertyExpression> superproperies =
          reasoner.getSuperObjectProperties(entity.asOWLObjectProperty(), true).getFlattened();
      for (OWLObjectPropertyExpression superexpression : superproperies) {
        if (superexpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty superproperty = superexpression.asOWLObjectProperty();
        OntologyHelper.copy(inputOntology, outputOntology, superproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubObjectPropertyOfAxiom(
                entity.asOWLObjectProperty(), superproperty));
        copyAncestors(
            reasoner,
            inputOntology,
            outputOntology,
            upperEntities,
            superproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLDataProperty()) {
      Set<OWLDataProperty> superproperies =
          reasoner.getSuperDataProperties(entity.asOWLDataProperty(), true).getFlattened();
      for (OWLDataProperty superproperty : superproperies) {
        OntologyHelper.copy(inputOntology, outputOntology, superproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubDataPropertyOfAxiom(entity.asOWLDataProperty(), superproperty));
        copyAncestors(
            reasoner,
            inputOntology,
            outputOntology,
            upperEntities,
            superproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    }

    // Annotate with rdfs:isDefinedBy (maybe)
    if (annotateSource) {
      maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
    }
  }

  /**
   * Given an ontology, a set of upper-level IRIs, and a set of annotation properties, return a new
   * ontology with just those terms and their named descendants, their subclass relations, and the
   * selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs these terms and their descendants will be copied
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getDescendants(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<OWLAnnotationProperty> annotationProperties)
      throws OWLOntologyCreationException {
    return getDescendants(inputOntology, upperIRIs, annotationProperties, false, null);
  }

  /**
   * Given an ontology, a set of upper-level IRIs, and a set of annotation properties, return a new
   * ontology with just those terms and their named descendants, their subclass relations, and the
   * selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs these terms and their descendants will be copied
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   * @param sourceMap map of term IRI to source IRI
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getDescendants(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inputOntology);

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology = outputManager.createOntology();

    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
      copyDescendants(
          reasoner,
          inputOntology,
          outputOntology,
          entity,
          annotationProperties,
          annotateSource,
          sourceMap);
    }

    return outputOntology;
  }

  /**
   * Given a reasoner, input and output ontologies, a target entity, and a set of annotation
   * properties, copy the target entity and all its named descendants (recursively) from the input
   * ontology to the output ontology, along with the specified annotations. The input ontology is
   * not changed.
   *
   * @param reasoner use to find superclasses
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param entity the target entity that will have its descendants copied
   * @param annotationProperties the annotations to copy, or null for all
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   */
  private static void copyDescendants(
      OWLReasoner reasoner,
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    if (entity.isOWLClass()) {
      Set<OWLClass> subclasses = reasoner.getSubClasses(entity.asOWLClass(), true).getFlattened();
      for (OWLClass subclass : subclasses) {
        if (subclass == dataFactory.getOWLNothing()) {
          continue;
        }
        OntologyHelper.copy(inputOntology, outputOntology, subclass, annotationProperties);
        outputManager.addAxiom(
            outputOntology, dataFactory.getOWLSubClassOfAxiom(subclass, entity.asOWLClass()));
        copyDescendants(
            reasoner,
            inputOntology,
            outputOntology,
            subclass,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> subproperies =
          EntitySearcher.getSubProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty subproperty : subproperies) {
        OntologyHelper.copy(inputOntology, outputOntology, subproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                subproperty, entity.asOWLAnnotationProperty()));
        copyDescendants(
            reasoner,
            inputOntology,
            outputOntology,
            subproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLObjectProperty()) {
      Set<OWLObjectPropertyExpression> subproperies =
          reasoner.getSubObjectProperties(entity.asOWLObjectProperty(), true).getFlattened();
      for (OWLObjectPropertyExpression subexpression : subproperies) {
        if (subexpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty subproperty = subexpression.asOWLObjectProperty();
        OntologyHelper.copy(inputOntology, outputOntology, subproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubObjectPropertyOfAxiom(subproperty, entity.asOWLObjectProperty()));
        copyDescendants(
            reasoner,
            inputOntology,
            outputOntology,
            subproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    } else if (entity.isOWLDataProperty()) {
      Set<OWLDataProperty> subproperies =
          reasoner.getSubDataProperties(entity.asOWLDataProperty(), true).getFlattened();
      for (OWLDataProperty subproperty : subproperies) {
        OntologyHelper.copy(inputOntology, outputOntology, subproperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubDataPropertyOfAxiom(subproperty, entity.asOWLDataProperty()));
        copyDescendants(
            reasoner,
            inputOntology,
            outputOntology,
            subproperty,
            annotationProperties,
            annotateSource,
            sourceMap);
      }
    }

    // Annotate with rdfs:isDefinedBy (maybe)
    if (annotateSource) {
      maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
    }
  }

  /**
   * Given an ontology, a manager for that ontology, an entity to annotate, and a map of source
   * replacements, add the rdfs:isDefinedBy annotation to the entity.
   *
   * @param ontology OWLOntology to add annotation in
   * @param manager OWLManager for the ontology
   * @param entity OWLEntity to add annotation on
   * @param sourceMap term-to-source map
   */
  private static void maybeAnnotateSource(
      OWLOntology ontology, OWLOntologyManager manager, OWLEntity entity, Map<IRI, IRI> sourceMap) {
    Set<OWLAnnotationValue> existingValues =
        OntologyHelper.getAnnotationValues(ontology, isDefinedBy, entity.getIRI());
    if (existingValues == null || existingValues.size() == 0) {
      manager.addAxiom(ontology, ExtractOperation.getIsDefinedBy(entity, sourceMap));
    }
  }
}
