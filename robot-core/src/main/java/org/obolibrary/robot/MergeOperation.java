package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLOntology;

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
     * add all the imported axioms into the ontology itself,
     * return the modified ontology.
     *
     * @param ontology the ontology to merge
     * @return the new ontology
     */
    public static OWLOntology merge(OWLOntology ontology) {
        List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
        ontologies.add(ontology);
        mergeInto(ontologies, ontology, false);
        return ontology;
    }

    /**
     * Given one or more ontologies,
     * add all their axioms into the first ontology,
     * and return the first ontology.
     * We use a list instead of a set because OWLAPI judges identity
     * simply by the ontology IRI, even if two ontologies have different axioms.
     *
     * @param ontologies the list of ontologies to merge
     * @return the first ontology
     */
    public static OWLOntology merge(List<OWLOntology> ontologies) {
        OWLOntology ontology = ontologies.get(0);
        mergeInto(ontologies, ontology, false);
        return ontology;
    }

    /**
     * Given a list of ontologies and a target ontology,
     * add all the axioms from the listed ontologies and their import closure
     * into the target ontology.
     * The target ontology is not itself merged,
     * so any of its imports remain distinct.
     *
     * @param ontologies the list of ontologies to merge
     * @param targetOntology the ontology to merge axioms into
     */
    public static void mergeInto(List<OWLOntology> ontologies,
            OWLOntology targetOntology) {
        mergeInto(ontologies, targetOntology, false);
    }

    /**
     * Given a list of ontologies and a target ontology,
     * add all the axioms from the listed ontologies and their import closure
     * into the target ontology.
     * The target ontology is not itself merged,
     * so any of its imports remain distinct.
     *
     * @param ontologies the list of ontologies to merge
     * @param targetOntology the ontology to merge axioms into
     * @param includeAnnotations true if ontology annotations should be merged;
     *   annotations on imports are not merged
     */
    public static void mergeInto(List<OWLOntology> ontologies,
            OWLOntology targetOntology, boolean includeAnnotations) {
        for (OWLOntology ontology: ontologies) {
            targetOntology.getOWLOntologyManager()
                .addAxioms(targetOntology, ontology.getAxioms());
            if (includeAnnotations) {
                for (OWLAnnotation annotation: ontology.getAnnotations()) {
                    OntologyHelper.addOntologyAnnotation(
                            targetOntology, annotation);
                }
            }
            for (OWLOntology imported: ontology.getImportsClosure()) {
                targetOntology.getOWLOntologyManager()
                    .addAxioms(targetOntology, imported.getAxioms());
            }
        }
    }
}

