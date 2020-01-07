package org.obolibrary.robot;

import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

public class TreeCommand implements Command {

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public TreeCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology generate tree from file");
    o.addOption("I", "input-iri", true, "load ontology to generate tree from an IRI");
    o.addOption("t", "tree", true, "save tree to a file (JSON, MD, or directory for HTML)");
    o.addOption("f", "format", true, "format to save tree view: json, html, md");
    o.addOption("u", "upper-term", true, "specify an upper term for top node of tree");
    o.addOption("M", "markdown-pattern", true, "pattern string for MD output");
    o.addOption("a", "annotation-property", true, "annotation property to include in tree details");
    o.addOption(
        "A",
        "annotation-properties",
        true,
        "set of annotation properties to include in tree details");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "tree";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "build a tree view from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot tree --input <file> " + "--tree <file>";
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
   * Handle the command-line and file operations for the TemplateOperation.
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
   * Given an input state and command line arguments, generate a tree view from the input ontology
   * and return a state with the unchanged ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with the filtered ontology
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
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line, false);
    OWLOntology ontology = state.getOntology();

    // Override default reasoner options with command-line options
    Map<String, String> treeOptions = TreeBuilder.getDefaultOptions();
    for (String option : treeOptions.keySet()) {
      if (line.hasOption(option)) {
        treeOptions.put(option, line.getOptionValue(option));
      }
    }

    Set<IRI> upperTerms = CommandLineHelper.getTerms(ioHelper, line, "upper-term", "upper-terms");
    Set<IRI> annotationProperties =
        CommandLineHelper.getTerms(ioHelper, line, "annotation-property", "annotation-properties");

    TreeBuilder treeOp = new TreeBuilder(ioHelper, ontology);
    treeOp.buildTree(upperTerms, annotationProperties, treeOptions);

    return state;
  }
}
