package org.obolibrary.robot;

import com.google.common.collect.Lists;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class RenameCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RenameCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "rename#";

  /** Error message for incorrect number of headers. */
  private static final String columnHeaderCountError =
      NS + "COLUMN COUNT ERROR file '%s' must have 2 or 3 header values";

  /** Error message when a row does not have exactly two columns. */
  private static final String columnCountError =
      NS + "COLUMN COUNT ERROR line %d in file '%s' must contain exactly %d values.";

  /** Error message when an old IRI value is duplicated in a mappings file. */
  private static final String duplicateMappingError =
      NS
          + "DUPLICATE MAPPING ERROR line %d in file '%s' contains a duplicate mapping for value '%s'.";

  /** Error message when a new IRI value is duplicated and fail-on-duplicates is true. */
  private static final String duplicateRenameError =
      NS + "DUPLICATE RENAME ERROR line %d in file '%s' contains a duplicate rename for value '%s'";

  /** Error message when a file is not in CSV or TSV/TXT format. */
  private static final String fileFormatError =
      NS + "FILE FORMAT ERROR the mappings file ('%s') must be a CSV or TSV/TXT file.";

  /** Error message when a mappings file does not exist. */
  private static final String missingFileError =
      NS + "MISSING FILE ERROR mappings file '%s' does not exist.";

  /** Error message when neither a full or prefix mappings file is provided. */
  private static final String missingMappingsError =
      NS
          + "MISSING MAPPINGS ERROR either a --mappings or a --prefix-mappings file must be specified.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public RenameCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("m", "mappings", true, "table of mappings for renaming");
    o.addOption("r", "prefix-mappings", true, "table of prefix mappings for renaming");
    o.addOption(
        "d",
        "allow-duplicates",
        true,
        "allow two or more terms to be renamed to the same full IRI (default: false)");
    o.addOption(
        "M",
        "allow-missing-entities",
        true,
        "allow mappings for entites that do not appear in the ontology (default: false)");
    o.addOption("A", "add-prefix", true, "add prefix 'foo: http://bar' to the output");

    Option opt = new Option(null, "mapping", true, "term to rename and new IRI");
    opt.setArgs(2);
    o.addOption(opt);

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "rename";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "rename entities based on given mappings";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot rename --input <file> " + "--mappings <file> " + "--output <file>";
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
   * Handle the command-line and file operations.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(getUsage(), getOptions(), e);
    }
  }

  /**
   * Given an input state and command line arguments, rename the entity IRIs in the ontology. Save
   * the ontology and return a new state with the updated ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state with the new ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();

    List<String> mappingStrings = CommandLineHelper.getOptionalValues(line, "mapping");
    List<List<String>> singleMappings = null;
    if (!mappingStrings.isEmpty()) {
      singleMappings = Lists.partition(mappingStrings, 2);
    }
    String fullFile = CommandLineHelper.getOptionalValue(line, "mappings");
    String prefixFile = CommandLineHelper.getOptionalValue(line, "prefix-mappings");
    if (fullFile == null && prefixFile == null && singleMappings == null) {
      throw new IOException(missingMappingsError);
    }

    boolean allowDuplicates = CommandLineHelper.getBooleanValue(line, "allow-duplicates", false);
    boolean allowMissingEntities =
        CommandLineHelper.getBooleanValue(line, "allow-missing-entities", false);

    char separator;
    Map<String, String> mappings = new HashMap<>();
    Map<IRI, String> labels = new HashMap<>();
    // Process full renames
    if (fullFile != null || singleMappings != null) {
      // mappings from file
      if (fullFile != null) {
        separator = getSeparator(fullFile);
        Map<String, List<String>> mappingsAndLabels =
            parseTableMappings(new File(fullFile), separator, allowDuplicates);
        for (Map.Entry<String, List<String>> map : mappingsAndLabels.entrySet()) {
          String newIRI = map.getValue().get(0);
          String newLabel = map.getValue().get(1);
          mappings.put(map.getKey(), newIRI);
          if (newLabel != null) {
            labels.put(ioHelper.createIRI(newIRI), newLabel);
          }
        }
      }
      // mappings from individual args
      if (singleMappings != null) {
        for (List<String> rn : singleMappings) {
          mappings.put(rn.get(0), rn.get(1));
        }
      }
      RenameOperation.renameFull(ontology, ioHelper, mappings, labels, allowMissingEntities);
    }
    // Process prefix renames (no need to fail on duplicates)
    if (prefixFile != null) {
      separator = getSeparator(prefixFile);
      for (Map.Entry<String, List<String>> map :
          parseTableMappings(new File(prefixFile), separator, true).entrySet()) {
        mappings.put(map.getKey(), map.getValue().get(0));
      }
      RenameOperation.renamePrefixes(ontology, ioHelper, mappings);
    }

    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }

  /**
   * Given the file name of a table, return the separator character for cells based on the file
   * extension (CSV = comma, TSV/TXT = tab).
   *
   * @param fileName name of file to get table separator for
   * @return character separator, either tab or comma
   * @throws IOException if file is not TSV/TXT or CSV
   */
  private static char getSeparator(String fileName) throws IOException {
    if (fileName.endsWith(".tsv") || fileName.endsWith(".txt")) {
      return '\t';
    } else if (fileName.endsWith(".csv")) {
      return ',';
    }
    throw new IOException(String.format(fileFormatError, fileName));
  }

  /**
   * Given a file containing the mappings table and a character to separate cells, return the map of
   * old values (left) to new values (right). Partial mappings can allow duplicate values, but full
   * mappings should provide a warning and allow user to kill the process if there are duplicate
   * values.
   *
   * @param mappingsFile File to parse mappings from
   * @param separator character to separate cells in mapping file
   * @param allowDuplicates boolean indicating if duplicate rename (new) should be allowed
   * @return map of old values -> new values
   * @throws Exception on any issue
   */
  private static Map<String, List<String>> parseTableMappings(
      File mappingsFile, char separator, boolean allowDuplicates) throws Exception {
    Map<String, List<String>> mappings = new HashMap<>();
    List<String> newIRIs = new ArrayList<>();

    if (!mappingsFile.exists()) {
      throw new IOException(String.format(missingFileError, mappingsFile.getPath()));
    }

    try (CSVReader reader =
        new CSVReaderBuilder(new FileReader(mappingsFile))
            .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
            .build()) {
      int lineNum = 1;
      // Check the headers for two or three columns
      String[] headers = reader.readNext();
      int headerLen = headers.length;
      if (headerLen < 2 || headerLen > 3) {
        throw new IOException(String.format(columnHeaderCountError, mappingsFile.getPath()));
      }
      for (String[] nextLine : reader) {
        lineNum++;
        if (nextLine.length < 2 || nextLine.length > 3) {
          throw new IOException(
              String.format(columnCountError, lineNum, mappingsFile.getPath(), headerLen));
        }
        String oldIRI = nextLine[0];
        String newIRI = nextLine[1];
        String newLabel = null;
        if (nextLine.length == 3) {
          newLabel = nextLine[2];
          if (newLabel.trim().equals("")) {
            newLabel = null;
          }
        }

        // The OLD IRI has two distinct new IRIs
        if (mappings.containsKey(oldIRI)) {
          throw new IOException(
              String.format(duplicateMappingError, lineNum, mappingsFile.getPath(), oldIRI));
        }
        // The new IRI is being used for one or more OLD IRI
        // This results in a merge
        if (newIRIs.contains(newIRI)) {
          if (!allowDuplicates) {
            throw new Exception(
                String.format(duplicateRenameError, lineNum, mappingsFile.getPath(), newIRI));
          }
          logger.warn(String.format("IRI '%s' will be used for two or more entities", newIRI));
        }

        newIRIs.add(newIRI);
        mappings.put(oldIRI, Lists.newArrayList(newIRI, newLabel));
      }
    }

    return mappings;
  }
}
