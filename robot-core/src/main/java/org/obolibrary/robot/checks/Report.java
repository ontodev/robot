package org.obolibrary.robot.checks;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.obolibrary.robot.OntologyHelper;
import org.obolibrary.robot.UnmergeOperation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /** Line separator */
  private static final String newLine = System.getProperty("line.separator");

  /** Reporting levels. */
  private static final String INFO = "INFO";

  private static final String WARN = "WARN";
  private static final String ERROR = "ERROR";

  /** Maps of rules and the violations for each level */
  public Map<String, List<Violation>> info;

  public Map<String, List<Violation>> warn;
  public Map<String, List<Violation>> error;

  /** Counts of violations for each level */
  private Integer infoCount;

  private Integer warnCount;
  private Integer errorCount;

  /** Labels from the ontology */
  private Map<IRI, String> labels;

  /**
   * Given an ontology, create a new report on it.
   *
   * @param ontology OWLOntology to report on
   */
  public Report(OWLOntology ontology) {
    info = new HashMap<>();
    warn = new HashMap<>();
    error = new HashMap<>();

    infoCount = 0;
    warnCount = 0;
    errorCount = 0;

    // get ontology-specific labels
    labels = OntologyHelper.getLabels(ontology);
    // add RDFS labels (TODO: more?)
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#subClassOf"), "superclass");
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#comment"), "comment");
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#label"), "label");
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
    } else {
      // TODO: error message
    }
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
   * Return the report details in YAML format.
   *
   * @return YAML string
   */
  public String toYaml() {
    StringBuilder sb = new StringBuilder();
    sb.append(yamlHelper(INFO, info));
    sb.append(yamlHelper(WARN, warn));
    sb.append(yamlHelper(ERROR, error));
    return sb.toString();
  }

  /**
   * Given a reporting level and the violations for that level, return the YAML format.
   *
   * @param level reporting level
   * @param violationSets map of rule name and violations
   * @return YAML string
   */
  private String yamlHelper(String level, Map<String, List<Violation>> violationSets) {
    StringBuilder sb = new StringBuilder();
    // if there are no violations, no need to add anything
    if (violationSets.isEmpty()) {
      return "";
    }
    sb.append("- level       : " + level + newLine);
    sb.append("  violations  :" + newLine);
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      // if there are no Violations for a query, no need to add it
      if (vs.getValue().isEmpty()) {
        continue;
      }
      sb.append("  - rule      : " + vs.getKey() + newLine);
      sb.append("    entities  :" + newLine);
      for (Violation v : vs.getValue()) {
        sb.append("    - subject : " + v.subject + newLine);
        String subjectLabel = labels.get(IRI.create(v.subject));
        if (subjectLabel != null) {
          sb.append("      label   : \"" + subjectLabel + "\"" + newLine);
        }
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = statement.getKey();
          String propertyLabel = labels.get(IRI.create(property));
          if (propertyLabel != null) {
            sb.append("      " + propertyLabel + " :" + newLine);
          } else {
            sb.append("      " + property + " :" + newLine);
          }
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "null";
            }
            // handle literal values with XML Schema datatypes
            // quote any strings, found with xsd:string or lang tag
            String formattedValue = value;
            if (value.endsWith("/XMLSchema#string")) {
              formattedValue = "\"" + value.substring(0, value.lastIndexOf("^") - 1) + "\"";
            } else if (value.endsWith("@en")) {
              formattedValue = "\"" + value.substring(0, value.lastIndexOf("@")) + "\"";
            } else if (value.contains("/XMLSchema#")) {
              formattedValue = value.substring(0, value.lastIndexOf("^") - 1);
            } else if (value.contains(" ")) {
              // unknown datatype with spaces, add quotes
              formattedValue = "\"" + value + "\"";
            }
            sb.append("      - value : " + formattedValue + newLine);
            IRI valueIRI;
            try {
              valueIRI = IRI.create(new URL(value));
            } catch (Exception e) {
              valueIRI = null;
            }
            // if an IRI was created, try and find a label
            if (valueIRI != null) {
              String valueLabel = labels.get(IRI.create(value));
              if (valueLabel != null) {
                sb.append("        label : \"" + valueLabel + "\"" + newLine);
              }
            }
          }
        }
      }
    }
    return sb.toString();
  }
}
