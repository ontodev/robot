package org.obolibrary.robot;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Add and remove annotations from an ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class AnnotateCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(AnnotateCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public AnnotateCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("R", "remove-annotations",
                false, "remove all annotations on the ontology");
        o.addOption("O", "ontology-iri", true, "set the ontology IRI");
        o.addOption("V", "version-iri",  true, "set the ontology version IRI");
        o.addOption("i", "input",     true, "convert ontology from a file");
        o.addOption("I", "input-iri", true, "convert ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        options = o;

        // This option takes two arguments
        Option a = new Option("a", "annotate ontology with PROP VALUE");
        a.setLongOpt("annotation");
        a.setArgs(2);
        options.addOption(a);
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "annotate";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "annotate ontology";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot annotate --input <file> -a rdfs:comment 'Comment'"
             + " --output <file>";
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
     * add or remove ontology annotations
     * and return the modified ontology.
     *
     * @param ontology the ontology from the previous command, or null
     * @param args the command-line arguments
     * @return the ontology with inferred axioms added
     * @throws Exception on any problem
     */
    public OWLOntology execute(OWLOntology ontology, String[] args)
            throws Exception {
        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

        if (ontology == null) {
            ontology = CommandLineHelper.getInputOntology(ioHelper, line);
        }

        // Remove all annotation on the ontology
        if (line.hasOption("remove-annotations")) {
            OntologyHelper.removeOntologyAnnotations(ontology);
        }

        // Add annotations
        List<String> pairs = Arrays.asList(line.getOptionValues("annotation"));
        for (int i = 0; i < pairs.size(); i += 2) {
            String property = pairs.get(i);
            String value = pairs.get(i + 1);
            OntologyHelper.addOntologyAnnotation(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createValue(value));
        }

        // Set ontology and version IRI
        String ontologyIRI =
            CommandLineHelper.getOptionalValue(line, "ontology-iri");
        String versionIRI =
            CommandLineHelper.getOptionalValue(line, "version-iri");
        if (ontologyIRI != null || versionIRI != null) {
            OntologyHelper.setOntologyIRI(ontology, ontologyIRI, versionIRI);
        }

        CommandLineHelper.maybeSaveOutput(line, ontology);

        return ontology;
    }
}
