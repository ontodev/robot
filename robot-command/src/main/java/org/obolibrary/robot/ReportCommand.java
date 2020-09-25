package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.util.Map;
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

  private static final String NS = "report#";

  private static final String missingOutputError =
      NS + "MISSING OUTPUT ERROR '%s' format requires an --output";

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
    o.addOption("P", "print", true, "specify a number of violations to print");
    o.addOption("t", "tdb", true, "if true, load RDF/XML or TTL onto disk");
    o.addOption("k", "keep-tdb-mappings", true, "if true, do not remove the TDB directory");
    o.addOption("d", "tdb-directory", true, "directory to put TDB mappings (default: .tdb)");
    o.addOption("L", "limit", true, "specify a number of results to limit queries to");
    o.addOption(
        null,
        "standalone",
        true,
        "If true, and the output format is HTML, generate the HTML report as a standalone file (this option is ignored if the output format is not HTML)");

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

    // Override default report options with command-line options
    Map<String, String> reportOptions = ReportOperation.getDefaultOptions();
    for (String option : reportOptions.keySet()) {
      if (line.hasOption(option)) {
        reportOptions.put(option, line.getOptionValue(option));
      }
    }

    // output is optional - no output means the file will not be written anywhere
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");
    String format = CommandLineHelper.getOptionalValue(line, "format");
    if (format != null) {
      if (Lists.newArrayList("html", "json", "yaml").contains(format.toLowerCase())
          && outputPath == null) {
        throw new IllegalArgumentException(String.format(missingOutputError, format));
      }
    }

    boolean success;

    boolean tdb = CommandLineHelper.getBooleanValue(line, "tdb", false);
    if (tdb) {
      // TDB is backed on disk and loads directly from path
      // Avoids extra time loading OWLOntology object for big ontologies
      String inputPath =
          CommandLineHelper.getRequiredValue(line, "input", "An input ontology is required");
      success = ReportOperation.tdbReport(inputPath, outputPath, reportOptions);
    } else {
      state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
      OWLOntology ontology = state.getOntology();
      success = ReportOperation.report(ontology, ioHelper, outputPath, reportOptions);
    }

    // Success is based on failOn
    // If any violations are found of the fail-on level, this will be false
    // If fail-on is "none" or if no violations are found, this will be true
    if (!success) {
      logger.error("Report failed!");
      System.exit(1);
    }
    return state;
  }
}
