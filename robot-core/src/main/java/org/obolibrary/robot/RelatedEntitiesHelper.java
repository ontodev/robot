package org.obolibrary.robot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Convenience methods to get related entities for a set of IRIs.
 * 
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RelatedEntitiesHelper {
	
	/**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(ReduceOperation.class);
    
    /**
     * Returns a set of related entities as OWLObjects for an IRI.
     * Handles one IRI, one relation option.
     *  
     * @param  iri IRI to retrieve related entities of
     * @param  ontology OWLOntology to retrieve entities from
     * @param  relationOption String option specifying what types of related
     *         entities to retrieve
     * @return Set of OWLObjects
     */
    public static Set<OWLObject> getRelatedEntities(OWLOntology ontology,
    		IRI iri, String relationOption) {
    	Set<IRI> IRIs = Sets.newHashSet(iri);
    	return getRelatedEntities(ontology, IRIs,
    			Arrays.asList(relationOption));
    }
    
    /**
     * Returns a set of related entities as OWLObjects for an IRI.
     * Handles multiple IRIs, one relation options.
     *  
     * @param  IRIs Set of IRIs to retrieve related entities of
     * @param  ontology OWLOntology to retrieve entities from
     * @param  relationOption String option specifying what types of related
     *         entities to retrieve
     * @return Set of OWLObjects
     */
    public static Set<OWLObject> getRelatedEntities(OWLOntology ontology,
    		Set<IRI> IRIs, String relationOption) {
    	return getRelatedEntities(ontology, IRIs,
    			Arrays.asList(relationOption));
    }
    
    /**
     * Returns a set of related entities as OWLObjects for an IRI.
     * Handles one IRI, multiple relation options.
     *  
     * @param  iri IRI to retrieve related entities of
     * @param  ontology OWLOntology to retrieve entities from
     * @param  relationOptions List of string options specifying what types of
     *         related entities to retrieve
     * @return Set of OWLObjects
     */
    public static Set<OWLObject> getRelatedEntities(OWLOntology ontology, 
    		IRI iri, List<String> relationOptions) {
    	Set<IRI> IRIs = Sets.newHashSet(iri);
    	return getRelatedEntities(ontology, IRIs, relationOptions);
    }

    /**
     * Returns a set of related entities as OWLObjects for a given set of IRIs.
     * 
     * @param  IRIs Set of IRIs to retrieve related entities of
     * @param  ontology OWLOntology to retrieve entities from
     * @param  relationOptions List of string options specifying what types of
     *         related entities to retrieve
     * @return Set of OWLObjects
     */
	public static Set<OWLObject> getRelatedEntities(OWLOntology ontology,
			Set<IRI> IRIs, List<String> relationOptions) {
		Set<OWLObject> relatedEntities = new HashSet<>();
		Map<EntityType<?>, Set<OWLEntity>> entitiesByType =
				sortTypes(ontology, IRIs);
		for (String opt : relationOptions) {
			if ("ancestors".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getAncestors(ontology, entitiesByType));
			} else if ("descendants".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDescendants(ontology, entitiesByType));
			} else if ("equivalents".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getEquivalents(ontology, entitiesByType));
			} else if ("disjoints".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDisjoints(ontology, entitiesByType));
			} else if ("domains".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDomains(ontology, entitiesByType));
			} else if ("ranges".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getRanges(ontology, entitiesByType));
			} else if ("inverses".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getInverses(ontology, entitiesByType));
			} else if ("types".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getTypes(ontology, entitiesByType));
			// TODO: property assertions
			} else {
				// TODO: Should this be an exception?
		    	logger.warn("Invalid relation option: " + opt);
			}
		}
		return relatedEntities;
	}

	/**
	 * Returns a set of ancestors as OWLObjects for a set of entities
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getAncestors(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> ancestors = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getAncestors(ontology, cls.asOWLClass(), ancestors);
		}
		for (OWLEntity dp : dataProperties) {
			getAncestors(ontology, dp.asOWLDataProperty(), ancestors);
		}
		for (OWLEntity op : objectProperties) {
			getAncestors(ontology, op.asOWLObjectProperty(), ancestors);
		}
		return ancestors;
	}
	
	/**
	 * Returns a set of descendants as OWLObjects for a set of entities
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getDescendants(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> descendants = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getDescendants(ontology, cls.asOWLClass(), descendants);
		}
		for (OWLEntity dp : dataProperties) {
			getDescendants(ontology, dp.asOWLDataProperty(), descendants);
		}
		for (OWLEntity op : objectProperties) {
			getDescendants(ontology, op.asOWLObjectProperty(), descendants);
		}
		return descendants;
	}
	
	/**
	 * Returns a set of disjoints as OWLObjects for a set of entities
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getDisjoints(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> disjoints = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getDisjoints(ontology, cls.asOWLClass(), disjoints);
		}
		for (OWLEntity dp : dataProperties) {
			getDisjoints(ontology, dp.asOWLDataProperty(), disjoints);
		}
		for (OWLEntity op : objectProperties) {
			getDisjoints(ontology, op.asOWLObjectProperty(), disjoints);
		}
		return disjoints;
	}
	
	/**
	 * Returns a set of domains as OWLObjects for a set of entities
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getDomains(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> domains = new HashSet<>();
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity dp : dataProperties) {
			getDomains(ontology, dp.asOWLDataProperty(), domains);
		}
		for (OWLEntity op : objectProperties) {
			getDomains(ontology, op.asOWLObjectProperty(), domains);
		}
		return domains;
	}
	
	/**
	 * Returns a set of equivalents as OWLObjects for a set of entities
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getEquivalents(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> equivalents = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getEquivalents(ontology, cls.asOWLClass(), equivalents);
		}
		for (OWLEntity dp : dataProperties) {
			getEquivalents(ontology, dp.asOWLDataProperty(), equivalents);
		}
		for (OWLEntity op : objectProperties) {
			getEquivalents(ontology, op.asOWLObjectProperty(), equivalents);
		}
		return equivalents;
	}
	
	/**
	 * Returns a set of inverses as OWLObjects for a set of entities.
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getInverses(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> inverses = new HashSet<>();
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity op : objectProperties) {
			getInverses(ontology, op.asOWLObjectProperty(), inverses);
		}
		return inverses;
	}

	/**
	 * Returns a set of ranges as OWLObjects for a set of entities.
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getRanges(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> ranges = new HashSet<>();
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity dp : dataProperties) {
			getRanges(ontology, dp.asOWLDataProperty(), ranges);
		}
		for (OWLEntity op : objectProperties) {
			getRanges(ontology, op.asOWLObjectProperty(), ranges);
		}
		return ranges;
	}
	
	/**
	 * Returns a set of types as OWLObjects for a set of entities.
	 * 
	 * @param  entitiesByType Map of EntityTypes (key) and sets of OWLEntities
	 *         of that type (val)
	 * @param  ontology OWLOntology to retrieve from
	 * @return Set of OWLObjects
	 */
	private static Set<OWLObject> getTypes(OWLOntology ontology,
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType) {
		Set<OWLObject> types = new HashSet<>();
		Set<OWLEntity> individual =
				entitiesByType.get(EntityType.NAMED_INDIVIDUAL);
		for (OWLEntity i : individual) {
			getTypes(ontology, i.asOWLNamedIndividual(), types);
		}
		return types;
	}
	
	/**
	 * Returns a set of ancestors for a class. Named classes are added as
	 * OWLClasses, anonymous classes are added as OWLClassExpressions.
	 * 
	 * @param cls OWLClass to get ancestors of
	 * @param ontology OWLOntology to retrieve from
	 * @param ancestors Set of OWLObjects representing the ancestors
	 */
	private static void getAncestors(OWLOntology ontology, OWLClass cls,
			Set<OWLObject> ancestors) {
		Set<OWLSubClassOfAxiom> axioms =
				ontology.getSubClassAxiomsForSubClass(cls);
		if (!axioms.isEmpty()) {
			for (OWLSubClassOfAxiom ax : axioms) {
				if (!ax.getSuperClass().isAnonymous()) {
					for (OWLClass c :
						ax.getSuperClass().getClassesInSignature()) {
						ancestors.add(c);
						getAncestors(ontology, c, ancestors);
					}
				} else {
					logger.debug("Anonymous Ancestor: "
							+ ax.getSuperClass().toString());
					ancestors.add(ax.getSuperClass());
				}
			}
		}
	}
	
	/**
	 * Returns a set of ancestors for a datatype property.
	 * 
	 * @param dataProperty OWLDataProperty to get ancestors of
	 * @param ontology OWLOntology to retrieve from
	 * @param ancestors Set of OWLObjects representing the ancestors
	 */
	private static void getAncestors(OWLOntology ontology, 
			OWLDataProperty dataProperty, Set<OWLObject> ancestors) {
		Set<OWLSubDataPropertyOfAxiom> axioms =
				ontology.getDataSubPropertyAxiomsForSubProperty(dataProperty);
		if (!axioms.isEmpty()) {
			for (OWLSubDataPropertyOfAxiom ax : axioms) {
				for (OWLDataProperty dp :
					ax.getSuperProperty().getDataPropertiesInSignature()) {
					ancestors.add(dp);
					getAncestors(ontology, dp, ancestors);
					
				}
			}
		}
	}
	
	/**
	 * Returns a set of ancestors for an object property.
	 * 
	 * @param objectProperty OWLObjectProperty to get ancestors of
	 * @param ontology OWLOntology to retrieve from
	 * @param ancestors Set of OWLObjects representing the ancestors
	 */
	private static void getAncestors(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> ancestors) {
		Set<OWLSubObjectPropertyOfAxiom> axioms =
				ontology.getObjectSubPropertyAxiomsForSubProperty(
						objectProperty);
		if (!axioms.isEmpty()) {
			for (OWLSubObjectPropertyOfAxiom ax : axioms) {
				for (OWLObjectProperty op :
					ax.getSuperProperty().getObjectPropertiesInSignature()) {
					ancestors.add(op);
					getAncestors(ontology, op, ancestors);
					
				}
			}
		}
	}
	
	/**
	 * Returns a set of descendants for a class. Named classes are added as
	 * OWLClasses, anonymous classes are added as OWLClassExpressions.
	 * 
	 * @param cls OWLClass to get descendants of
	 * @param ontology OWLOntology to retrieve from
	 * @param descendants Set of OWLObjects representing the descendants
	 */
	private static void getDescendants(OWLOntology ontology, OWLClass cls,
			Set<OWLObject> descendants) {
		Set<OWLSubClassOfAxiom> axioms =
				ontology.getSubClassAxiomsForSuperClass(cls);
		if (!axioms.isEmpty()) {
	    	for (OWLSubClassOfAxiom ax : axioms) {
	    		if (!ax.getSubClass().isAnonymous()) {
		    		for (OWLClass c :
		    			ax.getSubClass().getClassesInSignature()) {
		    			descendants.add(c);
		    			getDescendants(ontology, c, descendants);
		    		}
	    		} else {
	    			logger.debug("Anonymous Descendant: "
	    					+ ax.getSuperClass().toString());
	    			descendants.add(ax.getSubClass());
	    		}
	    	}
		}
	}
	
	/**
	 * Returns a set of descendantss for a datatype property.
	 * 
	 * @param dataProperty OWLDataProperty to get descendants of
	 * @param ontology OWLOntology to retrieve from
	 * @param descendants Set of OWLObjects representing the descendants
	 */
	private static void getDescendants(OWLOntology ontology,
			OWLDataProperty dataProperty, Set<OWLObject> descendants) {
		Set<OWLSubDataPropertyOfAxiom> axioms =
				ontology.getDataSubPropertyAxiomsForSuperProperty(dataProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLSubDataPropertyOfAxiom ax : axioms) {
	    		for (OWLDataProperty dp :
	    			ax.getSubProperty().getDataPropertiesInSignature()) {
	    			descendants.add(dp);
	    			getDescendants(ontology, dp, descendants);
	    		}
	    	}
		}
	}
	
	/**
	 * Returns a set of descendants for an object property.
	 * 
	 * @param objectProperty OWLObjectProperty to get descendants of
	 * @param ontology OWLOntology to retrieve from
	 * @param descendants Set of OWLObjects representing the descendants
	 */
	private static void getDescendants(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> descendants) {
		Set<OWLSubObjectPropertyOfAxiom> axioms =
    			ontology.getObjectSubPropertyAxiomsForSuperProperty(
    					objectProperty);
    	if (!axioms.isEmpty()) {
	    	for (OWLSubObjectPropertyOfAxiom ax : axioms) {
	    		for (OWLObjectProperty op :
	    			ax.getSubProperty().getObjectPropertiesInSignature()) {
	    			descendants.add(op);
	    			getDescendants(ontology, op, descendants);
	    		}
	    	}
    	}
	}
	
	/**
	 * Returns a set of disjoints for a class. Named classes are added as
	 * OWLClasses, anonymous classes are added as OWLClassExpressions.
	 * 
	 * @param cls OWLClass to get disjoints of
	 * @param ontology OWLOntology to retrieve from
	 * @param disjoints Set of OWLObjects representing the disjoints
	 */
	private static void getDisjoints(OWLOntology ontology, OWLClass cls,
			Set<OWLObject> disjoints) {
		Set<OWLDisjointClassesAxiom> axioms =
				ontology.getDisjointClassesAxioms(cls);
		if (!axioms.isEmpty()) {
	    	for (OWLDisjointClassesAxiom ax : axioms) {
	    		for (OWLClassExpression ex : ax.getClassExpressions()) {
	    			if (!ex.isAnonymous()) {
	    				disjoints.addAll(ex.getClassesInSignature());
	    			} else {
	    				logger.debug("Anonymous Disjoint: " + ex.toString());
	    				disjoints.add(ex);
	    			}
	    		}
	    	}
		}
		disjoints.remove(cls);
	}
	
	/**
	 * Returns a set of disjoints for a datatype property.
	 * 
	 * @param dataProperty OWLDataProperty to get disjoints of
	 * @param ontology OWLOntology to retrieve from
	 * @param disjoints Set of OWLObjects representing the disjoints
	 */
	private static void getDisjoints(OWLOntology ontology,
			OWLDataProperty dataProperty, Set<OWLObject> disjoints) {
		Set<OWLDisjointDataPropertiesAxiom> axioms =
				ontology.getDisjointDataPropertiesAxioms(dataProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLDisjointDataPropertiesAxiom ax : axioms) {
	    		disjoints.addAll(ax.getDataPropertiesInSignature());
	    	}
		}
		disjoints.remove(dataProperty);
	}
	
	/**
	 * Returns a set of disjoints for an object property.
	 * 
	 * @param objectProperty OWLObjectProperty to get disjoints of
	 * @param ontology OWLOntology to retrieve from
	 * @param disjoints Set of OWLObjects representing the disjoints
	 */
	private static void getDisjoints(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> disjoints) {
		Set<OWLDisjointObjectPropertiesAxiom> axioms =
				ontology.getDisjointObjectPropertiesAxioms(objectProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLDisjointObjectPropertiesAxiom ax : axioms) {
	    		disjoints.addAll(ax.getObjectPropertiesInSignature());
	    	}
		}
		disjoints.remove(objectProperty);
	}
	
	/**
	 * Returns a set of domains for a datatype property.
	 * 
	 * @param dataProperty OWLDataProperty to get domains of
	 * @param ontology OWLOntology to retrieve from
	 * @param domains Set of OWLObjects representing the domains
	 */
	private static void getDomains(OWLOntology ontology, 
			OWLDataProperty dataProperty, Set<OWLObject> domains) {
		Set<OWLDataPropertyDomainAxiom> axioms =
				ontology.getDataPropertyDomainAxioms(dataProperty);
		if (!axioms.isEmpty()) {
			for (OWLDataPropertyDomainAxiom ax : axioms) {
				// TODO: Support for anon domains is probably unnecessary
				domains.addAll(ax.getClassesInSignature());
			}
		}
	}
	
	/**
	 * Returns a set of domains for an object property.
	 * 
	 * @param objectProperty OWLObjectProperty to get domains of
	 * @param ontology OWLOntology to retrieve from
	 * @param domains Set of OWLObjects representing the domains
	 */
	private static void getDomains(OWLOntology ontology, 
			OWLObjectProperty objectProperty, Set<OWLObject> domains) {
		Set<OWLObjectPropertyDomainAxiom> axioms =
				ontology.getObjectPropertyDomainAxioms(objectProperty);
		if (!axioms.isEmpty()) {
			for (OWLObjectPropertyDomainAxiom ax : axioms) {
				// TODO: Support for anon domains is probably unnecessary
				domains.addAll(ax.getClassesInSignature());
			}
		}
	}
	
	/**
	 * Returns a set of equivalents for a class. Named classes are added as
	 * OWLClasses, anonymous classes are added as OWLClassExpressions.
	 * 
	 * @param cls OWLClass to get equivalents of
	 * @param ontology OWLOntology to retrieve from
	 * @param equivalents Set of OWLObjects representing the equivalents
	 */
	private static void getEquivalents(OWLOntology ontology, OWLClass cls,
			Set<OWLObject> equivalents) {
		Set<OWLEquivalentClassesAxiom> axioms =
				ontology.getEquivalentClassesAxioms(cls);
		if (!axioms.isEmpty()) {
	    	for (OWLEquivalentClassesAxiom ax : axioms) {
	    		for (OWLClassExpression ex : ax.getClassExpressions()) {
	    			if (!ex.isAnonymous()) {
	    				equivalents.addAll(ex.getClassesInSignature());
	    			} else {
	    				logger.debug("Anonymous Equivalent: " + ex.toString());
	    				equivalents.add(ex);
	    			}
	    		}
	    	}
		}
		equivalents.remove(cls);
	}
	
	/**
	 * Returns a set of equivalents for a datatype property. Inverse
	 * equivalents are added as OWLEquivalentDataPropertiesAxiom, otherwise
	 * added as OWLDataProperty.
	 * 
	 * @param dataProperty OWLDataProperty to get equivalents of
	 * @param ontology OWLOntology to retrieve from
	 * @param equivalents Set of OWLObjects representing the equivalents
	 */
	private static void getEquivalents(OWLOntology ontology,
			OWLDataProperty dataProperty, Set<OWLObject> equivalents) {
		Set<OWLEquivalentDataPropertiesAxiom> axioms =
				ontology.getEquivalentDataPropertiesAxioms(dataProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLEquivalentDataPropertiesAxiom ax : axioms) {
	    		if (!ax.toString().contains("InverseOf")) {
	    			equivalents.addAll(ax.getDataPropertiesInSignature());
	    		} else {
	    			logger.debug("Inverse Equivalent: " + ax.toString());
	    			equivalents.add(ax);
	    		}
	    	}
		}
		equivalents.remove(dataProperty);
	}
	
	/**
	 * Returns a set of equivalents for an object property. Inverse
	 * equivalents are added as OWLEquivalentObjectPropertiesAxiom, otherwise
	 * added as OWLObjectProperty.
	 * 
	 * @param objectProperty OWLObjectProperty to get equivalents of
	 * @param ontology OWLOntology to retrieve from
	 * @param equivalents Set of OWLObjects representing the equivalents
	 */
	private static void getEquivalents(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> equivalents) {
		Set<OWLEquivalentObjectPropertiesAxiom> axioms =
				ontology.getEquivalentObjectPropertiesAxioms(objectProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLEquivalentObjectPropertiesAxiom ax : axioms) {
	    		if (!ax.toString().contains("InverseOf")) {
	    			equivalents.addAll(ax.getObjectPropertiesInSignature());
	    		} else {
	    			logger.debug("Inverse Equivalent: " + ax.toString());
	    			equivalents.add(ax);
	    		}
	    	}
		}
		equivalents.remove(objectProperty);
	}
	
	/**
	 * Returns a set of inverse properties for an object property.
	 * 
	 * @param objectProperty OWLObjectProperty to get inverses of
	 * @param ontology OWLOntology to retrieve from
	 * @param inverses Set of OWLObjects representing the inverses
	 */
	private static void getInverses(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> inverses) {
		Set<OWLInverseObjectPropertiesAxiom> axioms =
				ontology.getInverseObjectPropertyAxioms(objectProperty);
		if (!axioms.isEmpty()) {
			for (OWLInverseObjectPropertiesAxiom ax : axioms) {
				inverses.addAll(ax.getObjectPropertiesInSignature());
			}
		}
		inverses.remove(objectProperty);
	}
	
	/**
	 * Returns a set of ranges for a datatype property.
	 * All ranges are returned as axioms.
	 * 
	 * @param dataProperty OWLDataProperty to get ranges of
	 * @param ontology OWLOntology to retrieve from
	 * @param ranges Set of OWLObjects representing the ranges
	 */
	private static void getRanges(OWLOntology ontology,
			OWLDataProperty dataProperty, Set<OWLObject> ranges) {
		ranges.addAll(ontology.getDataPropertyRangeAxioms(dataProperty));
	}
	
	/**
	 * Returns a set of ranges for an object property. Includes named classes as
	 * OWLClasses and anonymous classes as OWLClassExpressions.
	 * 
	 * @param objectProperty OWLObjectProperty to get ranges of
	 * @param ontology OWLOntology to retrieve from
	 * @param ranges Set of OWLObjects representing the ranges
	 */
	private static void getRanges(OWLOntology ontology,
			OWLObjectProperty objectProperty, Set<OWLObject> ranges) {
		Set<OWLObjectPropertyRangeAxiom> axioms =
				ontology.getObjectPropertyRangeAxioms(objectProperty);
		if (!axioms.isEmpty()) {
			for (OWLObjectPropertyRangeAxiom ax : axioms) {
				for (OWLClassExpression ex : ax.getNestedClassExpressions()) {
					if (!ex.isAnonymous()) {
						ranges.addAll(ex.getClassesInSignature());
					} else {
						logger.debug("Anonymous Object Property Range: "
								+ ex.toString());
						ranges.add(ex);
					}
				}
			}
		}
	}
	
	/**
	 * Returns a set of types for a named individual.
	 * 
	 * @param individual OWLNamedIndividual to get types of
	 * @param ontology OWLOntology to retrieve from
	 * @param types Set of OWLObjects representing the types
	 */
	private static void getTypes(OWLOntology ontology,
			OWLNamedIndividual individual, Set<OWLObject> types) {
		Set<OWLClassAssertionAxiom> axioms =
				ontology.getClassAssertionAxioms(individual);
		if (!axioms.isEmpty()) {
			for (OWLClassAssertionAxiom ax : axioms) {
				if (!ax.getClassExpression().isAnonymous()) {
					types.addAll(ax.getClassesInSignature());
				} else {
					logger.debug("Anonymous Type: "
							+ ax.getClassExpression().toString());
				}
			}
		}
	}
	
	/**
	 * Returns a map of the entities sorted by their EntityType where EntityType
	 * is the key, and a set of OWLEntities of that type is the value. Sorts for
	 * CLASS, DATA_PROPERTY, NAMED_INDIVIDUAL, and OBJECT_PROPERTY.
	 * 
	 * @param  IRIs Set of IRIs to sort into types
	 * @param  ontology OWLOntology to retrieve from
	 * @return Map of EntityTypes to set of OWLEntities of that type
	 */
	private static Map<EntityType<?>, Set<OWLEntity>> sortTypes(
			OWLOntology ontology, Set<IRI> IRIs) {
		Map<EntityType<?>, Set<OWLEntity>> entitiesByType = new HashMap<>();
		Set<OWLEntity> classes = new HashSet<>();
		Set<OWLEntity> dataProperties = new HashSet<>();
		Set<OWLEntity> individuals = new HashSet<>();
		Set<OWLEntity> objectProperties = new HashSet<>();
		for (IRI iri : IRIs) {
			// Get the OWLEntity for the IRI
			Set<OWLEntity> owlEntities = ontology.getEntitiesInSignature(iri);
			// Check that there is exactly one entity, throw exception if not
			if (owlEntities.size() == 0) {
				logger.warn("IRI " + iri.toString() + " does not exist in "
						+ ontology.getOntologyID().toString());
			} else if (owlEntities.size() > 1) {
				logger.warn("Multiple entities for IRI " + iri.toString() 
				        + " in " + ontology.getOntologyID().toString());
			}
			// Get EntityType
			for (OWLEntity e : owlEntities) {
				EntityType<?> entityType = e.getEntityType();
				if (entityType == EntityType.CLASS) {
					classes.add(e.asOWLClass());
				} else if (entityType == EntityType.DATA_PROPERTY) {
					dataProperties.add(e.asOWLDataProperty());
				} else if (entityType == EntityType.OBJECT_PROPERTY) {
					objectProperties.add(e.asOWLObjectProperty());
				} else if (entityType == EntityType.NAMED_INDIVIDUAL) {
					individuals.add(e.asOWLNamedIndividual());
				} else {
					logger.warn(e.toStringID() + " must be a class, datatype "
							+ "property, object property, or individual.");
				}
			}
		}
		// Add to map
		entitiesByType.put(EntityType.CLASS, classes);
		entitiesByType.put(EntityType.DATA_PROPERTY, dataProperties);
		entitiesByType.put(EntityType.NAMED_INDIVIDUAL, individuals);
		entitiesByType.put(EntityType.OBJECT_PROPERTY, objectProperties);
		return entitiesByType;
	}
}
