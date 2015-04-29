package org.obolibrary.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Simple mock command for testing.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MockCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CatalogXmlIRIMapper.class);

    /**
     * Store options.
     */
    private Options options;

    /**
     * Store the arguments from the last execution.
     */
    private String[] lastArgs = null;

    /**
     * Construct mock command with common options.
     */
    public MockCommand() {
        options = CommandLineHelper.getCommonOptions();
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "mock";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "a mock command, for testing only";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "mock";
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
     * Run mock command.
     *
     * @param args strings to use as arguments
     */
    public void main(String[] args) {
        try {
            execute(null, args);
        } catch (Exception e) {
            return;
        }
    }

    /**
     * Execute mock command, storing the arguments.
     *
     * @param inputOntology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the input ontology
     * @throws Exception on any problem
     */
    public OWLOntology execute(OWLOntology inputOntology, String[] args)
            throws Exception {
        lastArgs = args;

        return inputOntology;
    }

    /**
     * Return the args from the last execution.
     *
     * @return the args from the last execution, or null;
     */
    public String[] getLastArgs() {
        return lastArgs;
    }
}

