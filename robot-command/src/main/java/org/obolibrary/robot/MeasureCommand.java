package org.obolibrary.robot;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Handles inputs and outputs for the {@link MeasureOperation}.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MeasureCommand implements Command {

  /** Store the command-line options for the command. */
  private Options options;

  private final List<String> LEGAL_FORMATS = Arrays.asList("tsv", "csv", "html", "yaml", "json");

  /** Initialze the command. */
  public MeasureCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("r", "reasoner", true, "reasoner to use: (ELK, HermiT)");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption(
        "f",
        "format",
        true,
        "the measure result format: " + String.join(" ", LEGAL_FORMATS).trim() + ".");
    o.addOption("o", "output", true, "save updated metrics to a file");
    o.addOption(
        "m",
        "metrics",
        true,
        "select which set of metrics you would like to compute: essential (default), extended, all.");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "measure";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "compute the metrics of an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  // todo: --output?
  public String getUsage() {
    return "robot measure --input <file> --output <output>";
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
   * Handle the command-line and file operations for the MeasureOperation.
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
   * Given an input state and command line arguments, compute metrics in ontology. The input
   * ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the unchanged state
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

    String metricsType = CommandLineHelper.getDefaultValue(line, "metrics", "essential");
    String format = CommandLineHelper.getOptionalValue(line, "format");
    String output =
        CommandLineHelper.getRequiredValue(line, "output", "an output file must be specified");

    File output_file = new File(output);
    if (format == null) {
      String extension = FilenameUtils.getExtension(output_file.getName()).replace("yml", "yaml");
      if (LEGAL_FORMATS.contains(extension)) {
        format = extension;
      } else {
        format = "tsv";
      }
    }

    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line);

    // Override default reasoner options with command-line options
    Map<String, String> reasonerOptions = ReduceOperation.getDefaultOptions();
    for (String option : reasonerOptions.keySet()) {
      if (line.hasOption(option)) {
        reasonerOptions.put(option, line.getOptionValue(option));
      }
    }

    MeasureOperation.measure(
        state.getOntology(),
        reasonerFactory,
        metricsType,
        format,
        output_file,
        ioHelper.getPrefixes());

    return state;
  }
}
