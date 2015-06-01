package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Handles inputs and outputs for the {@link TemplateOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class TemplateCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(TemplateCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public TemplateCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",         true, "load ontology from a file");
        o.addOption("I", "input-iri",     true, "load ontology from an IRI");
        o.addOption("o", "output",        true, "save ontology to a file");
        o.addOption("O", "output-iri",    true, "set the output ontology IRI");
        o.addOption("V", "version-iri",   true, "set the output version IRI");
        o.addOption("t", "template-file", true, "read template from a file");
        o.addOption("m", "merge-before",
            false, "merge into input ontology before any output");
        o.addOption("M", "merge-after",
            false, "merge into input ontology after any output");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "template";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "build an ontology from a template";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot template --input <file> "
             + "--template-file <file> "
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
     * Handle the command-line and file operations for the TemplateOperation.
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
     * build a new ontology from the given template,
     * and return a state with the new ontology.
     *
     * @param state the state from the previous command, or null
     * @param args the command-line arguments
     * @return the state with the filtered ontology
     * @throws Exception on any problem
     */
    public CommandState execute(CommandState state, String[] args)
            throws Exception {
        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        if (state == null) {
            state = new CommandState();
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
        state = CommandLineHelper.updateInputOntology(
                ioHelper, state, line, false);
        OWLOntology inputOntology = state.getOntology();

        // Read the whole CSV into a nested list of strings.
        String templatePath = CommandLineHelper.getRequiredValue(
            line, "template-file", "a template-file is required");
        List<List<String>> rows = ioHelper.readTable(templatePath);

        OWLOntology outputOntology =
            TemplateOperation.template(rows, inputOntology, null, ioHelper);

        // Either merge-then-save, save-then-merge, or don't merge
        List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
        ontologies.add(outputOntology);
        if (line.hasOption("merge-before")) {
            MergeOperation.mergeInto(ontologies, inputOntology);
            CommandLineHelper.maybeSaveOutput(line, inputOntology);
        } else if (line.hasOption("merge-after")) {
            CommandLineHelper.maybeSaveOutput(line, outputOntology);
            MergeOperation.mergeInto(ontologies, inputOntology);
        } else {
            // Set ontology and version IRI
            String ontologyIRI =
                CommandLineHelper.getOptionalValue(line, "ontology-iri");
            String versionIRI =
                CommandLineHelper.getOptionalValue(line, "version-iri");
            if (ontologyIRI != null || versionIRI != null) {
                OntologyHelper.setOntologyIRI(
                    outputOntology, ontologyIRI, versionIRI);
            }
            CommandLineHelper.maybeSaveOutput(line, outputOntology);
            state.setOntology(outputOntology);
        }

        return state;
    }
}
