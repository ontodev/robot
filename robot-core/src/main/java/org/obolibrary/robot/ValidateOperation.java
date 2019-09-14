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
 * - Add more and better logging statements and user-friendly error messages
 * - Make sure that rule parsing is as robust as possble (also test that quoting is ok)
 * - Document the code
 * - Make the reasoner choice configurable via the command line (see the way other commands do it)
 * - Write unit test(s)
 * - Eventually extend to Excel
 * - Eventually need to tweak the command line options to be more consistent with the other commands
 *   and work seamlessly with robot's chaining feature.
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

  /** Reverse map from rule types (as Strings) to RTypeEnums, populated at load time */
  private static final Map<String, RTypeEnum> rule_type_to_rtenum_map = new HashMap<>();
  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      rule_type_to_rtenum_map.put(r.getRuleType(), r);
    }
  }

  /** Reverse map from rule types in the QUERY category (as Strings) to RTypeEnums, populated at
      load time */
  private static final Map<String, RTypeEnum> query_type_to_rtenum_map = new HashMap<>();
  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      if (r.getRuleCat() == RCatEnum.QUERY) {
        query_type_to_rtenum_map.put(r.getRuleType(), r);
      }
    }
  }

  /** Enum used with the writelog() method */
  private enum LogLevel {
    DEBUG,
    ERROR,
    INFO,
    TRACE,
    WARN;
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
    // associated lists of rules:
    List<String> header = csvData.remove(0);
    List<String> allRules = csvData.remove(0);
    HashMap<String, Map<String, List<String>>> headerToRuleMap = new HashMap();
    for (int i = 0; i < header.size(); i++) {
      headerToRuleMap.put(header.get(i), parse_rules(allRules.get(i)));
    }

    // Validate the data row by row, and column by column by column within a row. csv_row_index and
    // csv_col_index are class variables that will later be used to provide information to the user
    // about the location of any errors encountered.
    for (csv_row_index = 0; csv_row_index < csvData.size(); csv_row_index++) {
      List<String> row = csvData.get(csv_row_index);
      for (csv_col_index = 0; csv_col_index < header.size(); csv_col_index++) {
        // Get the rules for the current column:
        String colName = header.get(csv_col_index);
        Map<String, List<String>> colRules = headerToRuleMap.get(colName);

        // If there are no rules for this column, then skip this cell (this a "comment" column):
        if (colRules.isEmpty()) continue;

        // Get the contents of the current cell:
        String cell = row.get(csv_col_index).trim();

        // For each of the rules applicable to this column, validate the cell against it:
        for (String ruleType : colRules.keySet()) {
          for (String rule : colRules.get(ruleType)) {
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
    parser = new ManchesterSyntaxTool(ValidateOperation.ontology);
    // Use the given reasonerFactory to initialise the reasoner based on the given ontology:
    reasoner = reasonerFactory.createReasoner(ValidateOperation.ontology);

    // Extract from the ontology two maps from rdfs:labels to IRIs and vice versa:
    ValidateOperation.iri_to_label_map = OntologyHelper.getLabels(ValidateOperation.ontology);
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
  private static void writelog(boolean showCoords, LogLevel logLevel, String format,
                               Object... positionalArgs) {
    String logStr = "";
    if (showCoords) {
      logStr += String.format("At row: %d, column: %d: ", csv_row_index + 1, csv_col_index + 1);
    }
    logStr += String.format(format, positionalArgs);
    switch (logLevel) {
      case DEBUG:
        logger.debug(logStr);
        break;
      case ERROR:
        logger.error(logStr);
        break;
      case INFO:
        logger.info(logStr);
        break;
      case TRACE:
        logger.trace(logStr);
        break;
      case WARN:
        logger.warn(logStr);
        break;
    }
  }

  /**
   * INSERT DOC HERE
   */
  private static void writelog(LogLevel logLevel, String format, Object... positionalArgs) {
    writelog(true, logLevel, format, positionalArgs);
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
        writelog(LogLevel.WARN,
                 "Duplicate rdfs:label \"%s\". Overwriting value \"%s\" with \"%s\"",
                 reverseKey, target.get(reverseKey), reverseValue);
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
    // Skip over empty strings and strings that start with "##". In a rule string, there may
    // be multiple rules separated by semicolons. To comment out any one of them, add a #
    // to the beginning of it. To comment all of them out, add ## to the beginning of the string
    // as a whole. E.g.: "## rule 1; rule 2" comments out everything, while "# rule 1; rule 2"
    // only comments out only rule 1 and "rule 1; # rule 2" comments out only rule 2.
    if (!ruleString.trim().equals("") && !ruleString.trim().startsWith("##")) {
      // Rules are separated by semicolons:
      String[] rules = ruleString.split("\\s*;\\s*");
      for (String rule : rules) {
        // Skip any rules that begin with a '#' (comments):
        if (rule.trim().startsWith("#")) {
          continue;
        }
        // Each rule is of the form: rule-type: rule-content
        String[] ruleParts = rule.split("\\s*:\\s*", 2);
        String ruleType = ruleParts[0].trim();
        String ruleContent = ruleParts[1].trim();

        // Add, to the map, an empty list for the given ruleType if we haven't seen it before:
        if (!ruleMap.containsKey(ruleType)) {
          ruleMap.put(ruleType, new ArrayList<String>());
        }
        // Add the content of the given rule to the list of rules corresponding to its ruleType:
        ruleMap.get(ruleType).add(ruleContent);
      }
    }
    return ruleMap;
  }

  /**
   * INSERT DOC HERE
   */
  private static String[] split_rule_type(String ruleType) {
    // A rule type can be of the form: ruletype1|ruletype2|ruletype3...
    // where the first one is the primary type for lookup purposes:
    return ruleType.split("\\s*\\|\\s*");
  }

  /**
   * INSERT DOC HERE
   */
  private static String get_primary_rule_type(String ruleType) {
    return split_rule_type(ruleType)[0];
  }

  /**
   * INSERT DOC HERE
   */
  private static boolean rule_type_recognised(String ruleType) {
    return rule_type_to_rtenum_map.containsKey(get_primary_rule_type(ruleType));
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
  private static String wildcard_to_label(String wildcard, List<String> row) throws IOException {
    if (!wildcard.startsWith("%")) {
      writelog(LogLevel.ERROR, "Invalid wildcard: \"%s\".", wildcard);
      return null;
    }

    int colIndex = Integer.parseInt(wildcard.substring(1)) - 1;
    if (colIndex >= row.size()) {
      writeout("Rule: \"%s\" indicates a column number that is greater than the row length (%d).",
               wildcard, row.size());
      return null;
    }

    String term = row.get(colIndex).trim();
    if (term == null) {
      writeout("Failed to retrieve label from wildcard: %s. No term at position %d of this row.",
               wildcard, colIndex + 1);
      return null;
    }

    if (term.equals("")) {
      writeout("Failed to retrieve label from wildcard: %s. Term at position %d of row is empty.",
               wildcard, colIndex + 1);
      return null;
    }

    return get_label_from_term(term);
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
      writelog(LogLevel.INFO, "Interpolated: \"%s\" into \"%s\"", str, interpolatedString);
      return interpolatedString;
    }

    // Otherwise look for any substrings starting with a percent-symbol and followed by a number:
    Matcher m = Pattern.compile("%\\d+").matcher(str);
    int currIndex = 0;
    while (m.find()) {
      // Get the label corresponding to the wildcard:
      String label = wildcard_to_label(m.group(), row);
      // If there is a problem finding the label for one of the wildcards, then just send back the
      // string as is:
      if (label == null) {
        writeout("Unable to interpolate \"%s\" in string \"%s\".", m.group(), str);
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
    writelog(LogLevel.INFO, "Interpolated: \"%s\" into \"%s\"", str, interpolatedString);
    return interpolatedString;
  }

  /**
   * INSERT DOC HERE
   // Parses the given rule into a main part and optional when clause.
   */
  private static SimpleEntry<String, List<String[]>> separate_rule(String rule) throws IOException {
    // Check if there are any when clauses:
    Matcher m = Pattern.compile("(\\(\\s*(when:?)\\s+.+\\))(.*)").matcher(rule);
    String whenClauseStr = null;
    if (!m.find()) {
      // If there is no when clause, then just return back the rule string as it was passed with an
      // empty when clause list:
      return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
    }

    // If there is no main clause, inform the user of the problem and return the rule string as it
    // was passed with an empty when clause list:
    if (m.start() == 0) {
      writeout("Rule: \"%s\" has when clause but no main clause.", rule);
      return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
    }

    whenClauseStr = m.group(1);
    // Extract the actual content of the clause. m.group(2) is the "when" (or alternately "when:")
    // that opens the when-clause. We add 2 to the length to account for the leading '(' and the
    // succeeding space at the end of the string, e.g., "(when ", "(when: "
    whenClauseStr = whenClauseStr.substring(m.group(2).length() + 2, whenClauseStr.length() - 1);

    // Don't fail just because there is some extra garbage at the end of the rule, but notify
    // the user about it:
    if (!m.group(3).trim().equals("")) {
      writeout("Ignoring string \"%s\" at end of rule \"%s\".", m.group(3).trim(), rule);
    }

    // Within each when clause, multiple subclauses separated by ampersands are allowed. Each
    // subclass must be of the form: <Entity> <Rule-Type> <Axiom>.
    // <Entity> is in the form of a (not necessaruly interpolated) label: either a contiguous string
    // or a string with whitespace enclosed in single quotes. <Rule-Type> is a possibly hyphenated
    // alphanumeric string. <Axiom> can take any form. Here we resolve each sub-clause of the
    // when statement into a list of such triples.
    ArrayList<String[]> whenClauses = new ArrayList();
    for (String whenClause : whenClauseStr.split("\\s*&\\s*")) {
      m = Pattern.compile(
          "^([^\'\\s]+|\'[^\']+\')\\s+([a-z\\-\\|]+):?\\s+(.*)$")
          .matcher(whenClause);

      if (!m.find()) {
        writeout("Unable to decompose when-clause: \"%s\".", whenClause);
        // Return the rule as passed with an empty when clause list:
        return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
      }
      // Add the triple to the list of when clauses:
      whenClauses.add(new String[] {m.group(1), m.group(2), m.group(3)});
    }

    // Now get the main part of the rule (i.e. the part before the when clause):
    m = Pattern.compile("^(.+)\\s+\\(when:?\\s").matcher(rule);
    if (!m.find()) {
      // This shouldn't really ever happen ...
      writeout("Encountered unknown error while looking for main clause of rule \"%s\".", rule);
      // Return the rule as passed with an empty when clause list:
      return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
    }

    // Finally return the main clause that we just found and its associated list of when clauses:
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
      String ruleType) throws Exception, IOException {

    writelog(
        LogLevel.DEBUG,
        "validate_rule(): Called with parameters: " +
        "cell: \"%s\", " +
        "rule: \"%s\", " +
        "reasoner: \"%s\", " +
        "row: \"%s\", " +
        "rule type: \"%s\".",
        cell, rule, reasoner.getClass().getSimpleName(), row, ruleType);

    if (!rule_type_recognised(ruleType)) {
      writeout("Unrecognised rule type \"%s\".", ruleType);
      return;
    }

    // Separate the given rule into its main clause and optional when clauses:
    SimpleEntry<String, List<String[]>> separatedRule = separate_rule(rule);

    // Evaluate and validate any when clauses for this rule first:
    for (String[] whenClause : separatedRule.getValue()) {
      String subject = interpolate(whenClause[0], row);
      // Get the IRI for the interpolated subject, first removing any surrounding single quotes
      // from the label:
      IRI subjectIri = label_to_iri_map.get(subject.replaceAll("^\'|\'$", ""));
      if (subjectIri == null) {
        writeout("Could not determine IRI for label: \"%s\".", subject);
        continue;
      }

      // Determine the rule type and primary rule type of this when clause. For example, a rule of
      // type "subclass-of|equivalent-to" has a primary rule type of "subclass-of"
      String whenRuleType = whenClause[1];
      if (!rule_type_recognised(whenRuleType)) {
        writeout("Unrecognised rule type \"%s\".", whenRuleType);
        continue;
      }
      RTypeEnum whenPrimRType = rule_type_to_rtenum_map.get(get_primary_rule_type(whenRuleType));

      // Use the primary rule type to make sure the rule is of the right category for a when clause:
      if (whenPrimRType.getRuleCat() != RCatEnum.QUERY) {
        writeout("Only rules of type: %s are allowed in a when clause. Skipping clause: \"%s\".",
                 query_type_to_rtenum_map.keySet(), whenRuleType);
        continue;
      }

      // Interpolate the axiom to validate and send the query to the reasoner:
      String axiom = interpolate(whenClause[2], row);
      if (!execute_query(subjectIri, axiom, reasoner, row, whenRuleType)) {
        // If any of the when clauses fail to be satisfied, then we do not need to evaluate any
        // of the other when clauses, or the main clause, since the main clause may only be
        // evaluated when all of the when clauses are satisfied.
        writelog(
            LogLevel.INFO,
            "When clause: \"%s (%s) %s %s\" is not satisfied. Not running main clause.",
            subject, subjectIri.getShortForm(), whenRuleType, axiom);
        return;
      }
    }

    // Once all of the when clauses have been validated, get the RTypeEnum representation of the
    // primary rule type of this rule:
    RTypeEnum primRType = rule_type_to_rtenum_map.get(get_primary_rule_type(ruleType));

    // If the primary rule type for this rule is not in the QUERY category, process it at this step
    // and return control to the caller. The further steps below are only needed when queries are
    // going to be sent to the reasoner.
    if (primRType.getRuleCat() != RCatEnum.QUERY) {
      validate_generic_rule(rule, primRType, cell, row);
      return;
    }

    // If the cell contents are empty, just return to the caller silently (if the cell is not
    // expected to be empty, this will have been caught by one of the generic rules in the
    // previous step, assuming such a rule exists for the column).
    if (cell.trim().equals("")) return;

    // Get the rdfs:label corresponding to the cell; just exit if it can't be found:
    String cellLabel = get_label_from_term(cell);
    if (cellLabel == null) {
      writeout("Could not find \"%s\" in ontology.", cell);
      return;
    }

    // Get the cell's IRI, interpolate the axiom, and execute the query:
    IRI cellIri = label_to_iri_map.get(cellLabel);
    String axiom = interpolate(separatedRule.getKey(), row);
    boolean result = execute_query(cellIri, axiom, reasoner, row, ruleType);
    if (!result) {
      writeout("Rule: \"%s (%s) %s %s\" is not satisfied.",
               cellLabel, cellIri.getShortForm(), ruleType, axiom);
    }
    else {
      writelog(
          LogLevel.INFO,
          "Rule: \"%s (%s) %s %s\" is satisfied",
          cellLabel, cellIri.getShortForm(), ruleType, axiom);
    }
  }

  private static boolean execute_query(
      IRI iri,
      String rule,
      OWLReasoner reasoner,
      List<String> row,
      String unsplitQueryType) throws Exception, IOException {

    writelog(
        LogLevel.DEBUG,
        "execute_query(): Called with parameters: " +
        "iri: \"%s\", " +
        "rule: \"%s\", " +
        "reasoner: \"%s\", " +
        "row: \"%s\", " +
        "query type: \"%s\".",
        iri.getShortForm(), rule, reasoner.getClass().getSimpleName(), row, unsplitQueryType);

    OWLClassExpression ce;
    try {
      ce = parser.parseManchesterExpression(rule);
    }
    catch (ParserException e) {
      writeout("Unable to parse rule \"%s: %s\".\n\t%s.",
               unsplitQueryType, rule, e.getMessage().trim());
      return false;
    }

    OWLEntity iriEntity = OntologyHelper.getEntity(ontology, iri);
    String label = iri_to_label_map.get(iri);

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query types is unrecognised, inform the user but ignore it.
    String[] queryTypes = split_rule_type(unsplitQueryType);
    for (String queryType : queryTypes) {
      if (!rule_type_recognised(queryType)) {
        writeout("Query type \"%s\" not recognised in rule \"%s\".", queryType, unsplitQueryType);
        continue;
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB || qType == RTypeEnum.DIRECT_SUB) {
        // Check to see if the iri is a (direct) subclass of the given rule:
        NodeSet<OWLClass> subClassesFound =
            reasoner.getSubClasses(ce, qType == RTypeEnum.DIRECT_SUB);
        if (subClassesFound.containsEntity(iriEntity.asOWLClass())) {
          return true;
        }
      }
      else if (qType == RTypeEnum.SUPER || qType == RTypeEnum.DIRECT_SUPER) {
        // Check to see if the iri is a (direct) superclass of the given rule:
        NodeSet<OWLClass> superClassesFound =
            reasoner.getSuperClasses(ce, qType == RTypeEnum.DIRECT_SUPER);
        if (superClassesFound.containsEntity(iriEntity.asOWLClass())) {
          return true;
        }
      }
      else if (qType == RTypeEnum.INSTANCE || qType == RTypeEnum.DIRECT_INSTANCE) {
        NodeSet<OWLNamedIndividual> instancesFound = reasoner.getInstances(
            ce, qType == RTypeEnum.DIRECT_INSTANCE);
        if (instancesFound.containsEntity(iriEntity.asOWLNamedIndividual())) {
          return true;
        }
      }
      else if (qType == RTypeEnum.EQUIV) {
        Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ce);
        if (equivClassesFound.contains(iriEntity.asOWLClass())) {
          return true;
        }
      }
      else {
        writelog(LogLevel.ERROR, "Validation for query type: \"%s\" not yet implemented.", qType);
        return false;
      }
    }
    return false;
  }

  private static void validate_generic_rule(
      String rule,
      RTypeEnum rType,
      String cell,
      List<String> row) throws Exception, IOException {

    writelog(
        LogLevel.DEBUG,
        "validate_generic_rule(): Called with parameters: " +
        "rule: \"%s\", " +
        "rule type: \"%s\", " +
        "cell: \"%s\", " +
        "row: \"%s\".",
        rule, rType.getRuleType(), cell, row);

    switch (rType) {
      case REQUIRED:
        if (cell.trim().equals("")) {
          writeout("Cell is empty but rule: \"%s: %s\" does not allow this.",
                   rType.getRuleType(), rule);
        }
        break;
      case EXCLUDED:
        if (!cell.trim().equals("")) {
          writeout("Cell is non-empty (\"%s\") but rule: \"%s: %s\" does not allow this.",
                   cell, rType.getRuleType(), rule);
        }
        break;
      default:
        writeout("Generic validation of rule type: \"%s\" is not yet implemented.",
                 rType.getRuleType());
        break;
    }
  }
}
