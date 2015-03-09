package owltools2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for OWLTools2 command-line interface.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandLineInterface {
    /**
     * A CommandManager loaded with the default set of commands.
     */
    private static CommandManager manager = new CommandManager();

    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CommandLineInterface.class);

    /**
     * Construct a CommandManager with a set of default commands.
     */
    public CommandLineInterface() {
        // Add more commands as needed
        manager.addCommand("extract", new ExtractCommand());
    }

    /**
     * Execute the given command-line arguments, catching any exceptions.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        manager.main(args);
    }

    /**
     * Execute the given command-line arguments, throwing any exceptions.
     *
     * @param args the command-line arguments
     * @throws Exception on any problem
     */
    public static void execute(String[] args) throws Exception {
        manager.execute(null, args);
    }
}
