package org.obolibrary.robot;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link AnalyzeOperation}.
 *
 * @author cjm
 */
public class AnalyzeCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(AnalyzeCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public AnalyzeCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("r", "reasoner", true, "reasoner to use: ELK, HermiT, JFact");
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save report to a file");
    o.addOption("f", "format", true, "save report in a given format (TSV or YAML)");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "analyze";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "analyze axioms in an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot analyze --input <file> " + "--output <file> ";
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
   * Handle the command-line and file operations for the AnalyzeOperation.
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

    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);

    // Override default reasoner options with command-line options
    Map<String, String> reasonerOptions = ReasonOperation.getDefaultOptions();
    for (String option : reasonerOptions.keySet()) {
      if (line.hasOption(option)) {
        reasonerOptions.put(option, line.getOptionValue(option));
      }
    }

    // Override default report options with command-line options
    Map<String, String> reportOptions = AnalyzeOperation.getDefaultOptions();
    for (String option : reportOptions.keySet()) {
      if (line.hasOption(option)) {
        reportOptions.put(option, line.getOptionValue(option));
      }
    }

    // output is optional - no output means the file will not be written anywhere
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");

    // Success is based on failOn
    // If any violations are found of the fail-on level, this will be false
    // If fail-on is "none" or if no violations are found, this will be true
    double score = AnalyzeOperation.analyze(ontology, reasonerFactory, outputPath, reportOptions);
    logger.info("Score = " + score);
    return state;
  }
}
