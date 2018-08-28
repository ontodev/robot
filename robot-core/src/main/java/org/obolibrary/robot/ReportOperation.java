package org.obolibrary.robot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.DatasetGraph;
import org.obolibrary.robot.checks.Report;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Report issues with an ontology using a series of QC SPARQL queries.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReportOperation {

  /** Directory for queries. */
  private static final String queryDir = "report_queries";

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReportOperation.class);

  /** Namespace for general input error messages. */
  private static final String NS = "report#";

  /** Error message when user profiles an invalid fail-on level. */
  private static final String failOnError = NS + "FAIL ON ERROR '%s' is not a valid fail-on level.";

  /** Error message when the query does not have ?entity. */
  private static final String missingEntityBinding =
      NS + "MISSING ENTITY BINDING query '%s' must include an '?entity'";

  /** Error message when user provides a rule level other than INFO, WARN, or ERROR. */
  private static final String reportLevelError =
      NS + "REPORT LEVEL ERROR '%s' is not a valid reporting level.";

  /** Reporting level INFO. */
  private static final String INFO = "INFO";

  /** Reporting level WARN. */
  private static final String WARN = "WARN";

  /** Reporting level ERROR. */
  private static final String ERROR = "ERROR";

  /**
   * Return a map from option name to default option value, for all the available report options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    return new HashMap<>();
  }

  /**
   * Report on the ontology using the rules within the profile and print results. Prefer
   * report(OWLOntology ontology, String profilePath, String outputPath, String format, String
   * failOn).
   *
   * @param ontology the OWLOntology to report
   * @param iohelper IOHelper to work with ontology
   * @throws Exception on any reporting error
   */
  public static void report(OWLOntology ontology, IOHelper iohelper) throws Exception {
    report(ontology, null, null, null, null);
  }

  /**
   * Report on the ontology using the rules within the profile and print results. Prefer
   * report(OWLOntology ontology, String profilePath, String outputPath, String format, String
   * failOn).
   *
   * @param ontology the OWLOntology to report
   * @param iohelper IOHelper to work with ontology
   * @param options map of report options
   * @throws Exception on any reporting error
   */
  public static void report(OWLOntology ontology, IOHelper iohelper, Map<String, String> options)
      throws Exception {
    report(ontology, null, null, null, null);
  }

  /**
   * Given an ontology, a profile path (or null), an output path (or null), and a report format (or
   * null) report on the ontology using the rules within the profile and write results to the output
   * path. If profile is null, use the default profile in resources. If the output path is null,
   * write results to console. If the format is null, write results in TSV format.
   *
   * @param ontology OWLOntology to report on
   * @param profilePath user profile file path to use, or null
   * @param outputPath string path to write report file to, or null
   * @param format string format for the output report (TSV or YAML), or null
   * @param failOn logging level to fail execution
   * @return true if successful, false if failed
   * @throws Exception on any error
   */
  public static boolean report(
      OWLOntology ontology, String profilePath, String outputPath, String format, String failOn)
      throws Exception {
    return report(ontology, null, profilePath, outputPath, format, failOn, false);
  }

  public static boolean report(
      OWLOntology ontology,
      IOHelper ioHelper,
      String profilePath,
      String outputPath,
      String format,
      String failOn)
      throws Exception {
    return report(ontology, ioHelper, profilePath, outputPath, format, failOn, false);
  }

  /**
   * Given an ontology, a profile path (or null), an output path (or null), and a report format (or
   * null) report on the ontology using the rules within the profile and write results to the output
   * path. If profile is null, use the default profile in resources. If the output path is null,
   * write results to console. If the format is null, write results in TSV format.
   *
   * @param ontology OWLOntology to report on
   * @param ioHelper IOHelper to use
   * @param profilePath user profile file path to use, or null
   * @param outputPath string path to write report file to, or null
   * @param format string format for the output report (TSV or YAML), or null
   * @param failOn logging level to fail execution
   * @param useLabels if true, use labels for output
   * @return true if successful, false if failed
   * @throws Exception on any error
   */
  public static boolean report(
      OWLOntology ontology,
      IOHelper ioHelper,
      String profilePath,
      String outputPath,
      String format,
      String failOn,
      boolean useLabels)
      throws Exception {
    // Set failOn if null to default
    if (failOn == null) {
      failOn = ERROR;
    }
    // The profile is a map of rule name and reporting level
    Map<String, String> profile = getProfile(profilePath);
    // The queries is a map of rule name and query string
    Map<String, String> queries = getQueryStrings(profile.keySet());

    // Create the report object
    Report report;
    if (ioHelper != null) {
      report = new Report(ontology, ioHelper, useLabels);
    } else {
      report = new Report(ontology, useLabels);
    }

    // Load into dataset without imports
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, false);
    for (String queryName : queries.keySet()) {
      String fullQueryString = queries.get(queryName);
      String queryString;
      // Remove the headers
      if (fullQueryString.startsWith("#")) {
        queryString = fullQueryString.substring(fullQueryString.indexOf("PREFIX"));
      } else {
        queryString = fullQueryString;
      }
      List<Violation> violations = getViolations(dataset, queryString);
      // If violations is not returned properly, the query did not have the correct format
      if (violations == null) {
        throw new Exception(String.format(missingEntityBinding, queryName));
      }
      report.addViolations(queryName, profile.get(queryName), getViolations(dataset, queryString));
    }

    Integer violationCount = report.getTotalViolations();
    if (violationCount != 0) {
      System.out.println("Violations: " + violationCount);
      System.out.println("-----------------");
      System.out.println(ERROR + ":      " + report.getTotalViolations(ERROR));
      System.out.println(WARN + ":       " + report.getTotalViolations(WARN));
      System.out.println(INFO + ":       " + report.getTotalViolations(INFO));
    } else {
      System.out.println("No violations found.");
    }

    // System.out.println(report.getIRIs());
    String result;
    if (format != null && format.equalsIgnoreCase("yaml")) {
      result = report.toYAML();
    } else if (format != null && format.equalsIgnoreCase("csv")) {
      result = report.toCSV();
    } else {
      result = report.toTSV();
    }
    if (outputPath != null) {
      // If output is provided, write to that file
      try (FileWriter fw = new FileWriter(outputPath);
          BufferedWriter bw = new BufferedWriter(fw)) {
        logger.debug("Writing report to: " + outputPath);
        bw.write(result);
      }
    } else {
      // Otherwise output to terminal
      System.out.println(result);
    }

    // If a fail-on is provided, return false if there are violations of the given level
    if (failOn.equalsIgnoreCase("none")) {
      return true;
    } else if (failOn.equalsIgnoreCase(ERROR)) {
      return report.getTotalViolations(ERROR) <= 0;
    } else if (failOn.equalsIgnoreCase(WARN)) {
      return (report.getTotalViolations(ERROR) + report.getTotalViolations(WARN)) <= 0;
    } else if (failOn.equalsIgnoreCase(INFO)) {
      return report.getTotalViolations() <= 0;
    } else {
      throw new IllegalArgumentException(String.format(failOnError, failOn));
    }
  }

  /**
   * Given a set of rules (either as the default rule names or URL to a file), return a map of the
   * rule names and the corresponding query strings.
   *
   * @param rules set of rules to get queries for
   * @return map of rule name and query string
   * @throws IOException on any issue reading the query file
   * @throws URISyntaxException on issue converting URL to URI
   */
  private static Map<String, String> getQueryStrings(Set<String> rules)
      throws IOException, URISyntaxException {
    Set<String> defaultRules = new HashSet<>();
    Set<String> userRules = new HashSet<>();
    Map<String, String> queries = new HashMap<>();
    for (String rule : rules) {
      if (rule.startsWith("file://")) {
        userRules.add(rule);
      } else {
        defaultRules.add(rule);
      }
    }
    queries.putAll(getDefaultQueryStrings(defaultRules));
    queries.putAll(getUserQueryStrings(userRules));
    return queries;
  }

  /**
   * Given a set of user-provided query paths for a set of rules, return a map of the rule names and
   * the file (query) contents.
   *
   * @param rules set of file paths to user query files
   * @return map of rule name and query string
   * @throws URISyntaxException on issue converting file path URL to URI
   * @throws IOException on any issue reading the file
   */
  private static Map<String, String> getUserQueryStrings(Set<String> rules)
      throws URISyntaxException, IOException {
    Map<String, String> queries = new HashMap<>();
    for (String rule : rules) {
      File file = new File(new URL(rule).toURI());
      queries.put(rule, FileUtils.readFileToString(file));
    }
    return queries;
  }

  /**
   * Given a set of default rules, return a map of the rule names and query strings. This is a
   * "brute-force" method to retrieve default query file contents from packaged jar.
   *
   * @param rules subset of the default rules to include
   * @return map of rule name and query string
   * @throws URISyntaxException on issue converting path to URI
   * @throws IOException on any issue with accessing files or file contents
   */
  private static Map<String, String> getDefaultQueryStrings(Set<String> rules)
      throws IOException, URISyntaxException {
    URL dirURL = ReportOperation.class.getClassLoader().getResource(queryDir);
    Map<String, String> queries = new HashMap<>();
    // Handle simple file path, probably accessed during testing
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      String[] queryFilePaths = new File(dirURL.toURI()).list();
      if (queryFilePaths == null || queryFilePaths.length == 0) {
        throw new IOException(
            "Cannot access report query files. There are no files in the directory.");
      }
      for (String qPath : queryFilePaths) {
        String ruleName = qPath.substring(qPath.lastIndexOf("/")).split(".")[0];
        // Only add it to the queries if the rule set contains that rule
        // If rules == null, include all rules
        if (rules == null || rules.contains(ruleName)) {
          queries.put(ruleName, FileUtils.readFileToString(new File(qPath)));
        }
      }
      return queries;
    }
    // Handle inside jar file
    // This will be the case any time someone runs ROBOT via CLI
    if (dirURL == null) {
      String cls = ReportOperation.class.getName().replace(".", "/") + ".class";
      dirURL = ReportOperation.class.getClassLoader().getResource(cls);
    }
    if (dirURL == null) {
      throw new IOException(
          "Cannot access report query files in JAR. The resource does not exist.");
    }
    String protocol = dirURL.getProtocol();
    if (protocol.equals("jar")) {
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
      // Get all entries in jar
      Enumeration<JarEntry> entries;
      try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
        entries = jar.entries();
        if (!entries.hasMoreElements()) {
          throw new IOException(
              "Cannot access report query files in JAR. There are no entries in the JAR.");
        }
        // Track rules that have successfully been retrieved
        while (entries.hasMoreElements()) {
          JarEntry resource = entries.nextElement();
          String resourceName = resource.getName();
          if (resourceName.startsWith(queryDir) && !resourceName.endsWith("/")) {
            // Get just the rule name
            String ruleName =
                resourceName.substring(
                    resourceName.lastIndexOf("/") + 1, resourceName.indexOf(".rq"));
            // Only add it to the queries if the rule set contains that rule
            // If rules == null, include all rules
            if (rules == null || rules.contains(ruleName)) {
              InputStream is = jar.getInputStream(resource);
              StringBuilder sb = new StringBuilder();
              try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String chr;
                while ((chr = br.readLine()) != null) {
                  sb.append(chr);
                }
              }
              queries.put(ruleName, sb.toString());
            }
          }
        }
      }
      return queries;
    }
    // If nothing has been returned, it's an exception
    throw new IOException("Cannot access report query files.");
  }

  /**
   * Given the path to a profile file (or null), return the rules and their levels (info, warn, or
   * error). If profile == null, return the default profile in the resources.
   *
   * @param path path to profile, or null
   * @return map of rule name and its reporting level
   * @throws IOException on any issue reading the profile file
   */
  private static Map<String, String> getProfile(String path) throws IOException {
    Map<String, String> profile = new HashMap<>();
    InputStream is;
    // If the file was not provided, get the default
    if (path == null) {
      is = ReportOperation.class.getResourceAsStream("/report_profile.txt");
    } else {
      is = new FileInputStream(new File(path));
    }
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] split = line.trim().split("\t");
        String level = split[0].toUpperCase();
        // The level should be: INFO, WARN, or ERROR
        if (!INFO.equals(level) && !WARN.equals(level) && !ERROR.equals(level)) {
          throw new IllegalArgumentException(String.format(reportLevelError, split[0]));
        }
        String rule = split[1];
        profile.put(rule, level);
      }
    }
    return profile;
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
   * Given an ontology as a DatasetGraph and a query, return the violations found by that query.
   *
   * @param dsg the ontology as a graph
   * @param query the query
   * @return List of Violations
   * @throws IOException on issue parsing query
   */
  private static List<Violation> getViolations(DatasetGraph dsg, String query) throws IOException {
    return getViolations(DatasetFactory.create(dsg), query);
  }

  /**
   * Given an ontology as a Dataset and a query, return the violations found by that query.
   *
   * @param dataset the ontology/ontologies as a dataset
   * @param query the query
   * @return List of Violations
   * @throws IOException on issue parsing query
   */
  private static List<Violation> getViolations(Dataset dataset, String query) throws IOException {
    ResultSet violationSet = QueryOperation.execQuery(dataset, query);

    List<Violation> violations = new ArrayList<>();

    while (violationSet.hasNext()) {
      QuerySolution qs = violationSet.next();
      // entity should never be null
      String entity = getQueryResultOrNull(qs, "entity");
      if (entity == null) {
        return null;
      }
      // skip RDFS and OWL terms
      if (entity != null && (entity.contains("/rdf-schema#") || entity.contains("/owl#"))) {
        continue;
      }
      Violation violation = new Violation(entity);
      // try and get a property and value from the query
      String property = getQueryResultOrNull(qs, "property");
      String value = getQueryResultOrNull(qs, "value");
      // add details to Violation
      if (property != null) {
        violation.addStatement(property, value);
      }
      violations.add(violation);
    }
    return violations;
  }
}
