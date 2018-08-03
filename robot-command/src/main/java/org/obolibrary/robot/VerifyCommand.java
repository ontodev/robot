package org.obolibrary.robot;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
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
    o.addOption("i", "input", true, "Input Ontology");

    Option queries = new Option("q", "queries", true, "verify one or more SPARQL queries");
    queries.setArgs(Option.UNLIMITED_VALUES);
    o.addOption(queries);

    o.addOption("O", "output-dir", true, "Directory to place reports in");

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

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    // Load into dataset without imports
    Dataset dataset = QueryOperation.loadOntologyAsDataset(state.getOntology(), false);

    File outputDir = new File(CommandLineHelper.getDefaultValue(line, "output-dir", "."));

    String[] queryFilePaths = line.getOptionValues("queries");
    if (queryFilePaths.length == 0) {
      throw new Exception(missingQueryError);
    }
    boolean passing = true;
    for (String filePath : queryFilePaths) {
      File queryFile = new File(filePath);
      String queryString = FileUtils.readFileToString(queryFile);
      String csvPath = FilenameUtils.getBaseName(filePath).concat(".csv");
      boolean result =
          QueryOperation.runVerify(
              dataset, filePath, queryString, outputDir.toPath().resolve(csvPath), null);
      if (result) {
        passing = false;
      }
    }

    if (!passing) {
      throw new Exception(verificationFailed);
    }

    return state;
  }
}
