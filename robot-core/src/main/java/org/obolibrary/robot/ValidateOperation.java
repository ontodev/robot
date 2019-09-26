package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO:
 * - Take a look at the deprecation warnings for ValidateCommand
 * - Implement the rules required for the new csv that James sent
 *   - Hard code pipe-separation for now in cells.
 * - Follow logging conventions in:
 *     https://github.com/ontodev/robot/blob/master/CONTRIBUTING.md#documenting-errors
 * - Allow TSVs as well as CSVs to be passed.
 * - Make the reasoner choice configurable via the command line (see the way other commands do it)
 * - Eventually extend to Excel
 * - Eventually need to tweak the command line options to be more consistent with the other commands
 *   and work seamlessly with robot's chaining feature.
 */


/**
 * Implements the validate operation for a given CSV file and ontology.
 *
 * Validation rules are read from the second row of the CSV file as follows:
 *
 * header A                        header B                        header C ...
 * rule A1; rule A2; rule A3 ...   rule B1; rule B2; rule B3 ...   rule C1; rule C2; rule C3 ...
 * data                            data                            data
 * data                            data                            data
 * ...
 *
 * Rules for a given column must be separated by semicolons. To comment out all of the rules for
 * a given column, the list should be prefixed by '##'. To comment out individual rules from
 * the rules belonging to a given column, prefix the rule with '#'.
 * E.g.:
 *   "## rule 1; rule 2" comments out rule 1 and rule 2.
 *   "# rule 1; rule 2" comments out rule 1 but not rule 2.
 *   "rule 1; # rule 2" comments out only rule 2.
 *
 * Individual rules must be of the form:
 * <main-rule-type> <rule> [(when <when-class-expr-1> <when-rule-type-1> <when-rule-1> & ...)]
 *
 * <main-rule-type> can be one of:
 *   is-required
 *   is-excluded
 *   subclass-of
 *   direct-subclass-of
 *   superclass-of
 *   direct-superclass-of
 *   equivalent-to
 *   instance-of
 *   direct-instance-of
 *
 * <when-rule-type> can be one of:
 *   subclass-of
 *   direct-subclass-of
 *   superclass-of
 *   direct-superclass-of
 *   equivalent-to
 *   instance-of
 *   direct-instance-of
 *
 * <rule> and <when-rule>
 *   For rule types: is-required and is-excluded, <rule> is optional and if not specified defaults
 *     to true.
 *   For other rules types, <rule> is mandatory and must be in the form of a description logic (DL)
 *     class expression query, in Manchester syntax.
 *
 * <when-class-expr> must describe an individual class, and can be in the form of an rdfs:label, an
 *   IRI, an abbreviated IRI, or a wildcard.
 *
 * Wildcards:
 * ---------
 * Wildcards of the form "%n" can be specified with <rule>, <when-rule>, and <when-class-expr>
 * clauses, and are used to indicate the class described by the  of the nth cell of a given
 * row. E.g. the rule:
 *   subclass-of hasBasisIn in some %2 (when %1 subclass-of ('Dengue virus' or 'Dengue virus 2'))
 *
 * requires that, whenever the class indicated in column %1 of the current row is a subclass of the
 * class consisting of 'Dengue virus' and 'Dengue virus 2', the data in the current cell must be a
 * subclass of the set of classes that bear the relation hasBasisIn to the class indicated in column
 * 2 of the same row.
 *
 * When-clauses:
 * ------------
 * The optional when-clause indicates that the rule given in the main clause should be validated
 * only when the when-clause is satisfied. If multiple when-clauses are specified (separated by
 * '&', then each when-clause must evaluate to true in order for the main validation rule to
 * execute. E.g.:
 *   direct-subclass-of %2 (when %5 superclass-of 'exposure process' & %2 superclass-of vaccine)
 *
 * indicates that the validation rule 'direct-subclass-of %2' should only be run against the
 * current cell when both the cell in column 5 is a superclass of 'exposure process' and the cell in
 * column 2 is a superclass of vaccine.
 *
 * Compound rule-types:
 * -------------------
 * <rule-type> and <when-rule-type> can take the form: rule-type-1|rule-type-2|rule-type-3|...
 * E.g.
 *   subclass-of: %3 (when %4 subclass-of|equivalent-to %2)
 *
 * requires that, whenever the contents of the cell in column 4 of the given row are either a
 * subclass-of or equivalent-to the contents of the cell in column 2, then the contents of the
 * current cell must be a subclass-of the contents of the cell in column 3.
 *
 * Literals:
 * --------
 * Literal rdfs:label expressions must be enclosed in single quotes if they contain spaces.
 * E.g.
 *   subclass-of 'Dengue virus'      (single quotes required here)
 *   hasBasisIn some vaccination     (no single quotes required here)
 *
 * is-required and is-excluded:
 * ---------------------------
 * is-required indicates that the given cell must be non-empty, while is-excluded requires that
 * it be empty. These rules can be used with an optional when-clause. E.g.:
 *   is-required (when %1 subclass-of 'exposure process)
 *
 * indicates that the current cell must be non-empty whenever the cell in column 1 of the current
 * row is a subclass of 'exposure process'.
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /** Output writer */
  private static Writer writer;

  /** The ontology to use for validation */
  private static OWLOntology ontology;

  /** The reasoner to use for validation */
  private static OWLReasoner reasoner;

  /** The parser to use for evaluating class expressions */
  private static ManchesterOWLSyntaxClassExpressionParser parser;

  /** A map from rdfs:labels to IRIs */
  private static Map<String, IRI> label_to_iri_map;

  /** A map from IRIs to rdfs:labels */
  private static Map<IRI, String> iri_to_label_map;

  /** The row number of the CSV data currently being processed */
  private static int csv_row_index;

  /** The column number of the CSV data currently being processed */
  private static int csv_col_index;

  /**
   * An enum representation of the different categories of rules. We distinguish between queries,
   * which involve queries to a reasoner, and presence rules, which check for the existence of
   * content in a cell.
   */
  private enum RCatEnum {
    QUERY,
    PRESENCE
  }

  /**
   * An enum representation of the different types of rules. Each rule type belongs to larger
   * category, and is identified within the CSV file by a particular string.
   */
  private enum RTypeEnum {
    DIRECT_SUPER("direct-superclass-of", RCatEnum.QUERY),
    SUPER("superclass-of", RCatEnum.QUERY),
    EQUIV("equivalent-to", RCatEnum.QUERY),
    DIRECT_SUB("direct-subclass-of", RCatEnum.QUERY),
    SUB("subclass-of", RCatEnum.QUERY),
    DIRECT_INSTANCE("direct-instance-of", RCatEnum.QUERY),
    INSTANCE("instance-of", RCatEnum.QUERY),
    REQUIRED("is-required", RCatEnum.PRESENCE),
    EXCLUDED("is-excluded", RCatEnum.PRESENCE);

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

  /**
   * Reverse map from rule types in the QUERY category (as Strings) to RTypeEnums, populated at load
   * time
   */
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
    WARN;
  }

  /**
   * Given the string `format`, a logging level specification, and a number of formatting variables,
   * use the formating variables to fill in the format string in the manner of C's printf function,
   * and write to the log at the appropriate log level. If the parameter `showCoords` is true, then
   * include the current row and column number in the output string.
   */
  private static void writelog(
      boolean showCoords, LogLevel logLevel, String format, Object... positionalArgs) {
    String logStr = "";
    if (showCoords) {
      logStr += String.format("At row: %d, column: %d: ", csv_row_index + 1, csv_col_index + 1);
    }

    logStr += String.format(format, positionalArgs);
    switch (logLevel) {
      case ERROR:
        logger.error(logStr);
        break;
      case WARN:
        logger.warn(logStr);
        break;
      case INFO:
        logger.info(logStr);
        break;
      case DEBUG:
        logger.debug(logStr);
        break;
    }
  }

  /**
   * Given the string `format`, a logging level specification, and a number of formatting variables,
   * use the formating variables to fill in the format string in the manner of C's printf function,
   * and write to the log at the appropriate log level, including the current row and column number
   * in the output string.
   */
  private static void writelog(LogLevel logLevel, String format, Object... positionalArgs) {
    writelog(true, logLevel, format, positionalArgs);
  }

  /**
   * Given the string `format` and a number of formatting variables, use the formatting variables to
   * fill in the format string in the manner of C's printf function, and write the string to the
   * Writer object that belongs to ValidateOperation. If the parameter `showCoords` is true, then
   * include the current row and column number in the output string.
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
   * Given the string `format` and a number of formatting variables, use the formatting variables to
   * fill in the format string in the manner of C's printf function, and write the string to the
   * Writer object that belongs to ValidateOperation, including the current row and column number in
   * the output string.
   */
  private static void writeout(String format, Object... positionalArgs) throws IOException {
    writeout(true, format, positionalArgs);
  }

  /**
   * Given an ontology, a reasoner factory, and an output writer, initialise the static variables
   * belonging to ValidateOperation: The shared ontology, output writer, manchester syntax class
   * expression parser, and the teo maps from the ontology's IRIs to rdfs:labels and vice versa.
   */
  private static void initialize(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, Writer writer) throws IOException {

    ValidateOperation.ontology = ontology;
    ValidateOperation.writer = writer;

    // Robot's custom quoted entity checker will be used for parsing class expressions:
    QuotedEntityChecker checker = new QuotedEntityChecker();
    // Add the class that will be used for IO and for handling short-form IRIs by the quoted entity
    // checker:
    checker.setIOHelper(new IOHelper());
    checker.addProvider(new SimpleShortFormProvider());

    // Initialise an OWLDataFactory and add rdfs:label to the list of annotation properties which
    // will be looked up in the ontology by the quoted entity checker when finding names.
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ValidateOperation.ontology);

    // Create the parser using the data factory and entity checker.
    ValidateOperation.parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);

    // Use the given reasonerFactory to initialise the reasoner based on the given ontology:
    reasoner = reasonerFactory.createReasoner(ValidateOperation.ontology);

    // Extract from the ontology two maps from rdfs:labels to IRIs and vice versa:
    ValidateOperation.iri_to_label_map = OntologyHelper.getLabels(ValidateOperation.ontology);
    ValidateOperation.label_to_iri_map = reverse_iri_label_map(ValidateOperation.iri_to_label_map);
  }

  /** Deallocate any static variables that need to be deallocated. */
  private static void tearDown() {
    reasoner.dispose();
  }

  /** Given a map from IRIs to strings, return its inverse. */
  private static Map<String, IRI> reverse_iri_label_map(Map<IRI, String> source) {
    HashMap<String, IRI> target = new HashMap();
    for (Map.Entry<IRI, String> entry : source.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (target.containsKey(reverseKey)) {
        writelog(
            LogLevel.WARN,
            "Duplicate rdfs:label \"%s\". Overwriting value \"%s\" with \"%s\"",
            reverseKey,
            target.get(reverseKey),
            reverseValue);
      }
      target.put(reverseKey, reverseValue);
    }
    return target;
  }

  /**
   * Given a string specifying a list of rules of various types, return a map which contains, for
   * each rule type present in the string, the list of rules of that type that have been specified.
   */
  private static Map<String, List<String>> parse_rules(String ruleString) {
    HashMap<String, List<String>> ruleMap = new HashMap();
    // Skip over empty strings and strings that start with "##".
    if (!ruleString.trim().equals("") && !ruleString.trim().startsWith("##")) {
      // Rules are separated by semicolons:
      String[] rules = ruleString.split("\\s*;\\s*");
      for (String rule : rules) {
        // Skip any rules that begin with a '#' (comments):
        if (rule.trim().startsWith("#")) {
          continue;
        }
        // Each rule is of the form: <rule-type> <rule-content> but for the PRESENCE category, if
        // <rule-content> is left out it is implicitly understood to be "true"
        String[] ruleParts = rule.trim().split("\\s+", 2);
        String ruleType = ruleParts[0].trim();
        String ruleContent = null;
        if (ruleParts.length == 2) {
          ruleContent = ruleParts[1].trim();
        } else {
          RTypeEnum rTypeEnum = rule_type_to_rtenum_map.get(ruleType);
          if (rTypeEnum != null && rTypeEnum.getRuleCat() == RCatEnum.PRESENCE) {
            ruleContent = "true";
          } else {
            writelog(LogLevel.ERROR, "Invalid rule: %s", rule.trim());
            continue;
          }
        }

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
   * Given a string describing a compound rule type, return an array in which each component of the
   * compound rule occupies a position in the array.
   */
  private static String[] split_rule_type(String ruleType) {
    // A rule type can be of the form: ruletype1|ruletype2|ruletype3...
    // where the first one is the primary type for lookup purposes:
    return ruleType.split("\\s*\\|\\s*");
  }

  /**
   * Given a string describing a compound rule type, return the primary rule type of the compound
   * rule type.
   */
  private static String get_primary_rule_type(String ruleType) {
    return split_rule_type(ruleType)[0];
  }

  /**
   * Given a string describing a rule type, return a boolean indicating whether it is one of the
   * rules recognised by ValidateOperation.
   */
  private static boolean rule_type_recognised(String ruleType) {
    return rule_type_to_rtenum_map.containsKey(get_primary_rule_type(ruleType));
  }

  /**
   * Given a string describing one of the classes in the ontology, in either the form of an IRI, an
   * abbreviated IRI, or an rdfs:label, return the rdfs:label for that class.
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
   * Given a string in the form of a wildcard, and a list of strings representing a row of the CSV,
   * return the rdfs:label contained in the position of the row indicated by the wildcard.
   */
  private static String wildcard_to_label(String wildcard, List<String> row) {
    if (!wildcard.startsWith("%")) {
      writelog(LogLevel.ERROR, "Invalid wildcard: \"%s\".", wildcard);
      return null;
    }

    int colIndex = Integer.parseInt(wildcard.substring(1)) - 1;
    if (colIndex >= row.size()) {
      writelog(
          LogLevel.ERROR,
          "Rule: \"%s\" indicates a column number that is greater than the row length (%d).",
          wildcard,
          row.size());
      return null;
    }

    String term = row.get(colIndex).trim();
    if (term == null) {
      writelog(
          LogLevel.WARN,
          "Failed to retrieve label from wildcard: %s. No term at position %d of this row.",
          wildcard,
          colIndex + 1);
      return null;
    }

    if (term.equals("")) {
      writelog(
          LogLevel.WARN,
          "Failed to retrieve label from wildcard: %s. Term at position %d of row is empty.",
          wildcard,
          colIndex + 1);
      return null;
    }

    return get_label_from_term(term);
  }

  /**
   * Given a string, possibly containing wildcards, and a list of strings representing a row of the
   * CSV, return a string in which all of the wildcards in the input string have been replaced by
   * the rdfs:labels corresponding to the content in the positions of the row that they indicate.
   */
  private static String interpolate(String str, List<String> row) {
    String interpolatedString = "";

    // Look for any substrings starting with a percent-symbol and followed by a number:
    Matcher m = Pattern.compile("%\\d+").matcher(str);
    int currIndex = 0;
    while (m.find()) {
      // Get the label corresponding to the wildcard:
      String label = wildcard_to_label(m.group(), row);
      // If there is a problem finding the label for one of the wildcards, then return null:
      if (label == null) {
        return null;
      }

      // Iteratively build the interpolated string up to the current label, which we enclose in
      // single quotes:
      interpolatedString =
          interpolatedString + str.substring(currIndex, m.start()) + "'" + label + "'";
      currIndex = m.end();
    }
    // There may be text after the final wildcard, so add it now:
    interpolatedString += str.substring(currIndex);
    return interpolatedString;
  }

  /**
   * Given a string describing the content of a rule and a string describing its rule type, return a
   * simple map entry such that the `key` for the entry is the main clause of the rule, and the
   * `value` for the entry is a list of the rule's when-clauses. Each when-clause is itself stored
   * as an array of three strings, including the class to which the when-clause is to be applied,
   * the rule type for the when clause, and the actual axiom to be validated against the class.
   */
  private static SimpleEntry<String, List<String[]>> separate_rule(String rule, String ruleType) {
    // Check if there are any when clauses:
    Matcher m = Pattern.compile("(\\(\\s*when\\s+.+\\))(.*)").matcher(rule);
    String whenClauseStr = null;
    if (!m.find()) {
      // If there is no when clause, then just return back the rule string as it was passed with an
      // empty when clause list:
      writelog(LogLevel.INFO, "No when-clauses found in rule: \"%s\".", rule);
      return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
    }

    // If there is no main clause and this is not a PRESENCE rule, inform the user of the problem
    // and return the rule string as it was passed with an empty when clause list:
    if (m.start() == 0 && rule_type_to_rtenum_map.get(ruleType).getRuleCat() != RCatEnum.PRESENCE) {
      writelog(LogLevel.ERROR, "Rule: \"%s\" has when clause but no main clause.", rule);
      return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
    }

    whenClauseStr = m.group(1);
    // Extract the actual content of the when-clause.
    whenClauseStr = whenClauseStr.substring("(when ".length(), whenClauseStr.length() - 1);

    // Don't fail just because there is some extra garbage at the end of the rule, but notify
    // the user about it:
    if (!m.group(2).trim().equals("")) {
      writelog(
          LogLevel.WARN, "Ignoring string \"%s\" at end of rule \"%s\".", m.group(2).trim(), rule);
    }

    // Within each when clause, multiple subclauses separated by ampersands are allowed. Each
    // subclass must be of the form: <Entity> <Rule-Type> <Axiom>.
    // <Entity> is in the form of a (not necessaruly interpolated) label: either a contiguous string
    // or a string with whitespace enclosed in single quotes. <Rule-Type> is a possibly hyphenated
    // alphanumeric string. <Axiom> can take any form. Here we resolve each sub-clause of the
    // when statement into a list of such triples.
    ArrayList<String[]> whenClauses = new ArrayList();
    for (String whenClause : whenClauseStr.split("\\s*&\\s*")) {
      m = Pattern.compile("^([^\'\\s]+|\'[^\']+\')\\s+([a-z\\-\\|]+)\\s+(.*)$").matcher(whenClause);

      if (!m.find()) {
        writelog(LogLevel.ERROR, "Unable to decompose when-clause: \"%s\".", whenClause);
        // Return the rule as passed with an empty when clause list:
        return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
      }
      // Add the triple to the list of when clauses:
      whenClauses.add(new String[] {m.group(1), m.group(2), m.group(3)});
    }

    // Now get the main part of the rule (i.e. the part before the when clause):
    m = Pattern.compile("^(.+)\\s+\\(when\\s").matcher(rule);
    if (m.find()) {
      return new SimpleEntry<String, List<String[]>>(m.group(1), whenClauses);
    }

    // If no main clause is found, then if this is a PRESENCE rule, implicitly assume that the main
    // clause is "true":
    if (rule_type_to_rtenum_map.get(ruleType).getRuleCat() == RCatEnum.PRESENCE) {
      return new SimpleEntry<String, List<String[]>>("true", whenClauses);
    }

    // We should never get here since we have already checked for an empty main clause earlier ...
    writelog(
        LogLevel.ERROR,
        "Encountered unknown error while looking for main clause of rule \"%s\".",
        rule);
    // Return the rule as passed with an empty when clause list:
    return new SimpleEntry<String, List<String[]>>(rule, new ArrayList<String[]>());
  }

  /**
   * Given an IRI, a string describing a rule to query, a list of strings describing a row from the
   * CSV, and a string representing a compound query type: Determine whether, for any of the query
   * types specified in the compound string, the IRI is in the result set returned by a query of
   * that type on the given rule. Return true if it is in one of these result sets, and false if it
   * is not.
   */
  private static boolean execute_query(
      IRI iri, String rule, List<String> row, String unsplitQueryType) throws Exception {

    writelog(
        LogLevel.DEBUG,
        "execute_query(): Called with parameters: "
            + "iri: \"%s\", "
            + "rule: \"%s\", "
            + "row: \"%s\", "
            + "query type: \"%s\".",
        iri.getShortForm(),
        rule,
        row,
        unsplitQueryType);

    OWLClassExpression ce;
    try {
      ce = parser.parse(rule);
    } catch (OWLParserException e) {
      writelog(
          LogLevel.ERROR,
          "Unable to parse rule \"%s %s\".\n\t%s.",
          unsplitQueryType,
          rule,
          e.getMessage().trim());
      return false;
    }

    OWLEntity iriEntity = OntologyHelper.getEntity(ontology, iri);

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query types is unrecognised, inform the user but continue on.
    String[] queryTypes = split_rule_type(unsplitQueryType);
    for (String queryType : queryTypes) {
      if (!rule_type_recognised(queryType)) {
        writelog(
            LogLevel.ERROR,
            "Query type \"%s\" not recognised in rule \"%s\".",
            queryType,
            unsplitQueryType);
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
      } else if (qType == RTypeEnum.SUPER || qType == RTypeEnum.DIRECT_SUPER) {
        // Check to see if the iri is a (direct) superclass of the given rule:
        NodeSet<OWLClass> superClassesFound =
            reasoner.getSuperClasses(ce, qType == RTypeEnum.DIRECT_SUPER);
        if (superClassesFound.containsEntity(iriEntity.asOWLClass())) {
          return true;
        }
      } else if (qType == RTypeEnum.INSTANCE || qType == RTypeEnum.DIRECT_INSTANCE) {
        NodeSet<OWLNamedIndividual> instancesFound =
            reasoner.getInstances(ce, qType == RTypeEnum.DIRECT_INSTANCE);
        if (instancesFound.containsEntity(iriEntity.asOWLNamedIndividual())) {
          return true;
        }
      } else if (qType == RTypeEnum.EQUIV) {
        Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ce);
        if (equivClassesFound.contains(iriEntity.asOWLClass())) {
          return true;
        }
      } else {
        writelog(LogLevel.ERROR, "Validation for query type: \"%s\" not yet implemented.", qType);
        return false;
      }
    }
    return false;
  }

  /**
   * Given a string describing a rule, a rule type of the PRESENCE type, and a string representing a
   * cell from the CSV, determine whether the cell satisfies the given presence rule: E.g. Return
   * false if the rule requires content to be present in the cell but the cell is empty, or if the
   * rule requires the cell to be empty but there is content in it.
   */
  private static void validate_presence_rule(String rule, RTypeEnum rType, String cell)
      throws IOException {

    writelog(
        LogLevel.DEBUG,
        "validate_presence_rule(): Called with parameters: "
            + "rule: \"%s\", "
            + "rule type: \"%s\", "
            + "cell: \"%s\".",
        rule,
        rType.getRuleType(),
        cell);

    // Presence-type rules (is-required, is-excluded) must be in the form of a truth value:
    if ((Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1)
        && (Arrays.asList("false", "f", "0", "no", "n").indexOf(rule.toLowerCase()) == -1)) {
      writelog(
          LogLevel.ERROR,
          "Invalid rule: \"%s\" for rule type: %s. Must be one of: "
              + "true, t, 1, yes, y, false, f, 0, no, n",
          rule,
          rType.getRuleType());
      return;
    }

    // If the restriction isn't "true" then there is nothing to do. Just return:
    if (Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1) {
      writelog(LogLevel.INFO, "Nothing to validate for rule: \"%s %s\"", rType.getRuleType(), rule);
      return;
    }

    switch (rType) {
      case REQUIRED:
        if (cell.trim().equals("")) {
          writeout(
              "Cell is empty but rule: \"%s %s\" does not allow this.", rType.getRuleType(), rule);
        }
        break;
      case EXCLUDED:
        if (!cell.trim().equals("")) {
          writeout(
              "Cell is non-empty (\"%s\") but rule: \"%s %s\" does not allow this.",
              cell, rType.getRuleType(), rule);
        }
        break;
      default:
        writelog(
            LogLevel.ERROR,
            "Presence validation of rule type: \"%s\" is not yet implemented.",
            rType.getRuleType());
        break;
    }
  }

  /**
   * Given a string describing a cell from the CSV, a string describing a rule to be applied against
   * that cell, a string describing the type of that rule, and a list of strings describing the row
   * containing the given cell, validate the cell, indicating any validation errors via the output
   * writer.
   */
  private static void validate_rule(String cell, String rule, List<String> row, String ruleType)
      throws Exception, IOException {

    writelog(
        LogLevel.DEBUG,
        "validate_rule(): Called with parameters: "
            + "cell: \"%s\", "
            + "rule: \"%s\", "
            + "row: \"%s\", "
            + "rule type: \"%s\".",
        cell,
        rule,
        row,
        ruleType);

    writelog(LogLevel.INFO, "Validating rule \"%s %s\" against \"%s\".", ruleType, rule, cell);
    if (!rule_type_recognised(ruleType)) {
      writelog(LogLevel.ERROR, "Unrecognised rule type \"%s\".", ruleType);
      return;
    }

    // Separate the given rule into its main clause and optional when clauses:
    SimpleEntry<String, List<String[]>> separatedRule = separate_rule(rule, ruleType);

    // Evaluate and validate any when clauses for this rule first:
    for (String[] whenClause : separatedRule.getValue()) {
      String subject = interpolate(whenClause[0], row);
      if (subject == null) {
        writelog(LogLevel.WARN, "Unable to interpolate \"%s\" in when clause.", whenClause[0]);
        continue;
      }
      writelog(LogLevel.INFO, "Interpolated: \"%s\" into \"%s\"", whenClause[0], subject);

      // Get the IRI for the interpolated subject, first removing any surrounding single quotes
      // from the label:
      IRI subjectIri = label_to_iri_map.get(subject.replaceAll("^\'|\'$", ""));
      if (subjectIri == null) {
        writelog(LogLevel.INFO, "Could not determine IRI for label: \"%s\".", subject);
        continue;
      }

      // Make sure all of the rule types in the when clause are of the right category:
      String whenRuleType = whenClause[1];
      for (String whenRuleSubType : split_rule_type(whenRuleType)) {
        RTypeEnum whenSubRType = rule_type_to_rtenum_map.get(whenRuleSubType);
        if (whenSubRType == null || whenSubRType.getRuleCat() != RCatEnum.QUERY) {
          writelog(
              LogLevel.ERROR,
              "In clause: \"%s\": Only rules of type: %s are allowed in a when clause.",
              whenRuleType,
              query_type_to_rtenum_map.keySet());
          return;
        }
      }

      // Interpolate the axiom to validate and send the query to the reasoner:
      String axiom = whenClause[2];
      String interpolatedAxiom = interpolate(axiom, row);
      if (interpolatedAxiom == null) {
        writelog(LogLevel.WARN, "Unable to interpolate \"%s\" in when clause.", axiom);
        continue;
      }
      writelog(LogLevel.INFO, "Interpolated: \"%s\" into \"%s\"", axiom, interpolatedAxiom);

      if (!execute_query(subjectIri, interpolatedAxiom, row, whenRuleType)) {
        // If any of the when clauses fail to be satisfied, then we do not need to evaluate any
        // of the other when clauses, or the main clause, since the main clause may only be
        // evaluated when all of the when clauses are satisfied.
        writelog(
            LogLevel.INFO,
            "When clause: \"%s (%s) %s %s\" is not satisfied. Not running main clause.",
            subject,
            subjectIri.getShortForm(),
            whenRuleType,
            axiom);
        return;
      } else {
        writelog(
            LogLevel.INFO,
            "Validated when clause \"%s (%s) %s %s\"",
            subject,
            subjectIri.getShortForm(),
            whenRuleType,
            axiom);
      }
    }

    // Once all of the when clauses have been validated, get the RTypeEnum representation of the
    // primary rule type of this rule:
    RTypeEnum primRType = rule_type_to_rtenum_map.get(get_primary_rule_type(ruleType));

    // If the primary rule type for this rule is not in the QUERY category, process it at this step
    // and return control to the caller. The further steps below are only needed when queries are
    // going to be sent to the reasoner.
    if (primRType.getRuleCat() != RCatEnum.QUERY) {
      validate_presence_rule(separatedRule.getKey(), primRType, cell);
      return;
    }

    // If the cell contents are empty, just return to the caller silently (if the cell is not
    // expected to be empty, this will have been caught by one of the presence rules in the
    // previous step, assuming such a rule exists for the column).
    if (cell.trim().equals("")) return;

    // Get the rdfs:label corresponding to the cell; just exit if it can't be found:
    String cellLabel = get_label_from_term(cell);
    if (cellLabel == null) {
      writelog(LogLevel.ERROR, "Could not find \"%s\" in ontology.", cell);
      return;
    }

    // Get the cell's IRI, interpolate the axiom, and execute the query:
    IRI cellIri = label_to_iri_map.get(cellLabel);
    String axiom = separatedRule.getKey();
    String interpolatedAxiom = interpolate(axiom, row);
    if (interpolatedAxiom == null) {
      writelog(LogLevel.ERROR, "Unable to interpolate \"%s\" in rule \"%s\".", axiom, rule);
      return;
    }
    writelog(LogLevel.INFO, "Interpolated: \"%s\" into \"%s\"", axiom, interpolatedAxiom);

    boolean result = execute_query(cellIri, interpolatedAxiom, row, ruleType);
    if (!result) {
      writeout(
          "Validation failed for rule: \"%s (%s) %s %s\".",
          cellLabel, cellIri.getShortForm(), ruleType, axiom);
    } else {
      writelog(
          LogLevel.INFO,
          "Validated: \"%s (%s) %s %s\".",
          cellLabel,
          cellIri.getShortForm(),
          ruleType,
          axiom);
    }
  }

  /**
   * Given a list of lists of strings representing the rows of a CSV, an ontology, a reasoner
   * factory, and an output writer: Extract the rules to use for validation from the CSV, create a
   * reasoner from the given reasoner factory, and then validate the CSV using those extracted
   * rules, row by row and column by column within each row, using the reasoner when required to
   * perform lookups to the ontology, indicating any validation errors via the output writer.
   */
  public static void validate(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer)
      throws Exception {

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

        // If there are no rules for this column, then skip this cell (the cell is part of a
        // "comment" column):
        if (colRules.isEmpty()) continue;

        // Get the contents of the current cell:
        String cell = row.get(csv_col_index).trim();

        // For each of the rules applicable to this column, validate the cell against it:
        for (String ruleType : colRules.keySet()) {
          for (String rule : colRules.get(ruleType)) {
            validate_rule(cell, rule, row, ruleType);
          }
        }
      }
    }
    tearDown();
  }
}
