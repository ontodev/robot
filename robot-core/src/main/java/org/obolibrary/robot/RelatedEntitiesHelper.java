package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience methods to get related entities for a set of IRIs. Allowed relation options are: -
 * ancestors - descendants - disjoints - domains - equivalents - inverses - ranges - types
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RelatedEntitiesHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReduceOperation.class);

  /**
   * Returns a set of ancestors for a class. Named classes are added as OWLClasses, anonymous
   * classes are added as OWLClassExpressions.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get ancestors of
   * @param ancestors Set of OWLClassExpressions representing the ancestors
   * @param excludeAnonymous if true, do not include anonymous classes
   */
  public static void getAncestors(
      OWLOntology ontology, OWLClass cls, Set<OWLClassExpression> ancestors) {
    for (OWLClassExpression classExpression : EntitySearcher.getSuperClasses(cls, ontology)) {
      ancestors.add(classExpression);
      if (!classExpression.isAnonymous()) {
        OWLClass superClass = classExpression.asOWLClass();
        if (!superClass.isTopEntity()) {
          getAncestors(ontology, superClass, ancestors);
        }
      }
    }
  }

  /**
   * Returns a set of ancestors for a datatype property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get ancestors of
   * @param ancestors Set of OWLDataProperties representing the ancestors
   */
  public static void getAncestors(
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLDataProperty> ancestors) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(dataProperty, ontology)) {
      OWLDataProperty superProperty = propertyExpression.asOWLDataProperty();
      ancestors.add(superProperty);
      if (!superProperty.isTopEntity()) {
        getAncestors(ontology, superProperty, ancestors);
      }
    }
  }

  /**
   * Returns a set of ancestors for an object property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get ancestors of
   * @param ancestors Set of OWLObjectPropertyExpressions representing the ancestors
   * @param excludeAnonymous if true, do not include anonymous property expressions (e.g. inverses)
   */
  public static void getAncestors(
      OWLOntology ontology,
      OWLObjectProperty objectProperty,
      Set<OWLObjectPropertyExpression> ancestors) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(objectProperty, ontology)) {
      ancestors.add(propertyExpression);
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty superProperty = propertyExpression.asOWLObjectProperty();
        if (!superProperty.isTopEntity()) {
          getAncestors(ontology, superProperty, ancestors);
        }
      }
    }
  }

  /**
   * Given an ontology, an class, and an empty set, fills the set with descendants of the class.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get descendants of
   * @param descendants Set of OWLClasses representing the descendants
   * @return
   */
  public static void getDescendants(OWLOntology ontology, OWLClass cls, Set<OWLClass> descendants) {
    for (OWLClassExpression classExpression : EntitySearcher.getSubClasses(cls, ontology)) {
      OWLClass subClass = classExpression.asOWLClass();
      descendants.add(subClass);
      if (!subClass.isBottomEntity()) {
        getDescendants(ontology, subClass, descendants);
      }
    }
  }

  /**
   * Given an ontology, an annotation property, and an empty set, fills the set with descendants of
   * the annotation property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get descendants of
   * @param descendants Set of OWLDataProperties representing the descendants
   */
  public static void getDescendants(
      OWLOntology ontology,
      OWLAnnotationProperty annotationProperty,
      Set<OWLAnnotationProperty> descendants) {
    for (OWLAnnotationProperty subProperty :
        EntitySearcher.getSubProperties(annotationProperty, ontology)) {
      descendants.add(subProperty);
      if (!subProperty.isBottomEntity()) {
        getDescendants(ontology, subProperty, descendants);
      }
    }
  }

  /**
   * Given an ontology, a data property, and an empty set, fills the set with descendants of the
   * data property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get descendants of
   * @param descendants Set of OWLDataProperties representing the descendants
   */
  public static void getDescendants(
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLDataProperty> descendants) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getSubProperties(dataProperty, ontology)) {
      OWLDataProperty subProperty = propertyExpression.asOWLDataProperty();
      descendants.add(subProperty);
      if (!subProperty.isBottomEntity()) {
        getDescendants(ontology, subProperty, descendants);
      }
    }
  }

  /**
   * Given an ontology, a data property, and an empty set, fills the set with descendants of the
   * data property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get descendants of
   * @param descendants Set of OWLObjectProperties representing the descendants
   */
  public static void getDescendants(
      OWLOntology ontology, OWLObjectProperty objectProperty, Set<OWLObjectProperty> descendants) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getSubProperties(objectProperty, ontology)) {
      OWLObjectProperty subProperty = propertyExpression.asOWLObjectProperty();
      descendants.add(subProperty);
      if (!subProperty.isBottomEntity()) {
        getDescendants(ontology, subProperty, descendants);
      }
    }
  }

  /**
   * Given an ontology, a class, and whether anonymous classes should be included in the result or
   * not, return a set of class expressions representing disjoint classes of the given class.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get disjoints of
   * @return set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getDisjoints(OWLOntology ontology, OWLClass cls) {
    return (Set<OWLClassExpression>) EntitySearcher.getDisjointClasses(cls, ontology);
  }

  /**
   * Given an ontology and a data property, return a set of data properties that are disjoint from
   * the given property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get disjoints of
   * @return set of OWLDataProperties
   */
  public static Set<OWLDataProperty> getDisjoints(
      OWLOntology ontology, OWLDataProperty dataProperty) {
    Set<OWLDataProperty> disjoints = new HashSet<>();
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getDisjointProperties(dataProperty, ontology)) {
      disjoints.add(propertyExpression.asOWLDataProperty());
    }
    return disjoints;
  }

  /**
   * Given an ontology and an object property, return a set of object properties that are disjoint
   * from the given property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get disjoints of
   * @return set of OWLDataProperties
   */
  public static Set<OWLObjectPropertyExpression> getDisjoints(
      OWLOntology ontology, OWLObjectProperty objectProperty) {
    return (Set<OWLObjectPropertyExpression>)
        EntitySearcher.getDisjointProperties(objectProperty, ontology);
  }

  /**
   * Given an ontology, a data property, and whether to include anonymous classes or not, return the
   * set of class expressions representing the domains of the given property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get domains of
   * @param includeAnonymous if true, include anonymous classes
   * @return set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getDomains(
      OWLOntology ontology, OWLDataProperty dataProperty) {
    return (Set<OWLClassExpression>) EntitySearcher.getDomains(dataProperty, ontology);
  }

  /**
   * Given an ontology, an object property, and whether to include anonymous classes or not, return
   * the set of class expressions representing the domains of the given property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get domains of
   * @param includeAnonymous if true, include anonymous classes
   * @return set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getDomains(
      OWLOntology ontology, OWLObjectProperty objectProperty) {
    return (Set<OWLClassExpression>) EntitySearcher.getDomains(objectProperty, ontology);
  }

  /**
   * Given an ontology, a class, and an empty set, fill the set with class expressions representing
   * equivalent classes.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get equivalents of
   * @param equivalents Set of OWLObjects representing the equivalents
   */
  public static void getEquivalents(
      OWLOntology ontology, OWLClass cls, Set<OWLClassExpression> equivalents) {
    for (OWLClassExpression classExpression : EntitySearcher.getEquivalentClasses(cls, ontology)) {
      equivalents.add(classExpression);
      if (!classExpression.isAnonymous()) {
        OWLClass eqClass = classExpression.asOWLClass();
        if (!eqClass.isBottomEntity() && !eqClass.isTopEntity()) {
          getEquivalents(ontology, eqClass, equivalents);
        }
      }
    }
  }

  /**
   * Given an ontology, a data property, and an empty set, fill the set with equivalent data
   * properties.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get equivalents of
   * @param equivalents Set of OWLDataProperties representing the equivalents
   */
  public static void getEquivalents(
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLDataProperty> equivalents) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getEquivalentProperties(dataProperty, ontology)) {
      OWLDataProperty eqProperty = propertyExpression.asOWLDataProperty();
      equivalents.add(eqProperty);
      if (!eqProperty.isBottomEntity() && !eqProperty.isTopEntity()) {
        getEquivalents(ontology, eqProperty, equivalents);
      }
    }
  }

  /**
   * Given an ontology, an object property, and an empty set, fill the set with equivalent object
   * properties.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get equivalents of
   * @param equivalents Set of OWLObjectPropertyExpressions representing the equivalents
   */
  public static void getEquivalents(
      OWLOntology ontology,
      OWLObjectProperty objectProperty,
      Set<OWLObjectPropertyExpression> equivalents) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getEquivalentProperties(objectProperty, ontology)) {
      equivalents.add(propertyExpression);
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty eqProperty = propertyExpression.asOWLObjectProperty();
        if (!eqProperty.isBottomEntity() && !eqProperty.isTopEntity()) {
          getEquivalents(ontology, eqProperty, equivalents);
        }
      }
    }
  }

  /**
   * Given an ontology and an object property, return a set of inverse object properties.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get inverses of
   * @return Set of OWLObjectPropertyExpressions representing the inverses
   */
  public static Set<OWLObjectPropertyExpression> getInverses(
      OWLOntology ontology, OWLObjectProperty objectProperty) {
    return (Set<OWLObjectPropertyExpression>) EntitySearcher.getInverses(objectProperty, ontology);
  }

  /**
   * Given an ontology and a data property, return a set of ranges.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get ranges of
   * @return Set of OWLDataRanges
   */
  public static Set<OWLDataRange> getRanges(OWLOntology ontology, OWLDataProperty dataProperty) {
    return (Set<OWLDataRange>) EntitySearcher.getRanges(dataProperty, ontology);
  }

  /**
   * Given an ontology and an object property, return a set of ranges.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get ranges of
   * @return Set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getRanges(
      OWLOntology ontology, OWLObjectProperty objectProperty) {
    return (Set<OWLClassExpression>) EntitySearcher.getRanges(objectProperty, ontology);
  }

  /**
   * Given an ontology and an individual, return a set of types.
   *
   * @param ontology OWLOntology to retrieve from
   * @param individual OWLNamedIndividual to get types of
   * @return Set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getTypes(
      OWLOntology ontology, OWLNamedIndividual individual) {
    return (Set<OWLClassExpression>) EntitySearcher.getTypes(individual, ontology);
  }
}
