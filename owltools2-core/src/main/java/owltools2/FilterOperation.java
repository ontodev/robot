package owltools2;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Filter the axioms of an ontology by given criteria.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(FilterOperation.class);

    /**
     * Create a new ontology with a filtered set of axioms
     * from the input ontology.
     * This version expects a set of OWLObjectProperties.
     *
     * @param inputOntology the ontology to extract from
     * @param properties a set of OWLObjectProperties to retain
     * @param outputIRI the OntologyIRI of the new ontology
     * @return the new ontology
     * @throws OWLOntologyCreationException on any OWLAPI problem
     */
    public static OWLOntology filter(OWLOntology inputOntology,
            Set<OWLObjectProperty> properties, IRI outputIRI)
            throws OWLOntologyCreationException {
        System.out.println(properties);
        logger.debug("Filtering ontology for axioms with ObjectProperties "
                + properties);

        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();

        // For each axiom, get all its object properties,
        // then remove the properties that we're looking for.
        // If there are no object properties left, then we keep this axiom.
        // All annotation axioms, declarations, and subClass relations remains.
        for (OWLAxiom axiom: inputOntology.getAxioms()) {
            Set<OWLObjectProperty> ps = axiom.getObjectPropertiesInSignature();
            ps.removeAll(properties);
            if (ps.size() == 0) {
                axioms.add(axiom);
            }
        }

        return OWLManager.createOWLOntologyManager().createOntology(
                axioms, outputIRI);
    }
}
