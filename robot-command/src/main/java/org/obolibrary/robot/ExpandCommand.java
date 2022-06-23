package org.obolibrary.robot;

import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Handles inputs and outputs for the {@link ExpandOperation}.
 *
 * @author <a href="mailto:balhoff@renci.org">Jim Balhoff</a>
 */
public class ExpandCommand implements Command {

  /** Store the command-line options for the command. */
  private final Options options;

  /** Initialize the command. */
  public ExpandCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption(
        "c",
        "create-new-ontology",
        true,
        "if true, output ontology will only contain the expansions");
    o.addOption(
        "a",
        "annotate-expansion-axioms",
        true,
        "if true, annotate expansion axioms with 'dct:source <expansion property>'");
    o.addOption("t", "expand-term", true, "property to expand");
    o.addOption("T", "expand-term-file", true, "load properties to expand from a file");
    o.addOption("n", "no-expand-term", true, "properties to not expand");
    o.addOption("N", "no-expand-term-file", true, "load properties to not expand from a file");
    o.addOption("i", "input", true, "expand ontology from a file");
    o.addOption("I", "input-iri", true, "expand ontology from an IRI");
    o.addOption("o", "output", true, "save expanded ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "expand";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "expand ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot expand --input <file> " + "[options] " + "--output <file>";
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
   * Handle the command-line and file operations for the ExpandOperation.
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
   * Given an input state and command line arguments, expand macro relations, and add axioms to the
   * input ontology, returning a state with the updated ontology.
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
    // Override default expand options with command-line options
    ExpandOperation.ExpandConfig config = new ExpandOperation.ExpandConfig();
    if (line.hasOption("create-new-ontology")) {
      config.setCreateNewOntology(Boolean.parseBoolean(line.getOptionValue("create-new-ontology")));
    }
    if (line.hasOption("annotate-expansion-axioms")) {
      config.setAnnotateExpansionAxioms(
          Boolean.parseBoolean(line.getOptionValue("annotate-expansion-axioms")));
    }
    Set<IRI> includeTerms =
        CommandLineHelper.getTerms(ioHelper, line, "expand-term", "expand-term-file");
    Set<IRI> excludeTerms =
        CommandLineHelper.getTerms(ioHelper, line, "no-expand-term", "no-expand-term-file");
    ExpandOperation.expand(ontology, config, includeTerms, excludeTerms);
    CommandLineHelper.maybeSaveOutput(line, ontology);
    return state;
  }
}
