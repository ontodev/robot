package owltools2;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import owltools2.CommandLineHelper;
import owltools2.ExtendedPosixParser;
import owltools2.Command;

/**
 * The command line interface for OBOTools.
 * We use Apache CLI and follow the examples here:
 * http://commons.apache.org/proper/commons-cli/usage.html
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandLineInterface {
  protected static String usage = "owltools2 [command] [options] <arguments>";

  /**
   * We look up commands in these maps.
   * TODO: Replace these Maps with a better plugin strategy.
   */
  public static Map<String,Command> commands = new HashMap<String,Command>();
  public static Map<String,String> descriptions = new HashMap<String,String>();

  public static void main(String[] args) {
    // TODO: Replace these Maps with a better plugin strategy.
    commands.put("extract", new owltools2.ExtractCommand());
    descriptions.put("extract", owltools2.ExtractCommand.description);

    // Ignore unrecognized options
    CommandLineParser parser = new ExtendedPosixParser(true);
    Options options = CommandLineHelper.getCommonOptions();

    try {
      CommandLine line = parser.parse(options, args);
      String commandName = CommandLineHelper.getCommand(line);

      // Handle commands
      if(commandName == null) {
        System.out.println("No command provided.\n");
        printHelp(options);
        System.exit(1);
      } else if(CommandLineHelper.hasFlagOrCommand(line, "version")) {
        CommandLineHelper.printVersion();
        System.exit(0);
      } else if (commandName.equals("help")) {
        // If the argument "help foo", try to call 'foo'.
        String subcommandName = CommandLineHelper.getIndexValue(line, 1);
        if(subcommandName == null) {
          printHelp(options);
        } else if (commands.containsKey(subcommandName)) {
          commands.get(subcommandName).main(args);
        } else {
          System.out.println("Subcommand not recognized.\n");
          printHelp(options);
          System.exit(1);
        }
      } else if (commands.containsKey(commandName)) {
        commands.get(commandName).main(args);
      } else {
        System.out.println("Command not recognized.\n");
        printHelp(options);
        System.exit(1);
      }

    } catch(Exception e) {
      e.printStackTrace();
      printHelp(options);
      System.exit(1);
    }
  }

  /**
   * Print general help plus a list of available commands.
   */
  public static void printHelp(Options options) {
    CommandLineHelper.printHelp(usage, options);
    System.out.println("commands:");
    printHelpEntry("help", "print help for command");
    for(Map.Entry<String,String> entry: descriptions.entrySet()) {
      printHelpEntry(entry);
    }
  }

  public static void printHelpEntry(Map.Entry<String,String> entry) {
    printHelpEntry(entry.getKey(), entry.getValue());
  }

  public static void printHelpEntry(String name, String description) {
    System.out.println(
      String.format(" %-10s %s", name, description));
  }
}
