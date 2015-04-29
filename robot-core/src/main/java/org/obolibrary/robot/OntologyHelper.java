package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Provides convenience methods for working with OWL ontologies.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class OntologyHelper {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(OntologyHelper.class);

    /**
     * Given an OWLAnnotationValue, return its value as a string.
     *
     * @param value the OWLAnnotationValue to get the string value of
     * @return the string value
     */
    public static String getValue(OWLAnnotationValue value) {
        String result = null;
        if (value instanceof OWLLiteral) {
            result = ((OWLLiteral) value).getLiteral();
        }
        return result;
    }

    /**
     * Given an OWLAnnotation, return its value as a string.
     *
     * @param annotation the OWLAnnotation to get the string value of
     * @return the string value
     */
    public static String getValue(OWLAnnotation annotation) {
        return getValue(annotation.getValue());
    }

    /**
     * Given a set of OWLAnnotations, return the first string value
     * as determined by natural string sorting.
     *
     * @param annotations a set of OWLAnnotations to get the value of
     * @return the first string value
     */
    public static String getValue(Set<OWLAnnotation> annotations) {
        Set<String> valueSet = getValues(annotations);
        List<String> valueList = new ArrayList<String>(valueSet);
        Collections.sort(valueList);
        String value = null;
        if (valueList.size() > 0) {
            value = valueList.get(0);
        }
        return value;
    }

    /**
     * Given a set of OWLAnnotations, return a set of their value strings.
     *
     * @param annotations a set of OWLAnnotations to get the value of
     * @return a set of the value strings
     */
    public static Set<String> getValues(Set<OWLAnnotation> annotations) {
        Set<String> results = new HashSet<String>();
        for (OWLAnnotation annotation: annotations) {
            String value = getValue(annotation);
            if (value != null) {
                results.add(value);
            }
        }
        return results;
    }

    /**
     * Given an ontology, return a map from IRIs to rdfs:labels.
     * Includes labels asserted in for all imported ontologies.
     * If an entity has no label, do not include it.
     * If an entity has multiple labels, use the first.
     *
     * @param ontology the ontology to use
     * @return a map from IRIs to label strings
     */
    public static Map<IRI, String> getLabels(OWLOntology ontology) {
        Map<IRI, String> results = new HashMap<IRI, String>();
        OWLAnnotationProperty rdfsLabel =
            ontology.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
        for (OWLEntity entity: ontology.getSignature(true)) {
            for (OWLOntology ont: ontology.getImportsClosure()) {
                String value = getValue(entity.getAnnotations(ont, rdfsLabel));
                if (value != null) {
                    results.put(entity.getIRI(), value);
                }
            }
        }
        return results;
    }

}
