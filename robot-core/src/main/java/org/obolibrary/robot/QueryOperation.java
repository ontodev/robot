package org.obolibrary.robot;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import java.io.*;
import java.util.Map;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
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
   * Load an ontology into a DatasetGraph. The ontology is not changed. NOTE: This is not elegant!
   * It basically pipes Turtle output from OWLAPI to Jena Arq.
   *
   * @param ontology The ontology to load into the graph.
   * @return A new DatasetGraph with the ontology loaded into the default graph.
   * @throws OWLOntologyStorageException rarely
   */
  public static DatasetGraph loadOntology(OWLOntology ontology) throws OWLOntologyStorageException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ontology.getOWLOntologyManager().saveOntology(ontology, new TurtleDocumentFormat(), out);
    DatasetGraph dsg = DatasetGraphFactory.createMem();
    RDFDataMgr.read(
        dsg.getDefaultGraph(), new ByteArrayInputStream(out.toByteArray()), Lang.TURTLE);
    return dsg;
  }

  /**
   * Create an empty Dataset.
   *
   * @return an empty dataset
   */
  public static Dataset createEmptyDataset() {
    return DatasetFactory.create(DatasetGraphFactory.createMem());
  }

  /**
   * Given a SPARQL query, return its type as a string: ASK, CONSTRUCT, DESCRIBE, SELECT.
   *
   * @param queryString the SPARQL query string
   * @return the query type string or null
   * @throws IllegalArgumentException on bad query
   */
  public static String getQueryTypeName(String queryString) throws IllegalArgumentException {
    // TODO: This method is unused
    QueryExecution qexec = QueryExecutionFactory.create(queryString, createEmptyDataset());
    Query query = qexec.getQuery();
    String queryType = null;
    switch (query.getQueryType()) {
      case Query.QueryTypeAsk:
        queryType = "ASK";
        break;
      case Query.QueryTypeConstruct:
        queryType = "CONSTRUCT";
        break;
      case Query.QueryTypeDescribe:
        queryType = "DESCRIBE";
        break;
      case Query.QueryTypeSelect:
        queryType = "SELECT";
        break;
      default:
        throw new IllegalArgumentException(String.format(queryTypeError, query.getQueryType()));
    }
    return queryType;
  }

  /**
   * Given a SPARQL query, return its default file format as a string.
   *
   * @param query the SPARQL query string
   * @return the format name
   */
  public static String getDefaultFormatName(String query) {
    QueryExecution qexec = QueryExecutionFactory.create(query, createEmptyDataset());
    return getDefaultFormatName(qexec.getQuery());
  }

  /**
   * Given a SPARQL query, return its default file format as a string.
   *
   * @param query the SPARQL query
   * @return the format name
   * @throws IllegalArgumentException on bad query
   */
  public static String getDefaultFormatName(Query query) throws IllegalArgumentException {
    String formatName = null;
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
   * Run a SPARQL query (ASK, CONSTRUCT, DESCRIBE, SELECT) and return true if there were results,
   * false otherwise.
   *
   * @param qexec the SPARQL QueryExecution to run
   * @param formatName the name of the output format
   * @param output the OutputStream to write to
   * @return true if there are any results, false otherwise
   * @throws IllegalArgumentException on bad query
   */
  public static boolean runSparqlQuery(QueryExecution qexec, String formatName, OutputStream output)
      throws IllegalArgumentException {
    boolean result;
    Query query = qexec.getQuery();
    if (formatName == null) {
      formatName = getDefaultFormatName(query);
    }

    switch (query.getQueryType()) {
      case Query.QueryTypeAsk:
        result = true;
        writeResult(qexec.execAsk(), output);
        break;
      case Query.QueryTypeConstruct:
        result = maybeWriteResult(qexec.execConstruct(), formatName, output);
        break;
      case Query.QueryTypeDescribe:
        result = maybeWriteResult(qexec.execDescribe(), formatName, output);
        break;
      case Query.QueryTypeSelect:
        result = maybeWriteResult(qexec.execSelect(), formatName, output);
        break;
      default:
        throw new IllegalArgumentException(String.format(queryTypeError, query.getQueryType()));
    }

    return result;
  }

  /**
   * Run a SPARQL query (ASK, CONSTRUCT, DESCRIBE, SELECT) and return true if there were results,
   * false otherwise.
   *
   * @param dsg the graph to query over
   * @param query the SPARQL query string
   * @param formatName the name of the output format
   * @param output the OutputStream to write to
   * @return true if there are any results, false otherwise
   */
  public static boolean runSparqlQuery(
      DatasetGraph dsg, String query, String formatName, OutputStream output) {
    QueryExecution qexec = QueryExecutionFactory.create(query, DatasetFactory.create(dsg));
    return runSparqlQuery(qexec, formatName, output);
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
   * @param dsg the graph to query over
   * @param query the SPARQL query string
   * @return the result set
   */
  public static ResultSet execQuery(DatasetGraph dsg, String query) {
    QueryExecution qexec = QueryExecutionFactory.create(query, DatasetFactory.create(dsg));
    return qexec.execSelect();
  }

  /**
   * Execute a SPARQL CONSTRUCT query and return a model.
   *
   * @param dsg The graph to construct in.
   * @param query The SPARQL construct query string.
   * @return The result model
   */
  public static Model execConstruct(DatasetGraph dsg, String query) {
    QueryExecution qexec = QueryExecutionFactory.create(query, DatasetFactory.create(dsg));
    return qexec.execConstruct();
  }

  /**
   * Run a SELECT query and write the result to a file.
   *
   * @param dsg The graph to query over.
   * @param query The SPARQL query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   */
  public static void runQuery(DatasetGraph dsg, String query, File output, Lang outputFormat)
      throws FileNotFoundException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    writeResult(execQuery(dsg, query), outputFormat, new FileOutputStream(output));
  }

  /**
   * Run a CONSTRUCT query and write the result to a file.
   *
   * @param dsg The graph to construct in.
   * @param query The SPARQL construct query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   */
  public static void runConstruct(DatasetGraph dsg, String query, File output, Lang outputFormat)
      throws FileNotFoundException {
    writeResult(execConstruct(dsg, query), outputFormat, new FileOutputStream(output));
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
   * @param dsg the graph to query over
   * @param query the SPARQL query string
   * @return true if the are results, false otherwise
   */
  public static boolean execVerify(DatasetGraph dsg, String ruleName, String query) {
    ResultSetRewindable results = ResultSetFactory.copyResults(execQuery(dsg, query));
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
   * Run a SELECT query and write the result to a file. Prints violations to STDERR.
   *
   * @param dsg The graph to query over.
   * @param query The SPARQL query string.
   * @param output The file to write to, if there are results
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   * @return true if the are results (so file is written), false otherwise
   */
  public static boolean runVerify(
      DatasetGraph dsg, String ruleName, String query, OutputStream output, Lang outputFormat)
      throws FileNotFoundException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    ResultSetRewindable results = ResultSetFactory.copyResults(execQuery(dsg, query));
    if (results.size() == 0) {
      System.out.println("PASS Rule " + ruleName + ": 0 violation(s)");
      return false;
    } else {
      System.out.println("FAIL Rule " + ruleName + ": " + results.size() + " violation(s)");
      ResultSetMgr.write(System.err, results, Lang.CSV);
      results.reset();
      writeResult(results, outputFormat, new FileOutputStream(output));
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
}
