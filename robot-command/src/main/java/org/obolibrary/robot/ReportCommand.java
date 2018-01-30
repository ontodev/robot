package org.obolibrary.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.obolibrary.robot.report.ReportCard;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link ReportOperation}.
 *
 * @author cjm
 */
public class ReportCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReportCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public ReportCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save report a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "report";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "report terms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot report --input <file> " + "--output <file> " + "--output-iri <iri>";
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
   * Handle the command-line and file operations for the reportOperation.
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
   * Given an input state and command line arguments, report a new ontology and return an new state.
   * The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    OWLOntology outputOntology = null;

    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology inputOntology = state.getOntology();

    // TODO save results
    ReportCard reportCard = ReportOperation.report(inputOntology, ioHelper);

    File output = CommandLineHelper.getOutputFile(line);

    // TODO: allow JSON as option
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    if (output == null) {
      System.out.println(writer.writeValueAsString(reportCard));
    } else {
      writer.writeValue(output, reportCard);
    }

    return state;
  }
}
