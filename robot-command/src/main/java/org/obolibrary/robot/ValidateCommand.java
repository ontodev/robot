package org.obolibrary.robot;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateCommand implements Command {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

  /** Used to store the command-line options for the command. */
  private Options options;

  /** Namespace for error messages. */
  private static final String NS = "validate#";

  /** Constructor: Initialises the command with its various options. */
  public ValidateCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("c", "csv", true, "CSV file containing the data to validate");
    o.addOption("w", "owl", true, "OWL file containing the ontology data to validate against");
    o.addOption("o", "output", true, "Save results to file (if unspecified, output to STDOUT)");
    options = o;
  }

  /**
   * Returns the name of the command
   *
   * @return name
   */
  public String getName() {
    return "validate";
  }

  /**
   * Returns a brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "validate the data in the given file";
  }

  /**
   * Returns the command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "validate --csv <CSV> --owl <OWL> --output <file>";
  }

  /**
   * Returns the command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return options;
  }

  /**
   * Handles the command-line and file operations for the ValidateOperation
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
   * Accepts an input state and command line arguments and outputs a CSV table reporting the
   * validation results. Returns the final state of the command.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the input state, unchanged
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

    // Load the command line arguments:
    String csvPath = CommandLineHelper.getOptionalValue(line, "csv");
    List<List<String>> csvData = ioHelper.readCSV(csvPath);
    String owlPath = CommandLineHelper.getOptionalValue(line, "owl");
    OWLOntology owlData = ioHelper.loadOntology(owlPath);
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");

    // Initialise the writer to be the given output path, or STDOUT if that is left unspecified:
    Writer writer;
    if (outputPath != null) {
      writer = new FileWriter(outputPath);
    } else {
      writer = new PrintWriter(System.out);
    }

    // TODO: We should eventually make the reasoner configurable, as we do for the 'reason' command,
    // but for now just use ELK.
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    ValidateOperation.validate_immexp(csvData, owlData, reasonerFactory, writer);

    writer.flush();
    writer.close();
    return state;
  }
}
