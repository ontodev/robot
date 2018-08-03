package org.obolibrary.robot;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link ReduceOperation}.
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class ReduceCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReduceCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public ReduceCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("r", "reasoner", true, "reasoner to use: (ELK, HermiT)");
    o.addOption(
        "p",
        "preserve-annotated-axioms",
        true,
        "preserve annotated axioms when removing redundant subclass axioms");
    o.addOption("i", "input", true, "reduce ontology from a file");
    o.addOption("I", "input-iri", true, "reduce ontology from an IRI");
    o.addOption("o", "output", true, "save reduceed ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "reduce";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "reduce ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot reduce --input <file> " + "--reasoner <name> " + "[options] " + "--output <file>";
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
   * Handle the command-line and file operations for the reduceOperation.
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
   * Given an input state and command line arguments, run a reasoner, and add axioms to the input
   * ontology, returning a state with the updated ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with inferred axioms added to the ontology
   * @throws Exception on any problem
   */
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
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line);

    // Override default reasoner options with command-line options
    Map<String, String> reasonerOptions = ReduceOperation.getDefaultOptions();
    for (String option : reasonerOptions.keySet()) {
      if (line.hasOption(option)) {
        reasonerOptions.put(option, line.getOptionValue(option));
      }
    }

    ReduceOperation.reduce(ontology, reasonerFactory, reasonerOptions);

    CommandLineHelper.maybeSaveOutput(line, ontology);

    return state;
  }
}
