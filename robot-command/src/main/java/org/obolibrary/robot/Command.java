package org.obolibrary.robot;

import org.apache.commons.cli.Options;

/**
 * A simple interface for all ROBOT commands.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public interface Command {

  String global = "global#";
  String missingFileError = global + "MISSING FILE ERROR file '%s' for '%s' does not exist";

  /**
   * Name of the command.
   *
   * @return name
   */
  String getName();

  /**
   * Brief description of the command.
   *
   * @return description
   */
  String getDescription();

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  String getUsage();

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  Options getOptions();

  /**
   * All commands can be call from the Java command line with an array of strings as arguments.
   *
   * @param args strings to use as arguments
   */
  void main(String[] args);

  /**
   * All commands offer an execute method that can be chained from previous commands.
   *
   * @param inputState the input from the previous command, or null
   * @param args the command-line arguments
   * @return the updated state, or a new state, or null
   * @throws Exception on any problem
   */
  CommandState execute(CommandState inputState, String[] args) throws Exception;
}
