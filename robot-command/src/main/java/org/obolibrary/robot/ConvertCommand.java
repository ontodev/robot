package org.obolibrary.robot;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save ontology to a different format.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ConvertCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ConvertCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public ConvertCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "convert ontology from a file");
    o.addOption("I", "input-iri", true, "convert ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("f", "format", true, "the format: RDFXML, OBO, etc.");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "convert";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "convert ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot convert --input <file> " + "--format <format> " + "--output <file>";
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
      CommandLineHelper.handleException(getUsage(), getOptions(), e);
    }
  }

  /**
   * Given an input state and command line arguments, save the ontology to a new format and return
   * the state unchanged.
   *
   * <p>Supported formats:
   *
   * <ul>
   *   <li>OBO .obo
   *   <li>RDFXML .owl
   *   <li>Turtle .ttl
   *   <li>OWLXML .owx
   *   <li>Manchester .omn
   *   <li>OWL Functional .ofn
   *   <li>OboGraphs JSON .json
   * </ul>
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the input state unchanged, or a new state with the ontology
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

    String[] outputs = line.getOptionValues("output");
    if (outputs.length == 0) {
      throw new Exception("An output file is required.");
    } else if (outputs.length > 1) {
      throw new Exception("Only one output file is allowed.");
    }
    File outputFile = CommandLineHelper.getOutputFile(line);

    String formatName = CommandLineHelper.getOptionalValue(line, "format");
    if (formatName == null) {
      formatName = FilenameUtils.getExtension(outputFile.getName());
    }

    ioHelper.saveOntology(ontology, ioHelper.getFormat(formatName), outputFile);

    return state;
  }
}
