package org.obolibrary.robot.checks;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.UnmergeOperation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.PrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /** Reporting level INFO. */
  private static final String INFO = "INFO";

  /** Reporting level WARN. */
  private static final String WARN = "WARN";

  /** Reporting level ERROR. */
  private static final String ERROR = "ERROR";

  /** Map of rules and the violations for INFO level. */
  public Map<String, List<Violation>> info;

  /** Map of rules and the violations for WARN level. */
  public Map<String, List<Violation>> warn;

  /** Map of rules and the violations for ERROR level. */
  public Map<String, List<Violation>> error;

  /** Count of violations for INFO. */
  private Integer infoCount;

  /** Count of violations for WARN. */
  private Integer warnCount;

  /** Count of violations for ERROR. */
  private Integer errorCount;

  /** Create a new report object. */
  public Report() {
    info = new HashMap<>();
    warn = new HashMap<>();
    error = new HashMap<>();

    infoCount = 0;
    warnCount = 0;
    errorCount = 0;
  }

  /**
   * Given a rule name, it's reporting level, and a list of the violations from the ontology, add
   * the violations to the correct map.
   *
   * @param ruleName name of rule
   * @param level reporting level of rule
   * @param violations list of violations from this rule
   */
  public void addViolations(String ruleName, String level, List<Violation> violations) {
    logger.debug("violation found: " + ruleName);
    if (INFO.equals(level)) {
      info.put(ruleName, violations);
      infoCount += violations.size();
    } else if (WARN.equals(level)) {
      warn.put(ruleName, violations);
      warnCount += violations.size();
    } else if (ERROR.equals(level)) {
      error.put(ruleName, violations);
      errorCount += violations.size();
    }
    // Otherwise do nothing
  }

  /**
   * Return all the IRI strings in the current Violations.
   *
   * @return a set of IRI strings
   */
  public Set<String> getIRIs() {
    Set<String> iris = new HashSet<String>();
    iris.addAll(getIRIs(error));
    iris.addAll(getIRIs(warn));
    iris.addAll(getIRIs(info));
    return iris;
  }

  /**
   * Return all the IRI strings in the given list of Violations.
   *
   * @param violationSets map of rule name and violations
   * @return a set of IRI strings
   */
  public Set<String> getIRIs(Map<String, List<Violation>> violationSets) {
    Set<String> iris = new HashSet<String>();

    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      for (Violation v : vs.getValue()) {
        iris.add(v.subject);
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          iris.add(statement.getKey());
          for (String value : statement.getValue()) {
            if (!iris.contains(value)) {
              try {
                iris.add(IRI.create(new URL(value)).toString());
              } catch (Exception e) {
                // do nothing
              }
            }
          }
        }
      }
    }

    return iris;
  }

  /**
   * Return the total violations from reporting.
   *
   * @return number of violations
   */
  public Integer getTotalViolations() {
    return infoCount + warnCount + errorCount;
  }

  /**
   * Given a reporting level, return the number of violations.
   *
   * @param level reporting level
   * @return number of violations
   */
  public Integer getTotalViolations(String level) {
    if (INFO.equals(level)) {
      return infoCount;
    } else if (WARN.equals(level)) {
      return warnCount;
    } else if (ERROR.equals(level)) {
      return errorCount;
    } else {
      logger.error("Not a valid report level: " + level);
      return 0;
    }
  }

  /**
   * Return the report in TSV format.
   *
   * @return TSV string
   */
  public String toTSV() {
    StringBuilder sb = new StringBuilder();
    sb.append("Level\tRule Name\tSubject\tProperty\tValue\n");
    sb.append(tsvHelper(ERROR, error));
    sb.append(tsvHelper(WARN, warn));
    sb.append(tsvHelper(INFO, info));
    return sb.toString();
  }

  /**
   * Return the report in YAML format.
   *
   * @return YAML string
   */
  public String toYAML() {
    StringBuilder sb = new StringBuilder();
    sb.append(yamlHelper(ERROR, error));
    sb.append(yamlHelper(WARN, warn));
    sb.append(yamlHelper(INFO, info));
    return sb.toString();
  }

  /**
   * Given a prefix manager and an IRI as a string, return the CURIE if the prefix is available.
   * Otherwise, return the IRI as string.
   *
   * @param pm Prefix Manager to use
   * @param iriString IRI to convert to CURIE
   * @return CURIE or full IRI as string
   */
  private static String maybeGetCURIE(PrefixManager pm, String iriString) {
    IRI iri = IRI.create(iriString);
    if (iri != null) {
      String curie = pm.getPrefixIRI(iri);
      if (curie != null) {
        return curie;
      }
    }
    return iriString;
  }

  /**
   * Given a reporting level and a map of rules and violations, build a TSV output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return TSV string representation of the violations
   */
  private String tsvHelper(String level, Map<String, List<Violation>> violationSets) {
    // Get a prefix manager for creating CURIEs
    PrefixManager pm = new IOHelper().getPrefixManager();
    if (violationSets.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      String ruleName = vs.getKey();
      for (Violation v : vs.getValue()) {
        String subject = maybeGetCURIE(pm, v.subject);
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = maybeGetCURIE(pm, statement.getKey());
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "";
            } else {
              value = maybeGetCURIE(pm, value);
            }
            sb.append(level + "\t");
            sb.append(ruleName + "\t");
            sb.append(subject + "\t");
            sb.append(property + "\t");
            sb.append(value.replace("\t", " ").replace("\n", " ") + "\t");
            sb.append("\n");
          }
        }
      }
    }
    return sb.toString();
  }

  /**
   * Given a reporting level and a map of rules and violations, build a YAML output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return YAML string representation of the violations
   */
  private String yamlHelper(String level, Map<String, List<Violation>> violationSets) {
    // Get a prefix manager for creating CURIEs
    PrefixManager pm = new IOHelper().getPrefixManager();
    if (violationSets.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("- level : '" + level + "'");
    sb.append("\n");
    sb.append("  violations :");
    sb.append("\n");
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      String ruleName = vs.getKey();
      if (vs.getValue().isEmpty()) {
        continue;
      }
      sb.append("  - rule : '" + ruleName + "'");
      sb.append("\n");
      for (Violation v : vs.getValue()) {
        String subject = maybeGetCURIE(pm, v.subject);
        sb.append("    - subject : '" + subject + "'");
        sb.append("\n");
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = maybeGetCURIE(pm, statement.getKey());
          sb.append("      property : '" + property + "':");
          sb.append("\n");
          if (statement.getValue().isEmpty()) {
            continue;
          }
          sb.append("      values :");
          sb.append("\n");
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "null";
            } else {
              value = maybeGetCURIE(pm, value);
            }
            sb.append("        - '" + value + "'");
            sb.append("\n");
          }
        }
      }
    }
    return sb.toString();
  }
}
