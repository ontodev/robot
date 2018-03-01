package org.obolibrary.robot;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.obolibrary.robot.checks.CheckerQuery;
import org.obolibrary.robot.checks.Report;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Report issues with an ontology.
 *
 * <p>Currently this is minimal but we imagine later creating an extensive 'report card' for an
 * ontology, describing ways to make the ontology conform to OBO conventions
 *
 * <p>TODO: decide on report structure. Perhaps JSON-LD? Create vocabulary for report violations?
 */
public class ReportOperation {

  /** Directory for queries. */
  private static final String queryDir = "queries";

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReportOperation.class);

  /** Newline character. */
  // private static final String newLine = System.getProperty("line.separator");

  /**
   * Reports ontology using SPARQL queries.
   *
   * @param ontology OWLOntology to report
   * @param outputPath String path to write report file to (or null)
   * @throws Exception on any error
   */
  public static void report(OWLOntology ontology, String outputPath) throws Exception {
    List<String> queryStrings = getQueryStrings();
    Set<CheckerQuery> queries = new HashSet<>();
    for (String query : queryStrings) {
      CheckerQuery q = new CheckerQuery(query);
      queries.add(q);
    }
    Report report = createReport(ontology, queries);
    String yaml = report.toYaml();
    if (outputPath != null) {
      try (FileWriter fw = new FileWriter(outputPath);
          BufferedWriter bw = new BufferedWriter(fw)) {
        logger.debug("Writing YAML report to: " + outputPath);
        bw.write(yaml);
      }
    } else {
      // just output to terminal if no output path provided
      System.out.println(yaml);
    }
  }

  /**
   * Given an ontology and a set of queries, create a report containing all violations. Violations
   * are added based on the query results. If there are no violations, the Report will pass and no
   * Report object will be returned.
   *
   * @param ontology OWLOntology to report on
   * @param queries set of CheckerQueries
   * @return Report (on violations) or null (on no violations)
   * @throws OWLOntologyStorageException on issue loading ontology as DatasetGraph
   * @throws Exception
   */
  private static Report createReport(OWLOntology ontology, Set<CheckerQuery> queries)
      throws OWLOntologyStorageException {
    Report report = new Report(ontology);
    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    for (CheckerQuery query : queries) {
      report.addViolations(query.severity, query.title, getViolations(dsg, query));
    }
    Integer violationCount = report.getTotalViolations();
    if (violationCount != 0) {
      logger.error("REPORT FAILED! Violations: " + violationCount);
      logger.error("Severity 1 violations: " + report.getTotalViolations(1));
      logger.error("Severity 2 violations: " + report.getTotalViolations(2));
      logger.error("Severity 3 violations: " + report.getTotalViolations(3));
      logger.error("Severity 4 violations: " + report.getTotalViolations(4));
      logger.error("Severity 5 violations: " + report.getTotalViolations(5));
    } else {
      System.out.println("REPORT PASSED! No violations found.");
    }
    return report;
  }

  /**
   * Given a QuerySolution and a String var to retrieve, return the value of the the var. If the var
   * is not in the solution, return null.
   *
   * @param qs QuerySolution
   * @param var String variable to retrieve from result
   * @return string or null
   */
  private static String getQueryResultOrNull(QuerySolution qs, String var) {
    try {
      return qs.get(var).toString();
    } catch (NullPointerException e) {
      return null;
    }
  }

  /**
   * "Brute-force" method to retrieve query file contents from packaged jar.
   *
   * @return List of query strings
   * @throws URISyntaxException On issue converting path to URI
   * @throws IOException On any issue with accessing files or file contents
   */
  private static List<String> getQueryStrings() throws IOException, URISyntaxException {
    URL dirURL = ReportOperation.class.getClassLoader().getResource(queryDir);
    List<String> queryStrings = new ArrayList<>();
    // Handle simple file path, probably accessed during testing
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      String[] queryFilePaths = new File(dirURL.toURI()).list();
      if (queryFilePaths.length == 0) {
        throw new IOException("Cannot access report query files.");
      }
      for (String qPath : queryFilePaths) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(qPath)))) {
          String line;
          while ((line = br.readLine()) != null) {
            sb.append(line);
          }
        }
        queryStrings.add(sb.toString());
      }
      return queryStrings;
    }
    // Handle inside jar file
    // This will be the case any time someone runs ROBOT via CLI
    if (dirURL == null) {
      String cls = ReportOperation.class.getName().replace(".", "/") + ".class";
      dirURL = ReportOperation.class.getClassLoader().getResource(cls);
    }
    if (dirURL.getProtocol().equals("jar")) {
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
      // Get all entries in jar
      Enumeration<JarEntry> entries = null;
      try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
        entries = jar.entries();
        if (!entries.hasMoreElements()) {
          throw new IOException("Cannot access entries in JAR.");
        }

        while (entries.hasMoreElements()) {
          JarEntry resource = entries.nextElement();
          if (resource.getName().startsWith(queryDir)) {
            InputStream is = jar.getInputStream(resource);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
              String chr;
              while ((chr = br.readLine()) != null) {
                sb.append(chr);
              }
            }
            queryStrings.add(sb.toString());
          }
        }
      }
      // Remove any empty entries
      if (queryStrings.contains("")) {
        queryStrings.remove("");
      }
      return queryStrings;
    }
    // If nothing has been returned, it's an exception
    throw new IOException("Cannot access report query files.");
  }

  /**
   * Given an ontology as a DatasetGraph and a query as a CheckerQuery, return the violations found
   * by that query.
   *
   * @param dsg the ontology
   * @param query the query
   * @return List of Violations
   */
  private static List<Violation> getViolations(DatasetGraph dsg, CheckerQuery query) {
    ResultSet violationSet = QueryOperation.execQuery(dsg, query.queryString);

    Map<String, Violation> violations = new HashMap<>();
    Violation violation = null;

    while (violationSet.hasNext()) {
      QuerySolution qs = violationSet.next();
      // entity should never be null
      String entity = getQueryResultOrNull(qs, "entity");
      // skip RDFS and OWL terms
      // TODO: should we ignore oboInOwl, FOAF, and DC as well?
      if (entity.contains("/rdf-schema#") || entity.contains("/owl#")) {
        continue;
      }
      // find out if a this Violation already exists for this entity
      violation = violations.get(entity);
      // if the entity hasn't been added, create a new Violation
      if (violation == null) {
        violation = new Violation(entity);
      }
      // try and get a property and value from the query
      String property = getQueryResultOrNull(qs, "property");
      String value = getQueryResultOrNull(qs, "value");
      // add details to Violation
      if (property != null) {
        violation.addStatement(property, value);
      }
      violations.put(entity, violation);
    }
    return new ArrayList<>(violations.values());
  }
}
