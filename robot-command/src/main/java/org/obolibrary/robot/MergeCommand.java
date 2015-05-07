package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Handles inputs and outputs for the {@link MergeOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MergeCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public MergeCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "merge ontology from a file");
        o.addOption("I", "input-iri", true, "merge ontology from an IRI");
        o.addOption("o", "output",    true, "save merged ontology to a file");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "merge";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "merge ontologies";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot filter --input <file> --input <file> --output <file>";
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
     * Handle the command-line and file operations for the MergeOperation.
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
     * merge all ontology axioms to create a new ontology.
     *
     * @param inputOntology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the new merged ontology
     * @throws Exception on any problem
     */
    public OWLOntology execute(OWLOntology inputOntology, String[] args)
            throws Exception {
        OWLOntology outputOntology = null;

        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

        List<OWLOntology> inputOntologies = new ArrayList<OWLOntology>();
        if (inputOntology != null) {
            inputOntologies.add(inputOntology);
        }
        inputOntologies.addAll(
            CommandLineHelper.getInputOntologies(ioHelper, line));

        if (inputOntologies.size() < 1) {
            throw new IllegalArgumentException(
                    "at least one inputOntology must be specified");
        }

        outputOntology = MergeOperation.merge(inputOntologies);

        CommandLineHelper.maybeSaveOutput(line, outputOntology);

        return outputOntology;
    }
}
