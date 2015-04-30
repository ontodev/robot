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
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ReferencedEntitySetProvider;

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
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of annotation assertion axioms for those
     * subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param property an annotation property
     * @param subject an annotation subject IRIs
     * @return a filtered set of annotation assertion axioms
     */
    public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
            OWLOntology ontology, OWLAnnotationProperty property,
            IRI subject) {
        Set<OWLAnnotationProperty> properties =
            new HashSet<OWLAnnotationProperty>();
        properties.add(property);
        Set<IRI> subjects = new HashSet<IRI>();
        subjects.add(subject);
        return getAnnotationAxioms(ontology, properties, subjects);
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of annotation assertion axioms for those
     * subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param properties a set of annotation properties,
     *   or null if all properties should be included
     * @param subjects a set of annotation subject IRIs,
     *   or null if all subjects should be included
     * @return a filtered set of annotation assertion axioms
     */
    public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
            OWLOntology ontology, Set<OWLAnnotationProperty> properties,
            Set<IRI> subjects) {
        Set<OWLAnnotationAssertionAxiom> results =
            new HashSet<OWLAnnotationAssertionAxiom>();

        for (OWLAxiom axiom: ontology.getAxioms()) {
            if (!(axiom instanceof OWLAnnotationAssertionAxiom)) {
                continue;
            }
            OWLAnnotationAssertionAxiom aaa =
                (OWLAnnotationAssertionAxiom) axiom;
            if (properties != null && !properties.contains(aaa.getProperty())) {
                continue;
            }
            OWLAnnotationSubject subject = aaa.getSubject();
            if (subjects == null) {
                results.add(aaa);
            } else if (subject instanceof IRI
                       && subjects.contains((IRI) subject)) {
                results.add(aaa);
            }
        }
        return results;
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of annotation values for those
     * subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param property an annotation property
     * @param subject an annotation subject IRIs
     * @return a filtered set of annotation values
     */
    public static Set<OWLAnnotationValue> getAnnotationValues(
            OWLOntology ontology, OWLAnnotationProperty property,
            IRI subject) {
        Set<OWLAnnotationProperty> properties =
            new HashSet<OWLAnnotationProperty>();
        properties.add(property);
        Set<IRI> subjects = new HashSet<IRI>();
        subjects.add(subject);
        return getAnnotationValues(ontology, properties, subjects);
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of annotation values for those
     * subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param properties a set of annotation properties,
     *   or null if all properties should be included
     * @param subjects a set of annotation subject IRIs,
     *   or null if all subjects should be included
     * @return a filtered set of annotation values
     */
    public static Set<OWLAnnotationValue> getAnnotationValues(
            OWLOntology ontology, Set<OWLAnnotationProperty> properties,
            Set<IRI> subjects) {
        Set<OWLAnnotationValue> results = new HashSet<OWLAnnotationValue>();
        Set<OWLAnnotationAssertionAxiom> axioms =
            getAnnotationAxioms(ontology, properties, subjects);
        for (OWLAnnotationAssertionAxiom axiom: axioms) {
            results.add(axiom.getValue());
        }
        return results;
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of strings for those subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param property an annotation property
     * @param subject an annotation subject IRIs
     * @return a filtered set of annotation strings
     */
    public static Set<String> getAnnotationStrings(
            OWLOntology ontology, OWLAnnotationProperty property,
            IRI subject) {
        Set<OWLAnnotationProperty> properties =
            new HashSet<OWLAnnotationProperty>();
        properties.add(property);
        Set<IRI> subjects = new HashSet<IRI>();
        subjects.add(subject);
        return getAnnotationStrings(ontology, properties, subjects);
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return a set of strings for those subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param properties a set of annotation properties,
     *   or null if all properties should be included
     * @param subjects a set of annotation subject IRIs,
     *   or null if all subjects should be included
     * @return a filtered set of annotation strings
     */
    public static Set<String> getAnnotationStrings(
            OWLOntology ontology, Set<OWLAnnotationProperty> properties,
            Set<IRI> subjects) {
        Set<String> results = new HashSet<String>();
        Set<OWLAnnotationValue> values =
            getAnnotationValues(ontology, properties, subjects);
        for (OWLAnnotationValue value: values) {
            results.add(getValue(value));
        }
        return results;
    }
    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return the alphanumerically first annotation value string
     * for those subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param property an annotation property
     * @param subject an annotation subject IRIs
     * @return the first annotation string
     */
    public static String getAnnotationString(
            OWLOntology ontology, OWLAnnotationProperty property,
            IRI subject) {
        Set<OWLAnnotationProperty> properties =
            new HashSet<OWLAnnotationProperty>();
        properties.add(property);
        Set<IRI> subjects = new HashSet<IRI>();
        subjects.add(subject);
        return getAnnotationString(ontology, properties, subjects);
    }

    /**
     * Given an ontology, an optional set of annotation properties,
     * and an optional set of subject,
     * return the alphanumerically first annotation value string
     * for those subjects and those properties.
     *
     * @param ontology the ontology to search (including imports closure)
     * @param properties a set of annotation properties,
     *   or null if all properties should be included
     * @param subjects a set of annotation subject IRIs,
     *   or null if all subjects should be included
     * @return the first annotation string
     */
    public static String getAnnotationString(
            OWLOntology ontology, Set<OWLAnnotationProperty> properties,
            Set<IRI> subjects) {
        Set<String> valueSet =
            getAnnotationStrings(ontology, properties, subjects);
        List<String> valueList = new ArrayList<String>(valueSet);
        Collections.sort(valueList);
        String value = null;
        if (valueList.size() > 0) {
            value = valueList.get(0);
        }
        return value;
    }

    /**
     * Given an ontology, return a map from IRIs to rdfs:labels.
     * Includes labels asserted in for all imported ontologies.
     * If there are multiple labels, use the alphanumerically first.
     *
     * @param ontology the ontology to use
     * @return a map from IRIs to label strings
     */
    public static Map<IRI, String> getLabels(OWLOntology ontology) {
        Map<IRI, String> results = new HashMap<IRI, String>();
        OWLAnnotationProperty rdfsLabel =
            ontology.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        ontologies.add(ontology);
        ReferencedEntitySetProvider resp =
            new ReferencedEntitySetProvider(ontologies);
        for (OWLEntity entity: resp.getEntities()) {
            String value = getAnnotationString(
                    ontology, rdfsLabel, entity.getIRI());
            if (value != null) {
                results.put(entity.getIRI(), value);
            }
        }
        return results;
    }

}
