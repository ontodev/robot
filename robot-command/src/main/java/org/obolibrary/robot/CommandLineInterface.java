package org.obolibrary.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for ROBOT command-line interface.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class CommandLineInterface {
    /**
     * A CommandManager loaded with the default set of commands.
     */
    private static CommandManager manager = initManager();

    /**
     * Initialize a new CommandManager.
     *
     * @return the new manager
     */
    private static CommandManager initManager() {
        CommandManager m = new CommandManager();
        m.addCommand("diff",            new DiffCommand());
        m.addCommand("export-prefixes", new ExportPrefixesCommand());
        m.addCommand("extract",         new ExtractCommand());
        m.addCommand("filter",          new FilterCommand());
        m.addCommand("merge",           new MergeCommand());
        m.addCommand("reason",          new ReasonCommand());
        return m;
    }

    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CommandLineInterface.class);

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
