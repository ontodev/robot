package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove axioms from an ontology.
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class UnmergeOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /**
   * Given one or more ontologies, remove all the axioms from the listed ontologies and their import
   * closure from the first ontology, and return the first ontology
   *
   * <p>We use a list instead of a set because OWLAPI judges identity simply by the ontology IRI,
   * even if two ontologies have different axioms.
   *
   * @param ontologies the list of ontologies to merge
   * @return the first ontology
   */
  public static OWLOntology unmerge(List<OWLOntology> ontologies) {
    OWLOntology ontology = ontologies.get(0);
    unmergeFrom(ontologies.subList(1, ontologies.size()), ontology, false);
    return ontology;
  }

  /**
   * Given a source ontology and a target ontology, remove all the axioms from the listed ontologies
   * and their import closure from the target ontology.
   *
   * @param ontology the source ontology to unmerge
   * @param targetOntology the ontology to remove axioms from
   */
  public static void unmergeFrom(OWLOntology ontology, OWLOntology targetOntology) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    unmergeFrom(ontologies, targetOntology, false);
  }

  /**
   * Given a list of ontologies and a target ontology, remove all the axioms from the listed
   * ontologies and their import closure from the target ontology.
   *
   * @param ontologies the list of ontologies to unmerge
   * @param targetOntology the ontology to remove axioms from
   */
  public static void unmergeFrom(List<OWLOntology> ontologies, OWLOntology targetOntology) {
    unmergeFrom(ontologies, targetOntology, false);
  }

  /**
   * Given a source ontology and a target ontology, remove all the axioms from the listed ontologies
   * and their import closure from the target ontology.
   *
   * @param ontology the ontology to unmerge
   * @param targetOntology the ontology to remove axioms from
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   */
  public static void unmergeFrom(
      OWLOntology ontology, OWLOntology targetOntology, boolean includeAnnotations) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    unmergeFrom(ontologies, targetOntology, includeAnnotations);
  }

  /**
   * Given a list of ontologies and a target ontology, remove all the axioms from the listed
   * ontologies and their import closure from the target ontology.
   *
   * @param ontologies the list of ontologies to unmerge
   * @param targetOntology the ontology to remove axioms from
   * @param includeAnnotations true if ontology annotations should be remove
   */
  public static void unmergeFrom(
      List<OWLOntology> ontologies, OWLOntology targetOntology, boolean includeAnnotations) {
    for (OWLOntology ontology : ontologies) {
      logger.info("Removing axioms from: " + ontology);
      targetOntology.getOWLOntologyManager().removeAxioms(targetOntology, ontology.getAxioms());
      if (includeAnnotations) {
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
          RemoveOntologyAnnotation remove =
              new RemoveOntologyAnnotation(targetOntology, annotation);
          targetOntology.getOWLOntologyManager().applyChange(remove);
        }
      }
      for (OWLOntology imported : ontology.getImportsClosure()) {
        targetOntology.getOWLOntologyManager().removeAxioms(targetOntology, imported.getAxioms());
      }
    }
  }
}
