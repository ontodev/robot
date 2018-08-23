package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
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
    options.put("assertions", "include");
    return options;
  }

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

    String assertions = OptionsHelper.getOption(options, "assertions", "include");
    boolean excludeAssertions = false;
    if (assertions.equalsIgnoreCase("exclude") || assertions.equalsIgnoreCase("minimal")) {
      excludeAssertions = true;
    }

    Set<OWLEntity> entities = new HashSet<>();
    for (IRI term : terms) {
      entities.addAll(inputOntology.getEntitiesInSignature(term, true));
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
            inputOntology.getOWLOntologyManager(), ontIRI, axs, type, excludeAssertions);
    OWLOntology outputOntology =
        OWLManager.createOWLOntologyManager()
            .createOntology(extractor.extract(entities), outputIRI);

    // Maybe add the ABox axioms belonging to included individuals
    if (assertions.equalsIgnoreCase("minimal")) {
      Set<OWLNamedIndividual> individuals = outputOntology.getIndividualsInSignature(true);
      for (OWLNamedIndividual individual : individuals) {
        Set<OWLIndividualAxiom> individualAxioms = inputOntology.getAxioms(individual);
        outputOntology.getOWLOntologyManager().addAxioms(outputOntology, individualAxioms);
      }
    }
    return outputOntology;
  }
}
