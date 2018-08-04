package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.ReferencedEntitySetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides convenience methods for working with OWL ontologies.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class OntologyHelper {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(OntologyHelper.class);

  /** Namespace for general ontology error messages. */
  private static final String NS = "errors#";

  /** Error message when an unsupported axiom type is requested. Expects axiom Class. */
  private static final String axiomTypeError =
      NS + "AXIOM TYPE ERROR cannot annotate axioms of type: %s";

  /** Error message when the ontology does not contain any of the terms. */
  private static final String emptyTermsError =
      NS + "EMPTY TERMS ERROR ontology does not contain input terms";

  /** Error message when entity does not exist in the ontology. */
  private static final String missingEntityError =
      NS + "MISSING ENTITY ERROR ontology does not contain entity: %s";

  /** Error message when one IRI represents more than one entity. */
  private static final String multipleEntitiesError =
      NS + "MULTIPLE ENTITIES ERROR multiple entities represented by: %s";

  /** Error message when an import ontology does not have an IRI. */
  private static final String nullIRIError =
      NS + "NULL IRI ERROR import ontology does not have an IRI";

  /**
   * Given an ontology, an axiom, a property IRI, and a value string, add an annotation to this
   * ontology with that property and value.
   *
   * <p>Note that as axioms are immutable, the axiom is removed and replaced with a new one.
   *
   * @param ontology the ontology to modify
   * @param axiom the axiom to annotate
   * @param propertyIRI the IRI of the property to add
   * @param value the IRI or literal value to add
   */
  public static void addAxiomAnnotation(
      OWLOntology ontology, OWLAxiom axiom, IRI propertyIRI, OWLAnnotationValue value) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();

    OWLAnnotationProperty property = df.getOWLAnnotationProperty(propertyIRI);
    OWLAnnotation annotation = df.getOWLAnnotation(property, value);
    addAxiomAnnotation(ontology, axiom, Collections.singleton(annotation));
  }

  /**
   * Given an ontology, an axiom, and a set of annotations, annotate the axiom with the annotations
   * in the ontology.
   *
   * <p>Note that as axioms are immutable, the axiom is removed and replaced with a new one.
   *
   * @param ontology the ontology to modify
   * @param axiom the axiom to annotate
   * @param annotations the set of annotation to add to the axiom
   */
  public static void addAxiomAnnotation(
      OWLOntology ontology, OWLAxiom axiom, Set<OWLAnnotation> annotations) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory factory = manager.getOWLDataFactory();
    OWLAxiom newAxiom;
    if (axiom instanceof OWLSubClassOfAxiom) {
      OWLSubClassOfAxiom x = ((OWLSubClassOfAxiom) axiom);
      newAxiom = factory.getOWLSubClassOfAxiom(x.getSubClass(), x.getSuperClass(), annotations);
      logger.debug("ANNOTATED: " + newAxiom);
    } else {
      // TODO - See https://github.com/ontodev/robot/issues/67
      throw new UnsupportedOperationException(String.format(axiomTypeError, axiom.getClass()));
    }
    manager.removeAxiom(ontology, axiom);
    manager.addAxiom(ontology, newAxiom);
  }

  /**
   * Given an ontology, an annotation property IRI, and an annotation value, annotate all axioms in
   * the ontology with that property and value.
   *
   * @param ontology the ontology to modify
   * @param propertyIRI the IRI of the property to add
   * @param value the IRI or literal value to add
   */
  public static void addAxiomAnnotations(
      OWLOntology ontology, IRI propertyIRI, OWLAnnotationValue value) {
    for (OWLAxiom a : ontology.getAxioms()) {
      addAxiomAnnotation(ontology, a, propertyIRI, value);
    }
  }

  /**
   * Given an ontology, a property IRI, and a value string, add an annotation to this ontology with
   * that property and value.
   *
   * @param ontology the ontology to modify
   * @param propertyIRI the IRI of the property to add
   * @param value the IRI or literal value to add
   */
  public static void addOntologyAnnotation(
      OWLOntology ontology, IRI propertyIRI, OWLAnnotationValue value) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();

    OWLAnnotationProperty property = df.getOWLAnnotationProperty(propertyIRI);
    OWLAnnotation annotation = df.getOWLAnnotation(property, value);
    addOntologyAnnotation(ontology, annotation);
  }

  /**
   * Annotate the ontology with the annotation.
   *
   * @param ontology the ontology to modify
   * @param annotation the annotation to add
   */
  public static void addOntologyAnnotation(OWLOntology ontology, OWLAnnotation annotation) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    AddOntologyAnnotation addition = new AddOntologyAnnotation(ontology, annotation);
    manager.applyChange(addition);
  }

  /**
   * Given input and output ontologies, a target entity, and a set of annotation properties, copy
   * the target entity from the input ontology to the output ontology, along with the specified
   * annotations. If the entity is already in the outputOntology, then return without making any
   * changes. The input ontology is not changed.
   *
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param entity the target entity that will have its ancestors copied
   * @param annotationProperties the annotations to copy
   */
  public static void copy(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties) {
    OWLDataFactory dataFactory = inputOntology.getOWLOntologyManager().getOWLDataFactory();
    // Don't copy OWLThing
    if (entity == dataFactory.getOWLThing()) {
      return;
    }
    // Don't copy OWLNothing
    if (entity == dataFactory.getOWLNothing()) {
      return;
    }
    // Don't copy existing terms
    if (outputOntology.containsEntityInSignature(entity)) {
      return;
    }

    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();
    if (entity.isOWLAnnotationProperty()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLAnnotationProperty()));
    } else if (entity.isOWLObjectProperty()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLObjectProperty()));
    } else if (entity.isOWLDataProperty()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLDataProperty()));
    } else if (entity.isOWLDatatype()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLDatatype()));
    } else if (entity.isOWLClass()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLClass()));
    } else if (entity.isOWLNamedIndividual()) {
      outputManager.addAxiom(
          outputOntology, dataFactory.getOWLDeclarationAxiom(entity.asOWLNamedIndividual()));
    }

    Set<OWLAnnotationAssertionAxiom> axioms =
        inputOntology.getAnnotationAssertionAxioms(entity.getIRI());
    for (OWLAnnotationAssertionAxiom axiom : axioms) {
      if (annotationProperties == null || annotationProperties.contains(axiom.getProperty())) {
        // Copy the annotation property and then the axiom.
        copy(inputOntology, outputOntology, (OWLEntity) axiom.getProperty(), annotationProperties);
        outputManager.addAxiom(outputOntology, axiom);
      }
    }
  }

  /**
   * Given an ontology and a set of IRIs, filter the set of IRIs to only include those that exist in
   * the ontology.
   *
   * @param ontology the ontology to check for IRIs
   * @param IRIs Set of IRIs to filter
   * @param allowEmpty boolean specifying if an empty set can be returned
   * @return Set of filtered IRIs
   */
  public static Set<IRI> filterExistingTerms(
      OWLOntology ontology, Set<IRI> IRIs, boolean allowEmpty) {
    Set<IRI> missingIRIs = new HashSet<>();
    for (IRI iri : IRIs) {
      if (!ontology.containsEntityInSignature(iri)) {
        logger.warn("Ontology does not contain {}", iri.toQuotedString());
        missingIRIs.add(iri);
      }
    }

    if (missingIRIs.containsAll(IRIs) && !allowEmpty) {
      throw new IllegalArgumentException(emptyTermsError);
    }
    return IRIs;
  }

  /**
   * Get all OWLObjects associated with an axiom. This is builds on getSignature() by including
   * anonymous objects.
   *
   * @param axiom The axiom to check
   * @return The set of objects
   */
  public static Set<OWLObject> getObjects(OWLAxiom axiom) {
    Set<OWLObject> objects = new HashSet<>();
    objects.addAll(axiom.getSignature());

    // The following are special cases
    // where there might be something anonymous that we want to include
    // in addition to the (named) entities in the signature.
    if (axiom instanceof OWLAnnotationAssertionAxiom) {
      OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) axiom;
      objects.add(a.getSubject());
    } else if (axiom instanceof OWLClassAssertionAxiom) {
      OWLClassAssertionAxiom a = (OWLClassAssertionAxiom) axiom;
      objects.add(a.getClassExpression());
    } else if (axiom instanceof OWLDisjointUnionAxiom) {
      OWLDisjointUnionAxiom a = (OWLDisjointUnionAxiom) axiom;
      objects.addAll(a.getClassExpressions());
    } else if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {
      OWLEquivalentDataPropertiesAxiom a = (OWLEquivalentDataPropertiesAxiom) axiom;
      objects.addAll(a.asSubDataPropertyOfAxioms());
    } else if (axiom instanceof OWLNaryClassAxiom) {
      OWLNaryClassAxiom a = (OWLNaryClassAxiom) axiom;
      objects.addAll(a.getClassExpressions());
    } else if (axiom instanceof OWLNaryIndividualAxiom) {
      OWLNaryIndividualAxiom a = (OWLNaryIndividualAxiom) axiom;
      objects.addAll(a.getIndividuals());
    } else if (axiom instanceof OWLNaryPropertyAxiom) {
      OWLNaryPropertyAxiom a = (OWLNaryPropertyAxiom) axiom;
      objects.addAll(a.getProperties());
    } else if (axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) {
      OWLNegativeObjectPropertyAssertionAxiom a = (OWLNegativeObjectPropertyAssertionAxiom) axiom;
      objects.addAll(a.getAnonymousIndividuals());
    } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
      OWLObjectPropertyAssertionAxiom a = (OWLObjectPropertyAssertionAxiom) axiom;
      objects.addAll(a.getAnonymousIndividuals());
    } else if (axiom instanceof OWLObjectPropertyAxiom) {
      OWLObjectPropertyAxiom a = (OWLObjectPropertyAxiom) axiom;
      objects.addAll(a.getNestedClassExpressions());
    } else if (axiom instanceof OWLSameIndividualAxiom) {
      OWLSameIndividualAxiom a = (OWLSameIndividualAxiom) axiom;
      objects.addAll(a.getAnonymousIndividuals());
    } else if (axiom instanceof OWLSubClassOfAxiom) {
      OWLSubClassOfAxiom a = (OWLSubClassOfAxiom) axiom;
      objects.add(a.getSuperClass());
      objects.add(a.getSubClass());
    } else if (axiom instanceof OWLUnaryPropertyAxiom) {
      OWLUnaryPropertyAxiom a = (OWLUnaryPropertyAxiom) axiom;
      objects.add(a.getProperty());
    } else if (axiom instanceof OWLHasKeyAxiom) {
      OWLHasKeyAxiom a = (OWLHasKeyAxiom) axiom;
      objects.add(a.getClassExpression());
      objects.addAll(a.getPropertyExpressions());
    }

    // TODO - cannot have anonymous components?
    // OWLNegativeDataPropertyAssertionAxiom
    // OWLDataPropertyAssertionAxiom
    // OWLSubDataPropertyOfAxiom
    // OWLSubAnnotationPropertyOfAxiom

    // TODO - covered by OWLObjectPropertyAxiom
    // OWLObjectPropertyDomainAxiom
    // OWLObjectPropertyRangeAxiom
    // OWLObjectPropertyCharacteristicAxiom
    // OWLReflexiveObjectPropertyAxiom
    // OWLSubObjectPropertyOfAxiom
    // OWLSymmetricObjectPropertyAxiom
    // OWLTransitiveObjectPropertyAxiom
    // OWLEquivalentObjectPropertiesAxiom
    // OWLInverseObjectPropertiesAxiom

    // TODO - only need OWLObjectProperty...
    // OWLPropertyAssertionAxiom<P,O>
    // OWLPropertyAxiom
    // OWLPropertyDomainAxiom<P>
    // OWLPropertyRangeAxiom<P,R>
    // OWLSubPropertyAxiom<P>
    // OWLSubPropertyChainOfAxiom

    return objects;
  }

  /**
   * Given an ontology, an entity, and an empty set, fill the set with axioms representing any
   * anonymous superclasses in the line of ancestors.
   *
   * @param ontology the ontology to search
   * @param entity the entity to search ancestors of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnonymousAncestorAxioms(OWLOntology ontology, OWLEntity entity) {
    Set<OWLAxiom> anons = new HashSet<>();
    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    if (entity.isOWLClass()) {
      for (OWLClassExpression e : EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubClassOfAxiom(entity.asOWLClass(), e));
        } else {
          getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLClass(), anons);
        }
      }
    } else if (entity.isOWLObjectProperty()) {
      for (OWLObjectPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLObjectProperty(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubObjectPropertyOfAxiom(entity.asOWLObjectProperty(), e));
        } else {
          getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLObjectProperty(), anons);
        }
      }
    } else if (entity.isOWLDataProperty()) {
      for (OWLDataPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLDataProperty(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubDataPropertyOfAxiom(entity.asOWLDataProperty(), e));
        } else {
          getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLDataProperty(), anons);
        }
      }
    }
    return anons;
  }

  /**
   * Given an ontology, a data factory, a class, and an empty set, fill the set with axioms
   * containing any anonymous classes referenced in the ancestor of the class.
   *
   * @param ontology the ontology to search
   * @param dataFactory the data factory to create axioms
   * @param cls the class to search ancestors of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousAncestorAxioms(
      OWLOntology ontology, OWLDataFactory dataFactory, OWLClass cls, Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, cls, anons);
    for (OWLClassExpression e : EntitySearcher.getSuperClasses(cls, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLSubClassOfAxiom(cls, e));
      } else {
        getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLClass(), anons);
      }
    }
  }

  /**
   * Given an ontology, a data factory, an object property, and an empty set, fill the set with
   * axioms containing any anonymous classes referenced in the ancestor of the class.
   *
   * @param ontology the ontology to search
   * @param dataFactory the data factory to create axioms
   * @param property the object property to search ancestors of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousAncestorAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLObjectProperty property,
      Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, property, anons);
    for (OWLObjectPropertyExpression e : EntitySearcher.getSuperProperties(property, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLSubObjectPropertyOfAxiom(property, e));
      } else {
        getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLObjectProperty(), anons);
      }
    }
  }

  /**
   * Given an ontology, a data factory, a data property, and an empty set, fill the set with axioms
   * containing any anonymous classes referenced in the ancestor of the class.
   *
   * @param ontology the ontology to search
   * @param dataFactory the data factory to create axioms
   * @param property the data property to search ancestors of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousAncestorAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLDataProperty property,
      Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, property, anons);
    for (OWLDataPropertyExpression e : EntitySearcher.getSuperProperties(property, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLSubDataPropertyOfAxiom(property, e));
      } else {
        getAnonymousAncestorAxioms(ontology, dataFactory, e.asOWLDataProperty(), anons);
      }
    }
  }

  /**
   * Given an ontology and an entity, return a set of axioms containing any anonymous entities
   * referenced in the descendants of the entity. Includes supers & equivalents.
   *
   * @param ontology the ontology to search
   * @param entity the entity to search descendants of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnonymousDescendantAxioms(OWLOntology ontology, OWLEntity entity) {
    Set<OWLAxiom> anons = new HashSet<>();
    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    if (entity.isOWLClass()) {
      for (OWLClassExpression e : EntitySearcher.getSubClasses(entity.asOWLClass(), ontology)) {
        getAnonymousDescendantAxioms(ontology, dataFactory, e.asOWLClass(), anons);
      }
    } else if (entity.isOWLObjectProperty()) {
      for (OWLObjectPropertyExpression e :
          EntitySearcher.getSubProperties(entity.asOWLObjectProperty(), ontology)) {
        getAnonymousDescendantAxioms(ontology, dataFactory, e.asOWLObjectProperty(), anons);
      }
    } else if (entity.isOWLDataProperty()) {
      for (OWLDataPropertyExpression e :
          EntitySearcher.getSubProperties(entity.asOWLDataProperty(), ontology)) {
        getAnonymousDescendantAxioms(ontology, dataFactory, e.asOWLDataProperty(), anons);
      }
    }
    return anons;
  }

  /**
   * Given an ontology, a data factory, a class, and an empty set, fill the set with axioms
   * containing any anonymous classes referenced in the descendants of the entity. Also includes the
   * entity itself.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param cls the class to search descendants of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousDescendantAxioms(
      OWLOntology ontology, OWLDataFactory dataFactory, OWLClass cls, Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, cls, anons);
    for (OWLClassExpression e : EntitySearcher.getSubClasses(cls, ontology)) {
      OWLClass subclass = e.asOWLClass();
      for (OWLClassExpression se : EntitySearcher.getSuperClasses(subclass, ontology)) {
        if (se.isAnonymous()) {
          anons.add(dataFactory.getOWLSubClassOfAxiom(subclass, se));
        }
      }
      getAnonymousDescendantAxioms(ontology, dataFactory, subclass, anons);
    }
  }

  /**
   * Given an ontology, a data factory, an object property, and an empty set, fill the set with
   * axioms containing any anonymous classes referenced in the descendants of the entity. Also
   * includes the entity itself.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param property the object property to search descendants of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousDescendantAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLObjectProperty property,
      Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, property, anons);
    for (OWLObjectPropertyExpression e : EntitySearcher.getSubProperties(property, ontology)) {
      OWLObjectProperty subproperty = e.asOWLObjectProperty();
      for (OWLObjectPropertyExpression se :
          EntitySearcher.getSuperProperties(subproperty, ontology)) {
        if (se.isAnonymous()) {
          anons.add(dataFactory.getOWLSubObjectPropertyOfAxiom(subproperty, se));
        }
      }
      getAnonymousDescendantAxioms(ontology, dataFactory, subproperty, anons);
    }
  }

  /**
   * Given an ontology, a data factory, a data property, and an empty set, fill the set with axioms
   * containing any anonymous classes referenced in the descendants of the entity. Also includes the
   * entity itself.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param property the data property to search descendants of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousDescendantAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLDataProperty property,
      Set<OWLAxiom> anons) {
    getAnonymousEquivalentAxioms(ontology, dataFactory, property, anons);
    for (OWLDataPropertyExpression e : EntitySearcher.getSubProperties(property, ontology)) {
      OWLDataProperty subproperty = e.asOWLDataProperty();
      for (OWLDataPropertyExpression se :
          EntitySearcher.getSuperProperties(subproperty, ontology)) {
        if (se.isAnonymous()) {
          anons.add(dataFactory.getOWLSubDataPropertyOfAxiom(subproperty, se));
        }
      }
      getAnonymousDescendantAxioms(ontology, dataFactory, subproperty, anons);
    }
  }

  /**
   * Given an ontology and an entity, return a set of axioms containing any anonymous entities
   * referenced in equivalent entities.
   *
   * @param ontology the ontology to search
   * @param entity the entity to search equivalents of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnonymousEquivalentAxioms(OWLOntology ontology, OWLEntity entity) {
    Set<OWLAxiom> anons = new HashSet<>();
    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    if (entity.isOWLClass()) {
      getAnonymousEquivalentAxioms(ontology, dataFactory, entity.asOWLClass(), anons);
    } else if (entity.isOWLObjectProperty()) {
      getAnonymousEquivalentAxioms(ontology, dataFactory, entity.asOWLObjectProperty(), anons);
    } else if (entity.isOWLDataProperty()) {
      getAnonymousEquivalentAxioms(ontology, dataFactory, entity.asOWLDataProperty(), anons);
    }
    return anons;
  }

  /**
   * Given an ontology, a data factory, a class, and an empty set, fill the set with any axioms
   * containing any anonymous classes referenced in equivalent classes.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param cls the class to search equivalents of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousEquivalentAxioms(
      OWLOntology ontology, OWLDataFactory dataFactory, OWLClass cls, Set<OWLAxiom> anons) {
    for (OWLClassExpression e : EntitySearcher.getEquivalentClasses(cls, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLEquivalentClassesAxiom(cls, e));
      } else if (e.asOWLClass() != cls) {
        getAnonymousEquivalentAxioms(ontology, dataFactory, e.asOWLClass(), anons);
      }
    }
  }

  /**
   * Given an ontology, a data factory, an object property, and an empty set, fill the set with any
   * axioms containing any anonymous properties referenced in equivalent properties.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param property the object property to search equivalents of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousEquivalentAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLObjectProperty property,
      Set<OWLAxiom> anons) {
    for (OWLObjectPropertyExpression e :
        EntitySearcher.getEquivalentProperties(property, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLEquivalentObjectPropertiesAxiom(property, e));
      } else if (e.asOWLObjectProperty() != property) {
        getAnonymousEquivalentAxioms(ontology, dataFactory, e.asOWLObjectProperty(), anons);
      }
    }
  }

  /**
   * Given an ontology, a data factory, a data property, and an empty set, fill the set with any
   * axioms containing any anonymous properties referenced in equivalent properties.
   *
   * @param ontology the ontology to search
   * @param dataFactory a data factory to create axioms
   * @param property the data property to search equivalents of
   * @param anons set of OWLAxioms to fill
   */
  private static void getAnonymousEquivalentAxioms(
      OWLOntology ontology,
      OWLDataFactory dataFactory,
      OWLDataProperty property,
      Set<OWLAxiom> anons) {
    for (OWLDataPropertyExpression e : EntitySearcher.getEquivalentProperties(property, ontology)) {
      if (e.isAnonymous()) {
        anons.add(dataFactory.getOWLEquivalentDataPropertiesAxiom(property, e));
      } else if (e.asOWLDataProperty() != property) {
        getAnonymousEquivalentAxioms(ontology, dataFactory, e.asOWLDataProperty(), anons);
      }
    }
  }

  /**
   * Given an ontology and an entity, return a set of axioms only corresponding to anonymous
   * parents.
   *
   * @param ontology the ontology to search
   * @param entity the entity to search parents of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnonymousParentAxioms(OWLOntology ontology, OWLEntity entity) {
    Set<OWLAxiom> anons = new HashSet<>();
    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    if (entity.isOWLClass()) {
      for (OWLClassExpression e : EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubClassOfAxiom(entity.asOWLClass(), e));
        }
      }
    } else if (entity.isOWLObjectProperty()) {
      for (OWLObjectPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLObjectProperty(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubObjectPropertyOfAxiom(entity.asOWLObjectProperty(), e));
        }
      }
    } else if (entity.isOWLDataProperty()) {
      for (OWLDataPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLDataProperty(), ontology)) {
        if (e.isAnonymous()) {
          anons.add(dataFactory.getOWLSubDataPropertyOfAxiom(entity.asOWLDataProperty(), e));
        }
      }
    }
    return anons;
  }

  /**
   * Given an ontology, and an optional set of annotation properties, return a set of annotation
   * assertion axioms for those properties, for all subjects.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param property an annotation property
   * @return a filtered set of annotation assertion axioms
   */
  public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
      OWLOntology ontology, OWLAnnotationProperty property) {
    Set<OWLAnnotationProperty> properties = new HashSet<OWLAnnotationProperty>();
    properties.add(property);
    Set<IRI> subjects = null;
    return getAnnotationAxioms(ontology, properties, subjects);
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of annotation assertion axioms for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param property an annotation property
   * @param subject an annotation subject IRIs
   * @return a filtered set of annotation assertion axioms
   */
  public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
      OWLOntology ontology, OWLAnnotationProperty property, IRI subject) {
    Set<OWLAnnotationProperty> properties = new HashSet<OWLAnnotationProperty>();
    properties.add(property);
    Set<IRI> subjects = null;
    if (subject != null) {
      subjects = new HashSet<IRI>();
      subjects.add(subject);
    }
    return getAnnotationAxioms(ontology, properties, subjects);
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of annotation assertion axioms for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param properties a set of annotation properties, or null if all properties should be included
   * @param subjects a set of annotation subject IRIs, or null if all subjects should be included
   * @return a filtered set of annotation assertion axioms
   */
  public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
      OWLOntology ontology, Set<OWLAnnotationProperty> properties, Set<IRI> subjects) {
    Set<OWLAnnotationAssertionAxiom> results = new HashSet<OWLAnnotationAssertionAxiom>();

    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (!(axiom instanceof OWLAnnotationAssertionAxiom)) {
        continue;
      }
      OWLAnnotationAssertionAxiom aaa = (OWLAnnotationAssertionAxiom) axiom;
      if (properties != null && !properties.contains(aaa.getProperty())) {
        continue;
      }
      OWLAnnotationSubject subject = aaa.getSubject();
      if (subjects == null) {
        results.add(aaa);
      } else if (subject instanceof IRI && subjects.contains((IRI) subject)) {
        results.add(aaa);
      }
    }
    return results;
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return the alphanumerically first annotation value string for those subjects and those
   * properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param property an annotation property
   * @param subject an annotation subject IRIs
   * @return the first annotation string
   */
  public static String getAnnotationString(
      OWLOntology ontology, OWLAnnotationProperty property, IRI subject) {
    Set<OWLAnnotationProperty> properties = new HashSet<OWLAnnotationProperty>();
    properties.add(property);
    Set<IRI> subjects = new HashSet<IRI>();
    subjects.add(subject);
    return getAnnotationString(ontology, properties, subjects);
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return the alphanumerically first annotation value string for those subjects and those
   * properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param properties a set of annotation properties, or null if all properties should be included
   * @param subjects a set of annotation subject IRIs, or null if all subjects should be included
   * @return the first annotation string
   */
  public static String getAnnotationString(
      OWLOntology ontology, Set<OWLAnnotationProperty> properties, Set<IRI> subjects) {
    Set<String> valueSet = getAnnotationStrings(ontology, properties, subjects);
    List<String> valueList = new ArrayList<String>(valueSet);
    Collections.sort(valueList);
    String value = null;
    if (valueList.size() > 0) {
      value = valueList.get(0);
    }
    return value;
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of strings for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param property an annotation property
   * @param subject an annotation subject IRIs
   * @return a filtered set of annotation strings
   */
  public static Set<String> getAnnotationStrings(
      OWLOntology ontology, OWLAnnotationProperty property, IRI subject) {
    Set<OWLAnnotationProperty> properties = new HashSet<OWLAnnotationProperty>();
    properties.add(property);
    Set<IRI> subjects = new HashSet<IRI>();
    subjects.add(subject);
    return getAnnotationStrings(ontology, properties, subjects);
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of strings for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param properties a set of annotation properties, or null if all properties should be included
   * @param subjects a set of annotation subject IRIs, or null if all subjects should be included
   * @return a filtered set of annotation strings
   */
  public static Set<String> getAnnotationStrings(
      OWLOntology ontology, Set<OWLAnnotationProperty> properties, Set<IRI> subjects) {
    Set<String> results = new HashSet<String>();
    Set<OWLAnnotationValue> values = getAnnotationValues(ontology, properties, subjects);
    for (OWLAnnotationValue value : values) {
      results.add(getValue(value));
    }
    return results;
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of annotation values for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param property an annotation property
   * @param subject an annotation subject IRIs
   * @return a filtered set of annotation values
   */
  public static Set<OWLAnnotationValue> getAnnotationValues(
      OWLOntology ontology, OWLAnnotationProperty property, IRI subject) {
    Set<OWLAnnotationProperty> properties = new HashSet<OWLAnnotationProperty>();
    properties.add(property);
    Set<IRI> subjects = new HashSet<IRI>();
    subjects.add(subject);
    return getAnnotationValues(ontology, properties, subjects);
  }

  /**
   * Given an ontology, an optional set of annotation properties, and an optional set of subject,
   * return a set of annotation values for those subjects and those properties.
   *
   * @param ontology the ontology to search (including imports closure)
   * @param properties a set of annotation properties, or null if all properties should be included
   * @param subjects a set of annotation subject IRIs, or null if all subjects should be included
   * @return a filtered set of annotation values
   */
  public static Set<OWLAnnotationValue> getAnnotationValues(
      OWLOntology ontology, Set<OWLAnnotationProperty> properties, Set<IRI> subjects) {
    Set<OWLAnnotationValue> results = new HashSet<OWLAnnotationValue>();
    Set<OWLAnnotationAssertionAxiom> axioms = getAnnotationAxioms(ontology, properties, subjects);
    for (OWLAnnotationAssertionAxiom axiom : axioms) {
      results.add(axiom.getValue());
    }
    return results;
  }

  /**
   * Given an ontology and an IRI, get the OWLEntity object represented by the IRI. If it does not
   * exist in the ontology, throw an exception.
   *
   * @param ontology OWLOntology to retrieve from
   * @param iri IRI to get type of
   * @return OWLEntity
   * @throws Exception if IRI is not in ontology
   */
  public static OWLEntity getEntity(OWLOntology ontology, IRI iri) throws Exception {
    return getEntity(ontology, iri, false);
  }

  /**
   * Given an ontology, an IRI, and a boolean specifying if a null return value is allowed, get the
   * OWLEntity object represented by the IRI.
   *
   * @param ontology OWLOntology to retrieve from
   * @param iri IRI to get type of
   * @param allowNull if true, do not throw exception if IRI does not exist in ontology
   * @return OWLEntity
   * @throws Exception if allowNull is false and IRI is not in ontology
   */
  public static OWLEntity getEntity(OWLOntology ontology, IRI iri, boolean allowNull)
      throws Exception {
    // Get the OWLEntity for the IRI
    Set<OWLEntity> owlEntities = ontology.getEntitiesInSignature(iri);
    // Check that there is exactly one entity, throw exception if not
    if (owlEntities.size() == 0) {
      if (allowNull) {
        logger.warn("{} does not exist.", iri.toQuotedString());
        return null;
      } else {
        throw new Exception(String.format(missingEntityError, iri.toString()));
      }
    } else if (owlEntities.size() > 1) {
      throw new Exception(String.format(multipleEntitiesError, iri.toString()));
    }
    return owlEntities.iterator().next();
  }

  /**
   * Given an ontology and a set of term IRIs, return a set of entities for those IRIs. The input
   * ontology is not changed.
   *
   * @param ontology the ontology to search
   * @param iris the IRIs of the entities to find
   * @return a set of OWLEntities with the given IRIs
   */
  public static Set<OWLEntity> getEntities(OWLOntology ontology, Set<IRI> iris) {
    Set<OWLEntity> entities = new HashSet<>();
    if (iris == null) {
      return entities;
    }
    for (IRI iri : iris) {
      try {
        OWLEntity entity = getEntity(ontology, iri, true);
        if (entity != null) {
          // Do not add null entries
          entities.add(entity);
        }
      } catch (Exception e) {
      }
    }
    return entities;
  }

  /**
   * Given an ontology, return a set of all the entities in its signature.
   *
   * @param ontology the ontology to search
   * @return a set of all entities in the ontology
   */
  public static Set<OWLEntity> getEntities(OWLOntology ontology) {
    Set<OWLOntology> ontologies = new HashSet<>();
    ontologies.add(ontology);
    ReferencedEntitySetProvider resp = new ReferencedEntitySetProvider(ontologies);
    return resp.getEntities();
  }

  /**
   * Given an ontology, return a set of IRIs for all the entities in its signature.
   *
   * @param ontology the ontology to search
   * @return a set of IRIs for all entities in the ontology
   */
  public static Set<IRI> getIRIs(OWLOntology ontology) {
    Set<IRI> iris = new HashSet<IRI>();
    for (OWLEntity entity : getEntities(ontology)) {
      iris.add(entity.getIRI());
    }
    return iris;
  }

  /**
   * Generates a function that returns a label string for any named object in the ontology
   *
   * @param ontology to use
   * @param useIriAsDefault if true then label-less classes will return IRI
   * @return function mapping object to label
   */
  public static Function<OWLNamedObject, String> getLabelFunction(
      OWLOntology ontology, boolean useIriAsDefault) {
    Map<IRI, String> labelMap = new HashMap<>();
    for (OWLAnnotationAssertionAxiom ax : ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
      if (ax.getProperty().isLabel()
          && ax.getSubject() instanceof IRI
          && ax.getValue() instanceof OWLLiteral) {
        labelMap.put((IRI) ax.getSubject(), ax.getValue().asLiteral().toString());
      }
    }
    return (obj) -> {
      String label;
      if (labelMap.containsKey(obj.getIRI())) {
        label = labelMap.get(obj.getIRI());
      } else if (useIriAsDefault) {
        label = obj.getIRI().toString();
      } else {
        label = null;
      }

      return label;
    };
  }

  /**
   * Given an ontology, return a map from rdfs:label to IRIs. Includes labels asserted in for all
   * imported ontologies. Duplicates overwrite existing with a warning.
   *
   * @param ontology the ontology to use
   * @return a map from label strings to IRIs
   */
  public static Map<String, IRI> getLabelIRIs(OWLOntology ontology) {
    Map<String, IRI> results = new HashMap<String, IRI>();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLAnnotationProperty rdfsLabel = manager.getOWLDataFactory().getRDFSLabel();
    Set<OWLAnnotationAssertionAxiom> axioms = getAnnotationAxioms(ontology, rdfsLabel);
    for (OWLAnnotationAssertionAxiom axiom : axioms) {
      String value = getValue(axiom);
      if (value == null) {
        continue;
      }
      OWLAnnotationSubject subject = axiom.getSubject();
      if (subject == null || !(subject instanceof IRI)) {
        continue;
      }
      if (results.containsKey(value)) {
        logger.warn("Duplicate rdfs:label \"" + value + "\" for subject " + subject);
      }
      results.put(value, (IRI) subject);
    }
    return results;
  }

  /**
   * Given an ontology, return a map from IRIs to rdfs:labels. Includes labels asserted in for all
   * imported ontologies. If there are multiple labels, use the alphanumerically first.
   *
   * @param ontology the ontology to use
   * @return a map from IRIs to label strings
   */
  public static Map<IRI, String> getLabels(OWLOntology ontology) {
    logger.info("Fetching labels for " + ontology.getOntologyID());
    Map<IRI, String> results = new HashMap<IRI, String>();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLAnnotationProperty rdfsLabel = manager.getOWLDataFactory().getRDFSLabel();
    Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
    ontologies.add(ontology);
    ReferencedEntitySetProvider resp = new ReferencedEntitySetProvider(ontologies);
    logger.info("iterating through entities...");
    for (OWLEntity entity : resp.getEntities()) {
      String value = getAnnotationString(ontology, rdfsLabel, entity.getIRI());
      if (value != null) {
        results.put(entity.getIRI(), value);
      }
    }
    logger.info("Results: " + results.size());
    return results;
  }

  /**
   * Given an annotation value, return its datatype, or null.
   *
   * @param value the value to check
   * @return the datatype, or null if the value has none
   */
  public static OWLDatatype getType(OWLAnnotationValue value) {
    if (value instanceof OWLLiteral) {
      return ((OWLLiteral) value).getDatatype();
    }
    return null;
  }

  /**
   * Given an annotation value, return the IRI of its datatype, or null.
   *
   * @param value the value to check
   * @return the IRI of the datatype, or null if the value has none
   */
  public static IRI getTypeIRI(OWLAnnotationValue value) {
    OWLDatatype datatype = getType(value);
    if (datatype == null) {
      return null;
    } else {
      return datatype.getIRI();
    }
  }

  /**
   * Given an OWLAnnotationValue, return its value as a string.
   *
   * @param value the OWLAnnotationValue to get the string value of
   * @return the string value
   */
  public static String getValue(OWLAnnotationValue value) {
    String result = null;
    if (value instanceof OWLLiteral) {
      result = ((OWLLiteral) value).getLiteral();
    }
    return result;
  }

  /**
   * Given an OWLAnnotation, return its value as a string.
   *
   * @param annotation the OWLAnnotation to get the string value of
   * @return the string value
   */
  public static String getValue(OWLAnnotation annotation) {
    return getValue(annotation.getValue());
  }

  /**
   * Given an OWLAnnotationAssertionAxiom, return its value as a string.
   *
   * @param axiom the OWLAnnotationAssertionAxiom to get the string value of
   * @return the string value
   */
  public static String getValue(OWLAnnotationAssertionAxiom axiom) {
    return getValue(axiom.getValue());
  }

  /**
   * Given a set of OWLAnnotations, return the first string value as determined by natural string
   * sorting.
   *
   * @param annotations a set of OWLAnnotations to get the value of
   * @return the first string value
   */
  public static String getValue(Set<OWLAnnotation> annotations) {
    Set<String> valueSet = getValues(annotations);
    List<String> valueList = new ArrayList<String>(valueSet);
    Collections.sort(valueList);
    String value = null;
    if (valueList.size() > 0) {
      value = valueList.get(0);
    }
    return value;
  }

  /**
   * Given a set of OWLAnnotations, return a set of their value strings.
   *
   * @param annotations a set of OWLAnnotations to get the value of
   * @return a set of the value strings
   */
  public static Set<String> getValues(Set<OWLAnnotation> annotations) {
    Set<String> results = new HashSet<String>();
    for (OWLAnnotation annotation : annotations) {
      String value = getValue(annotation);
      if (value != null) {
        results.add(value);
      }
    }
    return results;
  }

  /**
   * Given an OWLAxiom and a set of OWLAxiom class, determine if the axiom's class is an extension
   * of at least one of the given set.
   *
   * @param axiom the OWLAxiom to check
   * @param axiomTypes classes of axioms desired
   * @return true if axiom is an instantiation of one of the classes in set
   */
  public static boolean extendsAxiomTypes(
      OWLAxiom axiom, Set<Class<? extends OWLAxiom>> axiomTypes) {
    for (Class<? extends OWLAxiom> axiomType : axiomTypes) {
      if (axiomType.isAssignableFrom(axiom.getClass())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given an OWLAxiom class and a set of OWLAxiom classes, determine if the axiom class provided
   * extends at least one of the classes in the set.
   *
   * @param axiom the OWLAxiom class to check
   * @param axiomTypes classes of axioms desired
   * @return true if axiom class extends one of the classes in set
   */
  public static boolean extendsAxiomTypes(
      Class<? extends OWLAxiom> axiom, Set<Class<? extends OWLAxiom>> axiomTypes) {
    for (Class<? extends OWLAxiom> at : axiomTypes) {
      if (at.isAssignableFrom(axiom)) {
        return true;
      }
    }
    return false;
  }

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
   * Remove all annotations on this ontology. Just annotations on the ontology itself, not
   * annotations on its classes, etc.
   *
   * @param ontology the ontology to modify
   */
  public static void removeOntologyAnnotations(OWLOntology ontology) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    for (OWLAnnotation annotation : ontology.getAnnotations()) {
      RemoveOntologyAnnotation remove = new RemoveOntologyAnnotation(ontology, annotation);
      manager.applyChange(remove);
    }
  }

  /**
   * Set the ontology IRI and version IRI using strings.
   *
   * @param ontology the ontology to change
   * @param ontologyIRIString the ontology IRI string, or null for no change
   * @param versionIRIString the version IRI string, or null for no change
   */
  public static void setOntologyIRI(
      OWLOntology ontology, String ontologyIRIString, String versionIRIString) {
    IRI ontologyIRI = null;
    if (ontologyIRIString != null) {
      ontologyIRI = IRI.create(ontologyIRIString);
    }

    IRI versionIRI = null;
    if (versionIRIString != null) {
      versionIRI = IRI.create(versionIRIString);
    }

    setOntologyIRI(ontology, ontologyIRI, versionIRI);
  }

  /**
   * Set the ontology IRI and version IRI.
   *
   * @param ontology the ontology to change
   * @param ontologyIRI the new ontology IRI, or null for no change
   * @param versionIRI the new version IRI, or null for no change
   */
  public static void setOntologyIRI(OWLOntology ontology, IRI ontologyIRI, IRI versionIRI) {
    OWLOntologyID currentID = ontology.getOntologyID();

    if (ontologyIRI == null && versionIRI == null) {
      // don't change anything
      return;
    } else if (ontologyIRI == null) {
      ontologyIRI = currentID.getOntologyIRI().orNull();
    } else if (versionIRI == null) {
      versionIRI = currentID.getVersionIRI().orNull();
    }

    OWLOntologyID newID;
    if (versionIRI == null) {
      newID = new OWLOntologyID(ontologyIRI);
    } else {
      newID = new OWLOntologyID(ontologyIRI, versionIRI);
    }

    SetOntologyID setID = new SetOntologyID(ontology, newID);
    ontology.getOWLOntologyManager().applyChange(setID);
  }

  /**
   * Given an ontology, remove any dangling entities.
   *
   * @param ontology the ontology to trim
   */
  public static void trimOntology(OWLOntology ontology) {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLObject> objects = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      objects.addAll(getObjects(axiom));
    }
    Set<OWLObject> trimObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLEntity) {
        OWLEntity entity = (OWLEntity) object;
        if (InvalidReferenceChecker.isDangling(ontology, entity)) {
          logger.debug("Trimming %s", entity.getIRI());
          trimObjects.add(object);
        }
      }
    }
    Set<OWLAxiom> axioms =
        RelatedObjectsHelper.getPartialAxioms(
            ontology, trimObjects, Sets.newHashSet(OWLAxiom.class));
    manager.removeAxioms(ontology, axioms);
  }
}
