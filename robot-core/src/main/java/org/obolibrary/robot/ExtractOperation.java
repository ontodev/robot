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
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
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

  /** Shared data factory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** RDFS isDefinedBy annotation property. */
  private static OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

  /** Namespace for errors. */
  private static final String NS = "extract#";

  /** Error message when an invalid intermediates opiton is provided. */
  private static final String unknownIntermediatesError =
      NS + "UNKNOWN INTERMEDIATES ERROR '%s' is not a valid --intermediates arg";

  /** Error message when an invalid argument is passed to --individuals. */
  private static final String unknownIndividualsError =
      NS + "UNKNOWN INDIVIDUALS ERROR %s is not a valid --individuals argument";

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
    options.put("annotate-with-source", "false");
    options.put("intermediates", "all");
    options.put("force", "false");
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
    return extract(inputOntology, terms, outputIRI, moduleType, getDefaultOptions(), null);
  }

  public static OWLOntology extract(
      OWLOntology inputOntology,
      Set<IRI> terms,
      IRI outputIRI,
      ModuleType moduleType,
      Map<String, String> options)
      throws OWLOntologyCreationException {
    return extract(inputOntology, terms, outputIRI, moduleType, options, null);
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
   * @param sourceMap map of term IRI to source IRI, or null (only used with annotate-with-source)
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology,
      Set<IRI> terms,
      IRI outputIRI,
      ModuleType moduleType,
      Map<String, String> options,
      Map<IRI, IRI> sourceMap)
      throws OWLOntologyCreationException {
    if (options == null) {
      options = getDefaultOptions();
    }

    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    String individuals = OptionsHelper.getOption(options, "individuals", "include");
    boolean excludeInstances;
    if (individuals.equalsIgnoreCase("exclude")
        || individuals.equalsIgnoreCase("minimal")
        || individuals.equalsIgnoreCase("definitions")) {
      excludeInstances = true;
    } else if (individuals.equalsIgnoreCase("include")) {
      excludeInstances = false;
    } else {
      throw new IllegalArgumentException(String.format(unknownIndividualsError, individuals));
    }

    String importsString = OptionsHelper.getOption(options, "imports", "include");
    Imports imports;
    if ("include".equalsIgnoreCase(importsString)) {
      imports = Imports.INCLUDED;
    } else {
      imports = Imports.EXCLUDED;
    }
    logger.debug("Extracting...");

    Set<OWLEntity> entities = new HashSet<>();
    for (IRI term : terms) {
      entities.addAll(inputOntology.getEntitiesInSignature(term, imports));
    }

    // Default moduleType is STAR
    ModuleType type = moduleType;
    if (type == null) {
      type = ModuleType.STAR;
    }

    // Get all axioms from the ontology
    Set<OWLAxiom> axs = new HashSet<>(inputOntology.getAxioms());
    if (imports.equals(Imports.INCLUDED)) {
      // Maybe get the axioms from the imported ontologies
      for (OWLOntology importedOnt : inputOntology.getImportsClosure()) {
        axs.addAll(importedOnt.getAxioms());
      }
    }
    // Maybe get an IRI
    IRI ontIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();

    SyntacticLocalityModuleExtractor extractor =
        new SyntacticLocalityModuleExtractor(
            inputOntology.getOWLOntologyManager(), ontIRI, axs, type, excludeInstances);

    // Create the output with the extracted terms
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology = manager.createOntology(extractor.extract(entities), outputIRI);

    // Maybe add the axioms belonging to individuals of class types included
    if (individuals.equalsIgnoreCase("minimal")) {
      addMinimalIndividualAxioms(inputOntology, outputOntology, imports);
    } else if (individuals.equalsIgnoreCase("definitions")) {
      addDefinitionIndividualAxioms(inputOntology, outputOntology, imports);
    } else if ("exclude".equalsIgnoreCase(individuals)) {
      // Make sure to completely remove individuals
      Set<OWLObject> indivs = new HashSet<>(outputOntology.getIndividualsInSignature());
      Set<OWLAxiom> indivAxioms =
          RelatedObjectsHelper.getCompleteAxioms(outputOntology, indivs, null, true);
      manager.removeAxioms(outputOntology, indivAxioms);
    }

    // Maybe copy ontology annotations
    boolean copyOntologyAnnotations =
        OptionsHelper.optionIsTrue(options, "copy-ontology-annotations");
    if (copyOntologyAnnotations) {
      for (OWLAnnotation annotation : inputOntology.getAnnotations()) {
        OntologyHelper.addOntologyAnnotation(outputOntology, annotation);
      }
    }
    // Maybe annotate entities with rdfs:isDefinedBy
    if (OptionsHelper.optionIsTrue(options, "annotate-with-source")) {
      Set<OWLAnnotationAxiom> sourceAxioms = new HashSet<>();
      for (OWLEntity entity : OntologyHelper.getEntities(outputOntology)) {
        // Check if rdfs:isDefinedBy already exists
        Set<OWLAnnotationValue> existingValues =
            OntologyHelper.getAnnotationValues(outputOntology, isDefinedBy, entity.getIRI());
        if (existingValues == null || existingValues.size() == 0) {
          // If not, add it
          OWLAnnotationAxiom def = getIsDefinedBy(entity, sourceMap);
          if (def != null) {
            sourceAxioms.add(def);
          }
        }
      }
      manager.addAxioms(outputOntology, sourceAxioms);
    }

    // Determine what to do based on intermediates
    if ("all".equalsIgnoreCase(intermediates)) {
      return outputOntology;
    } else if ("none".equalsIgnoreCase(intermediates)) {
      removeIntermediates(outputOntology, entities);
      return outputOntology;
    } else if ("minimal".equalsIgnoreCase(intermediates)) {
      OntologyHelper.collapseOntology(outputOntology, terms);
      return outputOntology;
    } else {
      throw new IllegalArgumentException(String.format(unknownIntermediatesError, intermediates));
    }
  }

  /**
   * Given an input ontology, an output ontology, and an Imports specification, copy individuals
   * used in logical definitions from the input to the output ontology.
   *
   * @param inputOntology OWLOntology to copy axioms from
   * @param outputOntology OWLOntology to copy axioms to
   * @param imports Imports.INCLUDED or Imports.EXCLUDED
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
   * @param imports Imports.INCLUDED or Imports.EXCLUDED
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
   * Given an input ontology, an output ontology, a set of individuals, and an Imports
   * specification, copy the individual axioms for the set of individuals from the input to the
   * output ontology.
   *
   * @param inputOntology OWLOntology to copy axioms from
   * @param outputOntology OWLOntology to copy axioms to
   * @param individuals Set of OWLIndividuals to copy axioms for
   * @param imports Imports.INCLUDED or Imports.EXCLUDED
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

  /**
   * Given an OWLEntity, return an OWLAnnotationAssertionAxiom indicating the source ontology with
   * rdfs:isDefinedBy.
   *
   * @param entity entity to get source of
   * @param sourceMap map of term IRI to source IRI, or null
   * @return OWLAnnotationAssertionAxiom with rdfs:isDefinedBy as the property
   */
  protected static OWLAnnotationAxiom getIsDefinedBy(OWLEntity entity, Map<IRI, IRI> sourceMap) {
    String iri = entity.getIRI().toString();
    IRI base;
    if (sourceMap != null && sourceMap.containsKey(entity.getIRI())) {
      // IRI exists in the prefixes
      base = sourceMap.get(entity.getIRI());
    } else {
      // Brute force edit the IRI string
      // Warning - this may not work with non-OBO Foundry terms, depending on the IRI format!
      if (iri.contains("#")) {
        if (iri.contains(".owl#")) {
          String baseStr = iri.substring(0, iri.lastIndexOf("#")).toLowerCase();
          base = IRI.create(baseStr);
        } else {
          String baseStr = iri.substring(0, iri.lastIndexOf("#")).toLowerCase() + ".owl";
          base = IRI.create(baseStr);
        }
      } else if (iri.contains("_")) {
        String baseStr = iri.substring(0, iri.lastIndexOf("_")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      } else if (iri.contains("/")) {
        String baseStr = iri.substring(0, iri.lastIndexOf("/")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      } else {
        logger.warn("Unable to get source for IRI " + iri);
        return null;
      }
    }
    return dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), base);
  }

  /**
   * Given an input ontology, an extracted output ontology, and a set of entities, remove all
   * intermediates. This leaves only the classes directly used in the logic of any input entities.
   *
   * @param outputOntology extracted module
   * @param entities Set of extracted entities
   */
  private static void removeIntermediates(OWLOntology outputOntology, Set<OWLEntity> entities) {
    Set<OWLObject> precious = new HashSet<>();
    OWLOntologyManager manager = outputOntology.getOWLOntologyManager();
    for (OWLEntity e : entities) {
      if (!e.isOWLClass()) {
        continue;
      }
      OWLClass cls = e.asOWLClass();
      precious.add(cls);
      for (OWLClassExpression expr : EntitySearcher.getSuperClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
      for (OWLClassExpression expr : EntitySearcher.getEquivalentClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
      for (OWLClassExpression expr : EntitySearcher.getDisjointClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
    }
    Set<OWLAxiom> removeAxioms =
        RelatedObjectsHelper.getPartialAxioms(
            outputOntology,
            RelatedObjectsHelper.selectClasses(
                RelatedObjectsHelper.selectComplement(outputOntology, precious)),
            null);
    manager.removeAxioms(outputOntology, removeAxioms);
  }
}
