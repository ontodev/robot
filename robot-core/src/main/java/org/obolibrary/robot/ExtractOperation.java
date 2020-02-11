package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.util.*;
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

  /** Error message when user provides invalid extraction method. */
  private static final String invalidMethodError =
      NS + "INVALID METHOD ERROR method must be: MIREOT, STAR, TOP, BOT, or mireot-rdfxml";

  /** Error message when upper or lower terms are used for SLME or mireot-rdfxml methods. */
  private static final String invalidTermsInConfigError =
      NS + "INVALID TERMS IN CONFIG The '%s' option should only be used for MIREOT";

  /** Error message when 'terms' is missing for SLME or mireot-rdfxml methods. */
  private static final String missingTermsInConfigError =
      NS + "MISSING TERMS IN CONFIG 'terms' is a required option in the configuration file";

  /** Error when 'input' or 'input-iri' is missing. */
  protected static final String missingInputInConfigError =
      NS + "MISSING INPUT IN CONFIG an 'input' or 'input-iri' is required in configuration file";

  /** Error message when an invalid intermediates option is provided. */
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
          sourceAxioms.add(getIsDefinedBy(entity, sourceMap));
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
   * Perform a extraction using parameters from a configuration file.
   *
   * @param ioHelper IOHelper to handle creating IRIs
   * @param options Map of options from config file
   * @return extracted subset
   * @throws Exception on any problem
   */
  public static OWLOntology extractFromConfig(IOHelper ioHelper, Map<String, List<String>> options)
      throws Exception {
    // Make sure we have terms to extract
    if (!options.containsKey("terms")) {
      throw new Exception(missingTermsInConfigError);
    }
    if (options.containsKey("lower-terms")) {
      throw new Exception(String.format(invalidTermsInConfigError, "lower-terms"));
    }
    if (options.containsKey("upper-terms")) {
      throw new Exception(String.format(invalidTermsInConfigError, "upper-terms"));
    }

    // Get an input
    OWLOntology inputOntology;
    if (options.containsKey("input")) {
      String inputPath = options.get("input").get(0);
      inputOntology = ioHelper.loadOntology(inputPath);
    } else if (options.containsKey("input-iri")) {
      IRI inputIRI = IRI.create(options.get("input-iri").get(0));
      inputOntology = ioHelper.loadOntology(inputIRI);
    } else {
      throw new Exception(missingInputInConfigError);
    }

    // Maybe get a target
    OWLOntology target = null;
    if (options.containsKey("target")) {
      String targetPath = options.get("target").get(0);
      target = ioHelper.loadOntology(targetPath);
    } else if (options.containsKey("target-iri")) {
      IRI targetIRI = IRI.create(options.get("target-iri").get(0));
      target = ioHelper.loadOntology(targetIRI);
    }

    // Init the checker for label processing
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(inputOntology);
    if (target != null) {
      checker.addAll(target);
    }

    // Parse terms
    Set<IRI> terms = new HashSet<>();
    Map<OWLEntity, Set<OWLEntity>> replaceParents = new HashMap<>();
    for (String termLine : options.get("terms")) {
      List<String> split = Lists.newArrayList(termLine.split("\t"));
      String termString = split.remove(0).trim();

      OWLEntity e = checker.getOWLEntity(termString);
      if (e == null) {
        logger.error(String.format("Unable to create entity from '%s'", termString));
        continue;
      }
      terms.add(e.getIRI());

      if (split.isEmpty()) {
        continue;
      }

      // IF there are remaining splits, add them as replacement parents
      Set<OWLEntity> replaceParentsForCurrent = new HashSet<>();
      for (String s : split) {
        if (s.trim().equals("")) {
          continue;
        }
        OWLEntity e2 = checker.getOWLEntity(s);
        if (e2 == null) {
          logger.error(String.format("Unable to create entity from '%s'", s));
          continue;
        }
        replaceParentsForCurrent.add(e2);
      }
      if (!replaceParentsForCurrent.isEmpty()) {
        replaceParents.put(e, replaceParentsForCurrent);
      }
    }

    // Parse annotation properties
    Map<OWLAnnotationProperty, OWLAnnotationProperty> mapToAnnotations = new HashMap<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> copyToAnnotations = new HashMap<>();
    for (String apLine : options.getOrDefault("annotations", new ArrayList<>())) {
      List<String> split = Lists.newArrayList(apLine.split("\t"));
      String apString = split.remove(0).trim();

      // Try to create an annotation property from the string
      OWLAnnotationProperty ap = checker.getOWLAnnotationProperty(apString, true);
      if (ap == null) {
        logger.error(String.format("Unable to create annotation property from '%s'", apString));
        continue;
      }

      if (split.isEmpty()) {
        continue;
      }

      String apOpt = split.remove(0);
      for (String s : split) {
        OWLAnnotationProperty ap2 = checker.getOWLAnnotationProperty(s, true);
        if (ap2 == null) {
          logger.error(String.format("Unable to create annotation property from '%s'", s));
          continue;
        }
        if (apOpt.equalsIgnoreCase("mapto")) {
          mapToAnnotations.put(ap, ap2);
        } else if (apOpt.equalsIgnoreCase("copyto")) {
          copyToAnnotations.put(ap, ap2);
        }
      }
    }

    // Map options from list to string
    Map<String, String> extractOptions = new HashMap<>();
    for (String key : getDefaultOptions().keySet()) {
      if (options.containsKey(key)) {
        String o = options.get(key).get(0);
        extractOptions.put(key, o);
      } else {
        extractOptions.put(key, getDefaultOptions().get(key));
      }
    }

    IRI outputIRI = null;
    if (options.containsKey("output-iri")) {
      String iriString = options.get("output-iri").get(0);
      outputIRI = IRI.create(iriString);
    }

    String method = options.get("method").get(0);
    ModuleType m;
    OWLOntology outputOntology;
    switch (method.toLowerCase()) {
      case "star":
        m = ModuleType.STAR;
        outputOntology =
            ExtractOperation.extract(inputOntology, terms, outputIRI, m, extractOptions);
        break;
      case "top":
        m = ModuleType.TOP;
        outputOntology =
            ExtractOperation.extract(inputOntology, terms, outputIRI, m, extractOptions);
        break;
      case "bot":
        m = ModuleType.BOT;
        outputOntology =
            ExtractOperation.extract(inputOntology, terms, outputIRI, m, extractOptions);
        break;
      default:
        throw new Exception(invalidMethodError);
    }

    updateExtractedModule(
        outputOntology, terms, copyToAnnotations, mapToAnnotations, replaceParents);
    return outputOntology;
  }

  /**
   * Perform a 'mireot-rdfxml' extraction using parameters from a configuration file. This method
   * never loads the ontology object, just parses XML. This is recommended for very large
   * ontologies.
   *
   * @param ioHelper IOHelper to handle creating IRIs
   * @param options Map of options from config file
   * @return extracted subset
   * @throws Exception on any problem
   */
  public static OWLOntology mireotRDFXMLExtractFromConfig(
      IOHelper ioHelper, Map<String, List<String>> options) throws Exception {
    // Make sure we have terms to extract
    if (!options.containsKey("terms")) {
      throw new Exception(missingTermsInConfigError);
    }
    if (options.containsKey("lower-terms")) {
      throw new Exception(String.format(invalidTermsInConfigError, "lower-terms"));
    }
    if (options.containsKey("upper-terms")) {
      throw new Exception(String.format(invalidTermsInConfigError, "upper-terms"));
    }

    // Get an input
    String inputPath = null;
    IRI inputIRI = null;
    if (options.containsKey("input")) {
      inputPath = options.get("input").get(0);
    } else if (options.containsKey("input-iri")) {
      inputIRI = IRI.create(options.get("input-iri").get(0));
    } else {
      throw new Exception(missingInputInConfigError);
    }

    // Maybe get a target
    // TODO - maybe allow simple parsing for this too
    OWLOntology target = null;
    if (options.containsKey("target")) {
      String targetPath = options.get("target").get(0);
      target = ioHelper.loadOntology(targetPath);
    } else if (options.containsKey("target-iri")) {
      IRI targetIRI = IRI.create(options.get("target-iri").get(0));
      target = ioHelper.loadOntology(targetIRI);
    }

    // If we have a target, we can init a checker
    QuotedEntityChecker checker = null;
    if (target != null) {
      checker = new QuotedEntityChecker();
      checker.setIOHelper(ioHelper);
      checker.addProperty(dataFactory.getRDFSLabel());
      checker.addAll(target);
    }

    IRI outputIRI = null;
    if (options.containsKey("output-iri")) {
      String outputIRIString = options.get("output-iri").get(0);
      outputIRI = IRI.create(outputIRIString);
    }

    // Init XML Helper
    XMLHelper xmlHelper;
    if (inputPath != null) {
      xmlHelper = new XMLHelper(inputPath, outputIRI);
    } else {
      // IRI should never be null here
      assert inputIRI != null;
      xmlHelper = new XMLHelper(inputIRI, outputIRI);
    }

    // Parse terms
    Set<IRI> terms = new HashSet<>();
    Map<OWLEntity, Set<OWLEntity>> replaceParents = new HashMap<>();
    for (String termLine : options.get("terms")) {
      List<String> split = Lists.newArrayList(termLine.split("\t"));
      String termString = split.remove(0).trim();

      // Try to get an IRI
      IRI iri = xmlHelper.getIRI(termString);
      if (iri == null) {
        iri = ioHelper.createIRI(termString);
      }
      if (iri == null) {
        logger.error(String.format("Unable to create IRI from '%s'", termString));
        continue;
      }

      EntityType<?> et = xmlHelper.getEntityType(iri);
      if (et == null) {
        logger.error(
            String.format(
                "Unable to create entity from '%s' - check that this entity exists in the input ontology!",
                termString));
        continue;
      }
      terms.add(iri);
      OWLEntity e = dataFactory.getOWLEntity(et, iri);

      if (split.isEmpty()) {
        continue;
      }

      // IF there are remaining splits, add them as replacement parents
      Set<OWLEntity> replaceParentsForCurrent = new HashSet<>();
      for (String s : split) {
        if (s.trim().equals("")) {
          continue;
        }
        IRI iri2 = xmlHelper.getIRI(s);
        if (iri2 == null) {
          iri2 = ioHelper.createIRI(s);
        }

        OWLEntity e2 = null;
        if (iri2 == null && checker != null) {
          // Maybe get the entity from the target ontology
          e2 = checker.getOWLEntity(s);
        } else if (iri2 == null) {
          // No target, no IRI -> we cannot add
          logger.error(String.format("Unable to create IRI from '%s'", s));
          continue;
        }

        if (e2 == null && iri2 != null) {
          // If the entity wasn't found in target, but has an IRI,
          // we can try to create an entity from input ontology
          EntityType<?> et2 = xmlHelper.getEntityType(iri2);
          if (et2 == null) {
            logger.error(
                String.format(
                    "Unable to create entity from '%s' - check that this entity exists in the input ontology!",
                    s));
            continue;
          }
          e2 = dataFactory.getOWLEntity(et2, iri2);
        }

        replaceParentsForCurrent.add(e2);
      }
      if (!replaceParentsForCurrent.isEmpty()) {
        replaceParents.put(e, replaceParentsForCurrent);
      }
    }

    // Parse annotation properties
    Set<IRI> annotationProperties = new HashSet<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> mapToAnnotations = new HashMap<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> copyToAnnotations = new HashMap<>();
    for (String apLine : options.getOrDefault("annotations", new ArrayList<>())) {
      List<String> split = Lists.newArrayList(apLine.split("\t"));
      String apString = split.remove(0).trim();

      // Try to create an annotation property from the string
      OWLAnnotationProperty ap = getAnnotationProperty(ioHelper, xmlHelper, checker, apString);
      if (ap == null) {
        logger.error(String.format("Unable to create annotation property from '%s'", apString));
        continue;
      }
      annotationProperties.add(ap.getIRI());

      if (split.isEmpty()) {
        continue;
      }

      String apOpt = split.remove(0);
      for (String s : split) {
        OWLAnnotationProperty ap2 = getAnnotationProperty(ioHelper, xmlHelper, checker, s);
        if (ap2 == null) {
          logger.error(String.format("Unable to create annotation property from '%s'", apString));
          continue;
        }
        if (apOpt.equalsIgnoreCase("mapto")) {
          mapToAnnotations.put(ap, ap2);
        } else if (apOpt.equalsIgnoreCase("copyto")) {
          copyToAnnotations.put(ap, ap2);
        }
      }
    }

    // Map options from list to string
    Map<String, String> extractOptions = new HashMap<>();
    for (String key : getDefaultOptions().keySet()) {
      if (options.containsKey(key)) {
        String o = options.get(key).get(0);
        extractOptions.put(key, o);
      } else {
        extractOptions.put(key, getDefaultOptions().get(key));
      }
    }

    // Create the output ontology
    OWLOntology outputOntology = xmlHelper.extract(terms, annotationProperties, extractOptions);

    updateExtractedModule(
        outputOntology, terms, copyToAnnotations, mapToAnnotations, replaceParents);
    return outputOntology;
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

  private static OWLAnnotationProperty getAnnotationProperty(
      IOHelper ioHelper, XMLHelper xmlHelper, QuotedEntityChecker checker, String apString) {
    IRI iri = xmlHelper.getIRI(apString);
    if (iri == null) {
      iri = ioHelper.createIRI(apString);
    }

    if (iri == null) {
      // Maybe find it in the target ontology
      return checker.getOWLAnnotationProperty(apString);
    } else {
      return dataFactory.getOWLAnnotationProperty(iri);
    }
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
      } else {
        String baseStr = iri.substring(0, iri.lastIndexOf("/")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
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

  /**
   * Update an extracted module. First copy needed annotations, keeping original annotations. Then
   * map new annotations, removing the original annotations. Finally, update parents by replacing
   * old parents with new parents. If terms are dangling after updating parents (and they are not in
   * the target IRIs), remove these.
   *
   * @param outputOntology OWLOntology to update
   * @param terms Set of IRIs to keep after updating parents
   * @param copyToAnnotations Map of annotation properties to copy
   * @param mapToAnnotations Map of annotation properties to replace
   * @param replaceParents Map of OWLEntity child -> new parent
   * @throws Exception on any problem
   */
  protected static void updateExtractedModule(
      OWLOntology outputOntology,
      Set<IRI> terms,
      Map<OWLAnnotationProperty, OWLAnnotationProperty> copyToAnnotations,
      Map<OWLAnnotationProperty, OWLAnnotationProperty> mapToAnnotations,
      Map<OWLEntity, Set<OWLEntity>> replaceParents)
      throws Exception {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    // Handle annotation options
    if (!copyToAnnotations.isEmpty()) {
      OntologyHelper.copyAnnotationObjects(outputOntology, copyToAnnotations);
    }
    if (!mapToAnnotations.isEmpty()) {
      OntologyHelper.mapAnnotationObjects(outputOntology, mapToAnnotations);
    }

    // Handle parent replacements
    if (!replaceParents.isEmpty()) {
      OntologyHelper.replaceParents(outputOntology, replaceParents);
      // Clean up ontology after moving terms around
      // If there is a dangling term not in target, remove it
      Set<OWLObject> remove = new HashSet<>();
      for (OWLClass c : outputOntology.getClassesInSignature()) {
        Collection<OWLSubClassOfAxiom> subAxs = outputOntology.getSubClassAxiomsForSubClass(c);
        // Remove subclass of OWL Thing
        subAxs.remove(dataFactory.getOWLSubClassOfAxiom(c, dataFactory.getOWLThing()));
        Collection<OWLSubClassOfAxiom> superAxs = outputOntology.getSubClassAxiomsForSuperClass(c);
        if (subAxs.isEmpty() && superAxs.isEmpty()) {
          if (!terms.contains(c.getIRI())) {
            remove.add(c);
          }
        }
      }
      Set<OWLAxiom> removeAxs = RelatedObjectsHelper.getPartialAxioms(outputOntology, remove, null);
      manager.removeAxioms(outputOntology, removeAxs);
    }
  }
}
