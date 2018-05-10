package org.obolibrary.robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCommand implements Command {
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
    public ImportCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "load ontology from a file");
        o.addOption("I", "input-iri", true, "load ontology from an IRI");
        o.addOption("o", "output",    true, "save ontology to a file");
        o.addOption("s", "source",    true, "source CSV for imports");
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "import";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "import ontology terms";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot import --input <file> "
             + "--source <file> "
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
     * Handle the command-line and file operations for the ImportOperation.
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
     * filter axioms from its ontology, modifying it,
     * and return a state with the modified ontology.
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
        state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
        OWLOntology ontology = state.getOntology();
        
        String sourceFile = CommandLineHelper.getRequiredValue(line, "source",
        		"Import source file required.");
        
        for (String importRow :
        	FileUtils.readLines(new File(sourceFile), "utf-8")) {
        	// ID,IRI,term file
        	String[] importDetails = importRow.split(",");
        	OWLOntology source =
        			ioHelper.loadOntology(IRI.create(importDetails[1]));
        	logger.debug("Importing terms from "
        			+ source.getOntologyID().getOntologyIRI() + " into "
        			+ ontology.getOntologyID().getOntologyIRI());
        	ImportOperation.importTerms(ontology, source,
        			FileUtils.readLines(new File(importDetails[2]), "utf-8"));
        }
        
        CommandLineHelper.maybeSaveOutput(line, ontology);
        return state;
    }
}
