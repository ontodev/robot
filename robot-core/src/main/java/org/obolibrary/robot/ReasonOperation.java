package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

/**
 * Reason over an ontology and add axioms.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReasonOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(ReasonOperation.class);

    /**
     * Given an ontology and the name of a reasoner,
     * return a new ontology with all asserted and inferred axioms.
     * The input ontology is copied, not modified.
     *
     * @param inputOntology the ontology to reason over
     * @param reasonerFactory the factory to create a reasoner instance from
     * @param outputIRI the IRI of the merged ontology
     * @return the new ontology
     * @throws OWLOntologyCreationException on any OWLAPI problem
     */
    public static OWLOntology reason(OWLOntology inputOntology,
            OWLReasonerFactory reasonerFactory, IRI outputIRI)
            throws OWLOntologyCreationException {
        logger.info("Input ontology has "
                    + inputOntology.getAxioms().size()
                    + " axioms.");
        logger.info("Starting reasoning...");

        int seconds;
        long elapsedTime;
        long startTime = System.currentTimeMillis();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        OWLOntology reasoned = manager.createOntology(outputIRI);

        // Copy the input ontology.
        manager.addAxioms(reasoned, inputOntology.getAxioms());

        OWLReasoner reasoner = reasonerFactory.createReasoner(reasoned);
        if (!reasoner.isConsistent()) {
            logger.info("Ontology is not consistent!");
            return null;
        }

        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        Node<OWLClass> unsatisfiableClasses =
            reasoner.getUnsatisfiableClasses();
        if (unsatisfiableClasses.getSize() > 1) {
            logger.info("There are "
                    + unsatisfiableClasses.getSize()
                    + " unsatisfiable classes in the ontology: ");
            for (OWLClass cls : unsatisfiableClasses) {
                if (!cls.isOWLNothing()) {
                    logger.info("    unsatisfiable: " + cls.getIRI());
                }
            }
        }

        // Make sure to add the axiom generators in this way!!!
        List<InferredAxiomGenerator<? extends OWLAxiom>> gens =
            new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        gens.add(new InferredSubClassAxiomGenerator());
        InferredOntologyGenerator generator =
            new InferredOntologyGenerator(reasoner, gens);
        logger.info("Using these axiom generators:");
        for (InferredAxiomGenerator inf: generator.getAxiomGenerators()) {
            logger.info("    " + inf);
        }

        elapsedTime = System.currentTimeMillis() - startTime;
        seconds = (int) Math.ceil(elapsedTime / 1000);
        logger.info("Reasoning took " + seconds + " seconds.");

        startTime = System.currentTimeMillis();
        generator.fillOntology(manager, reasoned);

        logger.info("Reasoned ontology has "
                    + reasoned.getAxioms().size()
                    + " axioms.");

        elapsedTime = System.currentTimeMillis() - startTime;
        seconds = (int) Math.ceil(elapsedTime / 1000);
        logger.info("Filling took " + seconds + " seconds.");
        return reasoned;
    }

}
