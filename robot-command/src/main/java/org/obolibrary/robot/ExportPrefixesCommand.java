package org.obolibrary.robot;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export prefixes to a file.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExportPrefixesCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExportPrefixesCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public ExportPrefixesCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("o", "output", true, "save prefixes to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "export-prefixes";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "export prefixes to a file";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot export-prefixes --output <file>";
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
   * Handle the command-line and file operations for the command.
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
   * Given an input state and command line arguments, export the current prefixes and return the
   * state unchanged.
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

    File outputFile = CommandLineHelper.getOutputFile(line);
    if (outputFile != null) {
      ioHelper.saveContext(outputFile);
    } else {
      System.out.println(ioHelper.getContextString());
    }

    return state;
  }
}
