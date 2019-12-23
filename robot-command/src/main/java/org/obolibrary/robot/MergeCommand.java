package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link MergeOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MergeCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public MergeCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "merge ontology from a file");
    o.addOption("I", "input-iri", true, "merge ontology from an IRI");
    o.addOption("p", "inputs", true, "merge ontologies matching wildcard pattern");
    o.addOption("o", "output", true, "save merged ontology to a file");
    o.addOption(
        "c", "collapse-import-closure", true, "if true, the imports closure will be merged");
    o.addOption("a", "include-annotations", true, "if true, ontology annotations will be merged");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "merge";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "merge ontologies";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot merge --input <file> " + "--input <file> " + "--output <file>";
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
   * Handle the command-line and file operations for the MergeOperation.
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
   * Given an input state and command line arguments, merge all ontology axioms into the first
   * ontology and return a state with the merged ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with the merged ontology
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

    List<OWLOntology> inputOntologies = new ArrayList<>();
    // inputOntologies should not be empty
    boolean notEmpty = false;
    if (state.getOntology() != null) {
      notEmpty = true;
      inputOntologies.add(state.getOntology());
    }
    inputOntologies.addAll(CommandLineHelper.getInputOntologies(ioHelper, line, notEmpty));

    boolean collapseImportClosure =
        CommandLineHelper.getBooleanValue(line, "collapse-import-closure", true);
    boolean includeAnnotations =
        CommandLineHelper.getBooleanValue(line, "include-annotations", false);
    OWLOntology outputOntology =
        MergeOperation.merge(inputOntologies, includeAnnotations, collapseImportClosure);

    CommandLineHelper.maybeSaveOutput(line, outputOntology);

    state.setOntology(outputOntology);
    return state;
  }
}
