package org.obolibrary.robot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

/**
 * Starts a gateway server for Py4J to execute ROBOT operations via Python.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class PythonCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MirrorCommand.class);

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

    int port = Integer.parseInt(CommandLineHelper.getDefaultValue(line, "port", "25333"));
    logger.debug(String.format("Starting Py4J on port %d", port));

    GatewayServer gs = new GatewayServer(port);
    try {
      gs.start();
    } catch (Exception e) {
      throw new Exception(
          String.format(
              "Cannot start server on port %d - check if something is already running and try again",
              port));
    }

    // Ignore this
    // you can't chain commands with the python command
    return state;
  }
}
