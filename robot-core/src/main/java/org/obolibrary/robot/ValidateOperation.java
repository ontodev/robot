package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
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
 * - Allow multiple rule types in a single clause (e.g. subclass-of-equivalent-to), i.e. the
 *   equivalent of checking multiple tickboxes in the dl query editor
 * - Get the requirements in the immune exposures document implemented
 * - Make sure rule parsing is as robust as possible
 *   - In when clause, allow a colon after the rule type
 * - Add more logging statements and make them and the error messages more user-friendly
 * - Eventually write a report to Excel.
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

  /** The ontology to use for validation */
  private static OWLOntology ontology;

  /** The reasoner to use for validation */
  private static OWLReasoner reasoner;

  /** The parser to use for evaluating class expressions */
  private static ManchesterSyntaxTool parser;

  /** A map from rdfs:labels to IRIs */
  private static Map<String, IRI> label_to_iri_map;

  /** A map from IRIs to rdfs:labels */
  private static Map<IRI, String> iri_to_label_map;

  /** The row of CSV data currently being processed */
  private static int csv_row_index;

  /** The column of CSV data currently being processed */
  private static int csv_col_index;

  /** An enum representation of the different categories of rules. We distinguish between queries,
      which involve queries to a reasoner, and other rules */
  private enum RCatEnum { QUERY, OTHER }

  /** An enum representation of the type rule to query. */
  private enum RTypeEnum {
    DIRECT_SUPER("direct-superclass-of", RCatEnum.QUERY),
    SUPER("superclass-of", RCatEnum.QUERY),
    EQUIV("equivalent-to", RCatEnum.QUERY),
    DIRECT_SUB("direct-subclass-of", RCatEnum.QUERY),
    SUB("subclass-of", RCatEnum.QUERY),
    DIRECT_INSTANCE("direct-instance-of", RCatEnum.QUERY),
    INSTANCE("instance-of", RCatEnum.QUERY),
    REQUIRED("is-required", RCatEnum.OTHER),
    EXCLUDED("is-excluded", RCatEnum.OTHER);

    private final String ruleType;
    private final RCatEnum ruleCat;

    RTypeEnum(String ruleType, RCatEnum ruleCat) {
      this.ruleType = ruleType;
      this.ruleCat = ruleCat;
    }

    public String getRuleType() {
      return ruleType;
    }

    public RCatEnum getRuleCat() {
      return ruleCat;
    }
  }

  /** Reverse map from RTypeEnum rule types to RTypeEnums, populated at load time */
  private static final Map<String, RTypeEnum> rule_type_to_rtenum_map = new HashMap<>();
  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      rule_type_to_rtenum_map.put(r.getRuleType(), r);
    }
  }

  /** Reverse map from RTypeEnum rule types in the QUERY category to RTypeEnums, populated at load
      time */
  private static final Map<String, RTypeEnum> query_type_to_rtenum_map = new HashMap<>();
  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      if (r.getRuleCat() == RCatEnum.QUERY) {
        query_type_to_rtenum_map.put(r.getRuleType(), r);
      }
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

        // If there are no rules for this column, then skip this cell (this a "comment" column).
        if (colRules.isEmpty()) continue;

        // Get the contents of the current cell.
        String cell = row.get(csv_col_index).trim();

        // For each of the defined rule types ...
        for (RTypeEnum ruleType : RTypeEnum.values()) {
          // For each rule of that type that is constraining this column ...
          for (String rule : colRules.getOrDefault(ruleType.getRuleType(), Arrays.asList())) {
            // Validate the cell against it:
            validate_rule(cell, rule, reasoner, row, ruleType);
          }
        }
      }
    }
    tearDown();
  }

  /**
   * INSERT DOC HERE
   */
  private static void initialize(
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer) {

    ValidateOperation.ontology = ontology;
    ValidateOperation.writer = writer;

    // Initialise the parser based on the given ontology:
    parser = new ManchesterSyntaxTool(ontology);
    // Use the given reasonerFactory to initialise the reasoner based on the given ontology:
    reasoner = reasonerFactory.createReasoner(ontology);

    // Extract from the ontology two maps from rdfs:labels to IRIs and vice versa:
    ValidateOperation.iri_to_label_map = OntologyHelper.getIRILabels(ValidateOperation.ontology);
    ValidateOperation.label_to_iri_map = reverse_iri_label_map(ValidateOperation.iri_to_label_map);
  }

  /**
   * INSERT DOC HERE
   */
  private static void tearDown() {
    parser.dispose();
    reasoner.dispose();
  }

  /**
   * INSERT DOC HERE
   */
  private static void writeout(boolean showCoords, String format, Object... positionalArgs)
      throws IOException {

    String outStr = "";
    if (showCoords) {
      outStr += String.format("At row: %d, column: %d: ", csv_row_index + 1, csv_col_index + 1);
    }
    outStr += String.format(format, positionalArgs);
    writer.write(outStr + "\n");
  }

  /**
   * INSERT DOC HERE
   */
  private static void writeout(String format, Object... positionalArgs) throws IOException {
    writeout(true, format, positionalArgs);
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
                "Duplicate rdfs:label \"%s\". Overwriting value \"%s\" with \"%s\"",
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
        if (!rule_type_to_rtenum_map.containsKey(ruleKey)) {
          writeout(false, "Unrecognised rule type \"" + ruleKey + "\" in rule \"" + rule + "\"");
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
    return Arrays.asList("true", "t", "1", "yes", "y").indexOf(reqStr.toLowerCase()) != -1;
  }

  /**
   * INSERT DOC HERE
   */
  private static boolean is_excluded(String exclStr) {
    return Arrays.asList("true", "t", "1", "yes", "y").indexOf(exclStr.toLowerCase()) != -1;
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
        writeout("Rule: \"%s\" indicates a column number that is greater than the row length (%d)",
                 rule, row.size());
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
  private static String interpolate(String str, List<String> row) throws IOException {
    String interpolatedString = "";

    // If the string consists in a single word without any occurrences of single or double quotes or
    // wildcard symbols (%), then assume it is a literal label, enclose it in single quotes and
    // return it:
    if (Pattern.matches("^[^\\s'\"%]+$", str)) {
      interpolatedString = "'" + str + "'";
      logger.info(String.format("Interpolated: \"%s\" into \"%s\"", str, interpolatedString));
      return interpolatedString;
    }

    Matcher m = Pattern.compile("%\\d+").matcher(str);
    int currIndex = 0;
    while (m.find()) {
      String label = wildcard_to_label(m.group(), row);
      // If there is a problem finding the label for one of the wildcards, then just send back the
      // string as is:
      if (label == null) {
        writeout("Unable to interpolate \"" + m.group() + "\" in string \"" + str + "\"");
        return str;
      }

      // Iteratively build the interpolated string up to the current label, which we enclose in
      // single quotes:
      interpolatedString =
          interpolatedString + str.substring(currIndex, m.start()) + "'" + label + "'";
      currIndex = m.end();
    }
    // There may be text after the final wildcard, so add it now:
    interpolatedString += str.substring(currIndex);
    logger.info(String.format("Interpolated: \"%s\" into \"%s\"", str, interpolatedString));
    return interpolatedString;
  }

  /**
   * INSERT DOC HERE
   // Parses the given rule into a main part and optional when clause.
   */
  private static SimpleEntry<String, List<String[]>> separate_rule(String rule) throws IOException {
    // By default return the rule as it was given (unseparatedRule) plus an
    // empty list of when-clauses:
    SimpleEntry<String, List<String[]>> unseparatedRule =
        new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());

    // Check if there are any when clauses
    Matcher m = Pattern.compile("(\\(\\s*(when:?)\\s+.+\\))(.*)").matcher(rule);
    String whenClauseStr = null;
    if (!m.find()) {
      // If there is no when clause, then just return back the rule as it was passed with an empty
      // when clause:
      return unseparatedRule;
    }

    if (m.start() == 0) {
      writeout("Rule: \"" + rule + "\" has when clause but no main clause.");
      return unseparatedRule;
    }

    whenClauseStr = m.group(1);
    // Extract the actual content of the clause. m.group(2) is the "when" (or alternately "when:")
    // that opens the when-clause. We add 2 to the length to account for the leading '(' and the
    // succeeding space, e.g., "(when ", "(when: "
    whenClauseStr = whenClauseStr.substring(m.group(2).length() + 2, whenClauseStr.length() - 1);

    // Don't fail just because there is some extra garbage at the end of the rule, but notify
    // about it:
    if (!m.group(3).trim().equals("")) {
      writeout("Ignoring string \"" + m.group(3).trim() + "\" at end of rule \"" + rule + "\".");
    }

    // Within each when clause, multiple subclauses separated by ampersands are allowed. Each
    // subclass must be of the form: <Entity> <Rule-Type> <Axiom>.
    // Entity is in the form of a (not necessaruly interpolated) label: either a contiguous string
    // or a string with whitespace enclosed in single quotes. <Rule-Type> is a hyphenated
    // alphanumeric string. <Axiom> can take any form. Here we resolve each sub-clause of the
    // when statement into a list of such triples.
    ArrayList<String[]> whenClauses = new ArrayList();
    for (String whenClause : whenClauseStr.split("\\s*&\\s*")) {
      m = Pattern.compile(
          "^([^\'\\s]+|\'[^\']+\')\\s+([a-z\\-]+:?)\\s+(.*)$")
          .matcher(whenClause);

      if (m.find()) {
        whenClauses.add(new String[] {m.group(1), m.group(2), m.group(3)});
      }
      else {
        writeout("Unable to decompose when-clause: \"" + whenClause + "\".");
        return unseparatedRule;
      }
    }

    // Now get the main part of the rule (i.e. the part before the when clause):
    m = Pattern.compile("^(.+)\\s+\\(when:?\\s").matcher(rule);
    if (!m.find()) {
      writeout("Encountered unknown error while looking for main clause of rule \"" + rule + "\"");
      return unseparatedRule;
    }

    return new SimpleEntry<String, List<String[]>>(m.group(1), whenClauses);
  }

  /**
   * INSERT DOC HERE
   */
  private static void validate_rule(
      String cell,
      String rule,
      OWLReasoner reasoner,
      List<String> row,
      RTypeEnum rType) throws Exception, IOException {

    logger.debug(String.format(
        "validate_rule(): Called with parameters: " +
        "cell: \"%s\", " +
        "rule: \"%s\", " +
        "reasoner: \"%s\", " +
        "row: \"%s\", " +
        "rule type: \"%s\".",
        cell, rule, reasoner.getClass().getSimpleName(), row, rType.getRuleType()));

    // Separate the given rule into its main clause and optional when clauses:
    SimpleEntry<String, List<String[]>> separatedRule = separate_rule(rule);

    // Evaluate and validate any when clauses for this rule:
    boolean whenIsSatisfied = true;
    for (String[] whenClause : separatedRule.getValue()) {
      String interpolatedSubject = interpolate(whenClause[0], row);
      // Get the IRI for the interpolated subject, first removing any surrounding single quotes
      // from the label:
      IRI subjectIri = label_to_iri_map.get(interpolatedSubject.replaceAll("^\'|\'$", ""));
      if (subjectIri == null) {
        writeout("Could not determine IRI for label: " + interpolatedSubject);
        continue;
      }

      // Determine the kind of rule this is supposed to be:
      RTypeEnum whenRType = rule_type_to_rtenum_map.get(whenClause[1]);
      if (whenRType == null) {
        writeout("Could not determine rule type of: " + whenClause[1]);
        continue;
      }

      // Make sure the rule is of the right category for a when clause:
      if (whenRType.getRuleCat() != RCatEnum.QUERY) {
        writeout("Only rules of type: %s are allowed in a when clause. Skipping clause: \"%s\"",
                 query_type_to_rtenum_map.keySet(), whenClause[1]);
        continue;
      }

      // Interpolate the axiom to validate and send the query to the reasoner:
      String interpolatedAxiom = interpolate(whenClause[2], row);
      if (!execute_query(subjectIri, interpolatedAxiom, reasoner, row, whenRType)) {
        whenIsSatisfied = false;
        logger.info(
            String.format(
                "When clause: \"%s (%s) %s %s\" is not satisfied",
                interpolatedSubject, subjectIri.getShortForm(), whenRType, interpolatedAxiom));
        // If any of the when clauses fail to be satisfied, then we can skip evaluating the others:
        break;
      }
    }

    // If any of the when clauses have not been satisfied, just exit:
    if (!whenIsSatisfied) {
      logger.debug("Not running main rule clause: When clauses have not been satisfied.");
      return;
    }

    // If this rule is not in the QUERY category, process it at this step and return from this
    // function. The further steps below are only needed for queries to the reasoner.
    if (rType.getRuleCat() != RCatEnum.QUERY) {
      validate_generic_rule(rule, rType, cell, row);
      return;
    }

    // Get the rdfs:label corresponding to the cell; just exit if it can't be found:
    String cellLabel = get_label_from_term(cell);
    if (cellLabel == null) {
      writeout("Could not find \"" + cell + "\" in ontology");
      return;
    }

    // Get the cell's IRI, interpolate the axiom, and execute the query:
    IRI cellIri = label_to_iri_map.get(cellLabel);
    String interpolatedMainAxiom = interpolate(separatedRule.getKey(), row);
    boolean result = execute_query(cellIri, interpolatedMainAxiom, reasoner, row, rType);
    if (!result) {
      writeout("Rule: \"%s (%s) %s %s\" is not satisfied",
               cellLabel, cellIri.getShortForm(), rType, interpolatedMainAxiom);
    }
    else {
      logger.info(
          String.format(
              "Rule: \"%s (%s) %s %s\" is satisfied",
              cellLabel, cellIri.getShortForm(), rType, interpolatedMainAxiom));
    }
  }

  private static boolean execute_query(
      IRI iri,
      String rule,
      OWLReasoner reasoner,
      List<String> row,
      RTypeEnum rType) throws Exception, IOException {

    logger.debug(String.format(
        "execute_query(): Called with parameters: " +
        "iri: \"%s\", " +
        "rule: \"%s\", " +
        "reasoner: \"%s\", " +
        "row: \"%s\", " +
        "query type: \"%s\".",
        iri.getShortForm(), rule, reasoner.getClass().getSimpleName(), row, rType.getRuleType()));

    boolean returnValue = false;

    OWLClassExpression ce;
    try {
      ce = parser.parseManchesterExpression(rule);
    }
    catch (ParserException e) {
      writeout("Unable to parse rule \"%s: %s\". %s", rType.getRuleType(), rule, e.getMessage());
      return returnValue;
    }

    OWLEntity iriEntity = OntologyHelper.getEntity(ontology, iri);
    String label = iri_to_label_map.get(iri);

    if (rType == RTypeEnum.SUB || rType == RTypeEnum.DIRECT_SUB) {
      // Check to see if the iri is a (direct) subclass of the given rule:
      NodeSet<OWLClass> subClassesFound = reasoner.getSubClasses(ce, rType == RTypeEnum.DIRECT_SUB);
      return subClassesFound.containsEntity(iriEntity.asOWLClass());
    }
    else if (rType == RTypeEnum.SUPER || rType == RTypeEnum.DIRECT_SUPER) {
      // Check to see if the iri is a (direct) superclass of the given rule:
      NodeSet<OWLClass> superClassesFound =
          reasoner.getSuperClasses(ce, rType == RTypeEnum.DIRECT_SUPER);
      return superClassesFound.containsEntity(iriEntity.asOWLClass());
    }
    else if (rType == RTypeEnum.INSTANCE || rType == RTypeEnum.DIRECT_INSTANCE) {
      NodeSet<OWLNamedIndividual> instancesFound = reasoner.getInstances(
          ce, rType == RTypeEnum.DIRECT_INSTANCE);
      return instancesFound.containsEntity(iriEntity.asOWLNamedIndividual());
    }
    else if (rType == RTypeEnum.EQUIV) {
      Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ce);
      return equivClassesFound.contains(iriEntity.asOWLClass());
    }
    else {
      logger.error("Unrecognised/unimplemented rule type: " + rType.getRuleType());
      returnValue = false;
    }

    return returnValue;
  }

  private static void validate_generic_rule(
      String rule,
      RTypeEnum rType,
      String cell,
      List<String> row) throws Exception, IOException {

    logger.debug(String.format(
        "validate_generic_rule(): Called with parameters: " +
        "rule: \"%s\", " +
        "rule type: \"%s\", " +
        "cell: \"%s\", " +
        "row: \"%s\".",
        rule, rType.getRuleType(), cell, row));

    switch (rType) {
      case REQUIRED:
        if (cell.trim().equals("")) {
          writeout("Empty cell in a required column.");
        }
        break;
      case EXCLUDED:
        if (!cell.trim().equals("")) {
          writeout("Non-empty cell (%s) in an excluded column.", cell.trim());
        }
        break;
      default:
        writeout("Generic validation of rule type: \"%s\" is not yet implemented",
                 rType.getRuleType());
        break;
    }
  }
}
