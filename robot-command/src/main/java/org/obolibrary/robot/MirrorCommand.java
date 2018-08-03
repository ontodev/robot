package org.obolibrary.robot;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mirrors an ontology plus its imports closure locally.
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class MirrorCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MirrorCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public MirrorCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "mirror ontology from a file");
    o.addOption("I", "input-iri", true, "mirror ontology from an IRI");
    o.addOption("d", "directory", true, "target directory");
    o.addOption("o", "output", true, "output file for catalog " + "(default catalog-v001.xml)");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "mirror";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "mirror ontology imports closure";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot mirror --input <file> " + "--directory <directory> " + "--output <file>";
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
   * Mirrors the input ontologgy and its imports closure.
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

    String dir = CommandLineHelper.getDefaultValue(line, "directory", ".");
    File catalogFile = CommandLineHelper.getOutputFile(line);
    if (catalogFile == null) {
      catalogFile = new File(dir + "/catalog-v001.xml");
      logger.info("Using default catalog with specified directory: " + catalogFile);
    }

    MirrorOperation.mirror(ontology, new File(dir), catalogFile);
    logger.info("Mirrored in " + catalogFile);
    return state;
  }
}
