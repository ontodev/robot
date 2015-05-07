package org.obolibrary.robot;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

/**
 * Save ontology to a different format.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ConvertCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(ConvertCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialize the command.
     */
    public ConvertCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "convert ontology from a file");
        o.addOption("I", "input-iri", true, "convert ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        o.addOption("f", "format",    true, "the format: RDFXML, OBO, etc.");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "convert";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "convert ontology";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot convert --input <file> --format <format>"
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
     * save it to a new format and return the ontology unchanged.
     *
     * Suported formats:
     *
     * <li>OBO .obo
     * <li>RDFXML .owl
     * <li>Turtle .ttl
     * <li>OWLXML .owx
     * <li>Manchester .omn
     * <li>OWL Functional .ofn
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

        File outputFile = CommandLineHelper.getOutputFile(line);
        if (outputFile == null) {
            throw new Exception("Output file is required.");
        }

        String formatName = CommandLineHelper.getOptionalValue(line, "format");
        if (formatName == null) {
            formatName = FilenameUtils.getExtension(outputFile.getName());
        }
        formatName = formatName.trim().toLowerCase();

        OWLOntologyFormat format;
        if (formatName.equals("obo")) {
            format = new org.coode.owlapi.obo.parser.OBOOntologyFormat();
        } else if (formatName.equals("owl")) {
            format = new org.semanticweb.owlapi.io.RDFXMLOntologyFormat();
        } else if (formatName.equals("ttl")) {
            format = new org.coode.owlapi.turtle.TurtleOntologyFormat();
        } else if (formatName.equals("owx")) {
            format = new org.semanticweb.owlapi.io.OWLXMLOntologyFormat();
        } else if (formatName.equals("omn")) {
            format = new org.coode.owlapi.manchesterowlsyntax
                .ManchesterOWLSyntaxOntologyFormat();
        } else if (formatName.equals("ofn")) {
            format = new org.semanticweb.owlapi.io
                .OWLFunctionalSyntaxOntologyFormat();
        } else {
            throw new Exception("Unknown ontology format: " + formatName);
        }

        if (format instanceof PrefixOWLOntologyFormat) {
            ((PrefixOWLOntologyFormat) format)
                .copyPrefixesFrom(ioHelper.getPrefixManager());
        }

        ioHelper.saveOntology(ontology, format, outputFile);

        return ontology;
    }
}
