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

  public Map<String, List<Violation>> severity1;
  public Map<String, List<Violation>> severity2;
  public Map<String, List<Violation>> severity3;
  public Map<String, List<Violation>> severity4;
  public Map<String, List<Violation>> severity5;

  private Integer count1;
  private Integer count2;
  private Integer count3;
  private Integer count4;
  private Integer count5;

  private Map<IRI, String> labels;

  public Report(OWLOntology ontology) {
    severity1 = new HashMap<>();
    severity2 = new HashMap<>();
    severity3 = new HashMap<>();
    severity4 = new HashMap<>();
    severity5 = new HashMap<>();

    count1 = 0;
    count2 = 0;
    count3 = 0;
    count4 = 0;
    count5 = 0;

    // get ontology-specific labels
    labels = OntologyHelper.getLabels(ontology);
    // add RDFS labels (TODO: more?)
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#subClassOf"), "superclass");
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#comment"), "comment");
    labels.put(IRI.create("http://www.w3.org/2000/01/rdf-schema#label"), "label");
  }

  public void addViolations(Integer severity, String title, List<Violation> violations) {
    logger.debug("violation found: " + title);
    switch (severity) {
      case 1:
        severity1.put(title, violations);
        count1 += violations.size();
      case 2:
        severity2.put(title, violations);
        count2 += violations.size();
      case 3:
        severity3.put(title, violations);
        count3 += violations.size();
      case 4:
        severity4.put(title, violations);
        count4 += violations.size();
      case 5:
        severity5.put(title, violations);
        count5 += violations.size();
    }
  }

  public Integer getTotalViolations() {
    return count1 + count2 + count3 + count4 + count5;
  }

  public Integer getTotalViolations(Integer severity) {
    switch (severity) {
      case 1:
        return count1;
      case 2:
        return count2;
      case 3:
        return count3;
      case 4:
        return count4;
      case 5:
        return count5;
      default:
        logger.error("Not a valid severity level: " + severity);
        return 0;
    }
  }

  public String toYaml() {
    StringBuilder sb = new StringBuilder();
    sb.append(yamlHelper(1, severity1));
    sb.append(yamlHelper(2, severity2));
    sb.append(yamlHelper(3, severity3));
    sb.append(yamlHelper(4, severity4));
    sb.append(yamlHelper(5, severity5));
    return sb.toString();
  }

  private String yamlHelper(Integer severity, Map<String, List<Violation>> violationSets) {
    StringBuilder sb = new StringBuilder();
    // if there are no violations, no need to add anything
    if (violationSets.isEmpty()) {
      return "";
    }
    sb.append("- severity    : " + severity + newLine);
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
