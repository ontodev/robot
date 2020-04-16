package org.obolibrary.robot;

import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/** */
public class CollapseCommand implements Command {

  /** Namespace for error messages. */
  private static final String NS = "minimize#";

  /** Error message when --threshold is not an integer. */
  private static final String thresholdError =
      NS + "THRESHOLD ERROR threshold ('%s') must be a valid integer.";

  /** Error message when --threshold is not an integer. */
  private static final String thresholdValueError =
      NS + "THRESHOLD VALUE ERROR threshold ('%d') must be 2 or greater.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public CollapseCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "collapse ontology from a file");
    o.addOption("I", "input-iri", true, "collapse ontology from an IRI");
    o.addOption("t", "threshold", true, "threshold to collapse below");
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
    return "collapse";
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
    return "robot collapse --input <file> " + "--threshold <int> " + "--output <file>";
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

    String thresholdString = CommandLineHelper.getDefaultValue(line, "threshold", "2");
    int threshold;
    try {
      threshold = Integer.parseInt(thresholdString);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format(thresholdError, thresholdString));
    }

    // If threshold is not 2 or greater
    // Nothing will be done
    if (threshold < 2) {
      throw new Exception(String.format(thresholdValueError, threshold));
    }

    Set<IRI> precious = CommandLineHelper.getTerms(ioHelper, line, "precious", "precious-terms");

    OntologyHelper.collapseOntology(ontology, threshold, precious, true);

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}
