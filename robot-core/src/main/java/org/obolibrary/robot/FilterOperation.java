package org.obolibrary.robot;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter the axioms of an ontology by given criteria. This implementation has been replaced by
 * methods in OntologyHelper and RelatedObjectsHelper. See the remove & filter documentation for
 * more details.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterOperation.class);

  /**
   * Remove axioms from the input ontology. This version expects a set of OWLObjectProperties.
   *
   * @param ontology the ontology to filter
   * @param properties a set of OWLObjectProperties to retain
   */
  public static void filter(OWLOntology ontology, Set<OWLObjectProperty> properties) {
    logger.debug("Filtering ontology for axioms with ObjectProperties " + properties);

    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLAxiom> axioms = ontology.getAxioms();
    logger.debug("Ontology has {} axioms before filtering", axioms.size());

    // For each axiom, get all its object properties,
    // then remove the properties that we're looking for.
    // If there are no object properties left, then we keep this axiom.
    // All annotation axioms, declarations, and subClass relations remains.
    for (OWLAxiom axiom : axioms) {
      Set<OWLObjectProperty> ps = axiom.getObjectPropertiesInSignature();
      ps.removeAll(properties);
      if (ps.size() > 0) {
        manager.removeAxiom(ontology, axiom);
      }
    }

    logger.debug("Ontology has {} axioms after filtering", ontology.getAxioms().size());
  }
}
