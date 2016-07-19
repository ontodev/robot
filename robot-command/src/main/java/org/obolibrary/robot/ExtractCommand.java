package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

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
        o.addOption("m", "method",    true, "extract method to use");
        o.addOption("t", "term",      true, "term to extract");
        o.addOption("T", "term-file", true, "load terms from a file");
        o.addOption("u", "upper-term",  true, "upper level term to extract");
        o.addOption("U", "upper-terms", true, "upper level terms to extract");
        o.addOption("l", "lower-term",  true, "lower level term to extract");
        o.addOption("L", "lower-terms", true, "lower level terms to extract");
        o.addOption("b", "branch-from-term",  true,
                "root term of branch to extract");
        o.addOption("B", "branch-from-terms", true,
                "root terms of branches to extract");
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
        return "robot extract --input <file> "
             + "--term-file <file> "
             + "--output <file> "
             + "--output-iri <iri>";
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
     * Given an input state and command line arguments,
     * extract a new ontology and return an new state.
     * The input ontology is not changed.
     *
     * @param state the state from the previous command, or null
     * @param args the command-line arguments
     * @return a new state with the extracted ontology
     * @throws Exception on any problem
     */
    public CommandState execute(CommandState state, String[] args)
            throws Exception {
        OWLOntology outputOntology = null;

        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
        state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
        OWLOntology inputOntology = state.getOntology();

        IRI outputIRI = CommandLineHelper.getOutputIRI(line);
        if (outputIRI == null) {
            outputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();
        }

        String method = CommandLineHelper
            .getDefaultValue(line, "method", "mireot")
            .trim().toLowerCase();
        ModuleType moduleType = null;
        if (method.equals("star")) {
            moduleType = ModuleType.STAR;
        } else if (method.equals("top")) {
            moduleType = ModuleType.TOP;
        } else if (method.equals("bot")) {
            moduleType = ModuleType.BOT;
        }

        if (method.equals("mireot")) {
            List<OWLOntology> outputOntologies = new ArrayList<OWLOntology>();

            Set<IRI> upperIRIs = CommandLineHelper.getTerms(
                    ioHelper, line, "upper-term", "upper-terms");
            if (upperIRIs != null && upperIRIs.size() == 0) {
                upperIRIs = null;
            }
            Set<IRI> lowerIRIs = CommandLineHelper.getTerms(
                    ioHelper, line, "lower-term", "lower-terms");
            if (lowerIRIs != null && lowerIRIs.size() != 0) {
                outputOntologies.add(
                    MireotOperation.getAncestors(inputOntology,
                        upperIRIs, lowerIRIs, null));
            }

            Set<IRI> branchIRIs = CommandLineHelper.getTerms(
                    ioHelper, line, "branch-from-term", "branch-from-terms");
            if (branchIRIs != null && branchIRIs.size() != 0) {
                outputOntologies.add(
                    MireotOperation.getDescendants(inputOntology,
                        branchIRIs, null));
            }

            outputOntology = MergeOperation.merge(outputOntologies);
        } else if (moduleType != null) {
            outputOntology = ExtractOperation.extract(
                    inputOntology,
                    CommandLineHelper.getTerms(ioHelper, line),
                    outputIRI,
                    moduleType);
        } else {
            throw new Exception("Method must be: MIREOT, STAR, TOP, BOT");
        }

        CommandLineHelper.maybeSaveOutput(line, outputOntology);

        state.setOntology(outputOntology);
        return state;
    }
}
