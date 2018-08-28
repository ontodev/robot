package org.obolibrary.robot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link ReportOperation}.
 *
 * @author cjm
 */
public class ReportCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReportCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public ReportCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save report to a file");
    o.addOption("p", "profile", true, "reporting rules and levels to use");
    o.addOption("f", "format", true, "save report in a given format (TSV or YAML)");
    o.addOption("F", "fail-on", true, "logging level to fail on");
    o.addOption("l", "labels", true, "if true, use labels for output");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "report";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "report terms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot report --input <file> " + "--output <file> ";
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
   * Handle the command-line and file operations for the reportOperation.
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
   * Given an input state and command line arguments, report a new ontology and return an new state.
   * The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state with the reported ontology
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

    // output is optional - no output means the file will not be written anywhere
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");
    // profile is optional - no profile means the default profile will be used
    String profilePath = CommandLineHelper.getOptionalValue(line, "profile");
    // format is optional - default is TSV, but if the prefix of file is 'csv' or 'yaml',
    // that will be automatically detected
    String format = CommandLineHelper.getOptionalValue(line, "format");
    if (format == null && outputPath != null) {
      format = outputPath.substring(outputPath.lastIndexOf(".") + 1);
    }
    // fail-on is optional - if null, will always exit with 0
    String failOn = CommandLineHelper.getDefaultValue(line, "fail-on", "error");
    boolean useLabels = CommandLineHelper.getBooleanValue(line, "labels", false);

    // Success is based on failOn
    // If any violations are found of the fail-on level, this will be false
    // If failOn is "none" or if no violations are found, this will be true
    boolean success =
        ReportOperation.report(
            ontology, ioHelper, profilePath, outputPath, format, failOn, useLabels);
    if (!success) {
      logger.error("Report failed!");
      System.exit(1);
    }
    return state;
  }
}
