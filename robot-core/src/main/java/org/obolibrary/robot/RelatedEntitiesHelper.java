package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLEntity;
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
  private static final Logger logger = LoggerFactory.getLogger(RelatedEntitiesHelper.class);

  /**
   * Given an ontology, a set of entities, and a set of relation types, return entities related to
   * the given set by the relation types.
   *
   * @param ontology the ontology to search
   * @param entities the entities to start with
   * @param relationTypes the relation types to search on
   * @return set of related entities
   */
  public static Set<OWLEntity> getRelated(
      OWLOntology ontology, Set<OWLEntity> entities, Set<RelationType> relationTypes) {
    Set<OWLEntity> relatedEntities = new HashSet<>();
    for (OWLEntity entity : entities) {
      relatedEntities.addAll(getRelated(ontology, entity, relationTypes));
    }
    return relatedEntities;
  }

  /**
   * Given an ontology, an entity, and a set of relation types, return entities related to the given
   * entity by the relation types.
   *
   * @param ontology the ontology to search
   * @param entity the entity to start with
   * @param relationTypes the relation types to search on
   * @return set of related entities
   */
  public static Set<OWLEntity> getRelated(
      OWLOntology ontology, OWLEntity entity, Set<RelationType> relationTypes) {
    Set<OWLEntity> relatedEntities = new HashSet<>();
    for (RelationType rt : relationTypes) {
      relatedEntities.addAll(getRelated(ontology, entity, rt));
    }
    return relatedEntities;
  }

  /**
   * Given an ontology, an entity, and a relation types, return entities related to the given entity
   * by the relation type.
   *
   * @param ontology the ontology to search
   * @param entity the entity to start with
   * @param relationType the relation type to search on
   * @return set of related entities
   */
  public static Set<OWLEntity> getRelated(
      OWLOntology ontology, OWLEntity entity, RelationType relationType) {
    if (relationType == null) {
      return OntologyHelper.getEntities(ontology);
    }
    switch (relationType) {
      case SELF:
        return Sets.newHashSet(entity);
      case CHILDREN:
        return getChildren(ontology, entity);
      case PARENTS:
        return getParents(ontology, entity);
      case DESCENDANTS:
        return getDescendants(ontology, entity);
      case ANCESTORS:
        return getAncestors(ontology, entity);
      case EQUIVALENTS:
        // TODO
      case TYPES:
        return getTypes(ontology, entity);
      case CLASSES:
        return getEntitiesOfType(ontology, EntityType.CLASS);
      case PROPERTIES:
        Set<OWLEntity> relatedEntities = new HashSet<>();
        relatedEntities.addAll(getEntitiesOfType(ontology, EntityType.ANNOTATION_PROPERTY));
        relatedEntities.addAll(getEntitiesOfType(ontology, EntityType.DATA_PROPERTY));
        relatedEntities.addAll(getEntitiesOfType(ontology, EntityType.OBJECT_PROPERTY));
        return relatedEntities;
      case INDIVIDUALS:
        return getEntitiesOfType(ontology, EntityType.NAMED_INDIVIDUAL);
      case OBJECT_PROPERTIES:
        return getEntitiesOfType(ontology, EntityType.OBJECT_PROPERTY);
      case ANNOTATION_PROPERTIES:
        return getEntitiesOfType(ontology, EntityType.ANNOTATION_PROPERTY);
      case DATA_PROPERTIES:
        return getEntitiesOfType(ontology, EntityType.DATA_PROPERTY);
    }
    return Sets.newHashSet();
  }

  /**
   * Given an ontology and an entity, return a set of OWLEntities that are all ancestors of that
   * entity.
   *
   * @param ontology OWLOntology to retrieve from
   * @param entity OWLEntity to get ancestors of
   * @return Set of OWLEntities
   */
  public static Set<OWLEntity> getAncestors(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> ancestors = new HashSet<>();
    if (entity.isOWLClass()) {
      getAncestors(ontology, entity.asOWLClass(), ancestors);
    } else if (entity.isOWLAnnotationProperty()) {
      getAncestors(ontology, entity.asOWLAnnotationProperty(), ancestors);
    } else if (entity.isOWLDataProperty()) {
      getAncestors(ontology, entity.asOWLDataProperty(), ancestors);
    } else if (entity.isOWLObjectProperty()) {
      getAncestors(ontology, entity.asOWLObjectProperty(), ancestors);
    }
    return ancestors;
  }

  /**
   * Returns a set of ancestors for a class. Named classes are added as OWLClasses, anonymous
   * classes are added as OWLClassExpressions.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get ancestors of
   * @param ancestors Set of OWLEntities representing the ancestors
   */
  private static void getAncestors(OWLOntology ontology, OWLClass cls, Set<OWLEntity> ancestors) {
    for (OWLClassExpression classExpression : EntitySearcher.getSuperClasses(cls, ontology)) {
      if (!classExpression.isAnonymous()) {
        OWLClass superClass = classExpression.asOWLClass();
        ancestors.add(superClass);
        if (!superClass.isTopEntity()) {
          getAncestors(ontology, superClass, ancestors);
        }
      }
    }
  }

  /**
   * Returns a set of ancestors for an annotation property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param annotationProperty OWLAnnotationProperty to get ancestors of
   * @param ancestors Set of OWLEntities representing the ancestors
   */
  private static void getAncestors(
      OWLOntology ontology, OWLAnnotationProperty annotationProperty, Set<OWLEntity> ancestors) {
    for (OWLAnnotationProperty superProperty :
        EntitySearcher.getSuperProperties(annotationProperty, ontology)) {
      ancestors.add(superProperty);
      if (!superProperty.isTopEntity()) {
        getAncestors(ontology, superProperty, ancestors);
      }
    }
  }

  /**
   * Returns a set of ancestors for a datatype property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param dataProperty OWLDataProperty to get ancestors of
   * @param ancestors Set of OWLEntities representing the ancestors
   */
  private static void getAncestors(
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLEntity> ancestors) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(dataProperty, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLDataProperty superProperty = propertyExpression.asOWLDataProperty();
        ancestors.add(superProperty);
        if (!superProperty.isTopEntity()) {
          getAncestors(ontology, superProperty, ancestors);
        }
      }
    }
  }

  /**
   * Returns a set of ancestors for an object property.
   *
   * @param ontology OWLOntology to retrieve from
   * @param objectProperty OWLObjectProperty to get ancestors of
   * @param ancestors Set of OWLEntities representing the ancestors
   */
  private static void getAncestors(
      OWLOntology ontology, OWLObjectProperty objectProperty, Set<OWLEntity> ancestors) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getSuperProperties(objectProperty, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty superProperty = propertyExpression.asOWLObjectProperty();
        ancestors.add(superProperty);
        if (!superProperty.isTopEntity()) {
          getAncestors(ontology, superProperty, ancestors);
        }
      }
    }
  }

  /**
   * Given an ontology and a set of annotations, return entities that are annotated with at least
   * one of the given annotations.
   *
   * @param ontology the ontology to search
   * @param annotations the set of annotations to search on
   * @return set of entities with annotations
   */
  public static Set<OWLEntity> getAnnotated(OWLOntology ontology, Set<OWLAnnotation> annotations) {
    Set<OWLEntity> annotatedEntities = new HashSet<>();
    for (OWLEntity entity : OntologyHelper.getEntities(ontology)) {
      for (OWLAnnotationAssertionAxiom axiom :
          ontology.getAnnotationAssertionAxioms(entity.getIRI())) {
        if (annotations.contains(axiom.getAnnotation())) {
          annotatedEntities.add(entity);
        }
      }
    }
    return annotatedEntities;
  }

  /**
   * Given an ontology and an entity, get the direct children of the entity.
   *
   * @param ontology the ontology to search
   * @param entity the entity to start with
   * @return set of children to the entity
   */
  public static Set<OWLEntity> getChildren(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> children = new HashSet<>();
    if (entity.isOWLClass()) {
      EntitySearcher.getSubClasses(entity.asOWLClass(), ontology)
          .forEach(c -> children.add(c.asOWLClass()));
    } else if (entity.isOWLAnnotationProperty()) {
      children.addAll(EntitySearcher.getSubProperties(entity.asOWLAnnotationProperty(), ontology));
    } else if (entity.isOWLDataProperty()) {
      EntitySearcher.getSubProperties(entity.asOWLDataProperty(), ontology)
          .forEach(p -> children.add(p.asOWLDataProperty()));
    } else if (entity.isOWLObjectProperty()) {
      EntitySearcher.getSubProperties(entity.asOWLObjectProperty(), ontology)
          .forEach(p -> children.add(p.asOWLObjectProperty()));
    }
    return children;
  }

  /**
   * Given an ontology and a set of entities from the ontology, return the complement set of
   * entities from the ontology.
   *
   * @param ontology the ontology to search
   * @param entities the entities to start with
   * @return set of complement entities
   */
  public static Set<OWLEntity> getComplements(OWLOntology ontology, Set<OWLEntity> entities) {
    Set<OWLEntity> complements = new HashSet<>();
    for (OWLEntity entity : OntologyHelper.getEntities(ontology)) {
      if (!entities.contains(entity)) {
        complements.add(entity);
      }
    }
    return complements;
  }

  /**
   * Given an ontology and an entity, return a set of OWLEntities that are all descendants of that
   * entity.
   *
   * @param ontology OWLOntology to retrieve from
   * @param entity OWLEntity to get descendants of
   * @return set of OWLEntities
   */
  public static Set<OWLEntity> getDescendants(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> descendants = new HashSet<>();
    if (entity.isOWLClass()) {
      getDescendants(ontology, entity.asOWLClass(), descendants);
    } else if (entity.isOWLAnnotationProperty()) {
      getDescendants(ontology, entity.asOWLAnnotationProperty(), descendants);
    } else if (entity.isOWLDataProperty()) {
      getDescendants(ontology, entity.asOWLDataProperty(), descendants);
    } else if (entity.isOWLObjectProperty()) {
      getDescendants(ontology, entity.asOWLObjectProperty(), descendants);
    }
    return descendants;
  }

  /**
   * Given an ontology, an class, and an empty set, fills the set with descendants of the class.
   *
   * @param ontology OWLOntology to retrieve from
   * @param cls OWLClass to get descendants of
   * @param descendants Set of OWLEntities representing the descendants
   * @return
   */
  private static void getDescendants(
      OWLOntology ontology, OWLClass cls, Set<OWLEntity> descendants) {
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
   * @param descendants Set of OWLEntities representing the descendants
   */
  private static void getDescendants(
      OWLOntology ontology, OWLAnnotationProperty annotationProperty, Set<OWLEntity> descendants) {
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
   * @param descendants Set of OWLEntities representing the descendants
   */
  private static void getDescendants(
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLEntity> descendants) {
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
   * @param descendants Set of OWLEntities representing the descendants
   */
  private static void getDescendants(
      OWLOntology ontology, OWLObjectProperty objectProperty, Set<OWLEntity> descendants) {
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
   * Given an ontology and an entity type, return only entities of that type.
   *
   * @param ontology the ontology to search
   * @param entityType the entity type to keep
   * @return set of entities of the entity type
   */
  private static Set<OWLEntity> getEntitiesOfType(OWLOntology ontology, EntityType<?> entityType) {
    Set<OWLEntity> filteredEntities = new HashSet<>();
    for (OWLEntity entity : OntologyHelper.getEntities(ontology)) {
      if (entity.isType(entityType)) {
        filteredEntities.add(entity);
      }
    }
    return filteredEntities;
  }

  /**
   * Given an ontology and an entity, return all equivalent entities.
   *
   * @param ontology the ontology to search
   * @param entity the entity to start with
   * @return set of equivalent entities
   */
  public static Set<OWLEntity> getEquivalents(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> equivalents = new HashSet<>();
    if (entity.isOWLClass()) {
      getEquivalents(ontology, entity.asOWLClass(), equivalents);
    } else if (entity.isOWLDataProperty()) {
      getEquivalents(ontology, entity.asOWLDataProperty(), equivalents);
    } else if (entity.isOWLObjectProperty()) {
      getEquivalents(ontology, entity.asOWLObjectProperty(), equivalents);
    }
    return equivalents;
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
      OWLOntology ontology, OWLClass cls, Set<OWLEntity> equivalents) {
    for (OWLClassExpression classExpression : EntitySearcher.getEquivalentClasses(cls, ontology)) {
      if (!classExpression.isAnonymous()) {
        OWLClass eqClass = classExpression.asOWLClass();
        if (eqClass != cls) {
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
      OWLOntology ontology, OWLDataProperty dataProperty, Set<OWLEntity> equivalents) {
    for (OWLDataPropertyExpression propertyExpression :
        EntitySearcher.getEquivalentProperties(dataProperty, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLDataProperty eqProperty = propertyExpression.asOWLDataProperty();
        equivalents.add(eqProperty);
        if (eqProperty != dataProperty) {
          getEquivalents(ontology, eqProperty, equivalents);
        }
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
      OWLOntology ontology, OWLObjectProperty objectProperty, Set<OWLEntity> equivalents) {
    for (OWLObjectPropertyExpression propertyExpression :
        EntitySearcher.getEquivalentProperties(objectProperty, ontology)) {
      if (!propertyExpression.isAnonymous()) {
        OWLObjectProperty eqProperty = propertyExpression.asOWLObjectProperty();
        if (eqProperty != objectProperty) {
          getEquivalents(ontology, eqProperty, equivalents);
        }
      }
    }
  }

  /**
   * Given an ontology and an entity, return the set of direct parent entities.
   *
   * @param ontology the ontology to search
   * @param entity the entity to start with
   * @return set of parent entities
   */
  public static Set<OWLEntity> getParents(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> parents = new HashSet<>();
    if (entity.isOWLClass()) {
      for (OWLClassExpression e : EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (!e.isAnonymous()) {
          parents.add(e.asOWLClass());
        }
      }
    } else if (entity.isOWLAnnotationProperty()) {
      parents.addAll(EntitySearcher.getSuperProperties(entity.asOWLAnnotationProperty(), ontology));
    } else if (entity.isOWLDataProperty()) {
      for (OWLDataPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLDataProperty(), ontology)) {
        if (!e.isAnonymous()) {
          parents.add(e.asOWLDataProperty());
        }
      }
    } else if (entity.isOWLObjectProperty()) {
      for (OWLObjectPropertyExpression e :
          EntitySearcher.getSuperProperties(entity.asOWLObjectProperty(), ontology)) {
        if (!e.isAnonymous()) {
          parents.add(e.asOWLObjectProperty());
        }
      }
    }
    return parents;
  }

  /**
   * Given an ontology and an individual entity, return a set of types.
   *
   * @param ontology the ontology to search
   * @param entity the individual to get types of
   * @return set of types, or empty if the entity is not an individual
   */
  public static Set<OWLEntity> getTypes(OWLOntology ontology, OWLEntity entity) {
    Set<OWLEntity> types = new HashSet<>();
    if (entity.isOWLNamedIndividual()) {
      for (OWLClassExpression e :
          EntitySearcher.getTypes(entity.asOWLNamedIndividual(), ontology)) {
        if (!e.isAnonymous()) {
          types.add(e.asOWLClass());
        }
      }
    }
    return types;
  }
}
