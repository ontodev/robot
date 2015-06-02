package org.obolibrary.robot;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

/**
 * Implements several variations on MIREOT, as first described in
 * "MIREOT: The minimum information to reference an external ontology term"
 * (<a href="http://dx.doi.org/10.3233/AO-2011-0087">link</a>).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MireotOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MireotOperation.class);

    /**
     * Given an ontology and a set of term IRIs,
     * return a new ontology with just the named ancestors of those
     * terms, their subclass relations, and their labels.
     * The input ontology is not changed.
     *
     * @param inputOntology the ontology to extract from
     * @param terms the starting terms to extract
     * @return a new ontology with the target terms and their named ancestors
     * @throws OWLOntologyCreationException on problems creating new ontology
     */
    public static OWLOntology getAncestors(OWLOntology inputOntology,
            Set<IRI> terms)
            throws OWLOntologyCreationException {
        Set<OWLEntity> entities = new HashSet<OWLEntity>();
        for (IRI term: terms) {
            entities.addAll(inputOntology.getEntitiesInSignature(term, true));
        }

        return getAncestors(inputOntology, entities, null);
    }

    /**
     * Given an ontology, a set of entities, and a set of annotation
     * properties, return a new ontology with just the named ancestors of those
     * terms, their subclass relations, and the selected annotations.
     * The input ontology is not changed.
     *
     * @param inputOntology the ontology to extract from
     * @param entities the starting entities to extract
     * @param annotationProperties the annotation properties to copy
     * @return a new ontology with the target terms and their named ancestors
     * @throws OWLOntologyCreationException on problems creating new ontology
     */
    public static OWLOntology getAncestors(OWLOntology inputOntology,
            Set<OWLEntity> entities,
            Set<OWLAnnotationProperty> annotationProperties)
            throws OWLOntologyCreationException {
        logger.debug("Extract with MIREOT ...");

        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(inputOntology);

        OWLOntologyManager outputManager =
            OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = outputManager.getOWLDataFactory();
        OWLOntology outputOntology = outputManager.createOntology();

        if (annotationProperties == null) {
            annotationProperties = new HashSet<OWLAnnotationProperty>();
            annotationProperties.add(
                outputManager.getOWLDataFactory().getRDFSLabel());
        }

        for (OWLEntity entity: entities) {
            copy(inputOntology, outputOntology, entity, annotationProperties);
            if (entity.isOWLClass()) {
                copyAncestors(reasoner, inputOntology, outputOntology, entity,
                    annotationProperties);
            }
        }

        return outputOntology;
    }

    /**
     * Given a reasoner, input and output ontologies, a target entity,
     * and a set of annotation properties,
     * copy the target entity and all its named ancestors (recursively)
     * from the input ontology to the output ontology,
     * along with the specified annotations.
     * The input ontology is not changed.
     *
     * @param reasoner use to find superclasses
     * @param inputOntology the ontology to copy from
     * @param outputOntology the ontology to copy to
     * @param entity the target entity that will have its ancestors copied
     * @param annotationProperties the annotations to copy
     */
    private static void copyAncestors(OWLReasoner reasoner,
            OWLOntology inputOntology, OWLOntology outputOntology,
            OWLEntity entity, Set<OWLAnnotationProperty> annotationProperties) {
        OWLOntologyManager outputManager =
            outputOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = outputManager.getOWLDataFactory();

        Set<OWLClass> superclasses =
            reasoner.getSuperClasses(entity.asOWLClass(), true).getFlattened();
        for (OWLClass superclass: superclasses) {
            copy(inputOntology, outputOntology, superclass,
                    annotationProperties);
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLSubClassOfAxiom(
                    entity.asOWLClass(), superclass));
            copyAncestors(reasoner, inputOntology, outputOntology, superclass,
                annotationProperties);
        }
    }

    /**
     * Given input and output ontologies, a target entity,
     * and a set of annotation properties,
     * copy the target entity from the input ontology to the output ontology,
     * along with the specified annotations.
     * If the entity is already in the outputOntology,
     * then return without making any changes.
     * The input ontology is not changed.
     *
     * @param inputOntology the ontology to copy from
     * @param outputOntology the ontology to copy to
     * @param entity the target entity that will have its ancestors copied
     * @param annotationProperties the annotations to copy
     */
    private static void copy(OWLOntology inputOntology,
            OWLOntology outputOntology, OWLEntity entity,
            Set<OWLAnnotationProperty> annotationProperties) {
        if (outputOntology.containsEntityInSignature(entity)) {
            return;
        }

        OWLOntologyManager outputManager =
            outputOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = outputManager.getOWLDataFactory();

        if (entity.isOWLAnnotationProperty()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLAnnotationProperty()));
        } else if (entity.isOWLObjectProperty()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLObjectProperty()));
        } else if (entity.isOWLDataProperty()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLDataProperty()));
        } else if (entity.isOWLDatatype()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLDatatype()));
        } else if (entity.isOWLClass()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLClass()));
        } else if (entity.isOWLNamedIndividual()) {
            outputManager.addAxiom(outputOntology,
                dataFactory.getOWLDeclarationAxiom(
                    entity.asOWLNamedIndividual()));
        }

        Set<OWLAnnotationAssertionAxiom> axioms =
            inputOntology.getAnnotationAssertionAxioms(entity.getIRI());
        for (OWLAnnotationAssertionAxiom axiom: axioms) {
            if (annotationProperties.contains(axiom.getProperty())) {
                outputManager.addAxiom(outputOntology, axiom);
            }
        }
    }
}
