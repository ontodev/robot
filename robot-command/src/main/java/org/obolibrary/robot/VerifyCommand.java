package org.obolibrary.robot;

import com.google.common.io.Files;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.obolibrary.robot.exceptions.CannotReadQuery;
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
    return "Runs a sparql query on an ontology. "
        + "Any results of the query are violations, counted, and reported";
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
      CommandLineHelper.handleException(getUsage(), getOptions(), e);
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
    DatasetGraph graph = QueryOperation.loadOntology(state.getOntology());

    File outputDir = new File(CommandLineHelper.getDefaultValue(line, "output-dir", "."));

    Map<File, Tuple<ResultSetRewindable, OutputStream>> resultMap = new HashMap<>();
    String[] queryFilePaths = line.getOptionValues("queries");
    if (queryFilePaths.length == 0) {
      throw new IllegalArgumentException(missingQueryError);
    }
    for (String filePath : queryFilePaths) {
      File query = new File(filePath);
      ResultSet results = QueryOperation.execQuery(graph, fileContents(query));
      ResultSetRewindable resultsCopy = ResultSetFactory.copyResults(results);
      String csvPath = FilenameUtils.getBaseName(filePath).concat(".csv");
      File resultCsv = outputDir.toPath().resolve(csvPath).toFile();
      if (resultsCopy.size() > 0) {
        resultMap.put(query, new Tuple<>(resultsCopy, new FileOutputStream(resultCsv)));
      } else {
        System.out.println("Rule " + resultCsv.getCanonicalPath() + ": 0 violations");
      }
    }

    boolean violationsExist = QueryOperation.execVerify(resultMap);
    if (violationsExist) {
      System.exit(1);
    }

    return state;
  }

  /**
   * Utility function to get file contents.
   *
   * @param file the file to read
   */
  private static String fileContents(File file) {
    try {
      return Files.toString(file, Charset.defaultCharset());
    } catch (IOException e) {
      String message = "Cannot read from " + file + ": " + e.getMessage();
      // TODO: Is this necessary?
      throw new CannotReadQuery(message, e);
    }
  }
}
