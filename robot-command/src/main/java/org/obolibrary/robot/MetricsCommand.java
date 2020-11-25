package org.obolibrary.robot;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link MetricsOperation}.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MetricsCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MetricsCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "metrics#";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public MetricsCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("f", "format", true, "the metrics result format: tsv," + " json, yaml, html.");
    o.addOption("o", "output", true, "save updated metrics to a file");
    o.addOption(
        "m",
        "metrics",
        true,
        "select which set of metrics you would like to compute: essential (default), extended, all.");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "metrics";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "compute the metrics of an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  // todo: --output?
  public String getUsage() {
    return "robot metrics --input <file> <output>";
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

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

    String metrics_type = CommandLineHelper.getDefaultValue(line, "metrics", "essential");
    String format = CommandLineHelper.getDefaultValue(line, "format", "tsv");
    String output = CommandLineHelper.getOptionalValue(line, "output");

    if (output == null) {
      throw new Exception(String.format(missingFileError, output, "metrics"));
    }

    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);

    MetricsOperation.executeMetrics(state.getOntology(), metrics_type, format, new File(output));

    return state;
  }
}
