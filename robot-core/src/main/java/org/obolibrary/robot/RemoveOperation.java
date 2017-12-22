package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
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
     * Remove specified entities from ontology.
     * 
     * @param ontology OWLOntology to remove from
     * @param entities map of entity type (key) and CURIE (val), 
     *                 see RemoveOperation variable declarations for types
     */
    public static void remove(OWLOntology ontology, 
    		Map<String, String> entities) {
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
     * Remove all subclasses (recursive) of a given class from the ontology.
     * Retains the original superclass.
     * 
     * @param ontology OWLOntology to remove from
     * @param className CURIE of class to remove children of
     */
    public static void removeDescendantClasses(OWLOntology ontology, 
    		String superClassID) {
    	// Create OWLClass from string name
    	OWLClass superClass = 
    			factory.getOWLClass(ioHelper.createIRI(superClassID));
    	// Get set of subClassOf*
    	Set<OWLClass> subClasses = new HashSet<>();
    	gatherDescendantClasses(ontology, superClass, subClasses);
    	
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
    	logger.debug("Removing all named individuals");
    	
    	Set<OWLNamedIndividual> indivs = ontology.getIndividualsInSignature();
    	for (OWLNamedIndividual i : indivs) {
    		remove(ontology, i);
    	}
    }
    
    /**
     * Recursively get all subclasses of a given class.
     * 
     * @param ontology OWLOntology to search
     * @param cls OWLClass to get subclasses of
     * @param subClasses Set of OWLClasses (pass in empty)
     */
    private static void gatherDescendantClasses(OWLOntology ontology, 
    		OWLClass cls, Set<OWLClass> subClasses) {
    	Set<OWLSubClassOfAxiom> scAxioms =
    			ontology.getSubClassAxiomsForSuperClass(cls);
    	if (!scAxioms.isEmpty()) {
	    	for (OWLSubClassOfAxiom ax : scAxioms) {
	    		for (OWLClass sc : ax.getSubClass().getClassesInSignature()) {
	    			subClasses.add(sc);
	    			gatherDescendantClasses(ontology, sc, subClasses);
	    		}
	    	}
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
