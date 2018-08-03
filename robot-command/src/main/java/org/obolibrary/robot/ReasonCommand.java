package org.obolibrary.robot;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link ReasonOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReasonCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReasonCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "reason#";

  /** Error message when user sets -m true and -n true. */
  private static final String createOntologyError =
      NS
          + "CREATE ONTOLOGY ERROR 'create-new-ontology' and 'create-new-ontology-with-annotations'"
          + " cannot both be set to true.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public ReasonCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("r", "reasoner", true, "reasoner to use: ELK, HermiT, JFact");
    o.addOption(
        "D",
        "dump-unsatisfiable",
        true,
        "if specified and ontology is incoherent, dump minimal explanatory module here");
    o.addOption(
        "s", "remove-redundant-subclass-axioms", true, "if true, remove redundant subclass axioms");
    o.addOption(
        "p",
        "preserve-annotated-axioms",
        true,
        "preserve annotated axioms when removing redundant subclass axioms");
    o.addOption(
        "n",
        "create-new-ontology",
        true,
        "if true, output ontology will only contain the inferences");
    o.addOption(
        "m",
        "create-new-ontology-with-annotations",
        true,
        "if true, output ontology will contain the inferences and their annotation properties");
    o.addOption(
        "a",
        "annotate-inferred-axioms",
        true,
        "if true, annotate inferred axioms with 'is_inferred true'");
    o.addOption(
        "x",
        "exclude-duplicate-axioms",
        true,
        "if true, do not add an axiom if it exists in import chain");
    o.addOption(
        "X",
        "exclude-external-entities",
        true,
        "if true, do not add an axiom if it is about classes in external ontologies");
    o.addOption("T", "exclude-owl-thing", true, "if true, exclude inferences to owl:Thing");
    o.addOption(
        "e",
        "equivalent-classes-allowed",
        true,
        "if 'none', any equivalent class will cause an error, if 'all', all equivalent classes are "
            + "allowed, and if 'asserted-only', inferred equivalent classes will cause an error.");
    o.addOption("i", "input", true, "reason ontology from a file");
    o.addOption("I", "input-iri", true, "reason ontology from an IRI");
    o.addOption("o", "output", true, "save reasoned ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "reason";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "reason ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot reason --input <file> " + "--reasoner <name> " + "[options] " + "--output <file>";
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
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);

    // Override default reasoner options with command-line options
    Map<String, String> reasonerOptions = ReasonOperation.getDefaultOptions();
    for (String option : reasonerOptions.keySet()) {
      if (line.hasOption(option)) {
        reasonerOptions.put(option, line.getOptionValue(option));
      }
    }
    if (reasonerOptions.get("create-new-ontology-with-annotations").equalsIgnoreCase("true")
        && reasonerOptions.get("create-new-ontology").equalsIgnoreCase("true")) {
      throw new IllegalArgumentException(createOntologyError);
    }
    ReasonOperation.reason(ontology, reasonerFactory, reasonerOptions);

    CommandLineHelper.maybeSaveOutput(line, ontology);

    return state;
  }
}
