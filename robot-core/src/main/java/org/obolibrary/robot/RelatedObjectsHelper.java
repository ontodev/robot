package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplPlain;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

/**
 * Convenience methods to get related entities for a set of IRIs. Allowed relation options are: -
 * ancestors - descendants - disjoints - domains - equivalents - inverses - ranges - types
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RelatedObjectsHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RelatedObjectsHelper.class);

  /** Namespace for error messages. */
  private static final String NS = "errors#";

  /**
   * Error message when a datatype is given for an annotation, but the annotation value does not
   * match the datatype.
   */
  private static final String literalValueError = NS + "LITERAL VALUE ERROR %s is not a %s value";

  /** Error message when an invalid IRI is provided. Expects the entry field and term. */
  private static final String invalidIRIError =
      NS + "INVALID IRI ERROR %1$s \"%2$s\" is not a valid CURIE or IRI";

  /** Error message when an IRI pattern to match does not have a wildcard character. */
  private static final String invalidIRIPatternError =
      NS
          + "INVALID IRI PATTERN ERROR the pattern '%s' must contain at least one wildcard character.";

  /** String for subclass/property mapping */
  private static final String SUB = "sub";
  /** String for superclass/property mapping */
  private static final String SUPER = "super";

  /**
   * Given an ontology and a set of objects, return the annotation axioms for all objects.
   *
   * @param ontology OWLOntology to get annotations from
   * @param objects OWLObjects to get annotations of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnnotationAxioms(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        OWLClass cls = (OWLClass) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(cls.getIRI()));
      } else if (object instanceof OWLObjectProperty) {
        OWLObjectProperty prop = (OWLObjectProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLDataProperty) {
        OWLDataProperty prop = (OWLDataProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLAnnotationProperty) {
        OWLAnnotationProperty prop = (OWLAnnotationProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLDatatype) {
        OWLDatatype dt = (OWLDatatype) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(dt.getIRI()));
      } else if (object instanceof OWLNamedIndividual) {
        OWLNamedIndividual indiv = (OWLNamedIndividual) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(indiv.getIRI()));
      }
    }
    return axioms;
  }

  public static Set<OWLAxiom> getAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean partial,
      boolean signature) {
    if (partial) {
      return RelatedObjectsHelper.getPartialAxioms(ontology, objects, axiomTypes, signature);
    } else {
      return RelatedObjectsHelper.getCompleteAxioms(ontology, objects, axiomTypes, signature);
    }
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> getCompleteAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return getCompleteAxioms(ontology, objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, consider only named OWLObjects
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> getCompleteAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    Set<OWLAxiom> axioms = new HashSet<>();
    Set<IRI> iris = getIRIs(objects);
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        // Check both the full annotated axiom and axiom without annotations (if annotated)
        Set<OWLObject> axiomObjects;
        if (namedOnly) {
          axiomObjects = OntologyHelper.getNamedObjects(axiom);
        } else {
          axiomObjects = OntologyHelper.getObjects(axiom);
        }

        Set<OWLObject> partialAxiomObjects = null;
        if (axiom.isAnnotated()) {
          if (namedOnly) {
            partialAxiomObjects =
                OntologyHelper.getNamedObjects(axiom.getAxiomWithoutAnnotations());
          } else {
            partialAxiomObjects = OntologyHelper.getObjects(axiom.getAxiomWithoutAnnotations());
          }
        }

        if (axiom instanceof OWLAnnotationAssertionAxiom) {
          OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) axiom;
          // Again, check full annotated axiom and axiom without annotations
          if (a.isAnnotated()) {
            OWLAnnotation annotation = a.getAnnotation();
            if (iris.contains(annotation.getProperty().getIRI())
                && objects.contains(a.getValue())) {
              if (iris.contains(a.getSubject()) && objects.contains(a.getProperty())) {
                // If the set contains all components, add the annotated axiom
                axioms.add(axiom);
              }
            }
          }
          if (iris.contains(a.getSubject()) && objects.contains(a.getProperty())) {
            // If the set only contains the un-annotated axiom, add that
            axioms.add(axiom.getAxiomWithoutAnnotations());
          }
        } else if (objects.containsAll(axiomObjects)) {
          axioms.add(axiom);
        } else if (partialAxiomObjects != null && objects.containsAll(partialAxiomObjects)) {
          // If all objects of the axiom are included, but not the annotation
          // add the un-annotated axiom
          axioms.add(axiom.getAxiomWithoutAnnotations());
        }
      }
    }

    return axioms;
  }

  /**
   * Given a list of base namespaces and a set of axioms, return only the axioms that DO NOT have a
   * subject in the base namespaces.
   *
   * @param baseNamespaces list of base namespaces
   * @param axioms set of OWLAxioms
   * @return external OWLAxioms
   */
  public static Set<OWLAxiom> getExternalAxioms(List<String> baseNamespaces, Set<OWLAxiom> axioms) {
    Set<OWLAxiom> externalAxioms = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      Set<IRI> subjects = getAxiomSubjects(axiom);
      if (!isBase(baseNamespaces, subjects)) {
        externalAxioms.add(axiom);
      }
    }
    return externalAxioms;
  }

  /**
   * Given a list of base namespaces and a set of axioms, return only the axioms that have a subject
   * in the base namespaces.
   *
   * @param baseNamespaces list of base namespaces
   * @param axioms set of OWLAxioms
   * @return internal OWLAxioms
   */
  public static Set<OWLAxiom> getInternalAxioms(List<String> baseNamespaces, Set<OWLAxiom> axioms) {
    Set<OWLAxiom> internalAxioms = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      Set<IRI> subjects = getAxiomSubjects(axiom);
      if (isBase(baseNamespaces, subjects)) {
        internalAxioms.add(axiom);
      }
    }
    return internalAxioms;
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> getPartialAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return getPartialAxioms(ontology, objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, only consider named OWLObjects
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> getPartialAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    Set<OWLAxiom> axioms = new HashSet<>();
    Set<IRI> iris = getIRIs(objects);
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        if (axiom instanceof OWLAnnotationAssertionAxiom) {
          OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) axiom;
          // Check if axiom is annotated
          if (a.isAnnotated()) {
            for (OWLAnnotation annotation : a.getAnnotations()) {
              // If any part of the annotation is in the set, add the axiom
              if (iris.contains(annotation.getProperty().getIRI())
                  || objects.contains(a.getValue())) {
                axioms.add(axiom);
              }
            }
          }
          if (iris.contains(a.getSubject()) || objects.contains(a.getProperty())) {
            axioms.add(axiom);
          }
        } else {
          Set<OWLObject> axiomObjects;
          if (namedOnly) {
            axiomObjects = OntologyHelper.getNamedObjects(axiom);
          } else {
            axiomObjects = OntologyHelper.getObjects(axiom);
          }
          for (OWLObject axiomObject : axiomObjects) {
            if (objects.contains(axiomObject)) {
              axioms.add(axiom);
              break;
            }
          }
        }
      }
    }

    return axioms;
  }

  /**
   * Given an ontology, a set of objects, and a list of select groups (as lists), return the objects
   * related to the set of OWLObjects based on each set of selectors. Each selector group will build
   * on the previous group.
   *
   * @param ontology OWLOntology to get related objects from
   * @param ioHelper IOHelper to use for prefixes
   * @param objects Set of objects to start with
   * @param selectorGroups types of related objects to return
   * @return Set of related objects
   * @throws Exception on annotation pattern issue
   */
  public static Set<OWLObject> selectGroups(
      OWLOntology ontology,
      IOHelper ioHelper,
      Set<OWLObject> objects,
      List<List<String>> selectorGroups)
      throws Exception {
    for (List<String> selectors : selectorGroups) {
      objects = select(ontology, ioHelper, objects, selectors);
    }
    return objects;
  }

  /**
   * Given an ontology, a set of objects, and a list of selector strings, return the objects related
   * to the set of OWLObjects based on the set of selectors.
   *
   * @param ontology OWLOntology to get related objects from
   * @param ioHelper IOHelper to use for prefixes
   * @param objects Set of objects to start with
   * @param selectors types of related objects to return
   * @return Set of related objects
   * @throws Exception on annotation pattern issue
   */
  public static Set<OWLObject> select(
      OWLOntology ontology, IOHelper ioHelper, Set<OWLObject> objects, List<String> selectors)
      throws Exception {
    // An empty list is essentially "self"
    if (selectors.isEmpty()) {
      return objects;
    }
    Set<OWLObject> union = new HashSet<>();
    for (String selector : selectors) {
      union.addAll(select(ontology, ioHelper, objects, selector));
    }
    return union;
  }

  /**
   * Given an ontology, a set of objects, and a selector, return the related objects related to the
   * set of OWL objects based on the selector.
   *
   * @param ontology OWLOntology to get related objects from
   * @param ioHelper IOHelper to use for prefixes
   * @param objects Set of objects to start with
   * @param selector type of related objects to return
   * @return Set of related objects
   * @throws Exception on annotation pattern issue
   */
  public static Set<OWLObject> select(
      OWLOntology ontology, IOHelper ioHelper, Set<OWLObject> objects, String selector)
      throws Exception {
    if (selector.equals("ancestors")) {
      return selectAncestors(ontology, objects);
    } else if (selector.equals("anonymous")) {
      return selectAnonymous(objects);
    } else if (selector.equals("annotation-properties")) {
      return selectAnnotationProperties(objects);
    } else if (selector.equals("children")) {
      return selectChildren(ontology, objects);
    } else if (selector.equals("classes")) {
      return selectClasses(objects);
    } else if (selector.equals("complement")) {
      return selectComplement(ontology, objects);
    } else if (selector.equals("data-properties")) {
      return selectDataProperties(objects);
    } else if (selector.equals("descendants")) {
      return selectDescendants(ontology, objects);
    } else if (selector.equals("equivalents")) {
      return selectEquivalents(ontology, objects);
    } else if (selector.equals("individuals")) {
      return selectIndividuals(objects);
    } else if (selector.equals("instances")) {
      return selectInstances(ontology, objects);
    } else if (selector.equals("named")) {
      return selectNamed(objects);
    } else if (selector.equals("object-properties")) {
      return selectObjectProperties(objects);
    } else if (selector.equals("parents")) {
      return selectParents(ontology, objects);
    } else if (selector.equals("properties")) {
      return selectProperties(objects);
    } else if (selector.equals("self")) {
      return objects;
    } else if (selector.equals("types")) {
      return selectTypes(ontology, objects);
    } else if (selector.equals("ranges")) {
      return selectRanges(ontology, objects);
    } else if (selector.equals("domains")) {
      return selectDomains(ontology, objects);
    } else if (selector.contains("=")) {
      return selectPattern(ontology, ioHelper, objects, selector);
    } else if (Pattern.compile("<.*>").matcher(selector).find()) {
      return selectIRI(objects, selector);
    } else if (selector.contains(":")) {
      return selectCURIE(ioHelper, objects, selector);
    } else {
      logger.error(String.format("%s is not a valid selector and will be ignored", selector));
      return new HashSet<>();
    }
  }

  /**
   * Given an ontology and a set of objects, return all ancestors (recursively) of those objects.
   *
   * @param ontology OWLOntology to retrieve ancestors from
   * @param objects OWLObjects to retrieve ancestors of
   * @return set of ancestors of the starting set
   */
  public static Set<OWLObject> selectAncestors(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        selectClassAncestors(ontology, (OWLClass) object, relatedObjects);
      } else if (object instanceof OWLAnnotationProperty) {
        selectAnnotationPropertyAncestors(ontology, (OWLAnnotationProperty) object, relatedObjects);
      } else if (object instanceof OWLDataProperty) {
        selectDataPropertyAncestors(ontology, (OWLDataProperty) object, relatedObjects);
      } else if (object instanceof OWLObjectProperty) {
        selectObjectPropertyAncestors(ontology, (OWLObjectProperty) object, relatedObjects);
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of annotation properties from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of OWLAnnotationProperties from the starting set
   */
  public static Set<OWLObject> selectAnnotationProperties(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLAnnotationProperty) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of anonymous objects from the starting set. Valid for
   * classes and individuals.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of anonymous objects from the starting set
   */
  public static Set<OWLObject> selectAnonymous(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if ((object instanceof OWLAnonymousClassExpression)
          || (object instanceof OWLAnonymousIndividual)) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return the children of the starting set.
   *
   * @param ontology OWLOntology to retrieve children from
   * @param objects Set of OWLObjects to get children of
   * @return set of children of the starting set
   */
  public static Set<OWLObject> selectChildren(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        relatedObjects.addAll(EntitySearcher.getSubClasses((OWLClass) object, ontology));
      } else if (object instanceof OWLObjectProperty) {
        relatedObjects.addAll(
            EntitySearcher.getSubProperties((OWLObjectProperty) object, ontology));
      } else if (object instanceof OWLDataProperty) {
        relatedObjects.addAll(EntitySearcher.getSubProperties((OWLDataProperty) object, ontology));
      } else if (object instanceof OWLAnnotation) {
        relatedObjects.addAll(
            EntitySearcher.getSubProperties((OWLAnnotationProperty) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of OWLClasses from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of OWLClasses from the starting set
   */
  public static Set<OWLObject> selectClasses(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return all objects contained in the ontology that are
   * NOT in the starting set.
   *
   * @param ontology OWLOntology to retrieve complement objects from
   * @param objects Set of OWLObjects to get the complement of
   * @return complement set of OWLObjects
   */
  public static Set<OWLObject> selectComplement(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      relatedObjects.addAll(OntologyHelper.getObjects(axiom));
    }
    relatedObjects.removeAll(objects);
    return relatedObjects;
  }

  /** */
  public static Set<OWLObject> selectCURIE(
      IOHelper ioHelper, Set<OWLObject> objects, String selector) {
    String prefix = selector.split(":")[0];
    String pattern = selector.split(":")[1];
    Map<String, String> prefixes = ioHelper.getPrefixes();
    String namespace = prefixes.getOrDefault(prefix, null);

    if (namespace == null) {
      logger.error(String.format("Prefix '%s' is not a loaded prefix and will be ignored", prefix));
      return objects;
    }

    String iriPattern = namespace + pattern;

    return selectIRI(objects, iriPattern);
  }

  /**
   * Given a set of objects, return a set of OWLDataProperties from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of OWLDataProperties from the starting set
   */
  public static Set<OWLObject> selectDataProperties(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLDataProperty) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return all descendants (recursively) of those objects.
   *
   * @param ontology OWLOntology to retrieve descendants from
   * @param objects Set of OWLObjects to get descendants of
   * @return set of descendants of the starting set
   */
  public static Set<OWLObject> selectDescendants(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        selectClassDescendants(ontology, (OWLClass) object, relatedObjects);
      } else if (object instanceof OWLAnnotationProperty) {
        selectAnnotationPropertyDescendants(
            ontology, (OWLAnnotationProperty) object, relatedObjects);
      } else if (object instanceof OWLDataProperty) {
        selectDataPropertyDescendants(ontology, (OWLDataProperty) object, relatedObjects);
      } else if (object instanceof OWLObjectProperty) {
        selectObjectPropertyDescendants(ontology, (OWLObjectProperty) object, relatedObjects);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return all domains of any properties in the set.
   *
   * @param ontology OWLOntology to retrieve domains from
   * @param objects OWLObjects to retrieve domains of
   * @return set of domains of the starting set
   */
  public static Set<OWLObject> selectDomains(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLAnnotationProperty) {
        relatedObjects.addAll(EntitySearcher.getDomains((OWLAnnotationProperty) object, ontology));
      } else if (object instanceof OWLDataProperty) {
        relatedObjects.addAll(EntitySearcher.getDomains((OWLDataProperty) object, ontology));
      } else if (object instanceof OWLObjectProperty) {
        relatedObjects.addAll(EntitySearcher.getDomains((OWLObjectProperty) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return all equivalent objects of the starting set.
   *
   * @param ontology OWLOntology to retrieve equivalents from
   * @param objects Set of OWLObjects to get equivalents of
   * @return set of equivalent OWLObjects
   */
  public static Set<OWLObject> selectEquivalents(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        relatedObjects.addAll(EntitySearcher.getEquivalentClasses((OWLClass) object, ontology));
      } else if (object instanceof OWLDataProperty) {
        relatedObjects.addAll(
            EntitySearcher.getEquivalentProperties((OWLDataProperty) object, ontology));
      } else if (object instanceof OWLObjectProperty) {
        relatedObjects.addAll(
            EntitySearcher.getEquivalentProperties((OWLObjectProperty) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of OWLIndividuals from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of OWLIndividuals from the starting set
   */
  public static Set<OWLObject> selectIndividuals(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLIndividual) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, select all the instances of the classes in the set of
   * objects.
   *
   * @param ontology OWLOntology to retrieve instances from
   * @param objects Set of OWLObjects to get instances of
   * @return set of instances as OWLObjects
   */
  public static Set<OWLObject> selectInstances(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        relatedObjects.addAll(EntitySearcher.getIndividuals((OWLClass) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, and a selector string (IRI pattern in angle brackets), select all the
   * objects that have an IRI matching that pattern.
   *
   * @param objects Set of OWLObjects to look through
   * @param selector IRI pattern to match (enclosed in angle brackets, using ? or * wildcards)
   * @return set of IRIs that match the selector pattern
   */
  public static Set<OWLObject> selectIRI(Set<OWLObject> objects, String selector) {
    Set<OWLObject> relatedObjects = new HashSet<>();

    // Check that we're matching a pattern here
    // --term should be used for full IRIs
    if (!selector.contains("~") && !selector.contains("*") && !selector.contains("?")) {
      throw new IllegalArgumentException(String.format(invalidIRIPatternError, selector));
    }
    // Get rid of brackets
    if (selector.startsWith("<") && selector.endsWith(">")) {
      selector = selector.substring(1, selector.length() - 1);
    }
    if (!selector.startsWith("~")) {
      // Transform wildcards into regex patterns and escape periods
      selector = selector.replace(".", "\\.").replace("?", ".?").replace("*", ".*");
    } else {
      // Otherwise, it is already a regex pattern
      // Just remove the tilde
      selector = selector.substring(1);
    }
    Pattern pattern = Pattern.compile(selector);
    for (OWLObject object : objects) {
      if (object instanceof OWLEntity) {
        OWLEntity entity = (OWLEntity) object;
        // Determine if IRI matches the pattern
        String iri = entity.getIRI().toString();
        if (pattern.matcher(iri).matches()) {
          relatedObjects.add(object);
        }
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of named objects from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of named objects from the starting set
   */
  public static Set<OWLObject> selectNamed(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLNamedObject) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given a set of objects, return a set of OWLObjectProperties from the starting set.
   *
   * @param objects Set of OWLObjects to filter
   * @return subset of OWLObjectProperties from the starting set
   */
  public static Set<OWLObject> selectObjectProperties(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLObjectProperty) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return the parent objects of all objects in the set.
   *
   * @param ontology OWLOntology to retrieve parents from
   * @param objects set of OWLObjects to get parents of
   * @return set of parent objects
   */
  public static Set<OWLObject> selectParents(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        relatedObjects.addAll(EntitySearcher.getSuperClasses((OWLClass) object, ontology));
      } else if (object instanceof OWLObjectProperty) {
        relatedObjects.addAll(
            EntitySearcher.getSuperProperties((OWLObjectProperty) object, ontology));
      } else if (object instanceof OWLDataProperty) {
        relatedObjects.addAll(
            EntitySearcher.getSuperProperties((OWLDataProperty) object, ontology));
      } else if (object instanceof OWLAnnotation) {
        relatedObjects.addAll(
            EntitySearcher.getSuperProperties((OWLAnnotationProperty) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology, a set of objects, and a pattern to match annotations to, return a set of
   * objects that are annotated with the matching annotation.
   *
   * @param ontology OWLOntology to get annotations from
   * @param ioHelper IOHelper to use for prefixes
   * @param objects Set of OWLObjects to filter
   * @param annotationPattern annotation to filter OWLObjects on
   * @return subset of OWLObjects matching the annotation pattern
   * @throws Exception on issue getting literal annotations
   */
  public static Set<OWLObject> selectPattern(
      OWLOntology ontology, IOHelper ioHelper, Set<OWLObject> objects, String annotationPattern)
      throws Exception {
    Set<OWLAnnotation> annotations = getAnnotations(ontology, ioHelper, annotationPattern);
    return selectAnnotated(ontology, objects, annotations);
  }

  /**
   * Given a set of objects, return a set of all OWLProperties from the starting set.
   *
   * @param objects Set of OWLObjects to filter OWLProperties of
   * @return subset of OWLProperties from the starting set
   */
  public static Set<OWLObject> selectProperties(Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLProperty) {
        relatedObjects.add(object);
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return all ranges of any properties in the set.
   *
   * @param ontology OWLOntology to retrieve ranges from
   * @param objects OWLObjects to retrieve ranges of
   * @return set of ranges of the starting set
   */
  public static Set<OWLObject> selectRanges(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLAnnotationProperty) {
        relatedObjects.addAll(EntitySearcher.getRanges((OWLAnnotationProperty) object, ontology));
      } else if (object instanceof OWLDataProperty) {
        relatedObjects.addAll(EntitySearcher.getRanges((OWLDataProperty) object, ontology));
      } else if (object instanceof OWLObjectProperty) {
        relatedObjects.addAll(EntitySearcher.getRanges((OWLObjectProperty) object, ontology));
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology and a set of objects, return a set of all the types of the individuals in
   * that set.
   *
   * @param ontology OWLOntology to retrieve types from
   * @param objects Set of OWLObjects to get the types of
   * @return set of types (OWLClass or OWLClassExpression)
   */
  public static Set<OWLObject> selectTypes(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLIndividual) {
        relatedObjects.addAll(EntitySearcher.getTypes((OWLIndividual) object, ontology));
      }
    }
    return relatedObjects;
  }

  public static Set<OWLAxiom> spanGaps(OWLOntology ontology, Set<OWLObject> objects) {
    return spanGaps(ontology, objects, false);
  }

  /**
   * Given an ontology and a set of objects, construct a set of subClassOf axioms that span the gaps
   * between classes to maintain a hierarchy.
   *
   * @param ontology input OWLOntology
   * @param objects set of Objects to build hierarchy
   * @return set of OWLAxioms to maintain hierarchy
   */
  public static Set<OWLAxiom> spanGaps(
      OWLOntology ontology, Set<OWLObject> objects, boolean excludeAnonymous) {
    Set<Map<String, OWLAnnotationProperty>> aPropPairs = new HashSet<>();
    Set<Map<String, OWLClassExpression>> classPairs = new HashSet<>();
    Set<Map<String, OWLDataPropertyExpression>> dPropPairs = new HashSet<>();
    Set<Map<String, OWLObjectPropertyExpression>> oPropPairs = new HashSet<>();
    Set<OWLAxiom> axioms = new HashSet<>();
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

    // Iterate through objects to generate sub-super pairs
    for (OWLObject object : objects) {
      if (object instanceof OWLAnnotationProperty) {
        OWLAnnotationProperty p = (OWLAnnotationProperty) object;
        spanGapsHelper(
            ontology, objects, aPropPairs, p, EntitySearcher.getSuperProperties(p, ontology));
      } else if (object instanceof OWLClass) {
        OWLClass cls = (OWLClass) object;
        spanGapsHelper(ontology, objects, classPairs, cls, getSuperClasses(ontology, cls));
      } else if (object instanceof OWLDataProperty) {
        OWLDataProperty p = (OWLDataProperty) object;
        spanGapsHelper(
            ontology, objects, dPropPairs, p, EntitySearcher.getSuperProperties(p, ontology));
      } else if (object instanceof OWLObjectProperty) {
        OWLObjectProperty p = (OWLObjectProperty) object;
        spanGapsHelper(
            ontology, objects, oPropPairs, p, EntitySearcher.getSuperProperties(p, ontology));
      }
    }

    // Generate axioms based on the sub-super pairs
    for (Map<String, OWLAnnotationProperty> propPair : aPropPairs) {
      OWLAnnotationProperty subProperty = propPair.get(SUB);
      OWLAnnotationProperty superProperty = propPair.get(SUPER);
      axioms.add(dataFactory.getOWLSubAnnotationPropertyOfAxiom(subProperty, superProperty));
    }
    for (Map<String, OWLClassExpression> classPair : classPairs) {
      OWLClass subClass = classPair.get(SUB).asOWLClass();
      OWLClassExpression superClass = classPair.get(SUPER);
      if (superClass.isAnonymous() && !excludeAnonymous || !superClass.isAnonymous()) {
        // Anonymous axioms may have been removed so we don't want to add them back
        axioms.add(dataFactory.getOWLSubClassOfAxiom(subClass, superClass));
      }
    }
    for (Map<String, OWLDataPropertyExpression> propPair : dPropPairs) {
      OWLDataProperty subProperty = propPair.get(SUB).asOWLDataProperty();
      OWLDataPropertyExpression superProperty = propPair.get(SUPER);
      axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(subProperty, superProperty));
    }
    for (Map<String, OWLObjectPropertyExpression> propPair : oPropPairs) {
      OWLObjectProperty subProperty = propPair.get(SUB).asOWLObjectProperty();
      OWLObjectPropertyExpression superProperty = propPair.get(SUPER);
      axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(subProperty, superProperty));
    }

    return axioms;
  }

  /**
   * Given an IOHelper and an annotation as CURIE=..., return the OWLAnnotation object(s).
   *
   * @param ontology OWLOntology to get annotations from
   * @param ioHelper IOHelper to get IRI
   * @param annotation String input
   * @return set of OWLAnnotations
   * @throws Exception on issue getting literal annotation
   */
  protected static Set<OWLAnnotation> getAnnotations(
      OWLOntology ontology, IOHelper ioHelper, String annotation) throws Exception {
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    // Create an IRI from the CURIE
    String propertyID = annotation.split("=")[0];
    IRI propertyIRI = ioHelper.createIRI(propertyID);
    if (propertyIRI == null) {
      throw new IllegalArgumentException(
          String.format(invalidIRIError, "annotation property", propertyID));
    }
    // Get the annotation property and string representation of value
    OWLAnnotationProperty annotationProperty = dataFactory.getOWLAnnotationProperty(propertyIRI);
    String value = annotation.split("=")[1];
    // Based on the value, determine the type of annotation
    if (value.contains("<") && value.contains(">")) {
      // Return an IRI annotation
      String valueID = value.substring(1, value.length() - 1);
      IRI valueIRI = ioHelper.createIRI(valueID);
      if (valueIRI == null) {
        throw new IllegalArgumentException(
            String.format(invalidIRIError, "annotation value (IRI)", valueID));
      }
      return Sets.newHashSet(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
    } else if (value.contains("~'")) {
      // Return a set of annotations in the ontology that match a regex pattern
      return getPatternAnnotations(ontology, annotationProperty, value);
    } else if (value.contains("'")) {
      // Return a literal (string, boolean, double, integer, float) annotation
      return Sets.newHashSet(
          getLiteralAnnotation(ioHelper, dataFactory, annotationProperty, value));
    } else {
      // Return an IRI annotation based on a CURIE
      IRI valueIRI = ioHelper.createIRI(value);
      if (valueIRI == null) {
        throw new IllegalArgumentException(
            String.format(invalidIRIError, "annotation value (CURIE)", value));
      }
      return Sets.newHashSet(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
    }
  }

  /**
   * Given an OWLAxiom, return a set of subjects. Most axioms will only have one subject, but
   * equivalent and disjoint axioms will have 2+. If the subject is anonymous, the return set will
   * be empty.
   *
   * @param axiom OWLAxiom to get subject(s)
   * @return set of zero or more IRIs for subject(s)
   */
  private static Set<IRI> getAxiomSubjects(OWLAxiom axiom) {
    Set<IRI> iris = new HashSet<>();

    if (axiom instanceof OWLDeclarationAxiom) {
      OWLDeclarationAxiom decAxiom = (OWLDeclarationAxiom) axiom;
      OWLEntity subject = decAxiom.getEntity();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.getIRI());
      }
    } else if (axiom instanceof OWLSubClassOfAxiom) {
      OWLSubClassOfAxiom scAxiom = (OWLSubClassOfAxiom) axiom;
      OWLClassExpression subject = scAxiom.getSubClass();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLClass().getIRI());
      }
    } else if (axiom instanceof OWLEquivalentClassesAxiom) {
      OWLEquivalentClassesAxiom eqAxiom = (OWLEquivalentClassesAxiom) axiom;
      for (OWLClassExpression expr : eqAxiom.getNamedClasses()) {
        if (!expr.isAnonymous()) {
          iris.add(expr.asOWLClass().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLDisjointClassesAxiom) {
      OWLDisjointClassesAxiom djAxiom = (OWLDisjointClassesAxiom) axiom;
      for (OWLClassExpression expr : djAxiom.getClassExpressions()) {
        if (!expr.isAnonymous()) {
          iris.add(expr.asOWLClass().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLSubDataPropertyOfAxiom) {
      OWLSubDataPropertyOfAxiom spAxiom = (OWLSubDataPropertyOfAxiom) axiom;
      OWLDataPropertyExpression subject = spAxiom.getSubProperty();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLDataProperty().getIRI());
      }
    } else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
      OWLSubObjectPropertyOfAxiom spAxiom = (OWLSubObjectPropertyOfAxiom) axiom;
      OWLObjectPropertyExpression subject = spAxiom.getSubProperty();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLObjectProperty().getIRI());
      }
    } else if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {
      OWLEquivalentDataPropertiesAxiom eqAxiom = (OWLEquivalentDataPropertiesAxiom) axiom;
      for (OWLSubDataPropertyOfAxiom spAxiom : eqAxiom.asSubDataPropertyOfAxioms()) {
        OWLDataPropertyExpression subject = spAxiom.getSubProperty();
        if (!subject.isAnonymous()) {
          iris.add(subject.asOWLDataProperty().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) {
      OWLEquivalentObjectPropertiesAxiom eqAxiom = (OWLEquivalentObjectPropertiesAxiom) axiom;
      for (OWLSubObjectPropertyOfAxiom spAxiom : eqAxiom.asSubObjectPropertyOfAxioms()) {
        OWLObjectPropertyExpression subject = spAxiom.getSubProperty();
        if (!subject.isAnonymous()) {
          iris.add(subject.asOWLObjectProperty().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLDisjointDataPropertiesAxiom) {
      OWLDisjointDataPropertiesAxiom djAxiom = (OWLDisjointDataPropertiesAxiom) axiom;
      for (OWLDataPropertyExpression expr : djAxiom.getProperties()) {
        if (!expr.isAnonymous()) {
          iris.add(expr.asOWLDataProperty().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
      OWLDisjointObjectPropertiesAxiom djAxiom = (OWLDisjointObjectPropertiesAxiom) axiom;
      for (OWLObjectPropertyExpression expr : djAxiom.getProperties()) {
        if (!expr.isAnonymous()) {
          iris.add(expr.asOWLObjectProperty().getIRI());
        }
      }
      return iris;
    } else if (axiom instanceof OWLAnnotationAssertionAxiom) {
      OWLAnnotationAssertionAxiom annAxiom = (OWLAnnotationAssertionAxiom) axiom;
      OWLAnnotationSubject subject = annAxiom.getSubject();
      if (subject.isIRI()) {
        return Sets.newHashSet((IRI) subject);
      }
    } else if (axiom instanceof OWLClassAssertionAxiom) {
      OWLClassAssertionAxiom classAxiom = (OWLClassAssertionAxiom) axiom;
      OWLIndividual subject = classAxiom.getIndividual();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLNamedIndividual().getIRI());
      }
    } else if (axiom instanceof OWLDataPropertyAssertionAxiom) {
      OWLDataPropertyAssertionAxiom dpAxiom = (OWLDataPropertyAssertionAxiom) axiom;
      OWLIndividual subject = dpAxiom.getSubject();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLNamedIndividual().getIRI());
      }
    } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
      OWLObjectPropertyAssertionAxiom dpAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
      OWLIndividual subject = dpAxiom.getSubject();
      if (!subject.isAnonymous()) {
        return Sets.newHashSet(subject.asOWLNamedIndividual().getIRI());
      }
    }
    // May have been anonymous, no IRI to return
    return new HashSet<>();
  }

  /**
   * Given a set of OWLObjects, return the set of IRIs for those objects.
   *
   * @param objects OWLObjects to get IRIs of
   * @return IRIs of the given objects
   */
  private static Set<IRI> getIRIs(Set<OWLObject> objects) {
    Set<IRI> iris = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLNamedObject) {
        OWLNamedObject namedObject = (OWLNamedObject) object;
        iris.add(namedObject.getIRI());
      }
    }
    return iris;
  }

  /**
   * Given an OWL ontology, an annotation property, and an annotation value (in regex pattern form),
   * return a set of OWLAnnotations that have values matching the regex value.
   *
   * @param ontology OWLOntology to retrieve annotations from
   * @param annotationProperty OWLAnnotationProperty
   * @param value regex pattern to match values to
   * @return set of matching OWLAnnotations
   */
  private static Set<OWLAnnotation> getPatternAnnotations(
      OWLOntology ontology, OWLAnnotationProperty annotationProperty, String value) {
    Set<OWLAnnotation> annotations = new HashSet<>();
    String patternString = value.split("\'")[1];
    Pattern pattern = Pattern.compile(patternString);
    for (OWLEntity e : OntologyHelper.getEntities(ontology)) {
      for (OWLAnnotation a : EntitySearcher.getAnnotations(e, ontology)) {
        if (a.getProperty().equals(annotationProperty)) {
          OWLAnnotationValue av = a.getValue();
          String annotationValue;
          // The annotation value ONLY expects a plain or string
          try {
            OWLLiteralImplPlain plain = (OWLLiteralImplPlain) av;
            annotationValue = plain.getLiteral();
          } catch (ClassCastException ex) {
            try {
              OWLLiteralImplString str = (OWLLiteralImplString) av;
              annotationValue = str.getLiteral();
            } catch (ClassCastException ex2) {
              continue;
            }
          }
          Matcher matcher = pattern.matcher(annotationValue);
          if (matcher.matches()) {
            annotations.add(a);
          }
        }
      }
    }
    return annotations;
  }

  /**
   * Given an OWL data factory, an annotation property, and a literal value, return the
   * OWLAnnotation object.
   *
   * @param ioHelper IOHelper to retrieve prefix manager
   * @param dataFactory OWLDataFactory to create entities
   * @param annotationProperty OWLAnnotationProperty
   * @param value annotation value as string
   * @return OWLAnnotation object
   * @throws Exception on issue parsing to datatype
   */
  private static OWLAnnotation getLiteralAnnotation(
      IOHelper ioHelper,
      OWLDataFactory dataFactory,
      OWLAnnotationProperty annotationProperty,
      String value)
      throws Exception {
    // ioHelper.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    OWLAnnotationValue annotationValue;
    if (value.contains("^^")) {
      // A datatype is given
      String content = value.split("\\^\\^")[0].replace("'", "");
      String dataTypeID = value.split("\\^\\^")[1];
      IRI dataTypeIRI = ioHelper.createIRI(dataTypeID);
      if (dataTypeIRI == null) {
        throw new IllegalArgumentException(String.format(invalidIRIError, "datatype", dataTypeID));
      }
      OWLDatatype dt = dataFactory.getOWLDatatype(dataTypeIRI);
      if (dt.isBoolean()) {
        if (content.equalsIgnoreCase("true")) {
          annotationValue = dataFactory.getOWLLiteral(true);
        } else if (content.equalsIgnoreCase("false")) {
          annotationValue = dataFactory.getOWLLiteral(false);
        } else {
          throw new Exception(String.format(literalValueError, dataTypeID, "boolean"));
        }
      } else if (dt.isDouble()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Double.parseDouble(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "double"));
        }
      } else if (dt.isFloat()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Float.parseFloat(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "float"));
        }
      } else if (dt.isInteger()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Integer.parseInt(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "integer"));
        }
      } else if (dt.isString()) {
        annotationValue = dataFactory.getOWLLiteral(content);
      } else {
        annotationValue = dataFactory.getOWLLiteral(content, dt);
      }
    } else {
      // If a datatype isn't provided, default to string literal
      annotationValue = dataFactory.getOWLLiteral(value.replace("'", ""));
    }
    return dataFactory.getOWLAnnotation(annotationProperty, annotationValue);
  }

  /**
   * Given an ontology and a class expression, return the set of superclasses while removing any
   * circular subclass definitions. Warn on any circular subclasses.
   *
   * @param ontology OWLOntology to get superclasses
   * @param cls OWLClass to get superclasses of
   * @return Set of OWLClassExpressions that are superclasses of cls
   */
  private static Set<OWLClassExpression> getSuperClasses(OWLOntology ontology, OWLClass cls) {
    Set<OWLClassExpression> superclasses = new HashSet<>();

    // We might get stuck if a class is both subclass and equivalent
    // So compare the eqs to the superclasses and don't add a super if it's also an eq
    Collection<OWLClassExpression> eqs = EntitySearcher.getEquivalentClasses(cls, ontology);

    for (OWLClassExpression expr : EntitySearcher.getSuperClasses(cls, ontology)) {
      if (expr.isAnonymous()) {
        superclasses.add(expr);
        continue;
      }
      if (eqs.contains(expr)) {
        logger.warn(
            String.format(
                "Class '%s' has equivalent class and superclass '%s'", cls.getIRI(), expr));
        continue;
      }
      if (expr.asOWLClass().getIRI() == cls.getIRI()) {
        logger.warn("Circular subclass definition: " + cls.getIRI());
        continue;
      }
      superclasses.add(expr);
    }
    return superclasses;
  }

  /**
   * Given a list of base namespaces and a set of subject IRIs, determine if at least one of the
   * subjects is in the set of base namespaces.
   *
   * @param baseNamespaces list of base namespaces as strings
   * @param subjects set of IRIs to check
   * @return true if at least one IRI is in one of the base namespaces
   */
  private static boolean isBase(List<String> baseNamespaces, Set<IRI> subjects) {
    for (String base : baseNamespaces) {
      for (IRI subject : subjects) {
        if (subject.toString().startsWith(base)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Given a set of axiom classes (or null), set OWLAxiom as the only entry if the set is empty or
   * null. Otherwise return the original set.
   *
   * @param axiomTypes set of OWLAxiom classes
   * @return set of OWLAxiom classes
   */
  private static Set<Class<? extends OWLAxiom>> setDefaultAxiomType(
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    if (axiomTypes == null || axiomTypes.isEmpty()) {
      axiomTypes = new HashSet<>();
      axiomTypes.add(OWLAxiom.class);
    }
    return axiomTypes;
  }

  /**
   * Given an ontology, a set of objects, and a set of annotations, return a set of objects that are
   * annotated with at least one of the annotations in the set.
   *
   * @param ontology OWLOntology to retrieve annotations from
   * @param annotations Set of OWLAnnotations to filter objects with
   * @return subset of objects annotated by one of the OWLAnnotations
   */
  private static Set<OWLObject> selectAnnotated(
      OWLOntology ontology, Set<OWLObject> objects, Set<OWLAnnotation> annotations) {
    Set<OWLObject> relatedObjects = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLEntity) {
        for (OWLAnnotationAssertionAxiom axiom :
            EntitySearcher.getAnnotationAssertionAxioms((OWLEntity) object, ontology)) {
          if (annotations.contains(axiom.getAnnotation())) {
            relatedObjects.add(object);
          }
        }
        // Handle annotated axioms as well
        if (object instanceof OWLClass) {
          for (OWLAxiom axiom : ontology.getAxioms((OWLClass) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLObjectProperty) {
          for (OWLAxiom axiom : ontology.getAxioms((OWLObjectProperty) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLDataProperty) {
          for (OWLAxiom axiom : ontology.getAxioms((OWLDataProperty) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLAnnotationProperty) {
          for (OWLAxiom axiom :
              ontology.getAxioms((OWLAnnotationProperty) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLDatatype) {
          for (OWLAxiom axiom : ontology.getAxioms((OWLDatatype) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLIndividual) {
          for (OWLAxiom axiom : ontology.getAxioms((OWLIndividual) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        } else if (object instanceof OWLObjectPropertyExpression) {
          for (OWLAxiom axiom :
              ontology.getAxioms((OWLObjectPropertyExpression) object, Imports.EXCLUDED)) {
            if (axiom.isAnnotated()) {
              for (OWLAnnotation annotation : axiom.getAnnotations()) {
                if (annotations.contains(annotation)) {
                  relatedObjects.add(object);
                }
              }
            }
          }
        }
      }
    }
    return relatedObjects;
  }

  /**
   * Given an ontology, an annotation property, and a set, recursively fill the set with the
   * ancestors of the annotation property.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param property OWLAnnotationProperty to get ancestors of
   * @param ancestors Set of OWLObjects to fill with ancestors
   */
  private static void selectAnnotationPropertyAncestors(
      OWLOntology ontology, OWLAnnotationProperty property, Set<OWLObject> ancestors) {
    for (OWLAnnotationProperty superProperty :
        EntitySearcher.getSuperProperties(property, ontology)) {
      ancestors.add(superProperty);
      if (!superProperty.isTopEntity()) {
        selectAnnotationPropertyAncestors(ontology, superProperty, ancestors);
      }
    }
  }

  /**
   * Given an ontology, an annotation property, and a set, recursively fill the set with the
   * descendants of the annotation property.
   *
   * @param ontology OWLOntology to get descendants from
   * @param property OWLAnnotationProperty to get descendants of
   * @param descendants Set of OWLObjects to fill with descendants
   */
  private static void selectAnnotationPropertyDescendants(
      OWLOntology ontology, OWLAnnotationProperty property, Set<OWLObject> descendants) {
    for (OWLAnnotationProperty subProperty : EntitySearcher.getSubProperties(property, ontology)) {
      descendants.add(subProperty);
      if (!EntitySearcher.getSubProperties(subProperty, ontology).isEmpty()) {
        selectAnnotationPropertyAncestors(ontology, subProperty, descendants);
      }
    }
  }

  /**
   * Given an ontology, a class, and a set, recursively fill the set with the ancestors of the
   * class.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param cls OWLClass to get ancestors of
   * @param ancestors Set of OWLObjects to fill with ancestors
   */
  private static void selectClassAncestors(
      OWLOntology ontology, OWLClass cls, Set<OWLObject> ancestors) {
    for (OWLClassExpression classExpression : EntitySearcher.getSuperClasses(cls, ontology)) {
      if (!classExpression.isAnonymous()) {
        OWLClass superClass = classExpression.asOWLClass();
        ancestors.add(superClass);
        if (!superClass.isTopEntity()) {
          selectClassAncestors(ontology, superClass, ancestors);
        }
      } else {
        ancestors.add(classExpression);
      }
    }
  }

  /**
   * Given an ontology, a class, and a set, recursively fill the set with the descendants of the
   * class.
   *
   * @param ontology OWLOntology to get descendants from
   * @param cls OWLClass to get descendants of
   * @param descendants Set of OWLObjects to fill with descendants
   */
  private static void selectClassDescendants(
      OWLOntology ontology, OWLClass cls, Set<OWLObject> descendants) {
    for (OWLClassExpression classExpression : EntitySearcher.getSubClasses(cls, ontology)) {
      if (!classExpression.isAnonymous()) {
        OWLClass subClass = classExpression.asOWLClass();
        descendants.add(subClass);
        if (!EntitySearcher.getSubClasses(subClass, ontology).isEmpty()) {
          selectClassDescendants(ontology, subClass, descendants);
        }
      } else {
        descendants.add(classExpression);
      }
    }
  }

  /**
   * Given an ontology, a data property, and a set, recursively fill the set with the ancestors of
   * the property.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param property OWLDataProperty to get ancestors of
   * @param ancestors Set of OWLObjects to fill with ancestors
   */
  private static void selectDataPropertyAncestors(
      OWLOntology ontology, OWLDataProperty property, Set<OWLObject> ancestors) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(property, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLDataProperty superProperty =
            propertyExpression.getDataPropertiesInSignature().iterator().next();
        ancestors.add(superProperty);
        if (!superProperty.isTopEntity()) {
          selectDataPropertyAncestors(ontology, superProperty, ancestors);
        }
      } else {
        ancestors.add(propertyExpression);
      }
    }
  }

  /**
   * Given an ontology, a data property, and a set, recursively fill the set with the descendants of
   * the property.
   *
   * @param ontology OWLOntology to get descendants from
   * @param property OWLDataProperty to get descendants of
   * @param descendants Set of OWLObjects to fill with descendants
   */
  private static void selectDataPropertyDescendants(
      OWLOntology ontology, OWLDataProperty property, Set<OWLObject> descendants) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getSubProperties(property, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLDataProperty subProperty =
            propertyExpression.getDataPropertiesInSignature().iterator().next();
        descendants.add(subProperty);
        if (!EntitySearcher.getSubProperties(subProperty, ontology).isEmpty()) {
          selectDataPropertyDescendants(ontology, subProperty, descendants);
        }
      } else {
        descendants.add(propertyExpression);
      }
    }
  }

  /**
   * Given an ontology, an object property, and a set, recursively fill the set with the ancestors
   * of the property.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param property OWLObjectProperty to get ancestors of
   * @param ancestors Set of OWLObjects to fill with ancestors
   */
  private static void selectObjectPropertyAncestors(
      OWLOntology ontology, OWLObjectProperty property, Set<OWLObject> ancestors) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(property, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty superProperty =
            propertyExpression.getObjectPropertiesInSignature().iterator().next();
        ancestors.add(superProperty);
        if (!superProperty.isTopEntity()) {
          selectObjectPropertyAncestors(ontology, superProperty, ancestors);
        }
      } else {
        ancestors.add(propertyExpression);
      }
    }
  }

  /**
   * Given an ontology, an object property, and a set, recursively fill the set with the descendants
   * of the property.
   *
   * @param ontology OWLOntology to get descendants from
   * @param property OWLObjectProperty to get descendants of
   * @param descendants Set of OWLObjects to fill with descendants
   */
  private static void selectObjectPropertyDescendants(
      OWLOntology ontology, OWLObjectProperty property, Set<OWLObject> descendants) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getSubProperties(property, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty subProperty =
            propertyExpression.getObjectPropertiesInSignature().iterator().next();
        descendants.add(subProperty);
        if (!EntitySearcher.getSubProperties(subProperty, ontology).isEmpty()) {
          selectObjectPropertyDescendants(ontology, subProperty, descendants);
        }
      } else {
        descendants.add(propertyExpression);
      }
    }
  }

  /**
   * Helper method to create subPropertyOf axioms that span gaps. This method fills in a list of
   * maps, each containing a sub-property/super-property pair. This list is used to generate the
   * appropriate subPropertyOf axioms.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param objects set of OWLObjects to include
   * @param propPairs list of sub-super pairs
   * @param property OWLAnnotationProperty in set to act as sub-property
   * @param superProperties Collection of super-properties
   */
  private static void spanGapsHelper(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Map<String, OWLAnnotationProperty>> propPairs,
      OWLAnnotationProperty property,
      Collection<OWLAnnotationProperty> superProperties) {
    for (OWLAnnotationProperty sp : superProperties) {
      if (objects.contains(sp)) {
        if (objects.containsAll(sp.getSignature())) {
          Map<String, OWLAnnotationProperty> propertyPair = new HashMap<>();
          propertyPair.put(SUB, property);
          propertyPair.put(SUPER, sp);
          if (propPairs.add(propertyPair)) {
            property = sp;
            spanGapsHelper(
                ontology,
                objects,
                propPairs,
                property,
                EntitySearcher.getSuperProperties(property, ontology));
          }
        }
      } else if (!sp.isAnonymous()) {
        spanGapsHelper(
            ontology,
            objects,
            propPairs,
            property,
            EntitySearcher.getSuperProperties(sp, ontology));
      }
    }
  }

  /**
   * Helper method to create subClassOf axioms that span gaps. This method fills in a list of maps,
   * each containing a subclass/superclass pair. This list is used to generate the appropriate
   * subClassOf axioms.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param objects set of OWLObjects to include
   * @param classPairs list of sub-super pairs
   * @param cls OWLClass in set to act as subclass
   * @param superClasses Collection of superclass OWLClassExpressions
   */
  private static void spanGapsHelper(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Map<String, OWLClassExpression>> classPairs,
      OWLClass cls,
      Collection<OWLClassExpression> superClasses) {
    for (OWLClassExpression sc : superClasses) {
      if (objects.containsAll(sc.getSignature())) {
        Map<String, OWLClassExpression> classPair = new HashMap<>();
        classPair.put(SUB, cls);
        classPair.put(SUPER, sc);
        if (classPairs.add(classPair)) {
          // only recurse if the pair just added was not already present in the set.
          if (!sc.isAnonymous()) {
            spanGapsHelper(
                ontology,
                objects,
                classPairs,
                sc.asOWLClass(),
                getSuperClasses(ontology, sc.asOWLClass()));
          }
        }
      } else if (!sc.isAnonymous()) {
        spanGapsHelper(
            ontology, objects, classPairs, cls, getSuperClasses(ontology, sc.asOWLClass()));
      }
    }
  }

  /**
   * Helper method to create subPropertyOf axioms that span gaps (for data properties). This method
   * fills in a list of maps, each containing a sub-property/super-property pair. This list is used
   * to generate the appropriate subPropertyOf axioms.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param objects set of OWLObjects to include
   * @param propPairs list of sub-super pairs
   * @param property OWLDataProperty in set to act as sub-property
   * @param superProperties Collection of super-properties OWLDataPropertyExpressions
   */
  private static void spanGapsHelper(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Map<String, OWLDataPropertyExpression>> propPairs,
      OWLDataProperty property,
      Collection<OWLDataPropertyExpression> superProperties) {
    for (OWLDataPropertyExpression sp : superProperties) {
      if (objects.containsAll(sp.getSignature())) {
        Map<String, OWLDataPropertyExpression> propertyPair = new HashMap<>();
        propertyPair.put(SUB, property);
        propertyPair.put(SUPER, sp);
        if (propPairs.add(propertyPair)) {
          if (!sp.isAnonymous()) {
            property = (OWLDataProperty) sp;
            spanGapsHelper(
                ontology,
                objects,
                propPairs,
                property,
                EntitySearcher.getSuperProperties(property, ontology));
          }
        }
      } else if (!sp.isAnonymous()) {
        spanGapsHelper(
            ontology,
            objects,
            propPairs,
            property,
            EntitySearcher.getSuperProperties(sp, ontology));
      }
    }
  }

  /**
   * Helper method to create subPropertyOf axioms that span gaps (for object properties). This
   * method fills in a list of maps, each containing a sub-property/super-property pair. This list
   * is used to generate the appropriate subPropertyOf axioms.
   *
   * @param ontology OWLOntology to get ancestors from
   * @param objects set of OWLObjects to include
   * @param propPairs list of sub-super pairs
   * @param property OWLObjectProperty in set to act as sub-property
   * @param superProperties Collection of super-properties OWLObjectPropertyExpressions
   */
  private static void spanGapsHelper(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Map<String, OWLObjectPropertyExpression>> propPairs,
      OWLObjectProperty property,
      Collection<OWLObjectPropertyExpression> superProperties) {
    for (OWLObjectPropertyExpression sp : superProperties) {
      if (objects.containsAll(sp.getSignature())) {
        Map<String, OWLObjectPropertyExpression> propertyPair = new HashMap<>();
        propertyPair.put(SUB, property);
        propertyPair.put(SUPER, sp);
        if (propPairs.add(propertyPair)) {
          if (!sp.isAnonymous()) {
            property = (OWLObjectProperty) sp;
            spanGapsHelper(
                ontology,
                objects,
                propPairs,
                property,
                EntitySearcher.getSuperProperties(property, ontology));
          }
        }
      } else if (!sp.isAnonymous()) {
        spanGapsHelper(
            ontology,
            objects,
            propPairs,
            property,
            EntitySearcher.getSuperProperties(sp, ontology));
      }
    }
  }
}
