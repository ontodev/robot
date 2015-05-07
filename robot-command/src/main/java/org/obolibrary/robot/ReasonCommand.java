package org.obolibrary.robot;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Handles inputs and outputs for the {@link ReasonOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReasonCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(ReasonCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public ReasonCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("r", "reasoner",  true, "reasoner to use: (ELK, HermiT)");
        o.addOption("s", "remove-redundant-subclass-axioms",
                true, "remove redundant subclass axioms");
        o.addOption("i", "input",     true, "reason ontology from a file");
        o.addOption("I", "input-iri", true, "reason ontology from an IRI");
        o.addOption("o", "output",    true, "save reasoned ontology to a file");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "reason";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "reason ontology";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot reason --input <file> --reasoner <name>"
             + "[options] --output <file>";
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
     * Handle the command-line and file operations for the ReasonOperation.
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
     * run a reasoner, and add axioms to the ontology.
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

        // ELK is the default reasoner
        String reasonerName = CommandLineHelper.getDefaultValue(
                line, "reasoner", "ELK").trim().toLowerCase();
        OWLReasonerFactory reasonerFactory;
        if (reasonerName.equals("structural")) {
            reasonerFactory = new org.semanticweb.owlapi.reasoner
                .structural.StructuralReasonerFactory();
        } else if (reasonerName.equals("hermit")) {
            reasonerFactory = new org.semanticweb
                .HermiT.Reasoner.ReasonerFactory();
        } else {
            reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        }

        // Override default reasoner options with command-line options
        Map<String, String> reasonerOptions =
            ReasonOperation.getDefaultOptions();
        for (String option: reasonerOptions.keySet()) {
            if (line.hasOption(option)) {
                reasonerOptions.put(option, line.getOptionValue(option));
            }
        }

        ReasonOperation.reason(ontology, reasonerFactory, reasonerOptions);

        CommandLineHelper.maybeSaveOutput(line, ontology);

        return ontology;
    }
}
