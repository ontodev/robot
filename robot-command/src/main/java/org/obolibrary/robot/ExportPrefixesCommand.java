package org.obolibrary.robot;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Works with prefixes for an IOHelper.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExportPrefixesCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(ExportPrefixesCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialze the command.
     */
    public ExportPrefixesCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("o", "output", true, "save prefixes to a file");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "export-prefixes";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "export prefixes to a file";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot export-prefixes --output <file>";
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
     * Handle the command-line and file operations for the command.
     *
     * @param args strings to use as arguments
     */
    public void main(String[] args) {
        try {
            execute(null, args);
        } catch (Exception e) {
            CommandLineHelper.handleException(getUsage(), getOptions(), e);
        }
    }

    /**
     * Given an input ontology (or null) and command line arguments,
     * export the current prefixes
     * and return the input ontology unchanged.
     *
     * @param inputOntology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the input ontology, unchanged
     * @throws Exception on any problem
     */
    public OWLOntology execute(OWLOntology inputOntology, String[] args)
            throws Exception {
        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

        File outputFile = CommandLineHelper.getOutputFile(line);
        if (outputFile != null) {
            ioHelper.saveJSONLDContext(outputFile);
        } else {
            System.out.println(ioHelper.getJSONLDContextString());
        }

        return inputOntology;
    }
}

