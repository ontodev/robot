package org.obolibrary.robot;

import com.google.common.io.Files;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;
import org.obolibrary.robot.exceptions.CannotReadQuery;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * Command that runs a sparql query expecting zero results. Any results represent violations in the queried ontology
 */
public class VerifyCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(VerifyCommand.class);

    @Override
    public String getName() {
        return "verify";
    }

    @Override
    public String getDescription() {
        return "Runs a sparql query on an ontology. Any results of the query are violations, counted, and reported";
    }

    @Override
    public String getUsage() {
        return "robot verify --input ONTOLOGY --queries FILE [FILE [...]] --report-dir DIR";
    }

    @Override
    public Options getOptions() {
        Options options = CommandLineHelper.getCommonOptions();
        Option queries = new Option("q", "queries", true, "List of sparql queries to find violations");
        queries.setRequired(true);
        queries.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(queries);
        Option report = new Option("O", "output-dir", true, "Directory to place reports in");
        report.setRequired(true);
        options.addOption(report);
        options.addOption("i", "input", true, "Input Ontology");
        return options;
    }

    @Override
    public void main(String[] args) {
        try {
            execute(null, args);
        } catch (Exception e) {
            CommandLineHelper.handleException(getUsage(), getOptions(), e);
        }
    }

    @Override
    public CommandState execute(CommandState inputState, String[] args) throws Exception {

        CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = ontologyManager.loadOntologyFromOntologyDocument(new File(line.getOptionValue("input")));
        DatasetGraph graph = QueryOperation.loadOntology(ontology);

        File resultDir = new File(line.getOptionValue("output-dir"));

        Map<File, Tuple<ResultSetRewindable, OutputStream>> resultMap = new HashMap<>();
        for(String filePath : line.getOptionValues("queries")) {
            File query = new File(filePath);
            ResultSet results = QueryOperation.execQuery(graph, fileContents(query));
            ResultSetRewindable resultsCopy = ResultSetFactory.copyResults(results);
            File resultCsv = resultDir.toPath().resolve(FilenameUtils.getBaseName(filePath).concat(".csv")).toFile();
            resultMap.put(query, new Tuple<>(resultsCopy, new FileOutputStream(resultCsv)));
        }

        boolean violationsExist = QueryOperation.execVerify(resultMap);
        if(violationsExist) {
            System.exit(1);
        }

        return inputState;
    }

    private static String fileContents(File file) {
        try {
            return Files.toString(file, Charset.defaultCharset());
        } catch (IOException e) {
            throw new CannotReadQuery("Cannot read from " + file + ": " + e.getMessage(), e);

        }
    }
}
