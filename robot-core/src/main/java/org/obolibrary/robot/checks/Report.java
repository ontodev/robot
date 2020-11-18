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

  /** Map of rules and the violations for INFO level. */
  public Map<String, List<Violation>> info = new HashMap<>();

  /** Map of rules and the violations for WARN level. */
  public Map<String, List<Violation>> warn = new HashMap<>();

  /** Map of rules and the violations for ERROR level. */
  public Map<String, List<Violation>> error = new HashMap<>();

  /** Boolean to use labels for output - defaults to false. */
  private boolean useLabels = false;

  /** Count of violations for INFO. */
  private Integer infoCount = 0;

  /** Count of violations for WARN. */
  private Integer warnCount = 0;

  /** Count of violations for ERROR. */
  private Integer errorCount = 0;

  /** IOHelper to use. */
  private IOHelper ioHelper;

  /** Manager to use. */
  private OWLOntologyManager manager;

  /** QuotedEntityChecker to use. */
  private QuotedEntityChecker checker = null;

  /** Report headers. */
  private final List<String> header =
      Lists.newArrayList("Level", "Rule Name", "Subject", "Property", "Value");

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
   * Given a rule name, return the violation count for that rule. Throw exception if rule does not
   * exists.
   *
   * @param ruleName rule name to get number of violations for
   * @return number of violations for given rule name
   * @throws Exception if the rule name is not in this Report object
   */
  public Integer getViolationCount(String ruleName) throws Exception {
    if (info.containsKey(ruleName)) {
      List<Violation> v = info.get(ruleName);
      return v.size();
    } else if (warn.containsKey(ruleName)) {
      List<Violation> v = warn.get(ruleName);
      return v.size();
    } else if (error.containsKey(ruleName)) {
      List<Violation> v = error.get(ruleName);
      return v.size();
    }
    throw new Exception(String.format("'%s' is not a rule in this Report", ruleName));
  }

  /**
   * Convert the report details to a Table object to save.
   *
   * @param format String output format
   * @return export Table object
   */
  public Table toTable(String format) {
    CURIEShortFormProvider curieProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
    QuotedAnnotationValueShortFormProvider nameProvider =
        new QuotedAnnotationValueShortFormProvider(
            manager,
            curieProvider,
            ioHelper.getPrefixManager(),
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());
    ShortFormProvider provider;
    if (useLabels) {
      provider = nameProvider;
    } else {
      provider = curieProvider;
    }

    Table table = new Table(format);
    for (String h : header) {
      Column c = new Column(h, provider);
      table.addColumn(c);
    }

    addToTable(table, provider, ERROR, error);
    addToTable(table, provider, WARN, warn);
    addToTable(table, provider, INFO, info);

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
    CURIEShortFormProvider curieProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
    QuotedAnnotationValueShortFormProvider nameProvider =
        new QuotedAnnotationValueShortFormProvider(
            manager,
            curieProvider,
            ioHelper.getPrefixManager(),
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());
    ShortFormProvider provider;
    if (useLabels) {
      provider = nameProvider;
    } else {
      provider = curieProvider;
    }
    return yamlHelper(provider, ERROR, error)
        + yamlHelper(provider, WARN, warn)
        + yamlHelper(provider, INFO, info);
  }

  /**
   * Add violations to the Table object.
   *
   * @param table Table to add to
   * @param provider ShortFormProvider used to render objects
   * @param level String violation level
   * @param violations Map of violations (rule -> violations)
   */
  private void addToTable(
      Table table,
      ShortFormProvider provider,
      String level,
      Map<String, List<Violation>> violations) {
    List<Column> columns = table.getColumns();
    RendererType displayRenderer = table.getDisplayRendererType();
    for (Entry<String, List<Violation>> vs : violations.entrySet()) {
      String ruleName = getRuleName(vs.getKey());
      Cell levelCell = new Cell(columns.get(0), level);
      Cell ruleCell = new Cell(columns.get(1), ruleName);
      for (Violation v : vs.getValue()) {
        // Subject of the violation for the following rows
        String subject;
        if (v.entity != null) {
          subject = OntologyHelper.renderManchester(v.entity, provider, displayRenderer);
        } else {
          subject = v.subject;
        }
        Cell subjectCell = new Cell(columns.get(2), subject);
        for (Entry<OWLEntity, List<OWLObject>> statement : v.entityStatements.entrySet()) {
          // Property of the violation for the following rows
          String property = "";
          if (statement.getKey() != null) {
            property =
                OntologyHelper.renderManchester(statement.getKey(), provider, displayRenderer);
          }
          Cell propertyCell = new Cell(columns.get(3), property);

          if (statement.getValue().isEmpty()) {
            Row row = new Row(level);
            Cell valueCell = new Cell(columns.get(4), "");
            row.add(levelCell);
            row.add(ruleCell);
            row.add(subjectCell);
            row.add(propertyCell);
            row.add(valueCell);
            table.addRow(row);
          } else {
            for (OWLObject o : statement.getValue()) {
              Row row = new Row(level);

              String value = "";
              if (o != null) {
                value = OntologyHelper.renderManchester(o, provider, displayRenderer);
              }

              Cell valueCell = new Cell(columns.get(4), value);
              row.add(levelCell);
              row.add(ruleCell);
              row.add(subjectCell);
              row.add(propertyCell);
              row.add(valueCell);
              table.addRow(row);
            }
          }
        }
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = statement.getKey();
          if (property == null) {
            property = "";
          }
          Cell propertyCell = new Cell(columns.get(3), property);

          if (statement.getValue().isEmpty()) {
            Row row = new Row(level);
            Cell valueCell = new Cell(columns.get(4), "");
            row.add(levelCell);
            row.add(ruleCell);
            row.add(subjectCell);
            row.add(propertyCell);
            row.add(valueCell);
            table.addRow(row);
          } else {
            for (String value : statement.getValue()) {
              if (value == null) {
                continue;
              }
              Row row = new Row(level);
              Cell valueCell = new Cell(columns.get(4), value);
              row.add(levelCell);
              row.add(ruleCell);
              row.add(subjectCell);
              row.add(propertyCell);
              row.add(valueCell);
              table.addRow(row);
            }
          }
        }
      }
    }
  }

  /**
   * Given a rule name, return a rule name. If the string starts with "file", the name is the path
   * and should be stripped to just the name. Otherwise, the input is returned.
   *
   * @param ruleName string to (maybe) format
   * @return rule name
   */
  private String getRuleName(String ruleName) {
    if (ruleName.contains("file:")) {
      try {
        return ruleName.substring(ruleName.lastIndexOf("/") + 1, ruleName.lastIndexOf("."));
      } catch (Exception e) {
        return ruleName;
      }
    }
    return ruleName;
  }

  /**
   * Given a reporting level and a map of rules and violations, build a YAML output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return YAML string representation of the violations
   */
  private String yamlHelper(
      ShortFormProvider provider, String level, Map<String, List<Violation>> violationSets) {
    // Get a prefix manager for creating CURIEs
    if (violationSets.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("- level: '").append(level).append("'");
    sb.append("\n");
    sb.append("  violations:");
    sb.append("\n");
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      String ruleName = getRuleName(vs.getKey());
      if (vs.getValue().isEmpty()) {
        continue;
      }
      sb.append("  - ").append(ruleName).append(":");
      sb.append("\n");
      for (Violation v : vs.getValue()) {
        String subject =
            OntologyHelper.renderManchester(v.entity, provider, RendererType.OBJECT_RENDERER);
        sb.append("    - subject: \"").append(subject).append("\"");
        sb.append("\n");
        for (Entry<OWLEntity, List<OWLObject>> statement : v.entityStatements.entrySet()) {
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
          for (OWLObject value : statement.getValue()) {
            String display = "";
            if (value != null) {
              display =
                  OntologyHelper.renderManchester(value, provider, RendererType.OBJECT_RENDERER);
            }
            sb.append("        - \"").append(display.replace("\"", "\\\"")).append("\"");
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
            sb.append("        - \"").append(value.replace("\"", "\\\"")).append("\"");
            sb.append("\n");
          }
        }
      }
    }
    return sb.toString();
  }

  /**
   * Return all the IRI strings in the current Violations.
   *
   * @return a set of IRI strings
   */
  @Deprecated
  public Set<String> getIRIs() {
    Set<String> iris = new HashSet<>();
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
  @Deprecated
  public Set<String> getIRIs(Map<String, List<Violation>> violationSets) {
    Set<String> iris = new HashSet<>();

    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      for (Violation v : vs.getValue()) {
        iris.add(v.entity.getIRI().toString());
        for (Entry<OWLEntity, List<OWLObject>> statement : v.entityStatements.entrySet()) {
          iris.add(statement.getKey().getIRI().toString());
          for (OWLObject value : statement.getValue()) {
            if (value instanceof OWLEntity) {
              OWLEntity e = (OWLEntity) value;
              iris.add(e.getIRI().toString());
            } else if (value instanceof IRI) {
              IRI iri = (IRI) value;
              iris.add(iri.toString());
            }
          }
        }
      }
    }

    return iris;
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
