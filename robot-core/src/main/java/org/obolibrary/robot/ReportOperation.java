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
import javax.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.obolibrary.robot.checks.Report;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.IRI;
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

  /** Error message when 'limit' is not a number. */
  private static final String limitNumberError =
      NS + "LIMIT NUMBER ERROR --limit argument '%s' must be an integer.";

  /** Error message when the query does not have ?entity. */
  private static final String missingEntityBinding =
      NS + "MISSING ENTITY BINDING query '%s' must include an '?entity'";

  /** Error message when a user-provided report query does not exist. */
  private static final String missingQueryError =
      NS + "MISSING QUERY ERROR query at '%s' does not exist.";

  /** Error message when 'print' is not a number. */
  private static final String printNumberError =
      NS + "PRINT NUMBER ERROR --print argument '%s' must be an integer.";

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
    Map<String, String> options = new HashMap<>();
    options.put("print", "0");
    options.put("fail-on", "error");
    options.put("labels", "false");
    options.put("format", null);
    options.put("profile", null);
    options.put("tdb", "false");
    options.put("tdb-directory", ".tdb");
    options.put("keep-tdb-mappings", "false");
    return options;
  }

  /**
   * Report on the ontology using the rules within the profile and print results. Prefer
   * report(OWLOntology ontology, String profilePath, String outputPath, String format, String
   * failOn).
   *
   * @param ontology the OWLOntology to report
   * @param ioHelper IOHelper to work with ontology
   * @throws Exception on any reporting error
   */
  public static void report(OWLOntology ontology, IOHelper ioHelper) throws Exception {
    report(ontology, ioHelper, null, null, null, null);
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

  /**
   * Given an ontology, an IOHelper, a profile path (or null), an output path (or null), a report
   * format (or null), and a level to fail on, report on the ontology using the rules within the
   * profile and write results to the output path. If profile is null, use the default profile in
   * resources. If the output path is null, write results to console. If the format is null, write
   * results in TSV format. Exit with status 1 if any violations of fail-on level are found.
   *
   * @param ontology OWLOntology to report on
   * @param ioHelper IOHelper to use
   * @param profilePath user profile file path to use, or null
   * @param outputPath string path to write report file to, or null
   * @param format string format for the output report (TSV or YAML), or null
   * @param failOn logging level to fail execution
   * @return true if successful, false if failed
   * @throws Exception on any error
   */
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
   * Report on the ontology using the rules within the profile and print results. Prefer
   * report(OWLOntology ontology, IOHelper ioHelper, String outputPath, Map&lt;String,String&gt;
   * options).
   *
   * @param ontology the OWLOntology to report
   * @param ioHelper IOHelper to work with ontology
   * @param options map of report options
   * @throws Exception on any reporting error
   */
  public static void report(OWLOntology ontology, IOHelper ioHelper, Map<String, String> options)
      throws Exception {
    report(ontology, ioHelper, null, options);
  }

  /**
   * Given an ontology, an IOHelper, a profile path (or null), an output path (or null), a report
   * format (or null), a level to fail on, and a boolean indicating to use labels, report on the
   * ontology using the rules within the profile and write results to the output path. If profile is
   * null, use the default profile in resources. If the output path is null, write results to
   * console. If the format is null, write results in TSV format. Exit with status 1 if any
   * violations of fail-on level are found.
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
    Map<String, String> options = getDefaultOptions();
    if (profilePath != null) {
      options.put("profile", profilePath);
    }
    if (format != null) {
      options.put("format", format);
    }
    if (failOn != null) {
      options.put("fail-on", failOn);
    }
    if (useLabels) {
      options.put("labels", "true");
    }
    return report(ontology, ioHelper, outputPath, options);
  }

  /**
   * Given an ontology, an IOHelper, an output path (or null), and a map of options (or null),
   * report on the ontology using the rules within the profile specified by the options and write
   * results to the output path. If profile is null, use the default profile in resources. If the
   * output path is null, write results to console. If the format is null, write results in TSV
   * format.
   *
   * @param ontology the OWLOntology to report
   * @param ioHelper IOHelper to work with ontology
   * @param outputPath string path to write report file to, or null
   * @param options map of report options
   * @return false if there are violations at or above the fail-on level, true otherwise
   * @throws Exception on any reporting error
   */
  public static boolean report(
      OWLOntology ontology, IOHelper ioHelper, String outputPath, Map<String, String> options)
      throws Exception {
    // Generate the report object with violation details
    Report report = getReport(ontology, ioHelper, options);
    return processReport(report, outputPath, options);
  }

  /**
   * Given an ontology, an IOHelper, and a map of options, create a Report object and run the report
   * queries specified in a profile (from options, or default). Return the completed Report object.
   *
   * @param ontology OWLOntology to report on
   * @param ioHelper IOHelper to resolve labels
   * @param options Map of report options
   * @return Report object with violation details
   * @throws Exception on any reporting error
   */
  public static Report getReport(
      OWLOntology ontology, IOHelper ioHelper, Map<String, String> options) throws Exception {
    // Get options specified in map or default options
    if (options == null) {
      options = getDefaultOptions();
    }

    String profilePath = OptionsHelper.getOption(options, "profile", null);
    boolean useLabels = OptionsHelper.optionIsTrue(options, "labels");

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

    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, false);
    for (String queryName : queries.keySet()) {
      String fullQueryString = queries.get(queryName);
      String queryString;
      // Remove any comments
      List<String> lines = new ArrayList<>();
      for (String line : fullQueryString.split("\n")) {
        if (!line.startsWith("#")) {
          lines.add(line);
        }
      }
      queryString = String.join("\n", lines);
      // Use the query to get violations
      List<Violation> violations = getViolations(dataset, queryName, queryString, options);
      // If violations is not returned properly, the query did not have the correct format
      if (violations == null) {
        throw new Exception(String.format(missingEntityBinding, queryName));
      }
      report.addViolations(queryName, profile.get(queryName), violations);
    }

    return report;
  }

  /**
   * Given an input path, an output path (or null), and a map of options, report on the ontology
   * using the rules within the profile specified by the options and write results to the output
   * path. Ontology is loaded to dataset backed on disk. The labels option is not supported with
   * TDB.
   *
   * @param inputPath String path of ontology to load
   * @param outputPath String path to write report file to, or null
   * @param options map of report options
   * @return false if there are violations at or above the fail-on level, true otherwise
   * @throws Exception on any reporting error
   */
  public static boolean tdbReport(String inputPath, String outputPath, Map<String, String> options)
      throws Exception {
    Report report = getTDBReport(inputPath, options);
    return processReport(report, outputPath, options);
  }

  /**
   * Given an input path to an ontology and a map of options, create a Report object and run (on TDB
   * dataset) the report queries specified in a profile (from options, or default). Return the
   * completed Report object. The labels option is not supported with TDB.
   *
   * @param inputPath path to load triples to TDB
   * @param options map of report options
   * @return Report object with violation details
   * @throws Exception on any loading, query, or reporting error
   */
  public static Report getTDBReport(String inputPath, Map<String, String> options)
      throws Exception {
    String tdbDir = OptionsHelper.getOption(options, "tdb-directory", ".tdb");

    // Load dataset
    // Fail if the input path is not in RDF/XML or TTL
    Dataset dataset = IOHelper.loadToTDBDataset(inputPath, tdbDir);

    Report report;
    boolean keepMappings = OptionsHelper.optionIsTrue(options, "keep-tdb-mappings");
    try {
      report = getTDBReport(dataset, options);
    } finally {
      // Close and release
      dataset.close();
      try {
        TDBFactory.release(dataset);
      } catch (TDBTransactionException e) {
        // Do nothing - already released
      }

      if (!keepMappings) {
        // Maybe delete
        boolean success = IOHelper.cleanTDB(tdbDir);
        if (!success) {
          logger.error(String.format("Unable to remove directory '%s'", tdbDir));
        }
      }
    }

    return report;
  }

  /**
   * Given a dataset and a map of options, create a Report object and run (on TDB dataset) the
   * report queries specified in a profile (from options, or default). Return the completed Report
   * object. The labels option is not supported with TDB.
   *
   * @param dataset TDB Dataset to perform Report operation on
   * @param options Map of report options
   * @return Report object with violation details
   * @throws Exception on any reporting error
   */
  public static Report getTDBReport(Dataset dataset, Map<String, String> options) throws Exception {
    // Get options specified in map or default options
    if (options == null) {
      options = getDefaultOptions();
    }
    // Make sure TDB is true here
    options.put("tdb", "true");

    String profilePath = OptionsHelper.getOption(options, "profile", null);

    // The profile is a map of rule name and reporting level
    Map<String, String> profile = getProfile(profilePath);
    // The queries is a map of rule name and query string
    Map<String, String> queries = getQueryStrings(profile.keySet());

    boolean useLabels = OptionsHelper.optionIsTrue(options, "labels");
    Map<IRI, String> labelMap = null;
    if (useLabels) {
      labelMap = new HashMap<>();
      // Run query over dataset to retrive all labels
      String query =
          "SELECT ?s ?label WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
      dataset.begin(ReadWrite.READ);
      try {
        ResultSet labelResults = QueryOperation.execQuery(dataset, query);
        while (labelResults.hasNext()) {
          QuerySolution qs = labelResults.next();
          IRI iri = IRI.create(qs.getResource("s").getURI());
          String label = qs.getLiteral("label").getString();
          labelMap.put(iri, label);
        }
      } finally {
        dataset.end();
      }
    }

    // Create the report object (maybe using labels)
    Report report = new Report(labelMap);

    for (String queryName : queries.keySet()) {
      String fullQueryString = queries.get(queryName);
      String queryString;
      // Remove any comments
      List<String> lines = new ArrayList<>();
      for (String line : fullQueryString.split("\n")) {
        if (!line.startsWith("#")) {
          lines.add(line);
        }
      }
      queryString = String.join("\n", lines);
      // Use the query to get violations
      List<Violation> violations = getViolations(dataset, queryName, queryString, options);
      // If violations is not returned properly, the query did not have the correct format
      if (violations == null) {
        throw new Exception(String.format(missingEntityBinding, queryName));
      }
      report.addViolations(queryName, profile.get(queryName), violations);
    }

    return report;
  }

  /**
   * Given a Report, an output path, and a map of report options, process the Report results and
   * save the report to the output path.
   *
   * @param report completed Report object
   * @param outputPath path to save report to
   * @param options Map of report options
   * @return true if report passed, false if it failed
   * @throws IOException on issue writing to file
   */
  public static boolean processReport(Report report, String outputPath, Map<String, String> options)
      throws IOException {
    // Print violations to terminal
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

    // Maybe print some of the lines
    String printString = OptionsHelper.getOption(options, "print", "0").trim();
    // Parse print N lines option to an int
    int print;
    try {
      print = Integer.parseInt(printString);
    } catch (NumberFormatException e) {
      // Not a number
      throw new IllegalArgumentException(String.format(printNumberError, printString));
    }

    // Format is determined either by --format or the extension of the output path
    String format = OptionsHelper.getOption(options, "format");
    if (format == null && outputPath != null) {
      format = outputPath.substring(outputPath.lastIndexOf(".") + 1);
      if (!format.equalsIgnoreCase("csv") && !format.equalsIgnoreCase("yaml")) {
        // Anything other than .yaml or .csv is written as TSV
        format = "tsv";
      }
    } else if (format == null) {
      // Null format means no output file, will be printed as TSV
      format = "tsv";
    }

    String result;
    if (format.equalsIgnoreCase("yaml")) {
      result = report.toYAML();
    } else if (format.equalsIgnoreCase("csv")) {
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
      // Maybe print the first N lines
      if (print > 0) {
        String[] lines = getLinesToPrint(report, result, format);
        printNViolations(lines, print);
      }
    } else {
      // Output goes to terminal
      if (print > 0) {
        String[] lines = getLinesToPrint(report, result, format);
        printNViolations(lines, print);
      } else {
        System.out.println(result);
      }
    }

    String failOn = OptionsHelper.getOption(options, "fail-on", "error");
    // Set failOn if null to default
    if (failOn == null) {
      failOn = ERROR;
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
   * Given a Report, a result, and a format, return the array of lines in TSV format to be printed
   * to terminal. The written output of the report will still be in the specified format.
   *
   * @param report Report object
   * @param result string result of the report
   * @param format format of the string result
   * @return array of TSV format lines
   */
  private static String[] getLinesToPrint(Report report, String result, @NotNull String format) {
    String[] lines;
    if (format.equalsIgnoreCase("yaml") || format.equalsIgnoreCase("csv")) {
      // Print YAML as TSV for this (output will still be YAML)
      lines = report.toTSV().split("\n");
    } else {
      lines = result.split("\n");
    }
    return lines;
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
      if (rule.startsWith("file:")) {
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
      if (rule.startsWith("file:///")) {
        // Process an absolute path
        File file = new File(new URL(rule).toURI());
        if (!file.exists()) {
          throw new IOException(String.format(missingQueryError, file.getPath()));
        }
        queries.put(rule, FileUtils.readFileToString(file));
      } else {
        // Process a relative path
        String path = rule.substring(5);
        File file = new File(path);
        if (!file.exists()) {
          throw new IOException(String.format(missingQueryError, file.getPath()));
        }
        queries.put(rule, FileUtils.readFileToString(file));
      }
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
              // Remove the headers
              String fullQueryString = sb.toString();
              String queryString = fullQueryString.substring(fullQueryString.indexOf("PREFIX"));
              queries.put(ruleName, queryString);
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
   * Given an ontology as a Dataset and a query, return the violations found by that query.
   *
   * @param dataset the ontology/ontologies as a dataset
   * @param query the query
   * @return List of Violations
   * @throws IOException on issue parsing query
   */
  private static List<Violation> getViolations(
      Dataset dataset, String queryName, String query, Map<String, String> options)
      throws Exception {
    boolean tdb = OptionsHelper.optionIsTrue(options, "tdb");
    String limitString = OptionsHelper.getOption(options, "limit", null);
    Integer limit = null;
    if (limitString != null) {
      try {
        limit = Integer.parseInt(limitString);
      } catch (NumberFormatException e) {
        throw new Exception(String.format(limitNumberError, limitString));
      }
    }

    if (tdb) {
      // If using TDB we must be in a read transaction to query
      dataset.begin(ReadWrite.READ);
      try {
        ResultSet violationSet = QueryOperation.execQuery(dataset, query);
        return getViolationsFromResults(queryName, violationSet, limit);
      } catch (Exception e) {
        // If query fails, return null
        // And warn that report may be incomplete
        logger.error(
            String.format(
                "Could not complete query '%s' - report may be incomplete.\nCause:\n%s",
                queryName, e.getMessage()));
        return null;
      } finally {
        // Always end the transaction
        dataset.end();
      }
    } else {
      try {
        ResultSet violationSet = QueryOperation.execQuery(dataset, query);
        return getViolationsFromResults(queryName, violationSet, limit);
      } catch (Exception e) {
        // If query fails, return null
        // And warn that report may be incomplete
        logger.error(
            String.format(
                "Could not complete query '%s' - report may be incomplete.\nCause:\n%s",
                queryName, e.getMessage()));
        return null;
      }
    }
  }

  /**
   * Given a query name, a result set, and a limit for results, return a list of Violation objects
   * for those results.
   *
   * @param queryName name of query that produced result set
   * @param violationSet ResultSet of query results
   * @param limit number of results to limit, or null for no limit
   * @return list of Violation objects
   * @throws Exception on malformed query
   */
  private static List<Violation> getViolationsFromResults(
      String queryName, ResultSet violationSet, Integer limit) throws Exception {
    List<Violation> violations = new ArrayList<>();

    // Counter for stopping at limit
    int c = 0;
    while (violationSet.hasNext()) {
      if (limit != null && limit <= c) {
        // Stop checking violations
        break;
      }

      // Wrap in try catch in case GC overflows
      // Will return results up to this point with warning that report may be incomplete
      QuerySolution qs;
      try {
        qs = violationSet.next();
      } catch (Exception e) {
        logger.error(
            String.format(
                "Could not retrieve all results for query '%s' - report may be incomplete.\nCause:\n%s",
                queryName, e.getMessage()));
        return violations;
      }

      // entity should never be null (missing entity binding error)
      String entity = getQueryResultOrNull(qs, "entity");
      if (entity == null) {
        throw new Exception(String.format(missingEntityBinding, queryName));
      }

      // skip RDFS and OWL terms
      if (entity.contains("/rdf-schema#") || entity.contains("/owl#")) {
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

      // Increase counter
      c++;
    }
    return violations;
  }

  /**
   * Given an array of lines and a number of lines to print, print that number of violations (one
   * per line).
   *
   * @param lines array of lines to print
   * @param n number of lines to print
   */
  private static void printNViolations(String[] lines, int n) {
    if (lines.length <= n) {
      n = lines.length - 1;
    }
    System.out.println(String.format("\nFirst %d violations:", n));
    for (int i = 0; i < n; i++) {
      // i + 1 to skip headers
      System.out.println(lines[i + 1]);
    }
  }
}
