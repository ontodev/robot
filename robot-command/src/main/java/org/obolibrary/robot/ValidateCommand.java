package org.obolibrary.robot;

import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
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
    o.addOption("t", "table", true, "file containing the data (in CSV or TSV format) to validate");
    o.addOption("i", "input", true, "input file containing the ontology data to validate against");
    o.addOption("r", "reasoner", true, "reasoner to use (structural, hermit, jfact, emr, elk)");
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
    return "validate --table <file> --input <file> --reasoner <name> [--output <file>]";
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
   * Accepts an input state and command line arguments and calls ValidateOperation to validate the
   * data that has been passed. Returns the final state of the command.
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
    String tablePath = CommandLineHelper.getOptionalValue(line, "table");

    // Only TSV and CSV tables are currently supported. If the path does not end in .tsv then we
    // assume it is a CSV file:
    List<List<String>> tableData;
    if (tablePath.toLowerCase().endsWith(".tsv")) {
      tableData = ioHelper.readTSV(tablePath);
    } else {
      tableData = ioHelper.readCSV(tablePath);
    }

    // Get the input path for the ontology and the output path to write the validation results to:
    String inputPath = CommandLineHelper.getOptionalValue(line, "input");
    OWLOntology ontology = ioHelper.loadOntology(inputPath);
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");

    // Get the reasoner specified by the user and if none is specified, use the default:
    if (CommandLineHelper.getOptionalValue(line, "reasoner") == null) {
      logger.info("No reasoner specified. Will use the default.");
    }
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);

    // Finally call the validator:
    ValidateOperation.validate(tableData, ontology, reasonerFactory, outputPath);

    return state;
  }
}
