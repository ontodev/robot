package org.obolibrary.robot;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.tdb.TDBFactory;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query an ontology using SPARQL.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class QueryOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(QueryOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "query#";

  /** Error message when query type is illegal. Expects: query type. */
  private static final String queryTypeError = NS + "QUERY TYPE ERROR unknown query type: %s";

  /**
   * Given an ontology, return a dataset containing that ontology as the only graph.
   *
   * @param ontology ontology to query
   * @return dataset to query
   * @throws IOException on issue creating temp files
   * @throws OWLOntologyStorageException on issue writing ontology to TTL format
   */
  public static Dataset loadOntology(OWLOntology ontology)
      throws IOException, OWLOntologyStorageException {
    return loadOntology(ontology, false);
  }

  /**
   * Given an ontology and a boolean indicating if imports should be included as graphs, return a
   * dataset either with just the ontology, or the ontology and its imports as separate graphs.
   *
   * @param ontology ontology to query
   * @param includeImports if true, include imports as separate graphs
   * @return dataset to query
   * @throws OWLOntologyStorageException on issue writing ontology to TTL format
   */
  public static Dataset loadOntology(OWLOntology ontology, boolean includeImports)
      throws OWLOntologyStorageException, UnsupportedEncodingException {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLOntology> ontologies = new HashSet<>();
    ontologies.add(ontology);
    if (includeImports) {
      ontologies.addAll(ontology.getImports());
    }
    // Instantiate an empty dataset
    Dataset dataset = TDBFactory.createDataset();
    // Load each ontology in the set as a named model
    for (OWLOntology ont : ontologies) {
      Model m = ModelFactory.createDefaultModel();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      manager.saveOntology(ont, new TurtleDocumentFormat(), os);
      String content = new String(os.toByteArray(), "UTF-8");
      RDFParser.fromString(content).lang(RDFLanguages.TTL).parse(m);
      String name = ont.getOntologyID().getOntologyIRI().orNull().toString();
      dataset.addNamedModel(name, m);
      logger.debug("Named graph added: " + name);
    }
    return dataset;
  }

  /**
   * Create an empty Dataset.
   *
   * @return an empty dataset
   */
  public static Dataset createEmptyDataset() {
    return DatasetFactory.create();
  }

  /**
   * Given a SPARQL query, return its default file format as a string.
   *
   * @param query the SPARQL query string
   * @return the format name
   */
  public static String getDefaultFormatName(String query) {
    QueryExecution qExec = QueryExecutionFactory.create(query, createEmptyDataset());
    return getDefaultFormatName(qExec.getQuery());
  }

  /**
   * Given a SPARQL query, return its default file format as a string.
   *
   * @param query the SPARQL query
   * @return the format name
   * @throws IllegalArgumentException on bad query
   */
  public static String getDefaultFormatName(Query query) throws IllegalArgumentException {
    String formatName;
    switch (query.getQueryType()) {
      case Query.QueryTypeAsk:
        formatName = "txt";
        break;
      case Query.QueryTypeConstruct:
        formatName = "ttl";
        break;
      case Query.QueryTypeDescribe:
        formatName = "ttl";
        break;
      case Query.QueryTypeSelect:
        formatName = "csv";
        break;
      default:
        throw new IllegalArgumentException(String.format(queryTypeError, query.getQueryType()));
    }
    return formatName;
  }

  /**
   * Convert a format name string to a language code.
   *
   * @param formatName the format name as a string
   * @return the format language code or null
   */
  public static Lang getFormatLang(String formatName) {
    Lang format;
    formatName = formatName.toLowerCase();
    switch (formatName) {
      case "tsv":
        format = ResultSetLang.SPARQLResultSetTSV;
        break;
      case "ttl":
        format = Lang.TTL;
        break;
      case "jsonld":
        format = Lang.JSONLD;
        break;
      case "nt":
        format = Lang.NT;
        break;
      case "nq":
        format = Lang.NQ;
        break;
      case "csv":
        format = Lang.CSV;
        break;
      case "xml":
        format = Lang.RDFXML;
        break;
      case "sxml":
        format = ResultSetLang.SPARQLResultSetXML;
        break;
      default:
        format = null;
    }
    return format;
  }

  /**
   * Given a dataset, a query string, a boolean indiciating to use named graphs or not, an output
   * format name, and an output stream, run the query and write to output.
   *
   * @param dataset Dataset to query over
   * @param queryString query to run
   * @param withGraphs if true, use named graphs, otherwise run the query on the union of all graphs
   * @param formatName format of output
   * @param output output stream to write to
   * @return
   */
  public static boolean runSparqlQuery(
      Dataset dataset,
      String queryString,
      boolean withGraphs,
      String formatName,
      OutputStream output) {
    if (!withGraphs) {
      return runSparqlQueryOnUnion(dataset, queryString, formatName, output);
    } else {
      return runSparqlQueryWithGraphs(dataset, queryString, formatName, output);
    }
  }

  /**
   * If a result set has results, return true, otherwise false.
   *
   * @param result the results to write
   * @return true if there are results, false otherwise
   */
  public static boolean hasResult(ResultSet result) {
    if (result.hasNext()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * If a model has statements, return true, otherwise false.
   *
   * @param result the results to write
   * @return true if there are statements, false otherwise
   */
  public static boolean hasResult(Model result) {
    if (result.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * If a result set has results, write to the output stream and return true. Otherwise return
   * false.
   *
   * @param result the results to write
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   * @return true if there were results, false otherwise
   */
  public static boolean maybeWriteResult(ResultSet result, String formatName, OutputStream output) {
    if (hasResult(result)) {
      writeResult(result, formatName, output);
      return true;
    } else {
      return false;
    }
  }

  /**
   * If a model has statements, write to the output stream and return true. Otherwise return false.
   *
   * @param result the model to write
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   * @return true if there were statements, false otherwise
   */
  public static boolean maybeWriteResult(Model result, String formatName, OutputStream output) {
    if (hasResult(result)) {
      writeResult(result, formatName, output);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Write a boolean result to an output stream.
   *
   * @param result the boolean to write
   * @param output the output stream to write to
   */
  public static void writeResult(boolean result, OutputStream output) {
    PrintStream printStream = new PrintStream(output);
    printStream.print(result);
  }

  /**
   * Write a result set to an output stream.
   *
   * @param resultSet the results to write
   * @param format the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(ResultSet resultSet, Lang format, OutputStream output) {
    if (format == null) {
      format = Lang.CSV;
    }
    ResultSetMgr.write(output, resultSet, format);
  }

  /**
   * Write a result set to an output stream.
   *
   * @param resultSet the results to write
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(ResultSet resultSet, String formatName, OutputStream output) {
    writeResult(resultSet, getFormatLang(formatName), output);
  }

  /**
   * Write a model to an output stream.
   *
   * @param model the model to write
   * @param format the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(Model model, Lang format, OutputStream output) {
    if (format == null) {
      format = Lang.TTL;
    }
    RDFDataMgr.write(output, model, format);
  }

  /**
   * Write a model to an output stream.
   *
   * @param model the model to write
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(Model model, String formatName, OutputStream output) {
    writeResult(model, getFormatLang(formatName), output);
  }

  /**
   * Execute a SPARQL SELECT query and return a result set.
   *
   * @param model the model to query over
   * @param query the SPARQL query string
   * @return the result set
   */
  public static ResultSet execQuery(Model model, String query) {
    QueryExecution qExec = QueryExecutionFactory.create(query, model);
    return qExec.execSelect();
  }

  /**
   * Execute a SPARQL CONSTRUCT query and return a model.
   *
   * @param model The model to construct in
   * @param query The SPARQL construct query string
   * @return The result model
   */
  public static Model execConstruct(Model model, String query) {
    QueryExecution qExec = QueryExecutionFactory.create(query, model);
    return qExec.execConstruct();
  }

  /**
   * Run a SELECT query and write the result to a file.
   *
   * @param dataset The dataset to query over.
   * @param query The SPARQL query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   */
  public static void runQuery(Dataset dataset, String query, File output, Lang outputFormat)
      throws FileNotFoundException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    writeResult(
        execQuery(dataset.getUnionModel(), query), outputFormat, new FileOutputStream(output));
  }

  /**
   * Run a CONSTRUCT query and write the result to a file.
   *
   * @param model the Model to construct in.
   * @param query The SPARQL construct query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   */
  public static void runConstruct(Model model, String query, File output, Lang outputFormat)
      throws FileNotFoundException {
    writeResult(execConstruct(model, query), outputFormat, new FileOutputStream(output));
  }

  /**
   * Execute a verification. Writes to STDERR.
   *
   * @param queriesResults a map from files to query results and output streams
   * @return true if there are any violations
   */
  public static boolean execVerify(
      Map<File, Tuple<ResultSetRewindable, OutputStream>> queriesResults) throws IOException {
    boolean isViolation = false;
    for (File outFile : queriesResults.keySet()) {
      Tuple<ResultSetRewindable, OutputStream> resultAndStream = queriesResults.get(outFile);
      ResultSetRewindable results = resultAndStream.left();
      OutputStream outStream = resultAndStream.right();
      System.out.println(
          "Rule " + outFile.getCanonicalPath() + ": " + results.size() + " violation(s)");
      if (results.size() > 0) {
        isViolation = true;
        ResultSetMgr.write(System.err, results, Lang.CSV);
        results.reset();
      }
      System.err.print('\n');
      ResultSetMgr.write(outStream, results, Lang.CSV);
    }
    return isViolation;
  }

  /**
   * Execute a SPARQL query and return true if there are any results, false otherwise. Prints
   * violations to STDERR.
   *
   * @param model the graph to query over
   * @param query the SPARQL query string
   * @return true if the are results, false otherwise
   */
  public static boolean execVerify(Model model, String ruleName, String query) {
    ResultSetRewindable results = ResultSetFactory.copyResults(execQuery(model, query));
    System.out.println("Rule " + ruleName + ": " + results.size() + " violation(s)");
    if (results.size() == 0) {
      System.out.println("PASS Rule " + ruleName + ": 0 violation(s)");
      return false;
    } else {
      ResultSetMgr.write(System.err, results, Lang.CSV);
      System.out.println("FAIL Rule " + ruleName + ": " + results.size() + " violation(s)");
      return true;
    }
  }

  /**
   * Run a SELECT query over the union of named graphs in a dataset and write the result to a file. Prints violations to STDERR.
   *
   * @param dataset The graph to query over.
   * @param query The SPARQL query string.
   * @param outputPath The file path to write to, if there are results
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   * @return true if the are results (so file is written), false otherwise
   */
  public static boolean runVerify(
      Dataset dataset, String ruleName, String query, Path outputPath, Lang outputFormat)
      throws FileNotFoundException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    ResultSetRewindable results =
        ResultSetFactory.copyResults(execQuery(dataset.getUnionModel(), query));
    if (results.size() == 0) {
      System.out.println("PASS Rule " + ruleName + ": 0 violation(s)");
      return false;
    } else {
      System.out.println("FAIL Rule " + ruleName + ": " + results.size() + " violation(s)");
      ResultSetMgr.write(System.err, results, Lang.CSV);
      results.reset();
      FileOutputStream csvFile = new FileOutputStream(outputPath.toFile());
      writeResult(results, outputFormat, csvFile);
      return true;
    }
  }

  /**
   * Count results.
   *
   * @param results The result set to count.
   * @return the size of the result set
   */
  public static int countResults(ResultSet results) {
    int i = 0;
    while (results.hasNext()) {
      results.next();
      i++;
    }
    return i;
  }

  /**
   * Given a query execution, a format name, and an output stream, run the query and write to
   * output.
   *
   * @param qExec QueryExecution to run
   * @param formatName format of output
   * @param output output stream to write to
   * @return true if successful
   */
  private static boolean runSparqlQuery(
      QueryExecution qExec, String formatName, OutputStream output) {
    Query query = qExec.getQuery();
    if (formatName == null) {
      formatName = getDefaultFormatName(query);
    }
    switch (query.getQueryType()) {
      case Query.QueryTypeAsk:
        writeResult(qExec.execAsk(), output);
        return true;
      case Query.QueryTypeConstruct:
        return maybeWriteResult(qExec.execConstruct(), formatName, output);
      case Query.QueryTypeDescribe:
        return maybeWriteResult(qExec.execDescribe(), formatName, output);
      case Query.QueryTypeSelect:
        return maybeWriteResult(qExec.execSelect(), formatName, output);
      default:
        throw new IllegalArgumentException(String.format(queryTypeError, query.getQueryType()));
    }
  }

  /**
   * Given a dataset, a query string, a format name, and an output stream, run the SPARQL query over
   * the named graphs and write the output to the stream.
   *
   * @param dataset Dataset to query over
   * @param queryString query to run
   * @param formatName format of output
   * @param output output stream to write to
   * @return true if successful
   */
  private static boolean runSparqlQueryWithGraphs(
      Dataset dataset, String queryString, String formatName, OutputStream output) {
    dataset.begin(ReadWrite.READ);
    boolean result;
    try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
      result = runSparqlQuery(qExec, formatName, output);
    } finally {
      dataset.end();
    }
    return result;
  }

  /**
   * Given a dataset, a query string, a format name, and an output stream, run the SPARQL query on
   * the union of all named graphs in the dataset and write the output to the output stream.
   *
   * @param dataset Dataset to query over
   * @param queryString query to run
   * @param formatName format of output
   * @param output output stream to write to
   * @return true if successful
   */
  private static boolean runSparqlQueryOnUnion(
      Dataset dataset, String queryString, String formatName, OutputStream output) {
    dataset.begin(ReadWrite.READ);
    Model model = dataset.getUnionModel();
    boolean result;
    try (QueryExecution qExec = QueryExecutionFactory.create(queryString, model)) {
      result = runSparqlQuery(qExec, formatName, output);
    } finally {
      dataset.end();
    }
    return result;
  }
}
