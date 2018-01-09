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
    public static Set<OWLObject> getRelatedEntities(IRI iri,
    		OWLOntology ontology, String relationOption) {
    	Set<IRI> IRIs = Sets.newHashSet(iri);
    	return getRelatedEntities(IRIs, ontology,
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
    public static Set<OWLObject> getRelatedEntities(Set<IRI> IRIs,
    		OWLOntology ontology, String relationOption) {
    	return getRelatedEntities(IRIs, ontology,
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
    public static Set<OWLObject> getRelatedEntities(IRI iri,
    		OWLOntology ontology, List<String> relationOptions) {
    	Set<IRI> IRIs = Sets.newHashSet(iri);
    	return getRelatedEntities(IRIs, ontology, relationOptions);
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
	public static Set<OWLObject> getRelatedEntities(Set<IRI> IRIs,
			OWLOntology ontology, List<String> relationOptions) {
		Set<OWLObject> relatedEntities = new HashSet<>();
		Map<EntityType<?>, Set<OWLEntity>> entitiesByType =
				sortTypes(IRIs, ontology);
		for (String opt : relationOptions) {
			if ("ancestors".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getAncestors(entitiesByType, ontology));
			} else if ("descendants".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDescendants(entitiesByType, ontology));
			} else if ("equivalents".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getEquivalents(entitiesByType, ontology));
			} else if ("disjoints".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDisjoints(entitiesByType, ontology));
			} else if ("domains".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getDomains(entitiesByType, ontology));
			} else if ("ranges".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getRanges(entitiesByType, ontology));
			} else if ("inverses".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getInverses(entitiesByType, ontology));
			} else if ("types".equals(opt.toLowerCase())) {
				relatedEntities.addAll(
						getTypes(entitiesByType, ontology));
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
	private static Set<OWLObject> getAncestors(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> ancestors = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getAncestors(cls.asOWLClass(), ontology, ancestors);
		}
		for (OWLEntity dp : dataProperties) {
			getAncestors(dp.asOWLDataProperty(), ontology, ancestors);
		}
		for (OWLEntity op : objectProperties) {
			getAncestors(op.asOWLObjectProperty(), ontology, ancestors);
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
	private static Set<OWLObject> getDescendants(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> descendants = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getDescendants(cls.asOWLClass(), ontology, descendants);
		}
		for (OWLEntity dp : dataProperties) {
			getDescendants(dp.asOWLDataProperty(), ontology, descendants);
		}
		for (OWLEntity op : objectProperties) {
			getDescendants(op.asOWLObjectProperty(), ontology, descendants);
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
	private static Set<OWLObject> getDisjoints(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> disjoints = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getDisjoints(cls.asOWLClass(), ontology, disjoints);
		}
		for (OWLEntity dp : dataProperties) {
			getDisjoints(dp.asOWLDataProperty(), ontology, disjoints);
		}
		for (OWLEntity op : objectProperties) {
			getDisjoints(op.asOWLObjectProperty(), ontology, disjoints);
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
	private static Set<OWLObject> getDomains(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> domains = new HashSet<>();
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity dp : dataProperties) {
			getDomains(dp.asOWLDataProperty(), ontology, domains);
		}
		for (OWLEntity op : objectProperties) {
			getDomains(op.asOWLObjectProperty(), ontology, domains);
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
	private static Set<OWLObject> getEquivalents(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> equivalents = new HashSet<>();
		Set<OWLEntity> classes = entitiesByType.get(EntityType.CLASS);
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity cls : classes) {
			getEquivalents(cls.asOWLClass(), ontology, equivalents);
		}
		for (OWLEntity dp : dataProperties) {
			getEquivalents(dp.asOWLDataProperty(), ontology, equivalents);
		}
		for (OWLEntity op : objectProperties) {
			getEquivalents(op.asOWLObjectProperty(), ontology, equivalents);
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
	private static Set<OWLObject> getInverses(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> inverses = new HashSet<>();
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity op : objectProperties) {
			getInverses(op.asOWLObjectProperty(), ontology, inverses);
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
	private static Set<OWLObject> getRanges(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> ranges = new HashSet<>();
		Set<OWLEntity> dataProperties =
				entitiesByType.get(EntityType.DATA_PROPERTY);
		Set<OWLEntity> objectProperties =
				entitiesByType.get(EntityType.OBJECT_PROPERTY);
		for (OWLEntity dp : dataProperties) {
			getRanges(dp.asOWLDataProperty(), ontology, ranges);
		}
		for (OWLEntity op : objectProperties) {
			getRanges(op.asOWLObjectProperty(), ontology, ranges);
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
	private static Set<OWLObject> getTypes(
			Map<EntityType<?>, Set<OWLEntity>> entitiesByType,
			OWLOntology ontology) {
		Set<OWLObject> types = new HashSet<>();
		Set<OWLEntity> individual =
				entitiesByType.get(EntityType.NAMED_INDIVIDUAL);
		for (OWLEntity i : individual) {
			getTypes(i.asOWLNamedIndividual(), ontology, types);
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
	private static void getAncestors(OWLClass cls,
			OWLOntology ontology, Set<OWLObject> ancestors) {
		Set<OWLSubClassOfAxiom> axioms =
				ontology.getSubClassAxiomsForSubClass(cls);
		if (!axioms.isEmpty()) {
			for (OWLSubClassOfAxiom ax : axioms) {
				if (!ax.getSuperClass().isAnonymous()) {
					for (OWLClass c :
						ax.getSuperClass().getClassesInSignature()) {
						ancestors.add(c);
						getAncestors(c, ontology, ancestors);
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
	private static void getAncestors(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> ancestors) {
		Set<OWLSubDataPropertyOfAxiom> axioms =
				ontology.getDataSubPropertyAxiomsForSubProperty(dataProperty);
		if (!axioms.isEmpty()) {
			for (OWLSubDataPropertyOfAxiom ax : axioms) {
				for (OWLDataProperty dp :
					ax.getSuperProperty().getDataPropertiesInSignature()) {
					ancestors.add(dp);
					getAncestors(dp, ontology, ancestors);
					
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
	private static void getAncestors(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> ancestors) {
		Set<OWLSubObjectPropertyOfAxiom> axioms =
				ontology.getObjectSubPropertyAxiomsForSubProperty(
						objectProperty);
		if (!axioms.isEmpty()) {
			for (OWLSubObjectPropertyOfAxiom ax : axioms) {
				for (OWLObjectProperty op :
					ax.getSuperProperty().getObjectPropertiesInSignature()) {
					ancestors.add(op);
					getAncestors(op, ontology, ancestors);
					
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
	private static void getDescendants(OWLClass cls, OWLOntology ontology,
			Set<OWLObject> descendants) {
		Set<OWLSubClassOfAxiom> axioms =
				ontology.getSubClassAxiomsForSuperClass(cls);
		if (!axioms.isEmpty()) {
	    	for (OWLSubClassOfAxiom ax : axioms) {
	    		if (!ax.getSubClass().isAnonymous()) {
		    		for (OWLClass c :
		    			ax.getSubClass().getClassesInSignature()) {
		    			descendants.add(c);
		    			getDescendants(c, ontology, descendants);
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
	private static void getDescendants(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> descendants) {
		Set<OWLSubDataPropertyOfAxiom> axioms =
				ontology.getDataSubPropertyAxiomsForSuperProperty(dataProperty);
		if (!axioms.isEmpty()) {
	    	for (OWLSubDataPropertyOfAxiom ax : axioms) {
	    		for (OWLDataProperty dp :
	    			ax.getSubProperty().getDataPropertiesInSignature()) {
	    			descendants.add(dp);
	    			getDescendants(dp, ontology, descendants);
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
	private static void getDescendants(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> descendants) {
		Set<OWLSubObjectPropertyOfAxiom> axioms =
    			ontology.getObjectSubPropertyAxiomsForSuperProperty(
    					objectProperty);
    	if (!axioms.isEmpty()) {
	    	for (OWLSubObjectPropertyOfAxiom ax : axioms) {
	    		for (OWLObjectProperty op :
	    			ax.getSubProperty().getObjectPropertiesInSignature()) {
	    			descendants.add(op);
	    			getDescendants(op, ontology, descendants);
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
	private static void getDisjoints(OWLClass cls, OWLOntology ontology,
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
	private static void getDisjoints(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> disjoints) {
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
	private static void getDisjoints(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> disjoints) {
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
	private static void getDomains(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> domains) {
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
	private static void getDomains(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> domains) {
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
	private static void getEquivalents(OWLClass cls, OWLOntology ontology,
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
	private static void getEquivalents(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> equivalents) {
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
	private static void getEquivalents(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> equivalents) {
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
	private static void getInverses(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> inverses) {
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
	private static void getRanges(OWLDataProperty dataProperty,
			OWLOntology ontology, Set<OWLObject> ranges) {
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
	private static void getRanges(OWLObjectProperty objectProperty,
			OWLOntology ontology, Set<OWLObject> ranges) {
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
	private static void getTypes(OWLNamedIndividual individual,
			OWLOntology ontology, Set<OWLObject> types) {
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
	private static Map<EntityType<?>, Set<OWLEntity>> sortTypes(Set<IRI> IRIs,
			OWLOntology ontology) {
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
