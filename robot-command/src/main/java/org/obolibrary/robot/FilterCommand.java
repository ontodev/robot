package org.obolibrary.robot;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Handles inputs and outputs for the {@link FilterOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(FilterCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public FilterCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "load ontology from a file");
        o.addOption("I", "input-iri", true, "load ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        o.addOption("O", "output-iri", true, "set OntologyIRI for output");
        o.addOption("t", "terms",     true, "space-separated terms to filter");
        o.addOption("T", "term-file", true, "load terms from a file");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "filter";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "filter ontology axioms";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot filter --input <file> --term-file <file> --output <file>";
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
     * Handle the command-line and file operations for the FilterOperation.
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
     * filter axioms to create a new ontology.
     *
     * @param inputOntology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the new filtered ontology
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

        if (inputOntology == null) {
            inputOntology = CommandLineHelper.getInputOntology(ioHelper, line);
        }

        Set<IRI> terms = CommandLineHelper.getTerms(ioHelper, line);
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        for (IRI term: terms) {
            properties.add(inputOntology.getOWLOntologyManager()
                    .getOWLDataFactory()
                    .getOWLObjectProperty(term));
        }

        IRI outputIRI = CommandLineHelper.getOutputIRI(line);
        if (outputIRI == null) {
            outputIRI = inputOntology.getOntologyID().getOntologyIRI();
        }

        outputOntology = FilterOperation.filter(
                inputOntology,
                properties,
                outputIRI);

        File outputFile = CommandLineHelper.getOutputFile(line);
        if (outputFile != null) {
            ioHelper.saveOntology(outputOntology, outputFile);
        }

        return outputOntology;
    }
}
