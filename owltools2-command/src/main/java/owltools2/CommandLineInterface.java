package owltools2;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

/**
 * The command line interface for OWLTools2.
 * We use Apache CLI and follow the examples here:
 * http://commons.apache.org/proper/commons-cli/usage.html
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandLineInterface {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CommandLineInterface.class);

    /**
     * General usage string.
     */
    private static String usage = "owltools2 [command] [options] <arguments>";

    /**
     * We look up commands in this map.
     */
    private static Map<String, Command> commands =
        new HashMap<String, Command>();


    /**
     * Given an array of command-line arguments, try to execute the chosen
     * command.
     *
     * @param args strings to use as arguments
     */
    public static void main(String[] args) {
        commands.put("extract", new owltools2.ExtractCommand());

        // Ignore unrecognized options
        CommandLineParser parser = new ExtendedPosixParser(true);
        Options options = CommandLineHelper.getCommonOptions();

        try {
            CommandLine line = parser.parse(options, args);
            String commandName = CommandLineHelper.getCommand(line);

            // Handle commands
            if (commandName == null) {
                System.out.println("No command provided.\n");
                printHelp(options);
                System.exit(1);
            } else if (CommandLineHelper.hasFlagOrCommand(line, "version")) {
                CommandLineHelper.printVersion();
                System.exit(0);
            } else if (commandName.equals("help")) {
                // If the argument "help foo", try to call 'foo'.
                String subcommandName = CommandLineHelper.
                    getIndexValue(line, 1);
                if (subcommandName == null) {
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

        } catch (Exception e) {
            e.printStackTrace();
            printHelp(options);
            System.exit(1);
        }
    }

    /**
     * Print general help plus a list of available commands.
     *
     * @param options the command-line options for this command
     */
    private static void printHelp(Options options) {
        CommandLineHelper.printHelp(usage, options);
        System.out.println("commands:");
        printHelpEntry("help", "print help for command");
        for (Map.Entry<String, Command> entry: commands.entrySet()) {
            printHelpEntry(entry);
        }
    }

    /**
     * Print a help entry for a single command.
     *
     * @param entry an entry from the map of commands
     */
    private static void printHelpEntry(Map.Entry<String, Command> entry) {
        printHelpEntry(entry.getKey(), entry.getValue().getDescription());
    }

    /**
     * Print a help entry for a single command.
     *
     * @param name the name of the command
     * @param description a brief description of the command
     */
    private static void printHelpEntry(String name, String description) {
        System.out.println(
                String.format(" %-10s %s", name, description));
    }
}
