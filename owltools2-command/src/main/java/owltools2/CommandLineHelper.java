package owltools2;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.IRI;

import owltools2.IOHelper;

/**
 * Convenience methods for working with command line options.
 */
public class CommandLineHelper {
  public static String getIndexValue(CommandLine line, int index) {
    String result = null;
    List<String> arguments = Arrays.asList(line.getArgs());
    if(arguments.size() > index) {
      result = arguments.get(index);
    }
    return result;
  }

  public static String getCommand(CommandLine line) {
    return getIndexValue(line, 0);
  }

  /**
   * Return true if a command line include the option with the given name,
   * or the command with the given name.
   */
  public static boolean hasFlagOrCommand(CommandLine line, String name) {
    if(line.hasOption(name)) {
      return true;
    } 
    String command = getCommand(line);
    if (command != null && command.equals(name)) {
      return true;
    }
    return false;
  }

  public static String getOptionalValue(CommandLine line, String name) {
    return getDefaultValue(line, name, null);
  }

  public static String getOptionalValue(CommandLine line, String name, int index) {
    return getDefaultValue(line, name, index, null);
  }

  public static String getDefaultValue(CommandLine line, String name, String defaultValue) {
    String result = defaultValue;
    if(line.hasOption(name)) {
      result = line.getOptionValue(name);
    }
    return result;
  }

  public static String getDefaultValue(CommandLine line, String name, int index, String defaultValue) {
    String result = defaultValue;
    String indexed = getIndexValue(line, index);
    if(line.hasOption(name)) {
      result = line.getOptionValue(name);
    } else if (indexed != null) {
      result = indexed;
    }
    return result;
  }

  public static String getRequiredValue(CommandLine line, String name, String message) {
    String result = getOptionalValue(line, name);
    if(result == null) {
      throw new IllegalArgumentException(message);
    }
    return result;
  }

  public static String getRequiredValue(CommandLine line, String name, int index, String message) {
    String result = getOptionalValue(line, name, index);
    if(result == null) {
      throw new IllegalArgumentException(message);
    }
    return result;
  }

  public static IOHelper getIOHelper(CommandLine line) {
    // TODO: handle --prefix options
    return new IOHelper();
  }

  /**
   * Covers common cases using --input and --input-iri flags,
   * with or without catalog files.
   *
   * NOTE: currenly only handles --input files with automatic catalog detection
   * TODO: handle catalogs, IRIs
   */
  public static OWLOntology getInputOntology(IOHelper ioHelper,
      CommandLine line) throws IllegalArgumentException, IOException {
    OWLOntology inputOntology = null;
    String inputOntologyPath = getOptionalValue(line, "input", 1);
    if(inputOntologyPath != null) {
      inputOntology = ioHelper.loadOntology(inputOntologyPath);
    } else {
      throw new IllegalArgumentException("inputOntology must be specified");
    }
    return inputOntology;
  }

  public static File getOutputFile(CommandLine line) {
    String outputPath = getRequiredValue(
        line, "output", "The output file must be specified.");
    return new File(outputPath);
  }

  public static IRI getOutputIRI(CommandLine line) {
    String outputIRIString = getOptionalValue(line, "output-iri");
    IRI outputIRI = null;
    if(outputIRIString != null) {
      outputIRI = IRI.create(outputIRIString);
    }
    return outputIRI;
  }

  public static Set<IRI> getTerms(IOHelper ioHelper, CommandLine line)
      throws IllegalArgumentException, IOException {
    Set<IRI> terms = null;
    if(line.hasOption("terms")) {
      terms = ioHelper.parseTerms(line.getOptionValue("terms"));
    } else if (line.hasOption("term-file")) {
      terms = ioHelper.loadTerms(line.getOptionValue("term-file"));
    }
    if(terms == null) {
      throw new IllegalArgumentException("The terms to extract must be specified.");
    }
    return terms;
  }

  public static void printHelp(String usage, Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(usage, options);
  }

  public static void printVersion() {
    System.out.println("TODO: command version 0.0.1");
  }

  /**
   * Create a new Options object with shared options.
   */
  public static Options getCommonOptions() {
    Options options = new Options();
    options.addOption("h", "help",       false, "print usage information");
    options.addOption("v", "version",    false, "print version information");
    return options;
  }

  /**
   * Parse the command line,
   * handle help and other common options,
   * WARN: May exit!
   * and return a CommandLine.
   */
  public static CommandLine getCommandLine(String usage, Options options, String[] args)
      throws ParseException {
    CommandLineParser parser = new PosixParser();
    CommandLine line = parser.parse(options, args);

    if(hasFlagOrCommand(line, "help")) {
      printHelp(usage, options);
      System.exit(0);
    }
    if(hasFlagOrCommand(line, "version")) {
      printVersion();
      System.exit(0);
    }

    return line;
  }

  /**
   * Shared method for dealing with exceptions, printing help, and exiting.
   */
  public static void handleException(String usage, Options options, Exception exception) {
    // TODO: decide how to handle the exception
    exception.printStackTrace();
    printHelp(usage, options);
    System.exit(1);
  }
}
