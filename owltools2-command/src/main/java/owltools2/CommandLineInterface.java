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
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CommandLineInterface.class);

    /**
     * Given command-line arguments, construct a new CommandManager
     * and execute it.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        CommandManager manager = new CommandManager();

        // Add more commands as needed
        manager.addCommand("extract", new ExtractCommand());

        manager.main(args);
    }
}
