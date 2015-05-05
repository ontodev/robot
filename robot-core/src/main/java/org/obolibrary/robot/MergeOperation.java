package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Merge multiple ontologies into a single ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MergeOperation.class);

    /**
     * Given a single ontology with zero or more imports,
     * return a single merged ontology.
     * The input ontology is copied, not modified.
     *
     * @param ontology the ontology to merge
     * @param outputIRI the IRI of the merged ontology
     * @return the new ontology
     * @throws OWLOntologyCreationException on any OWLAPI problem
     */
    public static OWLOntology merge(OWLOntology ontology,
            IRI outputIRI) throws OWLOntologyCreationException {
        List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
        ontologies.add(ontology);
        return merge(ontologies, outputIRI);
    }

    /**
     * Given one or more ontologies, merge all their axioms into a single
     * new ontology.
     * The input ontologies are copied, not modified.
     * We use a list instead of a set because OWLAPI judges identity
     * simply by the ontology IRI, even if two ontologies have different axioms.
     *
     * @param ontologies the list of ontologies to merge
     * @param outputIRI the IRI of the merged ontology
     * @return the new ontology
     * @throws OWLOntologyCreationException on any OWLAPI problem
     */
    public static OWLOntology merge(List<OWLOntology> ontologies,
            IRI outputIRI) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology merged = manager.createOntology(outputIRI);

        for (OWLOntology ontology: ontologies) {
            manager.addAxioms(merged, ontology.getAxioms());
        }

        return merged;
    }
}
