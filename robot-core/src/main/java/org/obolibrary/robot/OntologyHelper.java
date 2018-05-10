package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;
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

  private static final String emptyTermsError =
      NS + "EMPTY TERMS ERROR ontology does not contain input terms";

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
        logger.warn("Ontology does not contain term {}", iri.toString());
        missingIRIs.add(iri);
      }
    }

    if (missingIRIs.containsAll(IRIs) && !allowEmpty) {
      throw new IllegalArgumentException(emptyTermsError);
    }
    return IRIs;
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
   * Given an ontology and a set of term IRIs, return a set of entities for those IRIs. The input
   * ontology is not changed.
   *
   * @param ontology the ontology to search
   * @param iris the IRIs of the entities to find
   * @return a set of OWLEntities with the given IRIs
   */
  public static Set<OWLEntity> getEntities(OWLOntology ontology, Set<IRI> iris) {
    Set<OWLEntity> entities = new HashSet<OWLEntity>();
    if (iris == null) {
      return entities;
    }
    for (IRI iri : iris) {
      entities.addAll(ontology.getEntitiesInSignature(iri, true));
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
    Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
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
}
