package org.obolibrary.robot;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;
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
     * @param ontology the OWLOntology to remove from
     * @param entities map of entity type (key) and CURIE (val), 
     *                 see RemoveOperation variable declarations for types
     */
    public static void remove(OWLOntology ontology, Map<String, String> entities) {
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
     * Remove all axioms for given class from ontology.
     * 
     * @param ontology the OWLOntology to remove from
     * @param owlClass the OWLClass to remove
     */
    private static void remove(OWLOntology ontology, OWLClass owlClass) {
    	System.out.println(owlClass);
        logger.debug("Removing from ontology: " + owlClass);
        
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        // Get the logical axioms & declaration axioms
        Set<OWLClassAxiom> logicalAxioms = ontology.getAxioms(owlClass);
        Set<OWLDeclarationAxiom> decAxioms = 
        		ontology.getDeclarationAxioms(owlClass);
        // Remove both
        manager.removeAxioms(ontology, logicalAxioms);
        manager.removeAxioms(ontology, decAxioms);
    }
    
    /**
     * Remove all axioms for given individual from ontology.
     * 
     * @param ontology the OWLOntology to remove from
     * @param indiv the OWLIndividual to remove
     */
    private static void remove(OWLOntology ontology, OWLIndividual indiv) {
    	System.out.println(indiv);
        logger.debug("Removing from ontology: " + indiv);
        
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        // Remove axioms on the individual
        manager.removeAxioms(ontology, ontology.getAxioms(indiv));
    }
    
    /**
     * Remove all axioms for given property from ontology.
     * Can handle object, annotation, or datatype property.
     * 
     * @param ontology the OWLOntology to remove from
     * @param property the OWLProperty to remove
     */
    private static void remove(OWLOntology ontology, 
    		OWLProperty property) {
        logger.debug("Removing from ontology: " + property);
        
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        // Remove all axioms that use that property
        manager.removeAxioms(ontology, ontology.getReferencingAxioms(property));
    }
}
