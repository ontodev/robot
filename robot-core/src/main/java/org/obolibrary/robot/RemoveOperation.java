package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOperation {
	
	private static final String CLASS = "class";
	private static final String INDIV = "individual";
	private static final String OBJ = "object-property";
	private static final String ANN = "annotation-property";
	private static final String DATA = "datatype-property";
	
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
    	String className = entities.get(CLASS);
    	String indivName = entities.get(INDIV);
    	String objPropertyName = entities.get(OBJ);
    	String annPropertyName = entities.get(ANN);
    	String dataPropertyName = entities.get(DATA);
    	
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    	OWLDataFactory factory = manager.getOWLDataFactory();
    	
    	IOHelper ioHelper = new IOHelper();
    	
    	// For each, check if it was provided, and remove if so
    	// Class
    	if (className != null) {
    		IRI iri = ioHelper.createIRI(className);
    		remove(ontology, factory.getOWLClass(iri));
    	}
    	// Individual
    	if (indivName != null) {
    		IRI iri = ioHelper.createIRI(indivName);
    		remove(ontology, factory.getOWLNamedIndividual(iri));
    	}
    	// Object Property
    	if (objPropertyName != null) {
    		IRI iri = ioHelper.createIRI(objPropertyName);
    		remove(ontology, factory.getOWLObjectProperty(iri));
    	}
    	// Annotation Property
    	if (annPropertyName != null) {
    		IRI iri = ioHelper.createIRI(annPropertyName);
    		remove(ontology, factory.getOWLAnnotationProperty(iri));
    	}
    	// Datatype Property
    	if (dataPropertyName != null) {
    		IRI iri = ioHelper.createIRI(dataPropertyName);
    		remove(ontology, factory.getOWLDataProperty(iri));
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
