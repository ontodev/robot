package org.obolibrary.robot;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
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

  /** Error message when query parsing fails. Expects: error message from parse. */
  private static final String queryParseError =
      NS + "QUERY PARSE ERROR query cannot be parsed:\n%s";

  /** Error message when query type is illegal. Expects: query type. */
  private static final String queryTypeError = NS + "QUERY TYPE ERROR unknown query type: %s";

  /**
   * Load an ontology into a DatasetGraph. The ontology is not changed.
   *
   * @param ontology The ontology to load into the graph
   * @return A new DatasetGraph with the ontology loaded into the default graph
   * @throws OWLOntologyStorageException rarely
   */
  public static DatasetGraph loadOntology(OWLOntology ontology) throws OWLOntologyStorageException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ontology.getOWLOntologyManager().saveOntology(ontology, new TurtleDocumentFormat(), os);
    DatasetGraph dsg = DatasetGraphFactory.create();
    RDFDataMgr.read(dsg.getDefaultGraph(), new ByteArrayInputStream(os.toByteArray()), Lang.TURTLE);
    return dsg;
  }

  /**
   * Given an ontology, return a dataset containing that ontology as the only graph.
   *
   * @param ontology ontology to query
   * @return dataset to query
   * @throws IOException on issue creating temp files
   * @throws OWLOntologyStorageException on issue writing ontology to TTL format
   */
  public static Dataset loadOntologyAsDataset(OWLOntology ontology)
      throws IOException, OWLOntologyStorageException {
    return loadOntologyAsDataset(ontology, false);
  }

  /**
   * Given an ontology and a boolean indicating if imports should be included as graphs, return a
   * dataset either with just the ontology, or the ontology and its imports as separate graphs.
   *
   * @param ontology ontology to query
   * @param useGraphs if true, load imports as separate graphs
   * @return dataset to query
   * @throws OWLOntologyStorageException on issue writing ontology to TTL format
   * @throws UnsupportedEncodingException on parsing TTL string
   */
  public static Dataset loadOntologyAsDataset(OWLOntology ontology, boolean useGraphs)
      throws OWLOntologyStorageException, UnsupportedEncodingException {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLOntology> ontologies = new HashSet<>();
    ontologies.add(ontology);
    if (useGraphs) {
      ontologies.addAll(ontology.getImports());
    }
    // Instantiate an empty dataset
    Dataset dataset = TDBFactory.createDataset();
    TDB.getContext().set(TDB.symUnionDefaultGraph, true);
    // Load each ontology in the set as a named model
    for (OWLOntology ont : ontologies) {
      Model m = ModelFactory.createDefaultModel();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      manager.saveOntology(ont, new TurtleDocumentFormat(), os);
      String content = new String(os.toByteArray(), "UTF-8");
      RDFParser.fromString(content).lang(RDFLanguages.TTL).parse(m);
      // Get the name of the graph as the ontology IRI
      IRI iri = ont.getOntologyID().getOntologyIRI().orNull();
      String name;
      if (iri != null) {
        name = iri.toString();
      } else {
        // If there is no IRI, generate a random ID
        name = UUID.randomUUID().toString();
      }
      dataset.addNamedModel(name, m);
      logger.info("Named graph added: " + name);
    }
    return dataset;
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

  public static Dataset createEmptyDataset() {
    return DatasetFactory.create(DatasetGraphFactory.create());
  }

  /**
   * Execute a SPARQL CONSTRUCT query on a graph and return the model.
   *
   * @param dsg the graph to construct in
   * @param query the SPARQL construct query string
   * @return the result Model
   */
  public static Model execConstruct(DatasetGraph dsg, String query) {
    return execConstruct(DatasetFactory.create(dsg), query);
  }

  /**
   * Execute a SPARQL CONSTRUCT query on a dataset and return a model.
   *
   * @param dataset the Dataset to construct in
   * @param query the SPARQL construct query string
   * @return the result Model
   */
  public static Model execConstruct(Dataset dataset, String query) {
    QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
    return qExec.execConstruct();
  }

  /**
   * Execute a SPARQL SELECT query on a graph and return a result set.
   *
   * @param dsg the graph to query
   * @param query the SPARQL query string
   * @return the result set
   * @throws IOException on query parse error
   */
  public static ResultSet execQuery(DatasetGraph dsg, String query) throws IOException {
    return execQuery(DatasetFactory.create(dsg), query);
  }

  /**
   * Execute a SPARQL SELECT query on a dataset and return a result set.
   *
   * @param dataset the Dataset to query over
   * @param query the SPARQL query string
   * @return the result set
   * @throws IOException on query parse error
   */
  public static ResultSet execQuery(Dataset dataset, String query) throws IOException {
    QueryExecution qExec;
    try {
      qExec = QueryExecutionFactory.create(query, dataset);
    } catch (QueryParseException e) {
      throw new IOException(String.format(queryParseError, e.getMessage()));
    }
    return qExec.execSelect();
  }

  /**
   * Execute a verification. Writes to STDERR.
   *
   * @param queriesResults a map from files to query results and output streams
   * @return true if there are any violations
   * @throws IOException on file issues
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
   * @param ruleName name of rule to verify
   * @param query the SPARQL query string
   * @return true if the are results, false otherwise
   * @throws IOException on query parse error
   */
  public static boolean execVerify(DatasetGraph dsg, String ruleName, String query)
      throws IOException {
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
   * Given a SPARQL query, return its type as a string: ASK, CONSTRUCT, DESCRIBE, or SELECT.
   *
   * @param queryString the SPARQL query string
   * @return the query type string
   * @throws IllegalArgumentException on bad query
   */
  public static String getQueryTypeName(String queryString) {
    QueryExecution qExec = QueryExecutionFactory.create(queryString, createEmptyDataset());
    Query query = qExec.getQuery();
    String queryType;
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
   * If a result set has results, return true, otherwise false.
   *
   * @param result the results to write
   * @return true if there are results, false otherwise
   */
  public static boolean hasResult(ResultSet result) {
    return result.hasNext();
  }

  /**
   * If a model has statements, return true, otherwise false.
   *
   * @param result the results to write
   * @return true if there are statements, false otherwise
   */
  public static boolean hasResult(Model result) {
    return !result.isEmpty();
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
   * @param result the Model to write
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
   * Run a CONSTRUCT query and write the result to a file.
   *
   * @param dataset the Dataset to construct in.
   * @param query The SPARQL construct query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws FileNotFoundException if output file is not found
   */
  public static void runConstruct(Dataset dataset, String query, File output, Lang outputFormat)
      throws FileNotFoundException {
    writeResult(execConstruct(dataset, query), outputFormat, new FileOutputStream(output));
  }

  /**
   * Run a SELECT query on a graph and write the results to a file.
   *
   * @param dsg the graph to query
   * @param query The SPARQL query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws IOException if output file is not found or query cannot be parsed
   */
  public static void runQuery(DatasetGraph dsg, String query, File output, Lang outputFormat)
      throws IOException {
    runQuery(DatasetFactory.create(dsg), query, output, outputFormat);
  }

  /**
   * Run a SELECT query on a dataset and write the result to a file.
   *
   * @param dataset The dataset to query over.
   * @param query The SPARQL query string.
   * @param output The file to write to.
   * @param outputFormat The file format.
   * @throws IOException if output file is not found or query cannot be parsed
   */
  public static void runQuery(Dataset dataset, String query, File output, Lang outputFormat)
      throws IOException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    writeResult(execQuery(dataset, query), outputFormat, new FileOutputStream(output));
  }

  /**
   * Run a SPARQL query and return true if there were results, false otherwise.
   *
   * @param dsg the graph to query over
   * @param query the SPARQL query string
   * @param formatName the name of the output format
   * @param output the OutputStream to write to
   * @return true if results, false if otherwise
   * @throws IOException on issue parsing query
   */
  public static boolean runSparqlQuery(
      DatasetGraph dsg, String query, String formatName, OutputStream output) throws IOException {
    return runSparqlQuery(DatasetFactory.create(dsg), query, formatName, output);
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
   * @throws IOException on issue parsing query
   */
  public static boolean runSparqlQuery(
      Dataset dataset, String queryString, String formatName, OutputStream output)
      throws IOException {
    dataset.begin(ReadWrite.READ);
    boolean result;
    try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
      result = runSparqlQuery(qExec, formatName, output);
    } catch (QueryParseException e) {
      throw new IOException(String.format(queryParseError, e.getMessage()));
    } finally {
      dataset.end();
    }
    return result;
  }

  /**
   * Given a query execution, a format name, and an output stream, run the query and write to
   * output.
   *
   * @param qExec QueryExecution to run
   * @param formatName format of output
   * @param output output stream to write to
   * @return true if there are results
   */
  public static boolean runSparqlQuery(
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
   * Run a SELECT query over the graph and write the result to a file. Prints violations to STDERR.
   *
   * @param dsg The graph to query over.
   * @param ruleName name of the rule
   * @param query The SPARQL query string.
   * @param outputPath The file path to write to, if there are results
   * @param outputFormat The file format.
   * @throws IOException if output file is not found or query cannot be parsed
   * @return true if the are results (so file is written), false otherwise
   */
  public static boolean runVerify(
      DatasetGraph dsg, String ruleName, String query, Path outputPath, Lang outputFormat)
      throws IOException {
    return runVerify(DatasetFactory.create(dsg), ruleName, query, outputPath, outputFormat);
  }

  /**
   * Run a SELECT query over the union of named graphs in a dataset and write the result to a file.
   * Prints violations to STDERR.
   *
   * @param dataset The dataset to query over.
   * @param query The SPARQL query string.
   * @param outputPath The file path to write to, if there are results
   * @param outputFormat The file format.
   * @throws IOException if output file is not found or query cannot be parsed
   * @return true if the are results (so file is written), false otherwise
   */
  public static boolean runVerify(
      Dataset dataset, String ruleName, String query, Path outputPath, Lang outputFormat)
      throws IOException {
    if (outputFormat == null) {
      outputFormat = Lang.CSV;
    }
    ResultSetRewindable results = ResultSetFactory.copyResults(execQuery(dataset, query));
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
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(ResultSet resultSet, String formatName, OutputStream output) {
    writeResult(resultSet, getFormatLang(formatName), output);
  }

  /**
   * Write a result set to an output stream.
   *
   * @param resultSet the results to write
   * @param format the language to write in (if null, CSV)
   * @param output the output stream to write to
   */
  public static void writeResult(ResultSet resultSet, Lang format, OutputStream output) {
    if (format == null) {
      format = Lang.CSV;
    }
    ResultSetMgr.write(output, resultSet, format);
  }

  /**
   * Write a model to an output stream.
   *
   * @param model the Model to write
   * @param formatName the name of the language to write in
   * @param output the output stream to write to
   */
  public static void writeResult(Model model, String formatName, OutputStream output) {
    writeResult(model, getFormatLang(formatName), output);
  }

  /**
   * Write a model to an output stream.
   *
   * @param model the Model to write
   * @param format the language to write in (if null, TTL)
   * @param output the output stream to write to
   */
  public static void writeResult(Model model, Lang format, OutputStream output) {
    if (format == null) {
      format = Lang.TTL;
    }
    RDFDataMgr.write(output, model, format);
  }
}
