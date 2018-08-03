package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link UnmergeOperation}.
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class UnmergeCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public UnmergeCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "unmerge ontology from a file");
    o.addOption("I", "input-iri", true, "unmerge ontology from an IRI");
    o.addOption("p", "inputs", true, "unmerge ontologies matching wildcard pattern");
    o.addOption("o", "output", true, "save unmerged ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "unmerge";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "unmerge ontologies";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot unmerge --input <file> " + "--input <file> " + "--output <file>";
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
   * Handle the command-line and file operations for the UnergeOperation.
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
   * Given an input state and command line arguments, unmerge all ontology axioms into the first
   * ontology and return a state with the unmerged ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with the unmerged ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {

    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

    if (state == null) {
      state = new CommandState();
    }

    List<OWLOntology> inputOntologies = new ArrayList<OWLOntology>();
    boolean notEmpty = false;
    if (state != null && state.getOntology() != null) {
      notEmpty = true;
      inputOntologies.add(state.getOntology());
    }
    inputOntologies.addAll(CommandLineHelper.getInputOntologies(ioHelper, line, notEmpty));

    OWLOntology outputOntology = UnmergeOperation.unmerge(inputOntologies);

    CommandLineHelper.maybeSaveOutput(line, outputOntology);

    state.setOntology(outputOntology);
    return state;
  }
}
