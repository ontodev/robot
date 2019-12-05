package org.obolibrary.robot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Starts a gateway server for Py4J to execute ROBOT operations via Python.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class PythonCommand implements Command {

  /** Namespace for error messages. */
  private static final String NS = "python#";

  private static final String portNumberError =
      NS + "PORT NUMBER ERROR port '%s' must be an integer.";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public PythonCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption(null, "port", true, "port number for Py4J");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "python";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "start a server to run ROBOT with Py4J";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot python --port <port>";
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
   * Runs ROBOT accessible via Python.
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

    // Maybe get a port
    Integer port = null;
    String portString = CommandLineHelper.getOptionalValue(line, "port");
    if (portString != null) {
      try {
        port = Integer.parseInt(portString);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(String.format(portNumberError, portString));
      }
    }

    // This will run until killed
    PythonOperation.run(port);

    // Ignore this - we don't get here
    return state;
  }
}
