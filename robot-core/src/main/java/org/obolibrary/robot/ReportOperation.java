package org.obolibrary.robot;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.lang3.tuple.Triple;
import org.obolibrary.robot.checks.CheckerQuery;
import org.obolibrary.robot.checks.Violation;
import org.obolibrary.robot.checks.ViolationChecker;
import org.semanticweb.owlapi.model.OWLOntology;
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
  private static final String newLine = System.getProperty("line.separator");

  /**
   * Reports ontology using SPARQL queries. Also checks for the same violation as
   * InvalidReferenceViolations.
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
    Map<Integer, List<Violation>> violationMap = ViolationChecker.getViolations(ontology, queries);
    String yaml = createYamlReport(violationMap);
    // write to file (if provided)
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
   * Convert the Violation objects to YAML output.
   *
   * @param violations map of severity level (key, integer) and list of violation objects
   * @return YAML string
   */
  private static String createYamlReport(Map<Integer, List<Violation>> violationMap) {
    StringBuilder sb = new StringBuilder();
    for (Integer key = 1; key <= 5; key++) {
      sb.append("- severity    : " + key + newLine);
      sb.append("  violations  :" + newLine);
      List<Violation> violations = violationMap.get(key);
      for (Violation violation : violations) {
        sb.append("  - title   : " + violation.getTitle() + newLine);
        sb.append("    triples :" + newLine);
        for (Triple<String, String, String> triple : violation.getTriples()) {
          sb.append(
              "    - "
                  + triple.getLeft()
                  + " "
                  + triple.getMiddle()
                  + " "
                  + triple.getRight()
                  + newLine);
        }
      }
    }
    return sb.toString();
  }
}
