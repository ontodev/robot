package owltools2;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Handles inputs and outputs for the {@link ExtractOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(ExtractCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialze the command.
     */
    public ExtractCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "load ontology from a file");
        o.addOption("I", "input-iri", true, "load ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        o.addOption("O", "output-iri", true, "set OntologyIRI for output");
        o.addOption("t", "terms",     true, "space-separated terms to extract");
        o.addOption("T", "term-file", true, "load terms from a file");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "extract";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "extract terms from an ontology";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "owltools2 extract --input-file <file> "
             + "--term-file <file> --output <file>";
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
     * Handle the command-line and file operations for the ExtractOperation.
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
     * extract a new ontology.
     *
     * @param inputOntology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the extracted ontology
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

        IRI outputIRI = CommandLineHelper.getOutputIRI(line);
        if(outputIRI == null) {
            outputIRI = inputOntology.getOntologyID().getOntologyIRI();
        }

        outputOntology = ExtractOperation.extract(
                inputOntology,
                CommandLineHelper.getTerms(ioHelper, line),
                outputIRI,
                null);

        File outputFile = CommandLineHelper.getOutputFile(line);
        if (outputFile != null) {
            ioHelper.saveOntology(outputOntology, outputFile);
        }

        return outputOntology;
    }
}
