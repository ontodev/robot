package org.obolibrary.robot;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merge multiple ontologies into a single ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MergeOperation.class);

  /**
   * Given a single ontology with zero or more imports, add all the imported axioms into the
   * ontology itself, return the modified ontology.
   *
   * @param ontology the ontology to merge
   * @return the new ontology
   */
  public static OWLOntology merge(OWLOntology ontology) {
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    ontologies.add(ontology);
    // One ontology will always collapse the import closure
    mergeInto(ontologies, ontology, false, true);
    return ontology;
  }

  /**
   * Merge one or more ontologies with the default merge options (do not include annotations, do not
   * collapse import closure).
   *
   * @param ontologies the list of ontologies to merge
   * @return the first ontology
   */
  public static OWLOntology merge(List<OWLOntology> ontologies) {
    return merge(ontologies, false, false);
  }

  /**
   * Given one or more ontologies, add all their axioms first ontology, and return the first
   * ontology. Option to include ontology annotations and collapse import closure.
   *
   * <p>We use a list instead of a set because OWLAPI judges identity simply by the ontology IRI,
   * even if two ontologies have different axioms.
   *
   * @param ontologies the list of ontologies to merge
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   * @param collapseImportClosure if true, imports closure from all ontologies included
   * @return the first ontology
   */
  public static OWLOntology merge(
      List<OWLOntology> ontologies, boolean includeAnnotations, boolean collapseImportsClosure) {
    OWLOntology ontology = ontologies.get(0);
    mergeInto(ontologies, ontology, includeAnnotations, collapseImportsClosure);
    return ontology;
  }

  /**
   * Given one or more ontologies, add all their axioms (including their imports closures) into the
   * first ontology, and return the first ontology. Replaced by with method using explicit options;
   * the mergeOptions map is not used by the MergeCommand.
   *
   * @param ontologies the list of ontologies to merge
   * @param mergeOptions a map of option strings, or null
   * @return the first ontology
   */
  public static OWLOntology merge(List<OWLOntology> ontologies, Map<String, String> mergeOptions) {
    OWLOntology ontology = ontologies.get(0);

    // Use collapseImportsClosure and includeAnnotations instead
    mergeInto(ontologies, ontology, false, true);
    return ontology;
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology and
   * its import closure into the target ontology. The target ontology is not itself merged, so any
   * of its imports remain distinct.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   */
  public static void mergeInto(OWLOntology ontology, OWLOntology targetOntology) {
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    ontologies.add(ontology);
    // By default, do not include annotations and do not collapse import closure
    mergeInto(ontologies, targetOntology, false, false);
  }

  /**
   * Given one or more ontologies and a target ontology, add all the axioms from the listed
   * ontologies to the target ontology. Optionally, include annotations from the ontologies in the
   * list. It is recommended to use mergeInto with both includeAnnotations and
   * collapseImportsClosure options.
   *
   * @param ontologies the ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   */
  public static void mergeInto(
      List<OWLOntology> ontologies, OWLOntology targetOntology, boolean includeAnnotations) {
    // By default, do not collapse the imports closure
    mergeInto(ontologies, targetOntology, includeAnnotations, false);
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology and
   * its import closure into the target ontology. The target ontology is not itself merged, so any
   * of its imports remain distinct.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   */
  public static void mergeInto(
      OWLOntology ontology, OWLOntology targetOntology, boolean includeAnnotations) {
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    ontologies.add(ontology);
    mergeInto(ontologies, targetOntology, includeAnnotations);
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology into
   * the target ontology. Optionally, include annotations from the source and/or collapse the
   * imports closures.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   * @param collapseImportsClosure if true, imports closure from all ontologies included
   */
  public static void mergeInto(
      OWLOntology ontology,
      OWLOntology targetOntology,
      boolean includeAnnotations,
      boolean collapseImportsClosure) {
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    ontologies.add(ontology);
    mergeInto(ontologies, targetOntology, includeAnnotations, collapseImportsClosure);
  }

  /**
   * Given a list of ontologies and a target ontology, add all the axioms from the listed ontologies
   * and their import closure into the target ontology. The target ontology is not itself merged, so
   * any of its imports remain distinct.
   *
   * @param ontologies the list of ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   */
  public static void mergeInto(List<OWLOntology> ontologies, OWLOntology targetOntology) {
    // By default, do not include annotations and do not collapse import closure
    mergeInto(ontologies, targetOntology, false, false);
  }

  /**
   * Given a list of ontologies and a target ontology, add all the axioms from the listed ontologies
   * and their import closure into the target ontology. The target ontology is not itself merged, so
   * any of its imports remain distinct, unless collapsing imports closure.
   *
   * @param ontologies the list of ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   * @param collapseImportsClosure true if imports closure from all ontologies included
   */
  public static void mergeInto(
      List<OWLOntology> ontologies,
      OWLOntology targetOntology,
      boolean includeAnnotations,
      boolean collapseImportsClosure) {
    for (OWLOntology ontology : ontologies) {
      if (collapseImportsClosure) {
        // Merge the ontologies with imports included
        targetOntology
            .getOWLOntologyManager()
            .addAxioms(targetOntology, ontology.getAxioms(Imports.INCLUDED));
      } else {
        // Merge the ontologies with imports excluded
        Set<OWLOntology> imports = targetOntology.getDirectImports();
        try {
          OntologyHelper.removeImports(targetOntology);
        } catch (Exception e) {
          // Continue without removing imports
          continue;
        }
        targetOntology
            .getOWLOntologyManager()
            .addAxioms(targetOntology, ontology.getAxioms(Imports.EXCLUDED));
        OWLOntologyManager manager = targetOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        // Re-add the imports
        for (OWLOntology imp : imports) {
          OWLImportsDeclaration dec =
              dataFactory.getOWLImportsDeclaration(imp.getOntologyID().getOntologyIRI().orNull());
          manager.applyChange(new AddImport(targetOntology, dec));
        }
      }
      if (includeAnnotations) {
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
          // Add each set of ontology annotations to the target ontology
          OntologyHelper.addOntologyAnnotation(targetOntology, annotation);
        }
      }
    }
    if (collapseImportsClosure) {
      // Remove import statements, as they've been merged in
      removeImports(targetOntology);
    }
  }

  /**
   * Given an ontology, remove the import statements.
   *
   * @param ontology the ontology to remove import statements from
   */
  private static void removeImports(OWLOntology ontology) {
    Set<OWLImportsDeclaration> oids = ontology.getImportsDeclarations();
    for (OWLImportsDeclaration oid : oids) {
      RemoveImport ri = new RemoveImport(ontology, oid);
      ontology.getOWLOntologyManager().applyChange(ri);
    }
  }

  /**
   * Return a map from option name to default option value, for all the available merge options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<String, String>();
    options.put("collapse-imports-closure", "false");

    return options;
  }
}
