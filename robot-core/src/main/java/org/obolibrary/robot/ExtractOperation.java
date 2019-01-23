package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Extract a set of OWLEntities from the input ontology to an output ontology. Uses the OWLAPI's
 * SyntacticLocalityModuleExtractor (SLME).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExtractOperation.class);

  /**
   * Return a map from option name to default option value.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("individuals", "include");
    options.put("imports", "include");
    options.put("copy-ontology-annotations", "false");
    return options;
  }

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME) with default options. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param terms a set of IRIs for terms to extract
   * @param outputIRI the OntologyIRI of the new ontology
   * @param moduleType determines the type of extraction; defaults to STAR
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology, Set<IRI> terms, IRI outputIRI, ModuleType moduleType)
      throws OWLOntologyCreationException {
    return extract(inputOntology, terms, outputIRI, moduleType, getDefaultOptions());
  }

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME). The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param terms a set of IRIs for terms to extract
   * @param outputIRI the OntologyIRI of the new ontology
   * @param moduleType determines the type of extraction; defaults to STAR
   * @param options map of extract options
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology,
      Set<IRI> terms,
      IRI outputIRI,
      ModuleType moduleType,
      Map<String, String> options)
      throws OWLOntologyCreationException {
    if (options == null) {
      options = getDefaultOptions();
    }

    String individuals = OptionsHelper.getOption(options, "individuals", "include");
    boolean excludeInstances = false;
    if (individuals.equalsIgnoreCase("exclude")
        || individuals.equalsIgnoreCase("minimal")
        || individuals.equalsIgnoreCase("definitions")) {
      excludeInstances = true;
    }

    String importsString = OptionsHelper.getOption(options, "imports", "include");
    Imports imports;
    if ("include".equalsIgnoreCase(importsString)) {
      imports = Imports.INCLUDED;
    } else {
      imports = Imports.EXCLUDED;
    }

    Set<OWLEntity> entities = new HashSet<>();
    for (IRI term : terms) {
      entities.addAll(inputOntology.getEntitiesInSignature(term, imports));
    }

    // Default moduleType is STAR
    ModuleType type = moduleType;
    if (type == null) {
      type = ModuleType.STAR;
    }

    // Get all axioms from the ontology and its imports
    Set<OWLAxiom> axs = new HashSet<>(inputOntology.getAxioms());
    for (OWLOntology importedOnt : inputOntology.getImportsClosure()) {
      axs.addAll(importedOnt.getAxioms());
    }
    // Maybe get an IRI
    IRI ontIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();

    SyntacticLocalityModuleExtractor extractor =
        new SyntacticLocalityModuleExtractor(
            inputOntology.getOWLOntologyManager(), ontIRI, axs, type, excludeInstances);
    OWLOntology outputOntology =
        OWLManager.createOWLOntologyManager()
            .createOntology(extractor.extract(entities), outputIRI);

    // Maybe add the axioms belonging to individuals of class types included
    if (individuals.equalsIgnoreCase("minimal")) {
      addMinimalIndividualAxioms(inputOntology, outputOntology, imports);
    } else if (individuals.equalsIgnoreCase("definitions")) {
      addDefinitionIndividualAxioms(inputOntology, outputOntology, imports);
    }

    // Maybe copy ontology annotations
    boolean copyOntologyAnnotations =
        OptionsHelper.optionIsTrue(options, "copy-ontology-annotations");
    if (copyOntologyAnnotations) {
      for (OWLAnnotation annotation : inputOntology.getAnnotations()) {
        OntologyHelper.addOntologyAnnotation(outputOntology, annotation);
      }
    }

    return outputOntology;
  }

  /**
   * @param inputOntology
   * @param outputOntology
   * @param imports
   */
  private static void addDefinitionIndividualAxioms(
      OWLOntology inputOntology, OWLOntology outputOntology, Imports imports) {
    Set<OWLIndividual> individuals = new HashSet<>();
    Set<OWLClass> classes = outputOntology.getClassesInSignature();
    for (OWLClass cls : classes) {
      for (OWLClassExpression expr : EntitySearcher.getEquivalentClasses(cls, inputOntology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        individuals.addAll(expr.getIndividualsInSignature());
      }

      for (OWLClassExpression expr : EntitySearcher.getSubClasses(cls, inputOntology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        individuals.addAll(expr.getIndividualsInSignature());
      }
    }
    addIndiviudalsAxioms(inputOntology, outputOntology, individuals, imports);
  }

  /**
   * Given an input ontology and an output ontology, copy any individual axioms and their
   * annotations from the input to the output ontology as long as the class type is included in the
   * output ontology.
   *
   * @param inputOntology OWLOntology to copy axioms from
   * @param outputOntology OWLOntology to copy axioms to
   * @param imports
   */
  private static void addMinimalIndividualAxioms(
      OWLOntology inputOntology, OWLOntology outputOntology, Imports imports) {
    Set<OWLIndividual> individuals = new HashSet<>();
    Set<OWLClass> classes = outputOntology.getClassesInSignature();
    // Get the individuals for each of the included classes
    for (OWLClass cls : classes) {
      individuals.addAll(EntitySearcher.getIndividuals(cls, inputOntology));
    }
    addIndiviudalsAxioms(inputOntology, outputOntology, individuals, imports);
  }

  /**
   * @param inputOntology
   * @param outputOntology
   * @param individuals
   * @param imports
   */
  private static void addIndiviudalsAxioms(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      Set<OWLIndividual> individuals,
      Imports imports) {
    if (imports == null) {
      imports = Imports.INCLUDED;
    }
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLIndividual individual : individuals) {
      if (individual.isNamed()) {
        // Add axioms about named individuals (includes assertions)
        OWLNamedIndividual namedIndividual = individual.asOWLNamedIndividual();
        axioms.addAll(inputOntology.getAnnotationAssertionAxioms(namedIndividual.getIRI()));
        axioms.addAll(inputOntology.getAxioms(namedIndividual, imports));
      } else {
        axioms.addAll(inputOntology.getAxioms(individual, imports));
      }
    }
    outputOntology.getOWLOntologyManager().addAxioms(outputOntology, axioms);
  }
}
