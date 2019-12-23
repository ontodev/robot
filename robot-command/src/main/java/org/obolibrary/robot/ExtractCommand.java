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
    o.addOption(null, "annotation-property", true, "");
    o.addOption(null, "annotation-properties", true, "");
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

    // Special handling for config file
    String configFile = CommandLineHelper.getOptionalValue(line, "config");
    if (configFile != null) {
      outputOntology = configExtract(ioHelper, configFile);
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    }

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

    // Simple method never loads full ontology
    if (method.equals("simple")) {
      String fileName = CommandLineHelper.getOptionalValue(line, "input");
      String iriString = CommandLineHelper.getOptionalValue(line, "input-iri");
      if (fileName == null && iriString == null) {
        throw new Exception(CommandLineHelper.missingInputsError);
      }
      IRI outputIRI = CommandLineHelper.getOutputIRI(line);

      Set<IRI> terms = CommandLineHelper.getTerms(ioHelper, line, "term", "terms");
      Set<IRI> annotationProperties =
          CommandLineHelper.getTerms(
              ioHelper, line, "annotation-property", "annotation-properties");

      XMLHelper xmlHelper;
      if (fileName != null) {
        xmlHelper = new XMLHelper(fileName, outputIRI);
      } else {
        IRI inputIRI = IRI.create(iriString);
        xmlHelper = new XMLHelper(inputIRI, outputIRI);
      }
      outputOntology = xmlHelper.extract(terms, annotationProperties, extractOptions);

      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    }

    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology inputOntology = state.getOntology();

    if (method.equalsIgnoreCase("mireot")) {
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
   * Perform extraction using parameters from a configuration file.
   *
   * @param ioHelper IOHelper to handle ontology objects
   * @param configFile String path to configuration file
   * @return a new ontology containing the extracted subset
   * @throws Exception on any problem
   */
  private static OWLOntology configExtract(IOHelper ioHelper, String configFile) throws Exception {
    // Get everything from the config file, no need for other options
    File c = new File(configFile);
    if (!c.exists()) {
      throw new IOException(String.format("The config file '%s' does not exist!", configFile));
    }
    Map<String, List<String>> configOptions = parseConfig(FileUtils.readLines(c));
    String method = configOptions.get(METHOD_OPT).get(0);
    switch (method) {
      case "mireot":
        return MireotOperation.mireotFromConfig(ioHelper, configOptions);
      case "simple":
        return ExtractOperation.simpleExtractFromConfig(ioHelper, configOptions);
      case "top":
      case "bot":
      case "star":
        return ExtractOperation.extractFromConfig(ioHelper, configOptions);
      default:
        throw new IllegalArgumentException(
            String.format(
                "'%s' is an unknown extraction method from config file %s", method, configFile));
    }
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
  private static final String IMPORTS_OPT = "imports";
  private static final String ANNOTATE_SOURCE_OPT = "annotate-with-source";
  private static final String LOWER_TERMS_OPT = "lower-terms";
  private static final String UPPER_TERMS_OPT = "upper-terms";
  private static final String TERMS_OPT = "terms";

  /**
   * Parse options from a config file.
   *
   * @param lines List of file lines
   * @return Map of extraction options as string key to list of args
   */
  private static Map<String, List<String>> parseConfig(List<String> lines) {
    Iterator<String> lineItr = lines.iterator();
    Map<String, List<String>> configOptions = new HashMap<>();
    int ln = 0;

    boolean hasInput = false;
    boolean hasTarget = false;

    String currentOption = null;

    while (lineItr.hasNext()) {
      ln++;
      String line = lineItr.next().trim();
      if (line.trim().isEmpty()) {
        continue;
      }

      // '! ' indicates the start of a new option
      if (line.startsWith("! ")) {
        String option = line.substring(2);

        // Add single line options, or set the current option for multi-line tracking
        switch (option.toLowerCase()) {
          case INPUT_OPT:
            // Path to local ontology file
            if (hasInput) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one input on line %d", ln));
            }
            ln++;
            currentOption = null;
            String inputOntologyPath = lineItr.next().trim();
            if (inputOntologyPath.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Input path on line %d is empty!", ln));
            }
            configOptions.put(INPUT_OPT, Lists.newArrayList(inputOntologyPath));
            hasInput = true;
            continue;

          case INPUT_IRI_OPT:
            // IRI for remote ontology file
            if (hasInput) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one input on line %d", ln));
            }
            ln++;
            currentOption = null;
            String inputOntologyIRI = lineItr.next().trim();
            if (inputOntologyIRI.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Input IRI on line %d is empty!", ln));
            }
            configOptions.put(INPUT_IRI_OPT, Lists.newArrayList(inputOntologyIRI));
            hasInput = true;
            continue;

          case OUTPUT_IRI_OPT:
            if (configOptions.containsKey("output-iri")) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one output IRI on line %d", ln));
            }
            ln++;
            currentOption = null;
            String outputIRI = lineItr.next().trim();
            if (outputIRI.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Output IRI on line %d is empty!", ln));
            }
            configOptions.put(OUTPUT_IRI_OPT, Lists.newArrayList(outputIRI));
            continue;

          case TARGET_OPT:
            // Path to local ontology file
            if (hasTarget) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one target on line %d", ln));
            }
            ln++;
            currentOption = null;
            String target = lineItr.next().trim();
            if (target.equals("")) {
              throw new IllegalArgumentException(String.format("Target on line %d is empty!", ln));
            }
            configOptions.put(TARGET_OPT, Lists.newArrayList(target));
            hasTarget = true;
            continue;

          case TARGET_IRI_OPT:
            // Path to local ontology file
            if (hasTarget) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one target on line %d", ln));
            }
            ln++;
            currentOption = null;
            String targetIRI = lineItr.next().trim();
            if (targetIRI.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Target IRI on line %d is empty!", ln));
            }
            configOptions.put(TARGET_IRI_OPT, Lists.newArrayList(targetIRI));
            hasTarget = true;
            continue;

          case METHOD_OPT:
            // Extraction method
            if (configOptions.containsKey("method")) {
              throw new IllegalArgumentException(
                  String.format("Config file contains more than one method on line %d", ln));
            }
            ln++;
            currentOption = null;
            String method = lineItr.next().trim().toLowerCase();
            if (method.equals("")) {
              throw new IllegalArgumentException(String.format("Method on line %d is empty!", ln));
            }
            configOptions.put(METHOD_OPT, Lists.newArrayList(method));
            continue;

          case INTERMEDIATES_OPT:
            // How to handle intermediates
            if (configOptions.containsKey(INTERMEDIATES_OPT)) {
              throw new IllegalArgumentException(
                  String.format(
                      "Config file contains more than one intermediates option on line %d", ln));
            }
            ln++;
            currentOption = null;
            String intermediates = lineItr.next().trim().toLowerCase();
            if (intermediates.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Intermediates option on line %d is empty!", ln));
            }
            configOptions.put(INTERMEDIATES_OPT, Lists.newArrayList(intermediates));
            continue;

          case IMPORTS_OPT:
            // How to handle imports
            if (configOptions.containsKey(IMPORTS_OPT)) {
              throw new IllegalArgumentException(
                  String.format(
                      "Config file contains more than one imports option on line %d", ln));
            }
            ln++;
            currentOption = null;
            String imports = lineItr.next().trim().toLowerCase();
            if (imports.equals("")) {
              throw new IllegalArgumentException(
                  String.format("Imports option on line %d is empty!", ln));
            }
            configOptions.put(IMPORTS_OPT, Lists.newArrayList(imports));
            continue;

          case ANNOTATE_SOURCE_OPT:
            // How to handle imports
            if (configOptions.containsKey(ANNOTATE_SOURCE_OPT)) {
              throw new IllegalArgumentException(
                  String.format(
                      "Config file contains more than one annotate-with-source option on line %d",
                      ln));
            }
            ln++;
            currentOption = null;
            String annotateSource = lineItr.next().trim();
            if (annotateSource.equals("")) {
              throw new IllegalArgumentException(
                  String.format("annotate-with-source option on line %d is empty!", ln));
            }
            configOptions.put(ANNOTATE_SOURCE_OPT, Lists.newArrayList(annotateSource));
            continue;

          default:
            // Set option and hop to next line
            currentOption = option;
            continue;
        }
      }

      // Multi-line option handling
      // Always separated by tabs
      if (currentOption != null) {
        switch (currentOption) {
          case ANNOTATIONS_OPT:
            // Add this annotation property to list of APs to extract
            List<String> aps = configOptions.getOrDefault(ANNOTATIONS_OPT, new ArrayList<>());
            aps.add(line);
            configOptions.put(ANNOTATIONS_OPT, aps);
            break;
          case UPPER_TERMS_OPT:
            List<String> uts = configOptions.getOrDefault(UPPER_TERMS_OPT, new ArrayList<>());
            uts.add(line);
            configOptions.put(UPPER_TERMS_OPT, uts);
            break;
          case LOWER_TERMS_OPT:
            List<String> lts = configOptions.getOrDefault(LOWER_TERMS_OPT, new ArrayList<>());
            lts.add(line);
            configOptions.put(LOWER_TERMS_OPT, lts);
            break;
          case TERMS_OPT:
            List<String> ts = configOptions.getOrDefault(TERMS_OPT, new ArrayList<>());
            ts.add(line);
            configOptions.put(TERMS_OPT, ts);
            break;
          default:
            throw new IllegalArgumentException(
                String.format("'%s' is an unknown option on line %d", currentOption, ln));
        }
      }
    }

    // Make sure we have required arguments
    if (!hasInput) {
      throw new IllegalArgumentException("An input or input-iri is required!");
    }
    if (!configOptions.containsKey("method")) {
      throw new IllegalArgumentException("A method is required!");
    }

    // Add defaults where necessary
    if (!configOptions.containsKey(INTERMEDIATES_OPT)) {
      configOptions.put(INTERMEDIATES_OPT, Lists.newArrayList("all"));
    }
    if (!configOptions.containsKey(IMPORTS_OPT)) {
      configOptions.put(IMPORTS_OPT, Lists.newArrayList("include"));
    }
    if (!configOptions.containsKey(ANNOTATE_SOURCE_OPT)) {
      configOptions.put(ANNOTATE_SOURCE_OPT, Lists.newArrayList("true"));
    }

    return configOptions;
  }
}
