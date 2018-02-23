package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOperation {
	
	private static final String CLASS = "class";
	private static final String INDIV = "individual";
	private static final String OBJ = "object-property";
	private static final String ANN = "annotation-property";
	private static final String DATA = "datatype-property";
	
	private static final OWLDataFactory factory = OWLManager
												   .createOWLOntologyManager()
												   .getOWLDataFactory();
	private static final IOHelper ioHelper = new IOHelper();
	
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(FilterOperation.class);
    
    /**
     * Remove an OWLEntity of unknown type from the ontology.
     * 
     * @param ontology OWLOntology to remove from
     * @param entityID CURIE of the OWL entity to remove
     */
    public static void remove(OWLOntology ontology, String entityID) {
    	// Create IRI
    	IRI iri = ioHelper.createIRI(entityID);
    	
    	// Try for each OWLEntity type
    	remove(ontology, factory.getOWLClass(iri));
    	remove(ontology, factory.getOWLNamedIndividual(iri));
    	remove(ontology, factory.getOWLObjectProperty(iri));
    	remove(ontology, factory.getOWLAnnotationProperty(iri));
    	remove(ontology, factory.getOWLDataProperty(iri));
    }
    
    /**
     * Remove specified entities from ontology.
     * 
     * @param ontology OWLOntology to remove from
     * @param entities map of entity type (key) and CURIE (val), 
     *                 see RemoveOperation variable declarations for types
     */
    public static void remove(OWLOntology ontology, Map<String, String> entities) {
    	// Get the args of provided options
    	String classID = entities.get(CLASS);
    	String indivID = entities.get(INDIV);
    	String objPropertyID = entities.get(OBJ);
    	String annPropertyID = entities.get(ANN);
    	String dataPropertyID = entities.get(DATA);
    	
    	// For each, check if it was provided, and remove if so
    	// Allow for user to provide multiple types of entities
    	// Class
    	if (classID != null) {
    		remove(ontology, 
    				factory.getOWLClass(ioHelper.createIRI(classID)));
    	}
    	// Individual
    	if (indivID != null) {
    		remove(ontology, 
    				factory.getOWLNamedIndividual(
    						ioHelper.createIRI(indivID)));
    	}
    	// Object Property
    	if (objPropertyID != null) {
    		remove(ontology, 
    				factory.getOWLObjectProperty(
    						ioHelper.createIRI(objPropertyID)));
    	}
    	// Annotation Property
    	if (annPropertyID != null) {
    		remove(ontology, 
    				factory.getOWLAnnotationProperty(
    						ioHelper.createIRI(annPropertyID)));
    	}
    	// Datatype Property
    	if (dataPropertyID != null) {
    		remove(ontology, 
    				factory.getOWLDataProperty(
    						ioHelper.createIRI(dataPropertyID)));
    	}
    }
    
    /**
     * Remove all anonymous superclasses of a given class from the ontology.
     * 
     * @param ontology OWLOntology to remove from
     * @param subClassID CURIE of class to remove anon superclasses
     */
    public static void removeAnonymousSuperclasses(OWLOntology ontology,
    		String subClassID) {
    	// Create OWLClass from String name
    	OWLClass subClass = factory.getOWLClass(ioHelper.createIRI(subClassID));
    	// Get set of anonymous superclass axioms
    	Set<OWLAxiom> anons = new HashSet<>();
		for (OWLSubClassOfAxiom ax :
			ontology.getSubClassAxiomsForSubClass(subClass)) {
			for (OWLClassExpression ex : ax.getNestedClassExpressions()) {
				if (ex.isAnonymous()) {
					anons.add(ax);
				}
			}
		}
		// Remove axioms
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
    	manager.removeAxioms(ontology, anons);
    }
    
    /**
     * Remove all subclasses (recursive) of a given class from the ontology.
     * Retains the original superclass.
     * 
     * @param ontology OWLOntology to remove from
     * @param className CURIE of class to remove children of
     */
    public static void removeDescendantClasses(OWLOntology ontology, 
    		String superClassID) {
    	// Get set of subClassOf*
    	Set<OWLClass> subClasses = RelatedEntitiesHelper.getRelatedClasses(ontology, ioHelper.createIRI(superClassID), "descendants");
    	// Remove axioms for each class in the set
    	for (OWLClass cls : subClasses) {
    		  remove(ontology, cls);
    	}
    }

    /**
     * Remove all named individuals and associated axioms from the ontology.
     * 
     * @param ontology OWLOntology to remove from
     */
    public static void removeIndividuals(OWLOntology ontology) {
    	Set<OWLNamedIndividual> indivs = ontology.getIndividualsInSignature();
    	for (OWLNamedIndividual i : indivs) {
    		remove(ontology, i);
    	}
    }
    
    /**
     * Remove all axioms for given entity from the ontology.
     * This includes logical axioms and assertion axioms.
     * 
     * @param ontology OWLOntology to remove from
     * @param entity OWLClass, OWLNamedIndividual, OWLAnnotationProperty,
     *               OWLObjectProperty, or OWLDataProperty to remove
     */
    private static void remove(OWLOntology ontology, OWLEntity entity) {
    	logger.debug("Removing from ontology: " + entity);
        
        Set<OWLAxiom> axioms = new HashSet<>();
        // Add any logical axioms using the class entity
        for (OWLAxiom axiom : ontology.getAxioms()) {
        	if (axiom.getSignature().contains(entity)) {
        		axioms.add(axiom);
        	}
        }
        // Add any assertions on the class entity
        axioms.addAll(EntitySearcher
        		.getAnnotationAssertionAxioms(entity.getIRI(), ontology));
        // Remove all
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.removeAxioms(ontology, axioms);
    }
}
