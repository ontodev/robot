package org.obolibrary.robot.checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.OntologyHelper;
import org.obolibrary.robot.QuotedEntityChecker;
import org.obolibrary.robot.export.*;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.obolibrary.robot.providers.QuotedAnnotationValueShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(Report.class);

  /** Reporting level INFO. */
  private static final String INFO = "INFO";

  /** Reporting level WARN. */
  private static final String WARN = "WARN";

  /** Reporting level ERROR. */
  private static final String ERROR = "ERROR";

  /** Collection of the info-level report queries. */
  private final List<ReportQuery> infoViolations = new ArrayList<>();

  /** Collection of the warn-level report queries. */
  private final List<ReportQuery> warnViolations = new ArrayList<>();

  /** Collection of the error-level report queries. */
  private final List<ReportQuery> errorViolations = new ArrayList<>();

  /** Count of violations for INFO. */
  private Integer infoCount = 0;

  /** Count of violations for WARN. */
  private Integer warnCount = 0;

  /** Count of violations for ERROR. */
  private Integer errorCount = 0;

  /** Boolean to use labels for output - defaults to false. */
  private boolean useLabels = false;

  /** IOHelper to use. */
  private final IOHelper ioHelper;

  /** Manager to use. */
  private OWLOntologyManager manager;

  /** QuotedEntityChecker to use. */
  private QuotedEntityChecker checker = null;

  /** Report headers. */
  private final List<String> header =
      Lists.newArrayList("Level", "Rule Name", "Subject", "Property", "Value");

  private IRI ontologyIRI = null;

  /**
   * Map of rule name to number of violations for INFO level. Replaces info map to support violation
   * count by rule name.
   */
  public Map<String, Integer> infoCountByRule = new HashMap<>();

  /**
   * Map of rule name to number of violations for WARN level. Replaces warn map to support violation
   * count by rule name.
   */
  public Map<String, Integer> warnCountByRule = new HashMap<>();

  /**
   * Map of rule name to number of violations for ERROR level. Replaces error map to support
   * violation count by rule name.
   */
  public Map<String, Integer> errorCountByRule = new HashMap<>();

  /** Map of rules and the violations for INFO level. */
  @Deprecated public Map<String, List<Violation>> info = new HashMap<>();

  /** Map of rules and the violations for WARN level. */
  @Deprecated public Map<String, List<Violation>> warn = new HashMap<>();

  /** Map of rules and the violations for ERROR level. */
  @Deprecated public Map<String, List<Violation>> error = new HashMap<>();

  /** Comparator for sorting a list of Report Queries alphabetically by their rule name. */
  static class ReportQueryComparator implements Comparator<ReportQuery> {
    /**
     * Compare two Report Query objects by their rule name.
     *
     * @param rq1 Report Query 1
     * @param rq2 Report Query 2
     * @return int for sorting
     */
    public int compare(ReportQuery rq1, ReportQuery rq2) {
      return rq1.getRuleName().compareTo(rq2.getRuleName());
    }
  }

  /**
   * Create a new report object without an ontology or predefined IOHelper.
   *
   * @throws IOException on problem creating IOHelper
   */
  public Report() throws IOException {
    ioHelper = new IOHelper();
  }

  /**
   * Create a new report object with an ontology (maybe) using labels for output.
   *
   * @param ontology OWLOntology to get labels from
   * @param useLabels if true, use labels for output
   * @throws IOException on problem creating IOHelper
   */
  public Report(OWLOntology ontology, boolean useLabels) throws IOException {
    if (ontology != null) {
      manager = ontology.getOWLOntologyManager();
      ontologyIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    } else {
      manager = OWLManager.createOWLOntologyManager();
    }

    ioHelper = new IOHelper();

    if (useLabels) {
      checker = new QuotedEntityChecker();
      checker.setIOHelper(this.ioHelper);
      checker.addProvider(new SimpleShortFormProvider());
      checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
      if (ontology != null) {
        checker.addAll(ontology);
      }
    }
  }

  /**
   * Create a new report object with a QuotedEntityChecker loaded with entries from the label map.
   * Use labels for report output.
   *
   * @param labelMap Map of IRI to label for all entities in the ontology
   * @throws IOException on problem creating IOHelper
   */
  public Report(Map<IRI, String> labelMap) throws IOException {
    manager = OWLManager.createOWLOntologyManager();
    this.ioHelper = new IOHelper();
    checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
    if (labelMap != null) {
      useLabels = true;
      OWLDataFactory df = OWLManager.getOWLDataFactory();
      for (Entry<IRI, String> entry : labelMap.entrySet()) {
        // Set all the entities as class - will not matter for retrieving label
        OWLEntity e = df.getOWLEntity(EntityType.CLASS, entry.getKey());
        checker.add(e, entry.getValue());
      }
    }
  }

  /**
   * Create a new report object with an ontology to (maybe) get labels from and a defined IOHelper.
   *
   * @param ontology OWLOntology to get labels from
   * @param ioHelper IOHelper to use
   * @param useLabels if true, use labels for output
   */
  public Report(OWLOntology ontology, IOHelper ioHelper, boolean useLabels) {
    if (ontology != null) {
      manager = ontology.getOWLOntologyManager();
      ontologyIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    } else {
      manager = OWLManager.createOWLOntologyManager();
    }

    this.ioHelper = ioHelper;
    if (useLabels) {
      checker = new QuotedEntityChecker();
      checker.setIOHelper(this.ioHelper);
      checker.addProvider(new SimpleShortFormProvider());
      checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
      if (ontology != null) {
        checker.addAll(ontology);
      }
    }
    this.useLabels = useLabels;
  }

  /**
   * Create a new report object with an ontology and a new IOHelper.
   *
   * @deprecated Report will not do anything with the ontology when not using labels. Use either
   *     {@link #Report()} or {@link #Report(OWLOntology, boolean)} or {@link #Report(OWLOntology,
   *     IOHelper, boolean)}.
   * @param ontology OWLOntology to get labels from
   * @throws IOException on problem creating IOHelper
   */
  @Deprecated
  public Report(OWLOntology ontology) throws IOException {
    // Ontology input doesn't do anything without labels
    ioHelper = new IOHelper();
  }

  /**
   * Create a new report object with an ontology and an IOHelper.
   *
   * @deprecated Report will not do anything with the ontology when not using labels. Use either
   *     {@link #Report(OWLOntology, boolean)} or {@link #Report(OWLOntology, IOHelper, boolean)}.
   * @param ontology OWLOntology object
   * @param ioHelper IOHelper to use
   */
  @Deprecated
  public Report(OWLOntology ontology, IOHelper ioHelper) {
    // Ontology input doesn't do anything without labels
    this.ioHelper = ioHelper;
  }

  /**
   * Add a ReportQuery to this Report.
   *
   * @param rq ReportQuery to add
   */
  public void addReportQuery(ReportQuery rq) {
    String level = rq.getLevel();
    switch (level) {
      case ERROR:
        errorViolations.add(rq);
        errorCount += rq.getViolations().size();
        errorCountByRule.put(rq.getRuleName(), rq.getViolations().size());
        break;
      case WARN:
        warnViolations.add(rq);
        warnCount += rq.getViolations().size();
        warnCountByRule.put(rq.getRuleName(), rq.getViolations().size());
        break;
      case INFO:
        infoViolations.add(rq);
        infoCount += rq.getViolations().size();
        infoCountByRule.put(rq.getRuleName(), rq.getViolations().size());
        break;
      default:
        logger.error(
            String.format("Unknown violation level for '%s': %s", rq.getRuleName(), level));
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
   * Convert the report details to a Table object to save.
   *
   * @param format String output format
   * @return export Table object
   */
  public Table toTable(String format) {
    ShortFormProvider provider = getProvider();

    Table table = new Table(format);
    for (String h : header) {
      Column c = new Column(h, provider);
      table.addColumn(c);
    }

    // Sort results
    errorViolations.sort(new ReportQueryComparator());
    warnViolations.sort(new ReportQueryComparator());
    infoViolations.sort(new ReportQueryComparator());

    addToTable(table, provider, ERROR, errorViolations);
    addToTable(table, provider, WARN, warnViolations);
    addToTable(table, provider, INFO, infoViolations);

    return table;
  }

  /**
   * Return the report in JSON format. This converts the YAML format of the report to JSON.
   *
   * @return JSON String
   * @throws IOException on issue converting YAML to JSON
   */
  public String toJSON() throws IOException {
    Object obj = new ObjectMapper(new YAMLFactory()).readValue(toYAML(), Object.class);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(obj);
  }

  /**
   * Return the report in YAML format.
   *
   * @return YAML string
   */
  public String toYAML() {
    // Sort results
    errorViolations.sort(new ReportQueryComparator());
    warnViolations.sort(new ReportQueryComparator());
    infoViolations.sort(new ReportQueryComparator());

    // Create YAML
    ShortFormProvider provider = getProvider();
    return yamlHelper(provider, ERROR, errorViolations)
        + yamlHelper(provider, WARN, warnViolations)
        + yamlHelper(provider, INFO, infoViolations);
  }

  /**
   * Add violations to the Table object.
   *
   * @param table Table to add to
   * @param provider ShortFormProvider used to render objects
   * @param level String violation level
   * @param reportQueries collection of ReportQuery objects at this violation level
   */
  private void addToTable(
      Table table, ShortFormProvider provider, String level, List<ReportQuery> reportQueries) {
    List<Column> columns = table.getColumns();
    RendererType displayRenderer = table.getDisplayRendererType();
    for (ReportQuery rq : reportQueries) {
      // Create a reusable cell for the violation level
      Cell levelCell = new Cell(columns.get(0), level);

      // Create a reusable cell for the name of the rule, maybe adding a link if we have one
      String ruleName = rq.getRuleName();
      Cell ruleCell = new Cell(columns.get(1), ruleName);
      String ruleURL = rq.getRuleURL();
      if (ruleURL != null) {
        ruleCell.setHref(ruleURL);
      }

      // Add a row for each violation
      for (Violation v : rq.getViolations()) {
        // Subject of the violation for the following rows
        String subject;
        if (ontologyIRI != null
            && v.entity != null
            && !v.entity.isAnonymous()
            && v.entity.getIRI().toString().equals(ontologyIRI.toString())) {
          // If the IRI is the ontology IRI, keep this as the full string
          subject = ontologyIRI.toString();
        } else {
          // Otherwise, render the subject based on the display renderer
          if (v.entity != null) {
            subject = OntologyHelper.renderManchester(v.entity, provider, displayRenderer);
          } else {
            subject = v.subject;
          }
        }
        Cell subjectCell = new Cell(columns.get(2), subject);
        for (Entry<OWLEntity, List<OWLEntity>> statement : v.entityStatements.entrySet()) {
          // Property of the violation for the following rows
          String property = "";
          if (statement.getKey() != null) {
            property =
                OntologyHelper.renderManchester(statement.getKey(), provider, displayRenderer);
          }
          Cell propertyCell = new Cell(columns.get(3), property);

          if (statement.getValue().isEmpty()) {
            Cell valueCell = new Cell(columns.get(4), "");
            addRowToTable(table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
          } else {
            for (OWLEntity e : statement.getValue()) {
              String value = OntologyHelper.renderManchester(e, provider, displayRenderer);
              Cell valueCell = new Cell(columns.get(4), value);
              addRowToTable(
                  table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
            }
          }
        }

        for (Entry<OWLEntity, List<String>> statement : v.literalStatements.entrySet()) {
          // Property of the violation for the following rows
          String property = "";
          if (statement.getKey() != null) {
            property =
                OntologyHelper.renderManchester(statement.getKey(), provider, displayRenderer);
          }
          Cell propertyCell = new Cell(columns.get(3), property);

          if (statement.getValue().isEmpty()) {
            Cell valueCell = new Cell(columns.get(4), "");
            addRowToTable(table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
          } else {
            for (String value : statement.getValue()) {
              if (value == null) {
                value = "";
              }
              Cell valueCell = new Cell(columns.get(4), value);
              addRowToTable(
                  table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
            }
          }
        }

        // Support for old statements method
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = statement.getKey();
          if (property == null) {
            property = "";
          }
          Cell propertyCell = new Cell(columns.get(3), property);

          if (statement.getValue().isEmpty()) {
            Cell valueCell = new Cell(columns.get(4), "");
            addRowToTable(table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
          } else {
            for (String value : statement.getValue()) {
              if (value == null) {
                continue;
              }
              Cell valueCell = new Cell(columns.get(4), value);
              addRowToTable(
                  table, level, levelCell, ruleCell, subjectCell, propertyCell, valueCell);
            }
          }
        }
      }
    }
  }

  /**
   * Add a row to a table.
   *
   * @param table Table to add row to
   * @param level String violation level
   * @param levelCell Cell for level
   * @param ruleCell Cell for rule
   * @param subjectCell Cell for subject
   * @param propertyCell Cell for property
   * @param valueCell Cell for value
   */
  private void addRowToTable(
      Table table,
      String level,
      Cell levelCell,
      Cell ruleCell,
      Cell subjectCell,
      Cell propertyCell,
      Cell valueCell) {
    Row row = new Row(level);
    row.add(levelCell);
    row.add(ruleCell);
    row.add(subjectCell);
    row.add(propertyCell);
    row.add(valueCell);
    table.addRow(row);
  }

  /**
   * Get a ShortFormProvider to render entities based on if we are rendering labels or not.
   *
   * @return ShortFormProvider that either uses names (labels or CURIEs) or CURIEs
   */
  private ShortFormProvider getProvider() {
    CURIEShortFormProvider curieProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
    QuotedAnnotationValueShortFormProvider nameProvider =
        new QuotedAnnotationValueShortFormProvider(
            manager,
            curieProvider,
            ioHelper.getPrefixManager(),
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());
    if (useLabels) {
      return nameProvider;
    }
    return curieProvider;
  }

  /**
   * Given a reporting level and a map of rules and violations, build a YAML output.
   *
   * @param provider ShortFormProvider used to render objects
   * @param level reporting level
   * @param reportQueries collection of ReportQuery objects at this violation level
   * @return YAML string representation of the violations
   */
  private String yamlHelper(
      ShortFormProvider provider, String level, List<ReportQuery> reportQueries) {
    // Get a prefix manager for creating CURIEs
    if (reportQueries.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("- level: '").append(level).append("'");
    sb.append("\n");
    sb.append("  violations:");
    sb.append("\n");
    for (ReportQuery rq : reportQueries) {
      String ruleName = rq.getRuleName();
      if (rq.getViolations().isEmpty()) {
        continue;
      }
      sb.append("  - ").append(ruleName).append(":");
      sb.append("\n");
      for (Violation v : rq.getViolations()) {
        String subject =
            OntologyHelper.renderManchester(v.entity, provider, RendererType.OBJECT_RENDERER);
        sb.append("    - subject: \"").append(subject).append("\"");
        sb.append("\n");
        for (Entry<OWLEntity, List<OWLEntity>> statement : v.entityStatements.entrySet()) {
          String property =
              OntologyHelper.renderManchester(
                  statement.getKey(), provider, RendererType.OBJECT_RENDERER);
          sb.append("      property: \"").append(property).append("\"");
          sb.append("\n");
          if (statement.getValue().isEmpty()) {
            continue;
          }
          sb.append("      values:");
          sb.append("\n");
          for (OWLEntity value : statement.getValue()) {
            String display = "";
            if (value != null) {
              display =
                  OntologyHelper.renderManchester(value, provider, RendererType.OBJECT_RENDERER);
            }
            sb.append("        - \"").append(display).append("\"");
            sb.append("\n");
          }
        }
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = statement.getKey();
          if (property == null) {
            property = "";
          }
          sb.append("      property: \"").append(property).append("\"");
          sb.append("\n");
          if (statement.getValue().isEmpty()) {
            continue;
          }
          sb.append("      values:");
          sb.append("\n");
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "";
            }
            sb.append("        - \"").append(value).append("\"");
            sb.append("\n");
          }
        }
      }
    }
    return sb.toString();
  }

  /**
   * Given a rule name, return the violation count for that rule. Throw exception if rule does not
   * exists.
   *
   * @param ruleName rule name to get number of violations for
   * @return number of violations for given rule name
   * @throws Exception if the rule name is not in this Report object
   */
  public Integer getViolationCount(String ruleName) throws Exception {
    if (infoCountByRule.containsKey(ruleName)) {
      return infoCountByRule.get(ruleName);
    } else if (warnCountByRule.containsKey(ruleName)) {
      return warnCountByRule.get(ruleName);
    } else if (errorCountByRule.containsKey(ruleName)) {
      return errorCountByRule.get(ruleName);
    }
    throw new Exception(String.format("'%s' is not a rule in this Report", ruleName));
  }

  /**
   * Return all the IRI strings in the current Violations.
   *
   * @return a set of IRI strings
   */
  public Set<String> getIRIs() {
    List<Violation> allViolations = new ArrayList<>();
    for (ReportQuery rq : errorViolations) {
      allViolations.addAll(rq.getViolations());
    }
    for (ReportQuery rq : warnViolations) {
      allViolations.addAll(rq.getViolations());
    }
    for (ReportQuery rq : infoViolations) {
      allViolations.addAll(rq.getViolations());
    }
    return getIRIs(allViolations);
  }

  /**
   * Return all the IRI strings in the given list of Violations.
   *
   * @param violations list of Violations
   * @return a set of IRI strings
   */
  public Set<String> getIRIs(List<Violation> violations) {
    Set<String> iris = new HashSet<>();

    for (Violation v : violations) {
      iris.add(v.entity.getIRI().toString());
      for (Entry<OWLEntity, List<OWLEntity>> statement : v.entityStatements.entrySet()) {
        iris.add(statement.getKey().getIRI().toString());
        for (OWLEntity value : statement.getValue()) {
          iris.add(value.getIRI().toString());
        }
      }
    }

    return iris;
  }

  /**
   * Given a rule name, it's reporting level, and a list of the violations from the ontology, add
   * the violations to the correct map.
   *
   * @param ruleName name of rule
   * @param level reporting level of rule
   * @param violations list of violations from this rule
   * @deprecated violations should be added to their appropriate ReportQuery object
   */
  @Deprecated
  public void addViolations(String ruleName, String level, List<Violation> violations) {
    logger.debug("violation found: " + ruleName);
    if (INFO.equals(level)) {
      info.put(ruleName, violations);
      infoCount += violations.size();
      infoCountByRule.put(ruleName, violations.size());
    } else if (WARN.equals(level)) {
      warn.put(ruleName, violations);
      warnCount += violations.size();
      warnCountByRule.put(ruleName, violations.size());
    } else if (ERROR.equals(level)) {
      error.put(ruleName, violations);
      errorCount += violations.size();
      errorCountByRule.put(ruleName, violations.size());
    }
    // Otherwise do nothing
  }

  /**
   * Return the report in CSV format.
   *
   * @return CSV string
   */
  @Deprecated
  public String toCSV() {
    Table t = toTable("csv");
    StringBuilder sb = new StringBuilder();
    for (String[] row : t.toList("")) {
      sb.append(String.join(",", row));
    }
    return sb.toString();
  }

  /**
   * Return the report in TSV format.
   *
   * @return TSV string
   */
  @Deprecated
  public String toTSV() {
    Table t = toTable("tsv");
    StringBuilder sb = new StringBuilder();
    for (String[] row : t.toList("")) {
      sb.append(String.join("\t", row));
    }
    return sb.toString();
  }
}
