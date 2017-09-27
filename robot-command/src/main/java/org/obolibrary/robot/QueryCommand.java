package org.obolibrary.robot;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.hp.hpl.jena.sparql.algebra.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.core.DatasetGraph;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.resultset.ResultSetLang;


/**
 * Handles inputs and outputs for the {@link QueryOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class QueryCommand implements Command {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(QueryCommand.class);

    /**
     * Store the command-line options for the command.
     */
    private Options options;

    /**
     * Initialze the command.
     */
    public QueryCommand() {
        Options o = CommandLineHelper.getCommonOptions();
        o.addOption("i", "input",     true, "load ontology from a file");
        o.addOption("I", "input-iri", true, "load ontology from an IRI");
        o.addOption("f", "format",    true, "the query result format: CSV, TSV,"
                + " TTL, JSONLD, etc.");
        o.addOption("q", "sparql", true, "Path to a sparql query file");
        o.addOption("O", "output", true, "Path to file where the results will be written to");
        Option select = new Option("s", "select", true,
            "run a SPARQL select query and output result");
        select.setArgs(2);
        o.addOption(select);
        Option construct = new Option("c", "construct", true,
            "run a SPARQL construct query and output result");
        construct.setArgs(2);
        o.addOption(construct);
        options = o;
    }

    /**
     * Name of the command.
     *
     * @return name
     */
    public String getName() {
        return "query";
    }

    /**
     * Brief description of the command.
     *
     * @return description
     */
    public String getDescription() {
        return "query an ontology";
    }

    /**
     * Command-line usage for the command.
     *
     * @return usage
     */
    public String getUsage() {
        return "robot query --input <file> "
             + "--select <query> <result> \n" +
            "robot query --input <rdf data> --sparql <query file> --output <path to results>";
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
     * Handle the command-line and file operations for the QueryOperation.
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
     * query the ontolgy.
     * The input ontology is not changed.
     *
     * @param state the state from the previous command, or null
     * @param args the command-line arguments
     * @return the unchanged state
     * @throws Exception on any problem
     */
    public CommandState execute(CommandState state, String[] args)
            throws Exception {

        CommandLine line = CommandLineHelper
            .getCommandLine(getUsage(), getOptions(), args);
        if (line == null) {
            return null;
        }
        String formatName = CommandLineHelper.getOptionalValue(line, "format");
        Optional<Lang> outputFormat = Optional.empty();
        if (formatName != null) {
            formatName = formatName.toLowerCase();
            switch (formatName) {
                case "tsv":
                    outputFormat = Optional.of(ResultSetLang.SPARQLResultSetTSV);
                    break;
                case "ttl":
                    outputFormat = Optional.of(Lang.TTL);
                    break;
                case "jsonld":
                    outputFormat = Optional.of(Lang.JSONLD);
                    break;
                case "nt":
                    outputFormat = Optional.of(Lang.NT);
                    break;
                case "nq":
                    outputFormat = Optional.of(Lang.NQ);
                    break;
                case "csv":
                    outputFormat = Optional.of(Lang.CSV);
                    break;
                case "xml":
                    outputFormat = Optional.of(Lang.RDFXML);
                    break;
                case "sxml":
                    outputFormat = Optional.of(ResultSetLang.SPARQLResultSetXML);
                    break;
                default:
                    outputFormat = Optional.empty();
            }
        }

        IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
        state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
        DatasetGraph dsg = QueryOperation.loadOntology(state.getOntology());

        if(line.hasOption("sparql") && line.hasOption("output")) {
            String queryPath = line.getOptionValue("sparql");
            String outputPath = line.getOptionValue("output");
            String query = FileUtils.readFileToString(new File(queryPath));

            QueryOperation.runSparqlQuery(dsg, query, new File(outputPath), outputFormat);
            return state;
        }

        // Handle select
        if (line.hasOption("select")) {
            List<String> select =
                CommandLineHelper.getOptionValues(line, "select");

            for (int i = 0; i < select.size(); i = i + 2) {
                String query = FileUtils.readFileToString(new File(
                        select.get(i)));
                File output = new File(select.get(i + 1));
                QueryOperation.runQuery(dsg, query, output, outputFormat.orElse(Lang.CSV));
            }
        //Handle Construct
        } else if (line.hasOption("construct")) {
            List<String> select = CommandLineHelper.getOptionalValues(line,
                    "construct");

            for (int i = 0; i < select.size(); i += 2) {
                String query = FileUtils.readFileToString(new File(
                        select.get(i)));
                File output = new File(select.get(i + 1));
                QueryOperation.runConstruct(dsg, query, output, outputFormat.orElse(Lang.TTL));
            }
        }

        return state;
    }
}
