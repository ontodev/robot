package org.obolibrary.robot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link QueryOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class QueryCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "query#";

  /** Error message when a query is not provided */
  private static final String missingQueryError =
      NS + "MISSING QUERY ERROR at least one query must be provided";

  /** Error message when an invalid --imports option is provided */
  private static final String importsOptionError =
      NS + "IMPORTS OPTION ERROR --imports must be union, graphs, or ignore.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public QueryCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("f", "format", true, "the query result format: CSV, TSV," + " TTL, JSONLD, etc.");
    o.addOption("O", "output-dir", true, "Directory for output");
    o.addOption("g", "use-graphs", true, "if true, load imports as named graphs");

    Option opt;

    opt = new Option("s", "select", true, "run a SPARQL SELECT query (deprecated)");
    opt.setArgs(2);
    o.addOption(opt);

    opt = new Option("c", "construct", true, "run a SPARQL CONSTRUCT query (deprecated)");
    opt.setArgs(2);
    o.addOption(opt);

    opt = new Option("q", "query", true, "run a SPARQL query");
    opt.setArgs(2);
    o.addOption(opt);

    opt = new Option("Q", "queries", true, "verify one or more SPARQL queries");
    opt.setArgs(Option.UNLIMITED_VALUES);
    o.addOption(opt);

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "query";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "query an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot query --input <file> --query <query> <output>";
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
   * Handle the command-line and file operations for the QueryOperation.
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
   * Given an input state and command line arguments, query the ontolgy. The input ontology is not
   * changed.
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

    String format = CommandLineHelper.getOptionalValue(line, "format");
    String outputDir = CommandLineHelper.getDefaultValue(line, "output-dir", "");
    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);

    // Determine what to do with the imports and create a new dataset
    boolean useGraphs = CommandLineHelper.getBooleanValue(line, "use-graphs", false);
    Dataset dataset;
    if (useGraphs) {
      dataset = QueryOperation.loadOntologyAsDataset(state.getOntology(), true);
    } else {
      dataset = QueryOperation.loadOntologyAsDataset(state.getOntology(), false);
    }

    // Collect all queries as (queryPath, outputPath) pairs.
    List<List<String>> queries = new ArrayList<>();
    List<String> qs = CommandLineHelper.getOptionalValues(line, "query");
    for (int i = 0; i < qs.size(); i += 2) {
      queries.add(qs.subList(i, i + 2));
    }
    qs = CommandLineHelper.getOptionalValues(line, "select");
    for (int i = 0; i < qs.size(); i += 2) {
      queries.add(qs.subList(i, i + 2));
    }
    qs = CommandLineHelper.getOptionalValues(line, "construct");
    for (int i = 0; i < qs.size(); i += 2) {
      queries.add(qs.subList(i, i + 2));
    }
    qs = CommandLineHelper.getOptionalValues(line, "queries");
    for (String q : qs) {
      List<String> xs = new ArrayList<>();
      xs.add(q);
      xs.add(null);
      queries.add(xs);
    }

    if (queries.isEmpty()) {
      throw new IllegalArgumentException(missingQueryError);
    }

    // Run queries
    for (List<String> q : queries) {
      String queryPath = q.get(0);
      String outputPath = q.get(1);

      String query = FileUtils.readFileToString(new File(queryPath));

      String formatName = format;
      if (formatName == null) {
        if (outputPath == null) {
          formatName = QueryOperation.getDefaultFormatName(query);
        } else {
          formatName = FilenameUtils.getExtension(outputPath);
        }
      }

      if (outputPath == null) {
        String fileName = FilenameUtils.getBaseName(queryPath) + "." + formatName;
        outputPath = new File(outputDir).toPath().resolve(fileName).toString();
      }

      OutputStream output = new FileOutputStream(outputPath);
      QueryOperation.runSparqlQuery(dataset, query, formatName, output);
    }
    return state;
  }
}
