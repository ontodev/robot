package org.obolibrary.robot;

import java.util.ArrayList;
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
        o.addOption("A", "annotation-file",
                true, "add annotation from a file");
        o.addOption("O", "ontology-iri", true, "set the ontology IRI");
        o.addOption("V", "version-iri",  true, "set the ontology version IRI");
        o.addOption("i", "input",     true, "convert ontology from a file");
        o.addOption("I", "input-iri", true, "convert ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        options = o;

        Option a;

        // Annotate with a property and plain literal
        a = new Option("a", "annotate ontology with PROP VALUE");
        a.setLongOpt("annotation");
        a.setArgs(2);
        options.addOption(a);

        // Annotate with a property and plain literal
        a = new Option("k", "annotate ontology with PROP IRI");
        a.setLongOpt("link-annotation");
        a.setArgs(2);
        options.addOption(a);

        // Annotate with a property and a plain literal with a language tag
        a = new Option("l", "annotate ontology with PROP VALUE LANG");
        a.setLongOpt("language-annotation");
        a.setArgs(3);
        options.addOption(a);

        // Annotate with a property and a typed literal with a language tag
        a = new Option("t", "annotate ontology with PROP VALUE LANG");
        a.setLongOpt("typed-annotation");
        a.setArgs(3);
        options.addOption(a);

        // Annotate with a property and a typed literal with a language tag
        a = new Option("x", "annotate all axioms in the ontology "
                          + "with PROP VALUE");
        a.setLongOpt("axiom-annotation");
        a.setArgs(3);
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
        return "robot annotate --input <file> "
             + "--annotate <property> <value> "
             + "--output <file>";
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
     * Given an input state and command line arguments,
     * add or remove ontology annotations
     * and return the modified state.
     *
     * @param state the state from the previous command, or null
     * @param args the command-line arguments
     * @return the state with the updated ontology
     * @throws Exception on any problem
     */
    public CommandState execute(CommandState state, String[] args)
            throws Exception {
        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
        state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
        OWLOntology ontology = state.getOntology();

        // Remove all annotation on the ontology
        if (line.hasOption("remove-annotations")) {
            OntologyHelper.removeOntologyAnnotations(ontology);
        }

        // Add annotations
        List<String> items;

        // Add annotations with PROP VALUE
        items = CommandLineHelper.getOptionValues(line, "annotation");
        while (items.size() > 0) {
            String property = items.remove(0);
            String value    = items.remove(0);
            OntologyHelper.addOntologyAnnotation(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createLiteral(value));
        }

        // Add link-annotations with PROP LINK
        items = CommandLineHelper.getOptionValues(line, "link-annotation");
        while (items.size() > 0) {
            String property = items.remove(0);
            String value    = items.remove(0);
            OntologyHelper.addOntologyAnnotation(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createIRI(value));
        }

        // Add language-annotations with PROP VALUE LANG
        items = CommandLineHelper.getOptionValues(line, "language-annotation");
        while (items.size() > 0) {
            String property = items.remove(0);
            String value    = items.remove(0);
            String lang     = items.remove(0);
            OntologyHelper.addOntologyAnnotation(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createTaggedLiteral(value, lang));
        }

        // Add typed-annotations with PROP VALUE TYPE
        items = CommandLineHelper.getOptionValues(line, "typed-annotation");
        while (items.size() > 0) {
            String property = items.remove(0);
            String value    = items.remove(0);
            String type     = items.remove(0);
            OntologyHelper.addOntologyAnnotation(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createTypedLiteral(value, type));
        }

        // Add annotations with PROP VALUE
        items = CommandLineHelper.getOptionValues(line, "axiom-annotation");
        while (items.size() > 0) {
            String property = items.remove(0);
            String value    = items.remove(0);
            OntologyHelper.addAxiomAnnotations(
                    ontology,
                    ioHelper.createIRI(property),
                    ioHelper.createLiteral(value));
        }


        // Load any annotation files as ontologies and merge them in
        List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
        List<String> paths =
            CommandLineHelper.getOptionValues(line, "annotation-file");
        for (String path: paths) {
            ontologies.add(ioHelper.loadOntology(path));
        }
        if (ontologies.size() > 0) {
            MergeOperation.mergeInto(ontologies, ontology, true);
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

        return state;
    }
}
