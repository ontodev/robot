package org.obolibrary.robot;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to get the differences between two ontology files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class DiffCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(DiffCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "diff#";

  private static final String doubleInputError =
      NS + "DOUBLE INPUT ERROR only one of each side (right/left) allowed";

  /** Error message when --left is not provided. */
  private static final String missingLeftError =
      NS + "MISSING INPUT ERROR left ontology is required";

  /** Error message when --right is not provided. */
  private static final String missingRightError =
      NS + "MISSING INPUT ERROR right ontology is required";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public DiffCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("l", "left", true, "load left ontology from file");
    o.addOption("L", "left-iri", true, "load left ontology from IRI");
    o.addOption("r", "right", true, "load right ontology from file");
    o.addOption("R", "right-iri", true, "load right ontology from IRI");
    o.addOption("o", "output", true, "save results to file");
    o.addOption(null, "labels", true, "if true, use labels in place of entity IRIs");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "diff";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "find the differences between two ontologies";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot diff --left <file> " + "--right <file> " + "--output <file>";
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
   * Handle the command-line and file operations for the DiffOperation.
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
   * Given an input state and command line arguments, report on the differences between ontologies,
   * if any, and return the state unchanged.
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

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    if (state == null) {
      state = new CommandState();
    }

    OWLOntology leftOntology = null;
    if (state.getOntology() != null) {
      leftOntology = state.getOntology();
    }
    if (leftOntology == null) {
      String leftOntologyPath = CommandLineHelper.getOptionalValue(line, "left");
      String leftOntologyIRI = CommandLineHelper.getOptionalValue(line, "left-iri");
      leftOntology = setOntology(ioHelper, leftOntologyPath, leftOntologyIRI);
    }
    if (leftOntology == null) {
      throw new IllegalArgumentException(missingLeftError);
    }

    String rightOntologyPath = CommandLineHelper.getOptionalValue(line, "right");
    String rightOntologyIRI = CommandLineHelper.getOptionalValue(line, "right-iri");
    OWLOntology rightOntology = setOntology(ioHelper, rightOntologyPath, rightOntologyIRI);
    if (rightOntology == null) {
      throw new IllegalArgumentException(missingRightError);
    }

    Writer writer;
    String outputPath = CommandLineHelper.getOptionalValue(line, "output");
    if (outputPath != null) {
      writer = new FileWriter(outputPath);
    } else {
      writer = new PrintWriter(System.out);
    }

    Map<String, String> options = new HashMap<>();
    options.put("labels", CommandLineHelper.getDefaultValue(line, "labels", "false"));

    DiffOperation.compare(leftOntology, rightOntology, ioHelper, writer, options);
    writer.flush();
    writer.close();

    return state;
  }

  /**
   * Given an IOHelper, a path (or null), and an IRI (or null), return the OWLOntology loaded by
   * either path or IRI. Either path or iri must be included (not null), but both cannot be
   * included.
   *
   * @param ioHelper IOHelper to load ontology
   * @param path path to local ontology, or null
   * @param iri IRI to remote ontology, or null
   * @return loaded OWLOntology
   * @throws IOException on issue loading ontology
   */
  private static OWLOntology setOntology(IOHelper ioHelper, String path, String iri)
      throws IOException {
    if (path != null && iri != null) {
      throw new IllegalArgumentException(doubleInputError);
    } else if (path != null) {
      return ioHelper.loadOntology(path);
    } else if (iri != null) {
      return ioHelper.loadOntology(IRI.create(iri));
    } else return null;
  }
}
