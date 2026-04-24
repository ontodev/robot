package org.obolibrary.robot;

import java.io.File;
import java.nio.charset.Charset;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that runs a sparql query expecting zero results. Any results represent violations in the
 * queried ontology
 */
public class VerifyCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(VerifyCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "verify#";

  /** Error message when no query is provided. */
  private static final String missingQueryError =
      NS + "MISSING QUERY ERROR at least one query is required";

  /** Error message when no query is provided. */
  private static final String verificationFailed =
      NS + "VERIFICATION FAILED there were violations of at least one rule";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public VerifyCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("F", "fail-on-violation", true, "logging level to fail on");
    o.addOption("i", "input", true, "Input Ontology");
    o.addOption("O", "output-dir", true, "Directory to place reports in");
    o.addOption("t", "tdb", true, "if true, load RDF/XML or TTL onto disk");
    o.addOption("k", "keep-tdb-mappings", true, "if true, do not remove the TDB directory");
    o.addOption("d", "tdb-directory", true, "directory to put TDB mappings (default: .tdb)");

    Option queries = new Option("q", "queries", true, "verify one or more SPARQL queries");
    queries.setArgs(Option.UNLIMITED_VALUES);
    o.addOption(queries);

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "verify";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "verify an ontology does not violate rules (as queries)";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot verify --input ONTOLOGY " + "--queries FILE [FILE [...]] --output-dir DIR";
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
   * Handle the command-line and file operations for the VerifyOperation.
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
   * Given an input state and command line arguments, verify that the SPARQL queries return no
   * results. The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the unchanged state
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    String tdbDir = CommandLineHelper.getOptionalValue(line, "tdb-directory");
    boolean useTDB = CommandLineHelper.getBooleanValue(line, "tdb", false);
    if (tdbDir != null) {
      useTDB = true;
    }

    boolean passing;
    if (useTDB) {
      passing = executeOnDisk(line);
    } else {
      IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
      state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
      passing = executeInMemory(line, state.getOntology());
    }

    boolean failOnViolation = CommandLineHelper.getBooleanValue(line, "fail-on-violation", true);
    if (!passing && failOnViolation) {
      System.exit(1);
    }

    return state;
  }

  /**
   * Given a command line, create or open a TDB dataset.
   *
   * @param line the command line
   * @return dataset backed by TDB
   * @throws MissingArgumentException if neither input nor TDB directory is provided
   */
  private static Dataset createTDBDataset(CommandLine line) throws MissingArgumentException {
    String inputPath = CommandLineHelper.getOptionalValue(line, "input");
    String tdbDir = CommandLineHelper.getOptionalValue(line, "tdb-directory");
    if (inputPath == null && tdbDir == null) {
      throw new MissingArgumentException(
          "Either an --input or existing --tdb-directory is required");
    }
    if (inputPath != null) {
      if (tdbDir == null) {
        tdbDir = ".tdb";
      }
      // Load an input to a TDB directory
      return IOHelper.loadToTDBDataset(inputPath, tdbDir);
    }

    // Try loading dataset from existing TDB directory
    Dataset dataset = IOHelper.openTDBDataset(tdbDir);
    if (dataset == null) {
      throw new MissingArgumentException(
          "Running verify without an input requires an existing TDB directory");
    }
    return dataset;
  }

  /**
   * Given a command line and ontology, verify the ontology with any options.
   *
   * @param line CommandLine with options
   * @param inputOntology OWLOntology to verify
   * @return true if all queries pass
   * @throws Exception on issue loading ontology or running queries
   */
  private static boolean executeInMemory(CommandLine line, OWLOntology inputOntology)
      throws Exception {
    // Load into dataset without imports
    Dataset dataset = QueryOperation.loadOntologyAsDataset(inputOntology, false);
    try {
      return executeVerify(line, dataset);
    } finally {
      dataset.close();
    }
  }

  /**
   * Given a command line, execute 'verify' using TDB and writing mappings to disk.
   *
   * @param line CommandLine with options
   * @return true if all queries pass
   * @throws Exception on problem running verify
   */
  private static boolean executeOnDisk(CommandLine line) throws Exception {
    Dataset dataset = createTDBDataset(line);
    boolean keepMappings = CommandLineHelper.getBooleanValue(line, "keep-tdb-mappings", false);
    String tdbDir = CommandLineHelper.getDefaultValue(line, "tdb-directory", ".tdb");
    try {
      return executeVerify(line, dataset);
    } finally {
      dataset.close();
      TDBFactory.release(dataset);
      if (!keepMappings) {
        boolean success = IOHelper.cleanTDB(tdbDir);
        if (!success) {
          logger.error(String.format("Unable to remove directory '%s'", tdbDir));
        }
      }
    }
  }

  /**
   * Given a command line and dataset, run all verify queries.
   *
   * @param line CommandLine with options
   * @param dataset Dataset to verify against
   * @return true if all queries pass
   * @throws Exception on issue reading queries or running verify
   */
  private static boolean executeVerify(CommandLine line, Dataset dataset) throws Exception {
    File outputDir = new File(CommandLineHelper.getDefaultValue(line, "output-dir", "."));

    String[] queryFilePaths = line.getOptionValues("queries");
    if (queryFilePaths.length == 0) {
      throw new Exception(missingQueryError);
    }

    boolean passing = true;
    for (String filePath : queryFilePaths) {
      File queryFile = new File(filePath);
      String queryString = FileUtils.readFileToString(queryFile, Charset.defaultCharset());
      String csvPath = FilenameUtils.getBaseName(filePath).concat(".csv");
      boolean result =
          QueryOperation.runVerify(
              dataset, filePath, queryString, outputDir.toPath().resolve(csvPath), null);
      if (result) {
        passing = false;
      }
    }
    return passing;
  }
}
