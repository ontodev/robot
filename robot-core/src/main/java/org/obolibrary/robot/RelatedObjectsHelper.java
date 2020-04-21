package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience methods to get related entities for a set of IRIs. Allowed relation options are: -
 * ancestors - descendants - disjoints - domains - equivalents - inverses - ranges - types
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RelatedObjectsHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RelatedObjectsHelper.class);

  /** Shared data factory. */
  private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

  /** Namespace for error messages. */
  private static final String NS = "errors#";

  /** Error message when --axioms is not a valid AxiomType. Expects: input string. */
  private static final String axiomTypeError = NS + "AXIOM TYPE ERROR %s is not a valid axiom type";

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

  /**
   * Filter a set of OWLAxioms based on the provided arguments.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of OWLObjects to get axioms for
   * @param axiomSelectors List of string selectors for types of axioms
   * @param baseNamespaces List of string base namespaces used for internal/external selections
   * @param partial if true, get any axiom containing at least one OWLObject from objects
   * @param namedOnly if true, ignore anonymous OWLObjects in axioms
   * @return set of filtered OWLAxioms
   * @throws OWLOntologyCreationException on issue creating empty ontology for tautology checker
   */
  public static Set<OWLAxiom> filterAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      List<String> axiomSelectors,
      List<String> baseNamespaces,
      boolean partial,
      boolean namedOnly)
      throws OWLOntologyCreationException {

    // Go through the axiom selectors in order and process selections
    boolean internal = false;
    boolean external = false;
    Set<OWLAxiom> filteredAxioms = new HashSet<>();
    for (String axiomSelector : axiomSelectors) {
      if (axiomSelector.equalsIgnoreCase("internal")) {
        if (external) {
          logger.error(
              "ignoring 'internal' axiom selector - 'internal' and 'external' together will remove all axioms");
        }
        filteredAxioms.addAll(
            RelatedObjectsHelper.filterInternalAxioms(inputAxioms, baseNamespaces));
        internal = true;
      } else if (axiomSelector.equalsIgnoreCase("external")) {
        if (internal) {
          logger.error(
              "ignoring 'external' axiom selector - 'internal' and 'external' together will remove all axioms");
        }
        filteredAxioms.addAll(
            RelatedObjectsHelper.filterExternalAxioms(inputAxioms, baseNamespaces));
        external = true;
      } else if (axiomSelector.equalsIgnoreCase("tautologies")) {
        filteredAxioms.addAll(RelatedObjectsHelper.filterTautologicalAxioms(inputAxioms, false));
      } else if (axiomSelector.equalsIgnoreCase("structural-tautologies")) {
        filteredAxioms.addAll(RelatedObjectsHelper.filterTautologicalAxioms(inputAxioms, true));
      } else {
        // Assume this is a normal OWLAxiom type
        Set<Class<? extends OWLAxiom>> axiomTypes =
            RelatedObjectsHelper.getAxiomValues(axiomSelector);
        filteredAxioms.addAll(
            filterAxiomsByAxiomType(inputAxioms, objects, axiomTypes, partial, namedOnly));
      }
    }
    return filteredAxioms;
  }

  /**
   * Given a set of OWLAxioms, a set of OWLObjects, a set of OWLAxiom Classes, a partial boolean,
   * and a named-only boolean, return a set of OWLAxioms based on OWLAxiom type. The axiom is added
   * to the set if it is an instance of an OWLAxiom Class in the axiomTypes set. If partial, return
   * any axioms that use at least one object from the objects set. Otherwise, only return axioms
   * that use objects in the set. If namedOnly, only consider named OWLObjects in the axioms and
   * ignore any anonymous objects when selecting the axioms.
   *
   * @param axioms set of OWLAxioms to filter
   * @param objects set of OWLObjects to get axioms for
   * @param axiomTypes set of OWLAxiom Classes that determines what type of axioms will be selected
   * @param partial if true, include all axioms that use at least one OWLObject from objects
   * @param namedOnly if true, ignore anonymous OWLObjects used in axioms
   * @return set of filtered axioms
   */
  public static Set<OWLAxiom> filterAxiomsByAxiomType(
      Set<OWLAxiom> axioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean partial,
      boolean namedOnly) {
    if (partial) {
      return RelatedObjectsHelper.filterPartialAxioms(axioms, objects, axiomTypes, namedOnly);
    } else {
      return RelatedObjectsHelper.filterCompleteAxioms(axioms, objects, axiomTypes, namedOnly);
    }
  }

  /**
   * Given a st of axioms, a set of objects, and a set of axiom types, return a set of axioms where
   * all the objects in those axioms are in the set of objects.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, consider only named OWLObjects
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> filterCompleteAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    if (namedOnly) {
      return getCompleteAxiomsNamedOnly(inputAxioms, objects, axiomTypes);
    } else {
      return getCompleteAxiomsIncludeAnonymous(inputAxioms, objects, axiomTypes);
    }
  }

  /**
   * Given a list of base namespaces and a set of axioms, return only the axioms that DO NOT have a
   * subject in the base namespaces.
   *
   * @param axioms set of OWLAxioms
   * @param baseNamespaces list of base namespaces
   * @return external OWLAxioms
   */
  public static Set<OWLAxiom> filterExternalAxioms(
      Set<OWLAxiom> axioms, List<String> baseNamespaces) {
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
   * @param axioms set of OWLAxioms
   * @param baseNamespaces list of base namespaces
   * @return internal OWLAxioms
   */
  public static Set<OWLAxiom> filterInternalAxioms(
      Set<OWLAxiom> axioms, List<String> baseNamespaces) {
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
   * Given a set of axioms, a set of objects, and a set of axiom types, return a set of axioms where
   * at least one object in those axioms is also in the set of objects.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of OWLObjects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, only consider named OWLObjects
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> filterPartialAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    if (namedOnly) {
      return getPartialAxiomsNamedOnly(inputAxioms, objects, axiomTypes);
    } else {
      return getPartialAxiomsIncludeAnonymous(inputAxioms, objects, axiomTypes);
    }
  }

  /**
   * Given a set of OWLAxioms and a structural boolean, filter for tautological axioms, i.e., those
   * that would be true in any ontology.
   *
   * @param axioms set of OWLAxioms to filter
   * @param structural if true, only filter for structural tautological axioms based on a hard-coded
   *     set of patterns (e.g., X subClassOf owl:Thing, owl:Nothing subClassOf X, X subClassOf X)
   * @return tautological OWLAxioms from original set
   * @throws OWLOntologyCreationException on issue creating empty ontology for tautology checker
   */
  public static Set<OWLAxiom> filterTautologicalAxioms(Set<OWLAxiom> axioms, boolean structural)
      throws OWLOntologyCreationException {
    // If structural, checker will be null
    OWLReasoner tautologyChecker = ReasonOperation.getTautologyChecker(structural);
    Set<OWLAxiom> tautologies = new HashSet<>();
    for (OWLAxiom a : axioms) {
      if (ReasonOperation.isTautological(a, tautologyChecker, structural)) {
        tautologies.add(a);
      }
    }
    return tautologies;
  }

  /**
   * @deprecated replaced by {@link #filterAxiomsByAxiomType(Set, Set, Set, boolean, boolean)}
   * @param ontology OWLOntology to get axioms from
   * @param objects OWLObjects to get axioms about
   * @param axiomTypes Set of OWLAxiom Classes that are the types of OWLAxioms to return
   * @param partial if true, return any OWLAxiom that has at least one OWLObject from objects
   * @param signature if true, ignore anonymous OWLObjects in axioms
   * @return axioms from ontology based on options
   */
  @Deprecated
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
   * Given a string axiom selector, return the OWLAxiom Class type(s) or null. The selector is
   * either a special keyword that represents a set of axiom classes or the name of an OWLAxiom
   * Class.
   *
   * @param axiomSelector string option to get the OWLAxiom Class type or types for
   * @return set of OWLAxiom Class types based on the string option
   */
  public static Set<Class<? extends OWLAxiom>> getAxiomValues(String axiomSelector) {
    Set<String> ignore =
        new HashSet<>(
            Arrays.asList("internal", "external", "tautologies", "structural-tautologies"));
    if (ignore.contains(axiomSelector)) {
      // Ignore special axiom options
      return null;
    }

    Set<Class<? extends OWLAxiom>> axiomTypes = new HashSet<>();
    if (axiomSelector.equalsIgnoreCase("all")) {
      axiomTypes.add(OWLAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("logical")) {
      axiomTypes.add(OWLLogicalAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("annotation")) {
      axiomTypes.add(OWLAnnotationAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("subclass")) {
      axiomTypes.add(OWLSubClassOfAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("subproperty")) {
      axiomTypes.add(OWLSubObjectPropertyOfAxiom.class);
      axiomTypes.add(OWLSubDataPropertyOfAxiom.class);
      axiomTypes.add(OWLSubAnnotationPropertyOfAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("equivalent")) {
      axiomTypes.add(OWLEquivalentClassesAxiom.class);
      axiomTypes.add(OWLEquivalentObjectPropertiesAxiom.class);
      axiomTypes.add(OWLEquivalentDataPropertiesAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("disjoint")) {
      axiomTypes.add(OWLDisjointClassesAxiom.class);
      axiomTypes.add(OWLDisjointObjectPropertiesAxiom.class);
      axiomTypes.add(OWLDisjointDataPropertiesAxiom.class);
      axiomTypes.add(OWLDisjointUnionAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("type")) {
      axiomTypes.add(OWLClassAssertionAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("abox")) {
      axiomTypes.addAll(
          AxiomType.ABoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("tbox")) {
      axiomTypes.addAll(
          AxiomType.TBoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("rbox")) {
      axiomTypes.addAll(
          AxiomType.RBoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("declaration")) {
      axiomTypes.add(OWLDeclarationAxiom.class);
    } else {
      AxiomType<?> at = AxiomType.getAxiomType(axiomSelector);
      if (at != null) {
        // Attempt to get the axiom type based on AxiomType names
        axiomTypes.add(at.getActualClass());
      } else {
        throw new IllegalArgumentException(String.format(axiomTypeError, axiomSelector));
      }
    }
    return axiomTypes;
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects. Prefer {@link #filterCompleteAxioms(Set,
   * Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> getCompleteAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects. Prefer {@link #filterCompleteAxioms(Set,
   * Set, Set, boolean)}.
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
    return filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, namedOnly);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects. Prefer {@link
   * #filterPartialAxioms(Set, Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> getPartialAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects. Prefer {@link
   * #filterPartialAxioms(Set, Set, Set, boolean)}.
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
    return filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, namedOnly);
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

  /**
   * Given an IOHelper, a set of objects, and a selector string for CURIE pattern, return a set of
   * objects matching that CURIE pattern.
   *
   * @param ioHelper the IOHelper to resolve names
   * @param objects the set of objects to filter
   * @param selector the CURIE selector string
   * @return the subset of objects that match the CURIE selector
   */
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
   * @param excludeAnonymous when true, span across anonymous nodes
   * @return set of OWLAxioms to maintain hierarchy
   */
  public static Set<OWLAxiom> spanGaps(
      OWLOntology ontology, Set<OWLObject> objects, boolean excludeAnonymous) {
    Set<Map<String, OWLAnnotationProperty>> aPropPairs = new HashSet<>();
    Set<Map<String, OWLClassExpression>> classPairs = new HashSet<>();
    Set<Map<String, OWLDataPropertyExpression>> dPropPairs = new HashSet<>();
    Set<Map<String, OWLObjectPropertyExpression>> oPropPairs = new HashSet<>();
    Set<OWLAxiom> axioms = new HashSet<>();

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
        Set<OWLObjectPropertyExpression> superProps = new HashSet<>();
        for (OWLSubObjectPropertyOfAxiom ax :
            ontology.getObjectSubPropertyAxiomsForSubProperty(p)) {
          superProps.add(ax.getSuperProperty());
        }
        spanGapsHelper(ontology, objects, oPropPairs, p, superProps);
      }
    }

    // Generate axioms based on the sub-super pairs
    for (Map<String, OWLAnnotationProperty> propPair : aPropPairs) {
      OWLAnnotationProperty subProperty = propPair.get(SUB);
      OWLAnnotationProperty superProperty = propPair.get(SUPER);
      axioms.add(df.getOWLSubAnnotationPropertyOfAxiom(subProperty, superProperty));
    }
    for (Map<String, OWLClassExpression> classPair : classPairs) {
      OWLClass subClass = classPair.get(SUB).asOWLClass();
      OWLClassExpression superClass = classPair.get(SUPER);
      if (superClass.isAnonymous() && !excludeAnonymous || !superClass.isAnonymous()) {
        // Anonymous axioms may have been removed so we don't want to add them back
        axioms.add(df.getOWLSubClassOfAxiom(subClass, superClass));
      }
    }
    for (Map<String, OWLDataPropertyExpression> propPair : dPropPairs) {
      OWLDataProperty subProperty = propPair.get(SUB).asOWLDataProperty();
      OWLDataPropertyExpression superProperty = propPair.get(SUPER);
      axioms.add(df.getOWLSubDataPropertyOfAxiom(subProperty, superProperty));
    }
    for (Map<String, OWLObjectPropertyExpression> propPair : oPropPairs) {
      OWLObjectProperty subProperty = propPair.get(SUB).asOWLObjectProperty();
      OWLObjectPropertyExpression superProperty = propPair.get(SUPER);
      axioms.add(df.getOWLSubObjectPropertyOfAxiom(subProperty, superProperty));
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
    // Create an IRI from the CURIE
    String propertyID = annotation.split("=")[0];
    IRI propertyIRI = ioHelper.createIRI(propertyID);
    if (propertyIRI == null) {
      throw new IllegalArgumentException(
          String.format(invalidIRIError, "annotation property", propertyID));
    }

    // Get the annotation property and string representation of value
    OWLAnnotationProperty annotationProperty = df.getOWLAnnotationProperty(propertyIRI);
    String value = annotation.split("=")[1];
    // Based on the value, determine the type of annotation
    if (value.startsWith("@")) {
      String lang = value.substring(1);
      return getLangAnnotations(ontology, annotationProperty, lang);

    } else if (value.startsWith("^^")) {
      String datatypeString = value.substring(2).replace("<", "").replace(">", "");
      IRI datatypeIRI = ioHelper.createIRI(datatypeString);
      OWLDatatype datatype = df.getOWLDatatype(datatypeIRI);
      return getTypedAnnotations(ontology, annotationProperty, datatype);

    } else if (value.contains("<") && value.contains(">") && !value.contains("^^")) {
      // Return an IRI annotation
      String valueID = value.substring(1, value.length() - 1);
      IRI valueIRI = ioHelper.createIRI(valueID);
      if (valueIRI == null) {
        throw new IllegalArgumentException(
            String.format(invalidIRIError, "annotation value (IRI)", valueID));
      }
      return Sets.newHashSet(df.getOWLAnnotation(annotationProperty, valueIRI));

    } else if (value.contains("~'")) {
      // Return a set of annotations in the ontology that match a regex pattern
      return getPatternAnnotations(ontology, annotationProperty, value);

    } else if (value.contains("'")) {
      // Return a literal (string, boolean, double, integer, float) annotation
      return Sets.newHashSet(getLiteralAnnotation(ioHelper, annotationProperty, value));

    } else {
      // Return an IRI annotation based on a CURIE
      IRI valueIRI = ioHelper.createIRI(value);
      if (valueIRI == null) {
        throw new IllegalArgumentException(
            String.format(invalidIRIError, "annotation value (CURIE)", value));
      }
      return Sets.newHashSet(df.getOWLAnnotation(annotationProperty, valueIRI));
    }
  }

  private static Set<OWLAnnotation> getTypedAnnotations(
      OWLOntology ontology, OWLAnnotationProperty property, OWLDatatype datatype) {
    Set<OWLAnnotation> annotations = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (axiom instanceof OWLAnnotationAssertionAxiom) {
        OWLAnnotationAssertionAxiom annAxiom = (OWLAnnotationAssertionAxiom) axiom;
        OWLAnnotation ann = annAxiom.getAnnotation();
        OWLAnnotationProperty usedProp = ann.getProperty();
        if (!usedProp.getIRI().toString().equals(property.getIRI().toString())) {
          continue;
        }
        OWLAnnotationValue value = ann.getValue();
        if (value.isLiteral()) {
          OWLLiteral lit = value.asLiteral().orNull();
          if (lit != null
              && lit.getDatatype().getIRI().toString().equals(datatype.getIRI().toString())) {
            annotations.add(ann);
          }
        }
      }
    }
    return annotations;
  }

  private static Set<OWLAnnotation> getLangAnnotations(
      OWLOntology ontology, OWLAnnotationProperty property, String lang) {
    Set<OWLAnnotation> annotations = new HashSet<>();
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (axiom instanceof OWLAnnotationAssertionAxiom) {
        OWLAnnotationAssertionAxiom annAxiom = (OWLAnnotationAssertionAxiom) axiom;
        OWLAnnotation ann = annAxiom.getAnnotation();
        OWLAnnotationProperty usedProp = ann.getProperty();
        if (!usedProp.getIRI().toString().equals(property.getIRI().toString())) {
          continue;
        }
        OWLAnnotationValue value = ann.getValue();
        if (value.isLiteral()) {
          OWLLiteral lit = value.asLiteral().orNull();
          if (lit != null && lit.hasLang(lang)) {
            annotations.add(ann);
          }
        }
      }
    }
    return annotations;
  }

  /**
   * Given an OWLOntology, a set of OWLObjects, and a set of axiom types, return any axioms in the
   * ontology that contain only objects in the object set.
   *
   * @param inputAxioms axioms to filter
   * @param objects OWLObjects to select
   * @param axiomTypes OWLAxiom types to select
   * @return set of complete OWLAxioms
   */
  private static Set<OWLAxiom> getCompleteAxiomsIncludeAnonymous(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLAxiom> axioms = new HashSet<>();

    // Use the IRIs to compare named objects
    Set<IRI> iris = getIRIs(objects);

    for (OWLAxiom axiom : inputAxioms) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        // Track if the axiom, without annotations, contains the objects
        boolean axiomMatch = false;
        if (axiom instanceof OWLAnnotationAssertionAxiom) {
          // Special handler for OWLAnnotationAssertionAxiom
          OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) axiom;
          // First look at the subject
          // Its probably an IRI, in which case see if it is in the set of IRIs
          boolean subjectMatch = false;
          if (a.getSubject().isAnonymous() && objects.contains(a.getSubject())) {
            subjectMatch = true;
          } else if (a.getSubject().isIRI() && iris.contains(a.getSubject())) {
            subjectMatch = true;
          }
          // If the subject matches, look at the property and value
          if (subjectMatch) {
            // Property will always compare IRI
            if (iris.contains(a.getProperty().getIRI())) {
              if (a.getValue().isLiteral()) {
                // Value might be literal
                // In this case, ignore it
                axiomMatch = true;
              } else if (objects.contains(a.getValue())
                  || iris.contains(a.getValue().asIRI().orNull())) {
                // If the objects contain the value (e.g. an anonymous object)
                // or if its an IRI and it is in the set of IRIs, its a match
                axiomMatch = true;
              }
            }
          }
        } else {
          if (objects.containsAll(OntologyHelper.getObjects(axiom.getAxiomWithoutAnnotations()))) {
            axiomMatch = true;
          }
        }

        // Handle annotations on the axiom by looking at the property and value
        // Ignore all literal values (if the property matches, add it)
        boolean annotationMatch = true;
        if (axiomMatch && axiom.isAnnotated()) {
          for (OWLAnnotation annotation : axiom.getAnnotations()) {
            if (!objects.contains(annotation.getProperty())
                && !iris.contains(annotation.getProperty().getIRI())) {
              annotationMatch = false;
            } else if (!objects.contains(annotation.getValue())
                && !iris.contains(annotation.getValue().asIRI().orNull())) {
              annotationMatch = false;
            }
          }
        }

        if (axiomMatch && annotationMatch) {
          axioms.add(axiom);
        } else if (axiomMatch) {
          // If only the axiom matches, add the axiom without annotations
          axioms.add(axiom.getAxiomWithoutAnnotations());
        }
      }
    }

    return axioms;
  }

  /**
   * Given an OWLOntology, a set of OWLObjects, and a set of axiom types, return any axioms in the
   * ontology that contain only IRIs in the object set.
   *
   * @param inputAxioms axioms to filter
   * @param objects OWLObjects to select
   * @param axiomTypes OWLAxiom types to select
   * @return set of complete OWLAxioms
   */
  private static Set<OWLAxiom> getCompleteAxiomsNamedOnly(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLAxiom> axioms = new HashSet<>();

    // All the IRIs of named objects in the set of OWL Objects
    Set<IRI> iris = getIRIs(objects);

    for (OWLAxiom axiom : inputAxioms) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        Set<IRI> sigIRIs = OntologyHelper.getIRIsInSignature(axiom.getAxiomWithoutAnnotations());

        // Maybe try adding all the axiom annotations
        // TODO - right now this is all or nothing
        // TODO - we should be able to ONLY return the annotations that match
        boolean unannotatedMatch = false;
        if (axiom.isAnnotated()) {
          if (iris.containsAll(sigIRIs)) {
            // Check if the target axiom alone is a match
            // If the target axiom matches, but the annotations don't
            // we will add just the unannotated axiom
            unannotatedMatch = true;
          }
          for (OWLAnnotation ann : axiom.getAnnotations()) {
            // Add the property IRI
            sigIRIs.add(ann.getProperty().getIRI());
            if (ann.getValue().isIRI()) {
              // Only add the value if its an IRI
              sigIRIs.add((IRI) ann.getValue());
            }
          }
        }

        if (iris.containsAll(sigIRIs)) {
          axioms.add(axiom);
        } else if (unannotatedMatch) {
          // If there are missing IRIs but the annotated axiom was a match
          // add the axiom without annotations
          axioms.add(axiom.getAxiomWithoutAnnotations());
        }
      }
    }
    return axioms;
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
   * Given an OWLOntology, a set of OWLObjects, and a set of axiom types, return any axiom that
   * includes at least one object.
   *
   * @param inputAxioms axioms to filter
   * @param objects OWLObjects to select
   * @param axiomTypes OWLAxiom types to select
   * @return set of matching OWLAxioms
   */
  private static Set<OWLAxiom> getPartialAxiomsIncludeAnonymous(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLAxiom> axioms = new HashSet<>();
    Set<IRI> iris = getIRIs(objects);

    for (OWLAxiom axiom : inputAxioms) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        boolean axiomMatch = false;
        if (axiom instanceof OWLAnnotationAssertionAxiom) {
          // Special handler for annotations
          OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) axiom;

          // Check both anonymous and IRI subjects
          if (a.getSubject().isAnonymous() && objects.contains(a.getSubject())) {
            axiomMatch = true;
          } else if (a.getSubject().isIRI() && iris.contains(a.getSubject())) {
            axiomMatch = true;
          }

          // Check the property
          if (iris.contains(a.getProperty().getIRI())) {
            axiomMatch = true;
          }

          // Check the value
          if (objects.contains(a.getValue()) || iris.contains(a.getValue().asIRI().orNull())) {
            axiomMatch = true;
          }

        } else {
          for (OWLObject o : OntologyHelper.getObjects(axiom.getAxiomWithoutAnnotations())) {
            if (objects.contains(o)) {
              axiomMatch = true;
            }
          }
        }

        // Check annotations on the axiom
        if (axiom.isAnnotated()) {
          for (OWLAnnotation annotation : axiom.getAnnotations()) {
            // Check the property and value
            if (objects.contains(annotation.getProperty())
                || iris.contains(annotation.getProperty().getIRI())) {
              axiomMatch = true;
            } else if (objects.contains(annotation.getValue())
                || iris.contains(annotation.getValue().asIRI().orNull())) {
              axiomMatch = true;
            }
          }
        }

        if (axiomMatch) {
          axioms.add(axiom);
        }
      }
    }

    return axioms;
  }

  /**
   * Given an OWLOntology, a set of OWLObjects, and a set of axiom types, return any axiom that
   * includes at least one IRI in the set of objects.
   *
   * @param inputAxioms axioms to filter
   * @param objects OWLObjects to select
   * @param axiomTypes OWLAxiom types to select
   * @return set of matching OWLAxioms
   */
  private static Set<OWLAxiom> getPartialAxiomsNamedOnly(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes) {
    Set<OWLAxiom> axioms = new HashSet<>();

    // All the IRIs of named objects in the set of OWL Objects
    Set<IRI> iris = getIRIs(objects);

    for (OWLAxiom axiom : inputAxioms) {
      if (OntologyHelper.extendsAxiomTypes(axiom, axiomTypes)) {
        Set<IRI> sigIRIs = OntologyHelper.getIRIsInSignature(axiom.getAxiomWithoutAnnotations());

        // Maybe try adding all the axiom annotations
        if (axiom.isAnnotated()) {
          for (OWLAnnotation ann : axiom.getAnnotations()) {
            // Add the property IRI
            sigIRIs.add(ann.getProperty().getIRI());
            if (ann.getValue().isIRI()) {
              // Only add the value if its an IRI
              sigIRIs.add((IRI) ann.getValue());
            }
          }
        }

        // Compare the signature IRIs to the set of IRIs
        for (IRI i : sigIRIs) {
          if (iris.contains(i)) {
            axioms.add(axiom);
          }
        }
      }
    }

    return axioms;
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
    for (OWLAxiom axiom : ontology.getAxioms()) {
      if (axiom instanceof OWLAnnotationAssertionAxiom) {
        OWLAnnotationAssertionAxiom annAxiom = (OWLAnnotationAssertionAxiom) axiom;
        OWLAnnotation a = annAxiom.getAnnotation();
        if (a.getProperty().getIRI().toString().equals(annotationProperty.getIRI().toString())) {
          OWLAnnotationValue av = a.getValue();
          String annotationValue = null;
          if (av.isLiteral()) {
            OWLLiteral lit = av.asLiteral().orNull();
            if (lit != null) {
              annotationValue = lit.getLiteral();
            }
          }
          if (annotationValue == null) {
            continue;
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
   * Given an IOHelper, an annotation property, and a literal value, return the OWLAnnotation
   * object.
   *
   * @param ioHelper IOHelper to retrieve prefix manager
   * @param annotationProperty OWLAnnotationProperty
   * @param value annotation value as string
   * @return OWLAnnotation object
   * @throws Exception on issue parsing to datatype
   */
  private static OWLAnnotation getLiteralAnnotation(
      IOHelper ioHelper, OWLAnnotationProperty annotationProperty, String value) throws Exception {
    OWLAnnotationValue annotationValue;
    Matcher datatypeMatcher = Pattern.compile("'(.*)'\\^\\^(.*)").matcher(value);
    Matcher langMatcher = Pattern.compile("'(.*)'@(.*)").matcher(value);
    if (datatypeMatcher.matches()) {
      // A datatype is given
      String content = datatypeMatcher.group(1);
      String dataTypeID = datatypeMatcher.group(2);
      if (dataTypeID.startsWith("<") && dataTypeID.endsWith(">")) {
        dataTypeID = dataTypeID.replace("<", "").replace(">", "");
      }
      IRI dataTypeIRI = ioHelper.createIRI(dataTypeID);
      if (dataTypeIRI == null) {
        throw new IllegalArgumentException(String.format(invalidIRIError, "datatype", dataTypeID));
      }
      OWLDatatype dt = df.getOWLDatatype(dataTypeIRI);
      if (dt.isBoolean()) {
        if (content.equalsIgnoreCase("true")) {
          annotationValue = df.getOWLLiteral(true);
        } else if (content.equalsIgnoreCase("false")) {
          annotationValue = df.getOWLLiteral(false);
        } else {
          throw new Exception(String.format(literalValueError, dataTypeID, "boolean"));
        }
      } else if (dt.isDouble()) {
        try {
          annotationValue = df.getOWLLiteral(Double.parseDouble(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "double"));
        }
      } else if (dt.isFloat()) {
        try {
          annotationValue = df.getOWLLiteral(Float.parseFloat(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "float"));
        }
      } else if (dt.isInteger()) {
        try {
          annotationValue = df.getOWLLiteral(Integer.parseInt(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "integer"));
        }
      } else if (dt.isString()) {
        annotationValue = df.getOWLLiteral(content);
      } else {
        annotationValue = df.getOWLLiteral(content, dt);
      }
    } else if (langMatcher.matches()) {
      // A language is given - always a string literal
      String content = langMatcher.group(1);
      String lang = langMatcher.group(2);
      annotationValue = df.getOWLLiteral(content, lang);
    } else {
      // If a datatype isn't provided, default to string literal
      annotationValue = df.getOWLLiteral(value.replace("'", ""));
    }
    return df.getOWLAnnotation(annotationProperty, annotationValue);
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
   * Given an ontology and a class expression, return the set of superclasses while removing any
   * circular subclass definitions. Warn on any circular subclasses.
   *
   * @param ontology OWLOntology to get super properties
   * @param property OWLObjectProperty to get super properties of
   * @return Set of OWLObjectPropertyExpressions that are super properties of property
   */
  private static Set<OWLObjectPropertyExpression> getSuperObjectProperties(
      OWLOntology ontology, OWLObjectProperty property) {
    Set<OWLObjectPropertyExpression> superProperties = new HashSet<>();

    // We might get stuck if a class is both subclass and equivalent
    // So compare the eqs to the superclasses and don't add a super if it's also an eq
    Collection<OWLObjectPropertyExpression> eqs =
        EntitySearcher.getEquivalentProperties(property, ontology);

    for (OWLObjectPropertyExpression expr : EntitySearcher.getSuperProperties(property, ontology)) {
      if (expr.isAnonymous()) {
        superProperties.add(expr);
        continue;
      }
      if (eqs.contains(expr)) {
        logger.warn(
            String.format(
                "Class '%s' has equivalent property and super property '%s'",
                property.getIRI(), expr));
        continue;
      }
      if (expr.asOWLObjectProperty().getIRI().toString().equals(property.getIRI().toString())) {
        logger.warn("Circular subproperty definition: " + property.getIRI());
        continue;
      }
      superProperties.add(expr);
    }
    return superProperties;
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
    for (OWLObjectPropertyExpression sc : superProperties) {
      if (objects.containsAll(sc.getSignature())) {
        Map<String, OWLObjectPropertyExpression> propPair = new HashMap<>();
        propPair.put(SUB, property);
        propPair.put(SUPER, sc);
        if (propPairs.add(propPair)) {
          // only recurse if the pair just added was not already present in the set.
          if (!sc.isAnonymous()) {
            spanGapsHelper(
                ontology,
                objects,
                propPairs,
                sc.asOWLObjectProperty(),
                getSuperObjectProperties(ontology, sc.asOWLObjectProperty()));
          }
        }
      } else if (!sc.isAnonymous()) {
        spanGapsHelper(
            ontology,
            objects,
            propPairs,
            property,
            getSuperObjectProperties(ontology, sc.asOWLObjectProperty()));
      }
    }
  }
}
