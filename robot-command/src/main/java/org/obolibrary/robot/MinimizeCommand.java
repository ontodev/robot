package org.obolibrary.robot;

import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class MinimizeCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MinimizeCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "minimize#";

  /** Error message when --threshold is not an integer. */
  private static final String thresholdError =
      NS + "THRESHOLD ERROR threshold ('%s') must be a valid integer.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public MinimizeCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "mirror ontology from a file");
    o.addOption("I", "input-iri", true, "mirror ontology from an IRI");
    o.addOption("t", "threshold", true, "threshold to minimize");
    o.addOption("r", "precious", true, "CURIE or IRI of a class to keep");
    o.addOption("R", "precious-terms", true, "set of CURIEs or IRIs of classes to keep");
    o.addOption("o", "output", true, "save minimized ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "minimize";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "minimize an ontology based on a threshold";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot minimize --input <file> " + "--threshold <int> " + "--output <file>";
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
   * Handle the command-line and file operations for the ReasonOperation.
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

  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    if (state == null) {
      state = new CommandState();
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();

    String thresholdString =
        CommandLineHelper.getRequiredValue(line, "threshold", "A threshold level is required");
    int threshold;
    try {
      threshold = Integer.parseInt(thresholdString);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format(thresholdError, thresholdString));
    }

    Set<IRI> precious =
        CommandLineHelper.getTerms(ioHelper, line, "precious-term", "precious-terms");

    MinimizeOperation.minimize(ontology, threshold, precious);

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}
