package org.obolibrary.robot;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class RenameCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RenameCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "rename#";

  /** Error message when a row does not have exactly two columns. */
  private static final String columnCountError =
      NS + "COLUMN COUNT ERROR line %d in file '%s' must contain exactly two values.";

  /** Error message when an old IRI value is duplicated in a mappings file. */
  private static final String duplicateMappingError =
      NS
          + "DUPLICATE MAPPING ERROR line %d in file '%s' contains a duplicate mapping for value '%s'.";

  /** Error message when a file is not in CSV or TSV/TXT format. */
  private static final String fileFormatError =
      NS + "FILE FORMAT ERROR the mappings file ('%s') must be a CSV or TSV/TXT file.";

  /** Error message when a mappings file does not exist. */
  private static final String missingFileError =
      NS + "MISSING FILE ERROR mappings file '%s' does not exist.";

  /** Error message when neither a full or partial mappings file is provided. */
  private static final String missingMappingsError =
      NS + "MISSING MAPPINGS ERROR either a --full or a --partial mappings file must be specified.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public RenameCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("f", "full", true, "table of mappings for renaming");
    o.addOption("r", "partial", true, "table of partial mappings for renaming");
    o.addOption("A", "add-prefix", true, "add a new prefix to ontology file header");
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
    return "robot rename --input <file> " + "--mapping <file> " + "--output <file>";
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
    OWLOntologyManager manager = ontology.getOWLOntologyManager();

    // TODO - maybe make this a global option?
    // Get additional prefixes to add to prefix manager
    List<String> addPrefixes = CommandLineHelper.getOptionalValues(line, "add-prefix");
    for (String pref : addPrefixes) {
      ioHelper.addPrefix(pref);
    }

    String fullFile = CommandLineHelper.getOptionalValue(line, "full");
    String partialFile = CommandLineHelper.getOptionalValue(line, "partial");
    if (fullFile == null && partialFile == null) {
      throw new IOException(missingMappingsError);
    }

    char separator;
    // Process full renames
    if (fullFile != null) {
      separator = getSeparator(fullFile);
      Map<String, String> mappings = parseTableMappings(new File(fullFile), separator, true);
      RenameOperation.renameFull(ontology, ioHelper, mappings);
    }
    // Process partial renames
    if (partialFile != null) {
      separator = getSeparator(partialFile);
      Map<String, String> mappings = parseTableMappings(new File(partialFile), separator, false);
      RenameOperation.renamePartial(ontology, ioHelper, mappings);
    }

    if (!addPrefixes.isEmpty()) {
      ioHelper.addPrefixesAndSave(ontology, CommandLineHelper.getOutputFile(line), addPrefixes);
    } else {
      CommandLineHelper.maybeSaveOutput(line, ontology);
    }

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
   * @param warnDuplicateValues boolean indicating if duplicate map values should provoke a warning
   * @return map of old values -> new values
   * @throws IOException if the file does not exist, if each row does not have exactly two cells, or
   *     if there are multiple mappings for one key (old value)
   */
  private static Map<String, String> parseTableMappings(
      File mappingsFile, char separator, boolean warnDuplicateValues) throws IOException {
    Map<String, String> mappings = new HashMap<>();

    if (!mappingsFile.exists()) {
      throw new IOException(String.format(missingFileError, mappingsFile.getPath()));
    }

    try (CSVReader reader =
        new CSVReaderBuilder(new FileReader(mappingsFile))
            .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
            .build()) {
      int lineNum = 1;
      // Remove the headers
      reader.readNext();
      for (String[] nextLine : reader) {
        lineNum++;
        if (nextLine.length != 2) {
          throw new IOException(String.format(columnCountError, lineNum, mappingsFile.getPath()));
        }
        if (mappings.containsKey(nextLine[0])) {
          throw new IOException(
              String.format(duplicateMappingError, lineNum, mappingsFile.getPath(), nextLine[0]));
        }
        if (mappings.containsValue(nextLine[1]) && warnDuplicateValues) {
          Scanner s = new Scanner(System.in);
          System.out.println(
              String.format(
                  "---------- WARNING ----------\n"
                      + "Line %d in file '%s' contains a duplicate value ('%s').\n"
                      + "This will rename two separate entities to have the same IRI, resulting in a merge."
                      + "\nDo you wish to continue? [Y/N]",
                  lineNum, mappingsFile.getPath(), nextLine[1]));
          String cont = s.nextLine();
          if (!cont.equalsIgnoreCase("y")) {
            logger.error("rename operation aborted!");
            System.exit(1);
          }
          System.out.println("Continuing rename operation...");
        }
        mappings.put(nextLine[0], nextLine[1]);
      }
    }

    return mappings;
  }
}
