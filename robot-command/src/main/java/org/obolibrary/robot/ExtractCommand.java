package org.obolibrary.robot;

import com.google.common.collect.Lists;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.*;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * Handles inputs and outputs for the {@link ExtractOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractCommand implements Command {
  /** Namespace for error messages. */
  private static final String NS = "extract#";

  /** Error message when lower or branch terms are not specified with MIREOT. */
  private static final String missingMireotTermsError =
      NS
          + "MISSING MIREOT TERMS ERROR "
          + "either lower term(s) or branch term(s) must be specified for MIREOT";

  /** Error message when user provides invalid extraction method. */
  private static final String invalidMethodError =
      NS + "INVALID METHOD ERROR method must be: MIREOT, STAR, TOP, BOT";

  /** Error message when a MIREOT option is used for SLME. */
  private static final String invalidOptionError =
      NS
          + "INVALID OPTION ERROR "
          + "only --term or --term-file can be used to specify extract term(s) "
          + "for STAR, TOP, or BOT";

  /** Error message when the source map is not TSV or CSV. */
  private static final String invalidSourceMapError =
      NS + "INVALID SOURCE MAP ERROR --sources input must be .tsv or .csv";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public ExtractCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("C", "config", true, "load extract options from configuration file");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "output-iri", true, "set OntologyIRI for output");
    o.addOption("m", "method", true, "extract method to use");
    o.addOption("t", "term", true, "term to extract");
    o.addOption("T", "term-file", true, "load terms from a file");
    o.addOption("u", "upper-term", true, "upper level term to extract");
    o.addOption("U", "upper-terms", true, "upper level terms to extract");
    o.addOption("l", "lower-term", true, "lower level term to extract");
    o.addOption("L", "lower-terms", true, "lower level terms to extract");
    o.addOption("b", "branch-from-term", true, "root term of branch to extract");
    o.addOption("B", "branch-from-terms", true, "root terms of branches to extract");
    o.addOption("c", "copy-ontology-annotations", true, "if true, include ontology annotations");
    o.addOption("f", "force", true, "if true, warn on empty input terms instead of fail");
    o.addOption("a", "annotate-with-source", true, "if true, annotate terms with rdfs:isDefinedBy");
    o.addOption("s", "sources", true, "specify a mapping file of term to source ontology");
    o.addOption("n", "individuals", true, "handle individuals (default: include)");
    o.addOption("M", "imports", true, "handle imports (default: include)");
    o.addOption("N", "intermediates", true, "specify how to handle intermediate entities");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "extract";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "extract terms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot extract --input <file> "
        + "--term-file <file> "
        + "--output <file> "
        + "--output-iri <iri>";
  }

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations for the ExtractOperation.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(e);
    }
  }

  /**
   * Given an input state and command line arguments, extract a new ontology and return an new
   * state. The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state with the extracted ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    OWLOntology outputOntology;

    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

    String configFile = CommandLineHelper.getOptionalValue(line, "config");
    if (configFile != null) {
      // Get everything from the config file, no need for other options
      File c = new File(configFile);
      if (!c.exists()) {
        // TODO - config file does not exit
        throw new IOException();
      }
      outputOntology = parseInputConfiguration(ioHelper, FileUtils.readLines(c));
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    }

    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology inputOntology = state.getOntology();

    // Override default reasoner options with command-line options
    Map<String, String> extractOptions = ExtractOperation.getDefaultOptions();
    for (String option : extractOptions.keySet()) {
      if (line.hasOption(option)) {
        extractOptions.put(option, line.getOptionValue(option));
      }
    }

    // Get method, make sure it has been specified
    String method =
        CommandLineHelper.getRequiredValue(line, "method", "method of extraction must be specified")
            .trim()
            .toLowerCase();

    ModuleType moduleType = null;
    switch (method) {
      case "star":
        moduleType = ModuleType.STAR;
        break;
      case "top":
        moduleType = ModuleType.TOP;
        break;
      case "bot":
        moduleType = ModuleType.BOT;
        break;
    }

    if (method.equals("mireot")) {
      outputOntology = mireotExtract(ioHelper, inputOntology, line, extractOptions);
    } else if (moduleType != null) {
      outputOntology = slmeExtract(ioHelper, inputOntology, moduleType, line, extractOptions);
    } else {
      throw new Exception(invalidMethodError);
    }

    // Maybe copy ontology annotations
    boolean copyOntologyAnnotations =
        CommandLineHelper.getBooleanValue(line, "copy-ontology-annotations", false);
    if (copyOntologyAnnotations) {
      for (OWLAnnotation annotation : inputOntology.getAnnotations()) {
        OntologyHelper.addOntologyAnnotation(outputOntology, annotation);
      }
    }

    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    state.setOntology(outputOntology);
    return state;
  }

  /**
   * Perform a MIREOT extraction on an ontology after validating command line options.
   *
   * @param ioHelper IOHelper to use
   * @param inputOntology OWLOntology to extract from
   * @param line CommandLine with options
   * @param extractOptions Map of extract options
   * @return a new ontology containing extracted subset
   * @throws IOException on problem parsing terms
   * @throws OWLOntologyCreationException on OWLAPI issue
   */
  private static OWLOntology mireotExtract(
      IOHelper ioHelper,
      OWLOntology inputOntology,
      CommandLine line,
      Map<String, String> extractOptions)
      throws Exception {
    // Get terms from input (ensuring that they are in the input ontology)
    // It's okay for any of these to return empty (allowEmpty = true)
    // Checks for empty sets later
    Set<IRI> upperIRIs =
        OntologyHelper.filterExistingTerms(
            inputOntology,
            CommandLineHelper.getTerms(ioHelper, line, "upper-term", "upper-terms"),
            true);
    if (upperIRIs.size() == 0) {
      upperIRIs = null;
    }
    Set<IRI> lowerIRIs =
        OntologyHelper.filterExistingTerms(
            inputOntology,
            CommandLineHelper.getTerms(ioHelper, line, "lower-term", "lower-terms"),
            true);
    if (lowerIRIs.size() == 0) {
      lowerIRIs = null;
    }
    Set<IRI> branchIRIs =
        OntologyHelper.filterExistingTerms(
            inputOntology,
            CommandLineHelper.getTerms(ioHelper, line, "branch-from-term", "branch-from-terms"),
            true);
    if (branchIRIs.size() == 0) {
      branchIRIs = null;
    }

    // Need branch IRIs or lower IRIs to proceed
    if (branchIRIs == null && lowerIRIs == null) {
      throw new IllegalArgumentException(missingMireotTermsError);
    }

    Map<IRI, IRI> sourceMap =
        getSourceMap(ioHelper, CommandLineHelper.getOptionalValue(line, "sources"));

    // Create the output ontology
    OWLOntology outputOntology =
        MireotOperation.mireot(
            inputOntology, lowerIRIs, upperIRIs, branchIRIs, extractOptions, sourceMap, null);

    // Get the output IRI and create the output ontology
    IRI outputIRI = CommandLineHelper.getOutputIRI(line);
    if (outputIRI == null) {
      outputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();
    }
    if (outputIRI != null) {
      outputOntology.getOWLOntologyManager().setOntologyDocumentIRI(outputOntology, outputIRI);
    }
    return outputOntology;
  }

  /**
   * Perform a SLME extraction after validating command line options.
   *
   * @param inputOntology OWLOntology to extract from
   * @param moduleType type of extraction
   * @param line CommandLine with options
   * @param extractOptions Map of extract options
   * @return a new ontology containing extracted subset
   * @throws IOException on issue parsing terms
   * @throws OWLOntologyCreationException on OWLAPI issue
   */
  private static OWLOntology slmeExtract(
      IOHelper ioHelper,
      OWLOntology inputOntology,
      ModuleType moduleType,
      CommandLine line,
      Map<String, String> extractOptions)
      throws Exception {
    // upper-term, lower-term, and branch-from term should not be used
    List<String> mireotTerms =
        Arrays.asList(
            CommandLineHelper.getOptionalValue(line, "upper-term"),
            CommandLineHelper.getOptionalValue(line, "upper-terms"),
            CommandLineHelper.getOptionalValue(line, "lower-term"),
            CommandLineHelper.getOptionalValue(line, "lower-terms"),
            CommandLineHelper.getOptionalValue(line, "branch-from-term"),
            CommandLineHelper.getOptionalValue(line, "branch-from-terms"));
    for (String mt : mireotTerms) {
      if (mt != null) {
        throw new IllegalArgumentException(invalidOptionError);
      }
    }
    // Make sure the terms exist in the input ontology
    Set<IRI> terms =
        OntologyHelper.filterExistingTerms(
            inputOntology,
            CommandLineHelper.getTerms(ioHelper, line),
            OptionsHelper.optionIsTrue(extractOptions, "force"));

    // Determine what to do with sources
    Map<IRI, IRI> sourceMap =
        getSourceMap(ioHelper, CommandLineHelper.getOptionalValue(line, "sources"));
    // Get the output IRI
    IRI outputIRI = CommandLineHelper.getOutputIRI(line);
    if (outputIRI == null) {
      outputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();
    }

    return ExtractOperation.extract(
        inputOntology, terms, outputIRI, moduleType, extractOptions, sourceMap);
  }

  /**
   * Given an IOHelper and the path to a term-to-source map, return a map of term IRI to source IRI.
   *
   * @param ioHelper IOHelper to handle prefixes
   * @param sourceMapPath path of the term-to-source map
   * @return map of term IRI to source IRI
   * @throws Exception on file reading issue
   */
  private static Map<IRI, IRI> getSourceMap(IOHelper ioHelper, String sourceMapPath)
      throws Exception {
    // If no source map path is specified, just return null
    if (sourceMapPath == null) {
      return null;
    }

    // Otherwise, use the path to get a file containing the mappings
    File sourceMapFile = new File(sourceMapPath);
    if (!sourceMapFile.exists()) {
      throw new Exception(String.format(missingFileError, sourceMapPath, "--sources"));
    }

    char separator;
    if (sourceMapPath.endsWith(".tsv")) {
      separator = '\t';
    } else if (sourceMapPath.endsWith(".csv")) {
      separator = ',';
    } else {
      throw new Exception(invalidSourceMapError);
    }

    DefaultPrefixManager pm = ioHelper.getPrefixManager();

    Reader reader = new FileReader(sourceMapFile);
    CSVReader csv =
        new CSVReaderBuilder(reader)
            .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
            .build();
    // Skip first line
    csv.skip(1);

    Map<IRI, IRI> sourceMap = new HashMap<>();
    for (String[] line : csv) {
      IRI entity = ioHelper.createIRI(line[0]);

      // Maybe create a source IRI from a prefix
      // Otherwise the full IRI should be provided
      IRI source;
      String sourceStr = line[1];
      String namespace = pm.getPrefix(sourceStr + ":");
      if (namespace != null) {
        if (namespace.endsWith("_") || namespace.endsWith("#") || namespace.endsWith("/")) {
          namespace = namespace.substring(0, namespace.length() - 1);
        }
        source = IRI.create(namespace.toLowerCase() + ".owl");
      } else {
        source = IRI.create(sourceStr);
      }
      sourceMap.put(entity, source);
    }

    return sourceMap;
  }

  private static final String INPUT_OPT = "input";
  private static final String INPUT_IRI_OPT = "input-iri";
  private static final String OUTPUT_IRI_OPT = "output-iri";
  private static final String TARGET_OPT = "target";
  private static final String TARGET_IRI_OPT = "target-iri";
  private static final String METHOD_OPT = "method";
  private static final String ANNOTATIONS_OPT = "annotations";
  private static final String INTERMEDIATES_OPT = "intermediates";
  private static final String LOWER_TERMS_OPT = "lower-terms";
  private static final String UPPER_TERMS_OPT = "upper-terms";
  private static final String TERMS_OPT = "terms";
  private static final String PARENTS_OPT = "parents";

  private static OWLOntology parseInputConfiguration(IOHelper ioHelper, List<String> lines)
      throws Exception {

    String currentOption = null;
    Iterator<String> lineItr = lines.iterator();

    OWLOntology inputOntology = null;
    IRI inputIRI = null;
    OWLOntology targetOntology = null;
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());

    IRI outputIRI = null;
    String method = null;
    String intermediates = null;

    Set<OWLAnnotationProperty> annotationProperties = new HashSet<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> mapToAnnotations = new HashMap<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> copyToAnnotations = new HashMap<>();
    Map<IRI, IRI> sourceMap = new HashMap<>();
    Set<IRI> upperTerms = new HashSet<>();
    Set<IRI> lowerTerms = new HashSet<>();
    Set<IRI> terms = new HashSet<>();
    Map<OWLEntity, Set<OWLEntity>> replaceParents = new HashMap<>();

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();

    while (lineItr.hasNext()) {
      String line = lineItr.next().trim();
      if (line.trim().isEmpty()) {
        continue;
      }
      if (line.startsWith("! ")) {
        String option = line.substring(2);

        switch (option.toLowerCase()) {
          case INPUT_OPT:
            // Path to local ontology file
            if (inputOntology != null) {
              // TODO - already has input
              throw new Exception();
            }
            String path = lineItr.next().trim();
            inputOntology = ioHelper.loadOntology(path);
            inputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();

            // Use input ontology to get labels
            checker.addAll(inputOntology);
            continue;

          case INPUT_IRI_OPT:
            // IRI for remote ontology file
            if (inputOntology != null) {
              // TODO - already has input
              throw new Exception();
            }
            String iri = lineItr.next().trim();
            inputIRI = IRI.create(iri);
            inputOntology = ioHelper.loadOntology(inputIRI);

            // Use input ontology to get labels
            checker.addAll(inputOntology);
            continue;

          case OUTPUT_IRI_OPT:
            outputIRI = IRI.create(lineItr.next().trim());
            continue;

          case TARGET_OPT:
            // Path to local ontology file
            if (targetOntology != null) {
              // TODO - already has input
              throw new Exception();
            }
            String targetPath = lineItr.next().trim();
            targetOntology = ioHelper.loadOntology(targetPath);

            // Use target ontology to add to labels
            checker.addAll(targetOntology);
            continue;

          case TARGET_IRI_OPT:
            // IRI for remote ontology file
            if (targetOntology != null) {
              // TODO - already has input
              throw new Exception();
            }
            String targetIRI = lineItr.next().trim();
            targetOntology = ioHelper.loadOntology(IRI.create(targetIRI));

            // Use target ontology to add to labels
            checker.addAll(targetOntology);
            continue;

          case METHOD_OPT:
            // Extraction method
            method = lineItr.next().trim();
            continue;

          case INTERMEDIATES_OPT:
            intermediates = lineItr.next().trim();
            continue;

          case ANNOTATIONS_OPT:
          case UPPER_TERMS_OPT:
          case LOWER_TERMS_OPT:
          case TERMS_OPT:
          case PARENTS_OPT:
            // Configuration, hop to next section
            currentOption = option.toLowerCase();
            continue;

          default:
            // TODO - unknown option
            throw new Exception();
        }
      }

      if (currentOption != null) {
        // Always separated by tabs
        List<String> split = Lists.newArrayList(line.split("\t"));
        String term;
        OWLEntity entity;
        switch (currentOption) {
          case ANNOTATIONS_OPT:
            String ap1 = split.remove(0);
            OWLAnnotationProperty sourceAp = checker.getOWLAnnotationProperty(ap1, true);

            // Add to annotations to be extracted
            annotationProperties.add(sourceAp);

            // Maybe do something else with the annotation (copy or map)
            if (split.size() == 1) {
              // TODO - not enough entries
              throw new Exception();

            } else if (split.size() > 1) {
              String annotationOpt = split.remove(0);
              // Iterate over remaining to copy or map
              for (String s : split) {
                if (s.trim().isEmpty()) {
                  continue;
                }
                OWLAnnotationProperty newAP = checker.getOWLAnnotationProperty(s, true);
                if (annotationOpt.equalsIgnoreCase("copyTo")) {
                  copyToAnnotations.put(sourceAp, newAP);
                } else if (annotationOpt.equalsIgnoreCase("mapTo")) {
                  mapToAnnotations.put(sourceAp, newAP);
                }
              }
            }
            break;
          case UPPER_TERMS_OPT:
            term = split.remove(0);
            entity = checker.getOWLEntity(term);
            upperTerms.add(entity.getIRI());

            if (inputIRI != null) {
              sourceMap.put(entity.getIRI(), inputIRI);
            }

            // Maybe add parent or parents
            if (split.size() >= 1) {
              for (String s : split) {
                if (s.trim().isEmpty()) {
                  continue;
                }
                OWLEntity newParent = checker.getOWLEntity(s);
                Set<OWLEntity> newParents = replaceParents.getOrDefault(entity, new HashSet<>());
                newParents.add(newParent);
                replaceParents.put(entity, newParents);
              }
            }
            break;
          case LOWER_TERMS_OPT:
            term = split.remove(0);
            entity = checker.getOWLEntity(term);
            lowerTerms.add(entity.getIRI());

            if (inputIRI != null) {
              sourceMap.put(entity.getIRI(), inputIRI);
            }

            // Maybe add parent or parents
            if (split.size() >= 1) {
              for (String s : split) {
                if (s.trim().isEmpty()) {
                  continue;
                }
                OWLEntity newParent = checker.getOWLEntity(s);
                Set<OWLEntity> newParents = replaceParents.getOrDefault(entity, new HashSet<>());
                newParents.add(newParent);
                replaceParents.put(entity, newParents);
              }
            }
            break;
          case TERMS_OPT:
            term = split.remove(0);
            entity = checker.getOWLEntity(term);
            terms.add(entity.getIRI());

            if (inputIRI != null) {
              sourceMap.put(entity.getIRI(), inputIRI);
            }

            // Maybe add parent or parents
            if (split.size() >= 1) {
              for (String s : split) {
                if (s.trim().isEmpty()) {
                  continue;
                }
                OWLEntity newParent = checker.getOWLEntity(s);
                Set<OWLEntity> newParents = replaceParents.getOrDefault(entity, new HashSet<>());
                newParents.add(newParent);
                replaceParents.put(entity, newParents);
              }
            }
            break;
          default:
            throw new Exception(String.format("'%s' is an unknown option!", currentOption));
        }
      }
    }

    // Sanity checks
    if (inputOntology == null) {
      throw new Exception();
    } else if (method == null) {
      throw new Exception();
    }

    if (method.equalsIgnoreCase("mireot") && lowerTerms.isEmpty()) {
      // mireot without lower terms
      throw new Exception();
    }

    if (!method.equalsIgnoreCase("mireot") && (!lowerTerms.isEmpty() || !upperTerms.isEmpty())) {
      // Non-mireot method with lower/upper terms
      throw new Exception();
    }

    if (!method.equalsIgnoreCase("mireot") && terms.isEmpty()) {
      // Non-mireot without terms
      throw new Exception();
    }

    // Create map of options
    Map<String, String> extractOptions = new HashMap<>();
    if (!sourceMap.isEmpty()) {
      extractOptions.put("annotate-with-source", "true");
    }
    if (intermediates != null) {
      extractOptions.put(INTERMEDIATES_OPT, intermediates);
    }

    // Create the output ontology
    OWLOntology outputOntology;
    ModuleType m;
    switch (method.toLowerCase()) {
      case "mireot":
        // Generate the output ontology
        outputOntology =
            MireotOperation.mireot(
                inputOntology,
                lowerTerms,
                upperTerms,
                null,
                extractOptions,
                sourceMap,
                annotationProperties);
        if (outputIRI != null) {
          // Reset manager
          manager = outputOntology.getOWLOntologyManager();
          manager.setOntologyDocumentIRI(outputOntology, outputIRI);
        }
        break;
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

    // Handle annotation options
    if (!copyToAnnotations.isEmpty()) {
      OntologyHelper.copyAnnotationObjects(outputOntology, copyToAnnotations);
    }
    if (!mapToAnnotations.isEmpty()) {
      OntologyHelper.mapAnnotationObjects(outputOntology, mapToAnnotations);
    }

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
          if (!lowerTerms.contains(c.getIRI())
              && !upperTerms.contains(c.getIRI())
              && !terms.contains(c.getIRI())) {
            remove.add(c);
          }
        }
      }
      Set<OWLAxiom> removeAxs = RelatedObjectsHelper.getPartialAxioms(outputOntology, remove, null);
      manager.removeAxioms(outputOntology, removeAxs);
    }

    return outputOntology;
  }
}
