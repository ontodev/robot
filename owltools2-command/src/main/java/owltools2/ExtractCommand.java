package owltools2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

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
        LoggerFactory.getLogger(CatalogXmlIRIMapper.class);

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
            CommandLine line = CommandLineHelper.getCommandLine(
                    getUsage(), getOptions(), args);
            IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
            ioHelper.saveOntology(
                    ExtractOperation.extract(
                        CommandLineHelper.getInputOntology(ioHelper, line),
                        CommandLineHelper.getTerms(ioHelper, line),
                        CommandLineHelper.getOutputIRI(line),
                        null
                        ),
                    CommandLineHelper.getOutputFile(line));
        } catch (Exception e) {
            CommandLineHelper.handleException(getUsage(), getOptions(), e);
        }
    }
}
