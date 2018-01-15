package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute collections of commands.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandManager implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

  /** Store the command-line options for the command. */
  private Options globalOptions;

  /** Store a map from command names to Command objects. */
  private Map<String, Command> commands = new LinkedHashMap<String, Command>();

  /** Initialze the command. */
  public CommandManager() {
    globalOptions = CommandLineHelper.getCommonOptions();
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "robot";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "work with OWL ontologies";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot [command] [options] <arguments>";
  }

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return globalOptions;
  }

  /**
   * Add a new command to this manager.
   *
   * @param commandName the of the command (one word)
   * @param command the Command object to register
   */
  public void addCommand(String commandName, Command command) {
    commands.put(commandName, command);
  }

  /**
   * Convenience method to convert from a List to an array.
   *
   * @param list a list of strings
   * @return an array of strings
   */
  private String[] asArgs(List<String> list) {
    return list.toArray(new String[list.size()]);
  }

  /**
   * Given some Options and some arguments, collect all the options until the first non-option
   * argument, then remove those used argument strings from the arguments list and return the used
   * arguments as a new list.
   *
   * @param options the options to collect
   * @param arguments a list of remaining command-line arguments; used option strings are removed
   *     from this list
   * @return the list of used argument strings
   * @throws ParseException if command line cannot be parsed
   */
  public List<String> getOptionArgs(Options options, List<String> arguments) throws ParseException {
    // parse all options until a non-option is found
    CommandLineParser parser = new PosixParser();
    CommandLine line = parser.parse(options, asArgs(arguments), true);

    int index = arguments.size() - line.getArgList().size();
    List<String> used = new ArrayList<String>(arguments.subList(0, index));
    arguments.subList(0, index).clear();
    return used;
  }

  /**
   * Given command-line arguments, execute one or more commands.
   *
   * @param args the command line arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      e.printStackTrace();
      printHelp();
      System.exit(1);
    }
  }

  /**
   * Given an input state and command-line arguments, execute one or more commands.
   *
   * @param state an state to work with, or null
   * @param args the command-line arguments
   * @return the result state of the last subcommand
   * @throws Exception on any problems
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLineParser parser = new PosixParser();
    CommandLine line = parser.parse(getOptions(), args, true);
    if (line.hasOption("help")) {
      printHelp();
      return null;
    }
    if (line.hasOption("version")) {
      CommandLineHelper.printVersion();
      return null;
    }

    if (state == null) {
      state = new CommandState();
    }

    List<String> arguments = new ArrayList<String>(Arrays.asList(args));
    if (arguments.size() == 0) {
      throw new IllegalArgumentException("No command provided");
    }

    List<String> globalOptionArgs = getOptionArgs(globalOptions, arguments);

    while (arguments.size() > 0) {
      state = executeCommand(state, globalOptionArgs, arguments);
    }

    return state;
  }

  /**
   * Given an input state, global option strings, and remaining command-line argument strings, use
   * as many arguments as needed to execute a single command. The arguments used by the command are
   * removed from the arguments list, which can then be used to execute further commands.
   *
   * @param state the state from the previous command, or null
   * @param globalOptionArgs a list of global option strings
   * @param arguments the list of remaining command-line arguments; any arguments that are used will
   *     be removed from this list
   * @return the state that results from this command
   * @throws Exception on any problems
   */
  public CommandState executeCommand(
      CommandState state, List<String> globalOptionArgs, List<String> arguments) throws Exception {
    String commandName = null;
    if (arguments.size() > 0) {
      commandName = arguments.remove(0);
    }
    if (commandName == null) {
      throw new IllegalArgumentException("No command provided");
    }

    commandName = commandName.trim().toLowerCase();
    if (commandName.equals("help")) {
      if (arguments.size() == 0) {
        printHelp();
      } else {
        globalOptionArgs.add("--help");
      }
      return state;
    }
    if (commandName.equals("version")) {
      CommandLineHelper.printVersion();
      return state;
    }
    if (!commands.containsKey(commandName)) {
      throw new IllegalArgumentException("Unknown command or option: " + commandName);
    }

    Command command = commands.get(commandName);
    if (command == null) {
      throw new IllegalArgumentException("Command is null: " + commandName);
    }
    if (command.getOptions() == null) {
      throw new IllegalArgumentException("Command has no options: " + commandName);
    }

    List<String> localOptionArgs = getOptionArgs(command.getOptions(), arguments);
    List<String> optionArgs = new ArrayList<String>();
    optionArgs.addAll(globalOptionArgs);
    optionArgs.addAll(localOptionArgs);

    return command.execute(state, asArgs(optionArgs));
  }

  /** Print general help plus a list of available commands. */
  public void printHelp() {
    CommandLineHelper.printHelp(getUsage(), getOptions());
    System.out.println("commands:");
    printHelpEntry("help", "print help for command");
    for (Map.Entry<String, Command> entry : commands.entrySet()) {
      printHelpEntry(entry);
    }
  }

  /**
   * Print a help entry for a single command.
   *
   * @param entry an entry from the map of commands
   */
  public void printHelpEntry(Map.Entry<String, Command> entry) {
    printHelpEntry(entry.getKey(), entry.getValue().getDescription());
  }

  /**
   * Print a help entry for a single command.
   *
   * @param name the name of the command
   * @param description a brief description of the command
   */
  public void printHelpEntry(String name, String description) {
    System.out.println(String.format(" %-16s %s", name, description));
  }
}
