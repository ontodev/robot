package org.obolibrary.robot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

public class ExportCommand implements Command {

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public ExportCommand() {
    Options o = CommandLineHelper.getCommonOptions();

    o.addOption("i", "input", true, "load ontology to merge from a file");
    o.addOption("I", "input-iri", true, "load ontology to merge from an IRI");
    o.addOption("e", "export", true, "file to export ontology to");
    o.addOption("c", "columns", true, "ordered list of column names");
    o.addOption("s", "sort", true, "field to sort on (default: first column)");
    o.addOption("C", "exclude-classes", true, "if true, exclude classes (default: false)");
    o.addOption("P", "exclude-properties", true, "if false, include properties (default: true)");
    o.addOption("N", "exclude-individuals", true, "if true, exclude individuals (default: false)");
    o.addOption("A", "exclude-anonymous", true, "if true, exclude anonymous parents");

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "export";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "export ontology as spreadsheet";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot export --input <file> " + "--export <file> ";
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
   * Given an input state and command line arguments, build a new ontology from the given template,
   * and return a state with the new ontology.
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

    if (ontology == null) {
      if (line.hasOption("input")) {
        throw new IllegalArgumentException(
            String.format("Input ontology '%s' does not exist", line.getOptionValue("input")));
      }
    }

    // Override default reasoner options with command-line options
    Map<String, String> exportOptions = ExportOperation.getDefaultOptions();
    for (String option : exportOptions.keySet()) {
      if (line.hasOption(option)) {
        exportOptions.put(option, line.getOptionValue(option));
      }
    }

    String columnsString =
        CommandLineHelper.getRequiredValue(line, "columns", "--columns is a required option");
    String exportPath =
        CommandLineHelper.getRequiredValue(line, "export", "an export file must be specified");
    ExportOperation.export(
        ontology, ioHelper, splitColumns(columnsString), new File(exportPath), exportOptions);
    return state;
  }

  private static List<String> splitColumns(String columnsString) {
    List<String> split = new ArrayList<>();
    Matcher m = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'").matcher(columnsString);
    while (m.find()) {
      String s = m.group().trim().replace("'", "");
      split.add(s);
    }
    return split;
  }
}
