package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO:
 * Implement a when-then rule, e.g.:
 *   when: %1 subclass-of: particularTypeOfExposureProcess then: %2 particularTypeOfExposureMaterial superclass-of ...
 */


/**
 * Implements the validate operation for a given CSV file and ontology.
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  // Naming convention: methods and static variables are named using the underscore convention,
  // local (method-internal) variables are named using camelCase

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /** Output writer */
  private static Writer writer;

  /** The reasoner factory to use for validation */
  private static OWLReasonerFactory reasoner_factory;

  /** The ontology to use for validation */
  private static OWLOntology ontology;

  /** A map from rdfs:labels to IRIs */
  private static Map<String, IRI> label_to_iri_map;

  /** A map from IRIs to rdfs:labels */
  private static Map<IRI, String> iri_to_label_map;

  /** The row of CSV data currently being processed */
  private static int csv_row_index;

  /** The column of CSV data currently being processed */
  private static int csv_col_index;

  /** An enum representation of the type of query to make. */
  private enum QEnum {
    DIRECT_SUPER("direct-superclass-of"),
    SUPER("superclass-of"),
    EQUIV("equivalent-to"),
    DIRECT_SUB("direct-subclass-of"),
    SUB("subclass-of"),
    DIRECT_INSTANCE("direct-instance-of"),
    INSTANCE("instance-of");

    private String ruleType;

    QEnum(String ruleType) {
      this.ruleType = ruleType;
    }

    public String getRuleType() {
      return ruleType;
    }
  }

  /** Reverse map from QEnum rule types to QEnums, populated at load time */
  private static final Map<String, QEnum> rule_type_to_qenum_map = new HashMap<>();
  static {
    for (QEnum q : QEnum.values()) {
      rule_type_to_qenum_map.put(q.getRuleType(), q);
    }
  }

  /**
   * INSERT DOC HERE
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   */
  public static void validate(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer) throws Exception, IOException {

    // Initialize the shared variables:
    initialize(ontology, reasonerFactory, writer);

    // Create a new reasoner, from the reasoner factory, based on the ontology data:
    OWLReasoner reasoner = reasoner_factory.createReasoner(ontology);

    // Extract the header and rules rows from the CSV data and map the column names to their
    // associated rules:
    List<String> header = csvData.remove(0);
    List<String> allRules = csvData.remove(0);
    HashMap<String, Map<String, List<String>>> headerToRuleMap = new HashMap();
    for (int i = 0; i < header.size(); i++) {
      headerToRuleMap.put(header.get(i), parse_rules(allRules.get(i)));
    }

    // Validate the data rows:
    for (csv_row_index = 0; csv_row_index < csvData.size(); csv_row_index++) {
      List<String> row = csvData.get(csv_row_index);
      for (csv_col_index = 0; csv_col_index < header.size(); csv_col_index++) {
        String colName = header.get(csv_col_index);
        Map<String, List<String>> colRules = headerToRuleMap.get(colName);

        // Get the contents of the current cell. If it is empty then report on this if it is a
        // required column, and skip it in any case.
        String cell = row.get(csv_col_index).trim();
        if (cell.equals("")) {
          // Note that the list corresponding to the "required" rule always has only one element.
          if (colRules.containsKey("required") && is_required(colRules.get("required").get(0))) {
            writeout("Empty field in a required column");
          }
          continue;
        }

        // Get the rdfs:label and IRI corresponding to the cell:
        String cellLabel = get_label_from_term(cell);
        if (cellLabel == null) {
          writeout(
              "Could not find '" + cell + "' in ontology");
          continue;
        }
        IRI iri = label_to_iri_map.get(cellLabel);

        // Validate the various 'the entity in this column has the following axiom' rules. For each
        // type of rule there will in general be multiple particular rules.
        for (QEnum queryType : QEnum.values()) {
          for (String axiom : colRules.getOrDefault(queryType.getRuleType(), Arrays.asList())) {
            validate_axiom(iri, cellLabel, axiom, reasoner, row, queryType);
          }
        }
      }
    }
    reasoner.dispose();
  }

  /**
   * INSERT DOC HERE
   */
  private static void initialize(
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer) {

    ValidateOperation.ontology = ontology;
    ValidateOperation.reasoner_factory = reasonerFactory;
    ValidateOperation.writer = writer;

    // Extract from the ontology two maps from rdfs:labels to IRIs and vice versa:
    ValidateOperation.iri_to_label_map = OntologyHelper.getIRILabels(ValidateOperation.ontology);
    ValidateOperation.label_to_iri_map = reverse_iri_label_map(ValidateOperation.iri_to_label_map);
  }

  /**
   * INSERT DOC HERE
   */
  private static void writeout(String msg) throws IOException {
    writer.write(
        String.format("At row: %d, column: %d: %s\n", csv_row_index + 1, csv_col_index + 1, msg));
  }

  /**
   * INSERT DOC HERE
   */
  private static void writeout(String msg, boolean showCoords) throws IOException {
    if (showCoords) {
      writeout(msg);
    }
    else {
      writer.write(msg + "\n");
    }
  }

  /**
   * INSERT DOC HERE
   */
  private static Map<String, IRI> reverse_iri_label_map(Map<IRI, String> source) {
    HashMap<String, IRI> target = new HashMap();
    for (Map.Entry<IRI, String> entry : source.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (target.containsKey(reverseKey)) {
        logger.warn(
            String.format(
                "Duplicate rdfs:label '%s'. Overwriting value '%s' with '%s'",
                reverseKey, target.get(reverseKey), reverseValue));
      }
      target.put(reverseKey, reverseValue);
    }
    return target;
  }

  /**
   * INSERT DOC HERE
   */
  private static Map<String, List<String>> parse_rules(String ruleString) throws IOException {
    HashMap<String, List<String>> ruleMap = new HashMap();
    if (!ruleString.trim().equals("")) {
      String[] rules = ruleString.split("\\s*;\\s*");
      for (String rule : rules) {
        String[] ruleParts = rule.split("\\s*:\\s*", 2);
        String ruleKey = ruleParts[0].trim();
        String ruleVal = ruleParts[1].trim();
        if (!ruleKey.equals("required") && !rule_type_to_qenum_map.containsKey(ruleKey)) {
          writeout("Unrecognised rule type '" + ruleKey + "' in rule '" + rule + "'", false);
          continue;
        }
        if (!ruleMap.containsKey(ruleKey)) {
          ruleMap.put(ruleKey, new ArrayList<String>());
        }
        ruleMap.get(ruleKey).add(ruleVal);
      }
    }
    return ruleMap;
  }

  /**
   * INSERT DOC HERE
   */
  private static boolean is_required(String reqStr) {
    reqStr = reqStr.toLowerCase();
    return Arrays.asList("true", "t", "1", "yes", "y").indexOf(reqStr) != -1;
  }

  /**
   * INSERT DOC HERE
   */
  private static String get_label_from_term(String term) {
    // If the term is already a recognised label, then just send it back:
    if (label_to_iri_map.containsKey(term)) {
      return term;
    }

    // Check to see if the term is a recognised IRI (possibly in short form), and if so return its
    // corresponding label:
    for (IRI iri : iri_to_label_map.keySet()) {
      if (iri.toString().equals(term) || iri.getShortForm().equals(term)) {
        return iri_to_label_map.get(iri);
      }
    }

    // If the label isn't recognised, just return null:
    return null;
  }

  /**
   * INSERT DOC HERE
   */
  private static String wildcard_to_label(String rule, List<String> row) throws IOException {
    String term = null;
    if (rule.startsWith("%")) {
      int colIndex = Integer.parseInt(rule.substring(1)) - 1;
      if (colIndex >= row.size()) {
        writeout(
            String.format(
                "Rule: '%s' indicates a column number that is greater than the row length (%d)",
                rule, row.size()));
        return null;
      }
      term = row.get(colIndex).trim();
    }
    else {
      term = rule;
    }

    return (term != null && !term.equals("")) ? get_label_from_term(term) : null;
  }

  /**
   * INSERT DOC HERE
   */
  private static String interpolate_axiom(String axiom, List<String> row) throws IOException {
    String interpolatedAxiom = "";

    // If the axiom consists in a single word without any occurrences of single or double quotes or
    // wildcard symbols (%), then assume it is a literal label, enclose it in single quotes and
    // return it:
    if (Pattern.matches("^[^\\s'\"%]+$", axiom)) {
      interpolatedAxiom = "'" + axiom + "'";
      logger.info(String.format("Interpolated: \"%s\" into \"%s\"", axiom, interpolatedAxiom));
      return interpolatedAxiom;
    }

    Matcher m = Pattern.compile("%\\d+").matcher(axiom);
    int currIndex = 0;
    while (m.find()) {
      String label = wildcard_to_label(m.group(), row);
      // If there is a problem finding the label for one of the wildcards, then just send back the
      // axiom as is:
      if (label == null) {
        writeout("Unable to interpolate '" + m.group() + "' in axiom '" + axiom + "'");
        return axiom;
      }

      // Iteratively build the interpolated axiom up to the current label, which we enclose in
      // single quotes:
      interpolatedAxiom =
          interpolatedAxiom + axiom.substring(currIndex, m.start()) + "'" + label + "'";
      currIndex = m.end();
    }
    // There may be text after the final wildcard, so add it now:
    interpolatedAxiom += axiom.substring(currIndex);
    logger.info(String.format("Interpolated: \"%s\" into \"%s\"", axiom, interpolatedAxiom));
    return interpolatedAxiom;
  }

  /**
   * INSERT DOC HERE
   */
  private static void validate_axiom(
      IRI iri,
      String label,
      String axiom,
      OWLReasoner reasoner,
      List<String> row,
      QEnum qType) throws Exception, IOException {

    logger.debug(String.format(
        "validate_axiom(): Called with parameters: " +
        "iri: '%s', " +
        "label: '%s', " +
        "axiom: '%s', " +
        "reasoner: '%s', " +
        "row: '%s', " +
        "query type: '%s'.",
        iri.getShortForm(), label, axiom, reasoner.getClass().getSimpleName(), row, qType.name()));

    // Interpolate any wildcards in the axiom into rdfs:label strings and then try to parse it:
    String interpolatedAxiom = interpolate_axiom(axiom, row);
    ManchesterSyntaxTool parser = new ManchesterSyntaxTool(ontology);
    OWLClassExpression ce;
    try {
      ce = parser.parseManchesterExpression(interpolatedAxiom);
    }
    catch (ParserException e) {
      writeout(
          String.format("Unable to parse rule '%s: %s'. %s",
                        qType.getRuleType(), axiom, e.getMessage()));
      return;
    }

    OWLEntity iriEntity = OntologyHelper.getEntity(ontology, iri);

    if (qType == QEnum.SUB || qType == QEnum.DIRECT_SUB) {
      // Check to see if the iri is a (direct) subclass of the given axiom:
      NodeSet<OWLClass> subClassesFound = reasoner.getSubClasses(ce, qType == QEnum.DIRECT_SUB);
      if (!subClassesFound.containsEntity(iriEntity.asOWLClass())) {
        writeout(
            String.format(
                "%s (%s) is not a%s descendant of '%s'",
                iri.getShortForm(), label, qType == QEnum.SUB ? "" : " direct", interpolatedAxiom));
      }
      else {
        logger.info(
            String.format(
                "Validated that %s (%s) is a%s descendant of '%s'",
                iri.getShortForm(), label, qType == QEnum.SUB ? "" : " direct", interpolatedAxiom));
      }
    }
    else if (qType == QEnum.SUPER || qType == QEnum.DIRECT_SUPER) {
      // Check to see if the iri is a (direct) superclass of the given axiom:
      NodeSet<OWLClass> superClassesFound =
          reasoner.getSuperClasses(ce, qType == QEnum.DIRECT_SUPER);
      if (!superClassesFound.containsEntity(iriEntity.asOWLClass())) {
        writeout(
            String.format(
                "%s (%s) does not%s subsume '%s'",
                iri.getShortForm(), label, qType == QEnum.SUPER ? "" : " directly",
                interpolatedAxiom));
      }
      else {
        logger.info(
            String.format(
                "Validated that %s (%s)%s subsumes '%s'",
                iri.getShortForm(), label, qType == QEnum.SUPER ? "" : " directly",
                interpolatedAxiom));
      }
    }
    else if (qType == QEnum.INSTANCE || qType == QEnum.DIRECT_INSTANCE) {
      NodeSet<OWLNamedIndividual> instancesFound = reasoner.getInstances(
          ce, qType == QEnum.DIRECT_INSTANCE);
      if (!instancesFound.containsEntity(iriEntity.asOWLNamedIndividual())) {
        writeout(
            String.format(
                "%s (%s) is not a%s instance of '%s'",
                iri.getShortForm(), label,
                qType == QEnum.INSTANCE ? "n" : " direct", interpolatedAxiom));
      }
      else {
        logger.info(
            String.format(
                "Validated that %s (%s) is a%s instance of '%s'",
                iri.getShortForm(), label,
                qType == QEnum.INSTANCE ? "n" : " direct", interpolatedAxiom));
      }
    }
    else if (qType == QEnum.EQUIV) {
      Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ce);
      if (!equivClassesFound.contains(iriEntity.asOWLClass())) {
        writeout(
            String.format(
                "%s (%s) is not equivalent to '%s'",
                iri.getShortForm(), label, interpolatedAxiom));
      }
      else {
        logger.info(
            String.format(
                "Validated that %s (%s) is equivalent to '%s'",
                iri.getShortForm(), label, interpolatedAxiom));
      }
    }
    else {
      logger.error("Unrecognised/unimplemented query type: " + qType.name());
    }

    parser.dispose();
  }
}
