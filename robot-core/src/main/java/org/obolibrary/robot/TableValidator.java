package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.obolibrary.robot.export.*;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.obolibrary.robot.providers.QuotedAnnotationValueShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableValidator {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "validate#";

  /** Error message for a rule that couldn't be parsed */
  private static final String malformedRuleError = NS + "MALFORMED RULE ERROR malformed rule: %s";

  /**
   * Error message for an invalid presence rule. Presence rules must be in the form of a truth
   * value.
   */
  private static final String invalidPresenceRuleError =
      NS
          + "INVALID PRESENCE RULE ERROR in column %d: invalid rule: \"%s\" for rule type: %s. Must be "
          + "one of: true, t, 1, yes, y, false, f, 0, no, n";

  /** Error message for invalid output format. */
  private static final String invalidFormatError =
      NS + "INVALID FORMAT ERROR '%s' must be one of: html, xlsx, or txt";

  /**
   * Error reported when a wildcard in a rule specifies a column greater than the number of columns
   * in the table.
   */
  private static final String columnOutOfRangeError =
      NS
          + "COLUMN OUT OF RANGE ERROR in column %d: rule \"%s\" indicates a column number that is "
          + "greater than the row length (%d).";

  /** Error reported when a when-clause does not have a corresponding main clause */
  private static final String noMainError =
      NS + "NO MAIN ERROR in column %d: rule: \"%s\" has when clause but no main clause.";

  /** Error reported when a when-clause can't be parsed */
  private static final String malformedWhenClauseError =
      NS + "MALFORMED WHEN CLAUSE ERROR in column %d: unable to decompose when-clause: \"%s\".";

  /** Error reported when a when-clause is of an invalid or inappropriate type */
  private static final String invalidWhenTypeError =
      NS
          + "INVALID WHEN TYPE ERROR in column %d: in clause: \"%s\": Only rules of type: %s are "
          + "allowed in a when clause.";

  /** Error reported when a query type is unrecognized */
  private static final String unrecognizedQueryTypeError =
      NS
          + "UNRECOGNIZED QUERY TYPE ERROR in column %d: query type \"%s\" not recognized in rule "
          + "\"%s\".";

  /** Error reported when a rule type is not recognized */
  private static final String unrecognizedRuleTypeError =
      NS + "UNRECOGNIZED RULE TYPE ERROR in column %d: unrecognized rule type \"%s\".";

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

  private OWLOntology ontology;
  private String outFormat = null;
  private String outDir;

  /** The parser to use when validating class expressions */
  private ManchesterOWLSyntaxClassExpressionParser parser;

  private OWLReasoner reasoner;

  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  private Map<IRI, String> iriToLabelMap;
  private Map<String, IRI> labelToIRIMap;

  private List<String> invalidTables = new ArrayList<>();
  private List<String> messages = new ArrayList<>();

  private Table outTable = null;
  private String currentTable;
  private int colNum;
  private int rowIdx;
  private int rowNum;
  private boolean valid;
  private boolean silent;

  private List<String[]> errors = new ArrayList<>();
  private int errCount = 0;

  private Cell currentCell = null;

  private ShortFormProvider provider;

  public TableValidator(
      OWLOntology ontology,
      IOHelper ioHelper,
      ManchesterOWLSyntaxClassExpressionParser parser,
      OWLReasoner reasoner,
      String outFormat,
      String outDir) {
    this.ontology = ontology;
    this.parser = parser;
    this.reasoner = reasoner;
    if (outFormat != null) {
      // Add the format and validate it
      this.outFormat = outFormat.toLowerCase();
      if (!Lists.newArrayList("xlsx", "html", "txt").contains(this.outFormat)) {
        throw new IllegalArgumentException(String.format(invalidFormatError, outFormat));
      }
    }
    this.outDir = outDir;

    // Extract from the ontology two convenience maps from rdfs:labels to IRIs and vice versa:
    iriToLabelMap = OntologyHelper.getLabels(ontology);
    labelToIRIMap = reverseIRILabelMap(iriToLabelMap);

    // Create some providers for rendering entities
    ShortFormProvider oboProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
    provider =
        new QuotedAnnotationValueShortFormProvider(
            ontology.getOWLOntologyManager(),
            oboProvider,
            ioHelper.getPrefixManager(),
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());

    errors.add(
        new String[] {"ID", "table", "cell", "level", "rule ID", "rule name", "value", "fix"});
  }

  /** Turn logging on or off. */
  public void toggleLogging() {
    silent = !silent;
  }

  public List<String[]> getErrors() {
    return errors;
  }

  /**
   * Validate a set of tables.
   *
   * @param tables tables to validate (map of table name to table contents)
   * @param options map of validate options
   * @return List of invalid tables (or empty list on success)
   * @throws Exception on any problem
   */
  public List<String> validate(Map<String, List<List<String>>> tables, Map<String, String> options)
      throws Exception {

    int skippedRow = Integer.parseInt(OptionsHelper.getOption(options, "skip-row", "0"));

    // Validate all of the tables in turn:
    for (Map.Entry<String, List<List<String>>> table : tables.entrySet()) {
      // Reset valid for new table
      valid = true;
      outTable = new Table(outFormat);
      String tablePath = table.getKey();
      List<List<String>> data = table.getValue();

      currentTable =
          String.format(
              "%s.%s", FilenameUtils.getBaseName(tablePath), FilenameUtils.getExtension(tablePath));
      if (outFormat == null) {
        System.out.println(String.format("Validating %s ...", currentTable));
      }

      // Get the header and rules rows
      List<String> headerRow = data.remove(0);
      List<String> rulesRow = data.remove(0);

      // Get correct index for the rule row based on if a row was skipped
      int ruleRowIdx = 2;
      if (skippedRow < 3) {
        ruleRowIdx = 3;
      }

      // Get number to add to rowIdx to get true row number from input table
      // This will be either 3 or 4 (skipped row in header)
      // as rowIdx starts at 0 and does not include header and rule rows
      int addToRow;
      if (skippedRow > 0 && skippedRow <= 3) {
        // Skipped row is in header, add 1 to our reporting
        addToRow = 4;
      } else {
        addToRow = 3;
      }

      // Add header and rules rows to Table object
      for (int i = 0; i < headerRow.size(); i++) {
        String rawRule = i < rulesRow.size() ? rulesRow.get(i) : "";
        // TODO - allow different providers?
        Column c = new Column(headerRow.get(i), parseRules(rawRule), rawRule, provider);
        outTable.addColumn(c);
      }
      List<Column> columns = outTable.getColumns();

      // Validate data row by row, column by column
      for (rowIdx = 0; rowIdx < data.size(); rowIdx++) {
        rowNum = rowIdx + addToRow;
        if (rowNum == skippedRow) {
          // Skipped row occurs in the data
          addToRow = 4;
        }

        List<String> row = data.get(rowIdx);
        if (!hasContent(row)) {
          logger.debug(String.format("Skipping empty row %d", rowNum));
          continue;
        }

        Row outRow = null;
        if (outFormat != null) {
          outRow = new Row();
        }

        for (colNum = 0; colNum < columns.size(); colNum++) {
          Column c = columns.get(colNum);
          Map<String, List<String>> rules = c.getRules();

          // Get the contents of the current cell:
          String cellString = colNum < row.size() ? row.get(colNum) : "";

          // Extract all the data entries contained within the current cell:
          List<String> cellData = Lists.newArrayList(cellString.trim().split("\\|"));

          // Create the cell object
          currentCell = getCell(c, cellData);

          if (rules == null || rules.isEmpty()) {
            // No rules to validate, just add the cell exactly as is
            if (outRow != null) {
              outRow.add(currentCell);
            }
            continue;
          }

          // For each of the rules applicable to this column, validate each entry in the cell
          // against it:
          for (Map.Entry<String, List<String>> ruleEntry : rules.entrySet()) {
            for (String rule : ruleEntry.getValue()) {
              List<String> interpolatedRules = interpolateRule(rule, row);
              for (String interpolatedRule : interpolatedRules) {
                for (String d : cellData) {
                  String errorMsg = validateRule(d, interpolatedRule, row, ruleEntry.getKey());
                  if (errorMsg != null) {
                    // An error was returned, add to errors
                    errCount++;
                    errors.add(
                        new String[] {
                          String.valueOf(errCount),
                          currentTable,
                          IOHelper.cellToA1(rowNum, colNum + 1),
                          "error",
                          FilenameUtils.getBaseName(currentTable)
                              + "!"
                              + IOHelper.cellToA1(ruleRowIdx, colNum + 1),
                          errorMsg,
                          d,
                          ""
                        });
                  }
                }
              }
            }
          }
          if (outRow != null) {
            outRow.add(currentCell);
          }
        }
        if (outFormat != null) {
          outTable.addRow(outRow);
        }
      }

      if (!valid) {
        invalidTables.add(currentTable);
      }

      boolean standalone = OptionsHelper.optionIsTrue(options, "standalone");
      boolean writeAll = OptionsHelper.optionIsTrue(options, "write-all");

      // Write table if: write-all is true OR table is not valid (for non-null formats)
      if ((writeAll || !valid) && outFormat != null) {
        String outPath =
            outDir + "/" + FilenameUtils.getBaseName(tablePath) + "." + outFormat.toLowerCase();
        switch (outFormat.toLowerCase()) {
          case "xlsx":
            try (Workbook wb = outTable.asWorkbook("|");
                FileOutputStream fos = new FileOutputStream(outPath)) {
              wb.write(fos);
            }
            break;
          case "html":
            try (PrintWriter out = new PrintWriter(outPath)) {
              out.print(outTable.toHTML("|", standalone, true));
            }
            break;
          case "txt":
            try (PrintWriter out = new PrintWriter(outPath)) {
              for (String m : messages) {
                out.println(m);
              }
            }
            break;
        }
      }
    }
    return invalidTables;
  }

  /** Given a map from IRIs to strings, return its inverse. */
  private static Map<String, IRI> reverseIRILabelMap(Map<IRI, String> source) {
    HashMap<String, IRI> target = new HashMap<>();
    for (Map.Entry<IRI, String> entry : source.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (target.containsKey(reverseKey)) {
        logger.warn(
            "Duplicate rdfs:label \"{}\". Overwriting value \"{}\" with \"{}\"",
            reverseKey,
            target.get(reverseKey),
            reverseValue);
      }
      target.put(reverseKey, reverseValue);
    }
    return target;
  }

  /**
   * Given an OWLClass describing a subject class from the ontology, an OWLClassExpression
   * describing a rule to query that subject class against, a string representing the query types to
   * use when evaluating the results of the query, and a list of strings describing a row from the
   * CSV: Determine whether, for any of the given query types, the given subject is in the result
   * set returned by the reasoner for that query type. Return true if it is in at least one of these
   * result sets, and false if it is not.
   */
  private boolean executeClassQuery(
      OWLClass subjectClass, OWLClassExpression ruleCE, List<String> row, String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_class_query(): Called with parameters: "
                + "subjectClass: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectClass, ruleCE, row, unsplitQueryType));

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query type is unrecognized, inform the user but continue on.
    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB
          || qType == RTypeEnum.DIRECT_SUB
          || qType == RTypeEnum.NOT_SUB
          || qType == RTypeEnum.NOT_DIRECT_SUB) {
        // Check to see if the subjectClass is a (direct) subclass of the given rule:
        // Get direct and not bools
        boolean direct = false;
        if (qType == RTypeEnum.DIRECT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB) {
          direct = true;
        }
        boolean not = false;
        if (qType == RTypeEnum.NOT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB) {
          not = true;
        }
        NodeSet<OWLClass> subClassesFound = reasoner.getSubClasses(ruleCE, direct);
        if (not && !subClassesFound.containsEntity(subjectClass)
            || !not && subClassesFound.containsEntity(subjectClass)) {
          // NOT and not in set OR in set
          return true;
        }

      } else if (qType == RTypeEnum.SUPER
          || qType == RTypeEnum.DIRECT_SUPER
          || qType == RTypeEnum.NOT_SUPER
          || qType == RTypeEnum.NOT_DIRECT_SUPER) {
        // Check to see if the subjectClass is a (direct) superclass of the given rule:
        // Get direct and not bools
        boolean direct = false;
        if (qType == RTypeEnum.DIRECT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER) {
          direct = true;
        }
        boolean not = false;
        if (qType == RTypeEnum.NOT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER) {
          not = true;
        }

        NodeSet<OWLClass> superClassesFound = reasoner.getSuperClasses(ruleCE, direct);
        if (not && !superClassesFound.containsEntity(subjectClass)
            || !not && superClassesFound.containsEntity(subjectClass)) {
          // NOT and not in set OR in set
          return true;
        }

      } else if (qType == RTypeEnum.EQUIV || qType == RTypeEnum.NOT_EQUIV) {
        // Check to see if the subjectClass is an equivalent of the given rule:
        boolean not = false;
        if (qType == RTypeEnum.NOT_EQUIV) {
          not = true;
        }
        Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ruleCE);
        if (!not && equivClassesFound.contains(subjectClass)
            || not && !equivClassesFound.contains(subjectClass)) {
          return true;
        }

      } else {
        // Spit out an error in this case but continue validating the other rules:
        logger.error(
            String.format(
                "%s validation not possible for OWLClass %s.", qType.getRuleType(), subjectClass));
      }
    }
    return false;
  }

  /**
   * Given an OWLClassExpression describing an unnamed subject class from the ontology, an
   * OWLClassExpression describing a rule to query that subject class against, a string representing
   * the query types to use when evaluating the results of the query, and a list of strings
   * describing a row from the CSV: Determine whether, for any of the given query types, the given
   * subject is in the result set returned by the reasoner for that query type. Return true if it is
   * in at least one of these result sets, and false if it is not.
   */
  private boolean executeGeneralizedClassQuery(
      OWLClassExpression subjectCE,
      OWLClassExpression ruleCE,
      List<String> row,
      String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_generalized_class_query(): Called with parameters: "
                + "subjectCE: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectCE, ruleCE, row, unsplitQueryType));

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query type is unrecognized, inform the user but continue on.
    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB) {
        // Check to see if the subjectClass is a subclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUB) {
        // Check to see if the subjectClass is a subclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.SUPER) {
        // Check to see if the subjectClass is a superclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(ruleCE, subjectCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUPER) {
        // Check to see if the subjectClass is a superclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(ruleCE, subjectCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.EQUIV) {
        OWLEquivalentClassesAxiom axiom =
            dataFactory.getOWLEquivalentClassesAxiom(subjectCE, ruleCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_EQUIV) {
        OWLEquivalentClassesAxiom axiom =
            dataFactory.getOWLEquivalentClassesAxiom(subjectCE, ruleCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else {
        // Spit out an error in this case but continue validating the other rules:
        logger.error(
            String.format(
                "%s validation not possible for OWLClassExpression %s.",
                qType.getRuleType(), subjectCE));
      }
    }
    return false;
  }

  /**
   * Given an OWLNamedIndividual describing a subject individual from the ontology, an
   * OWLClassExpression describing a rule to query that subject individual against, a string
   * representing the query types to use when evaluating the results of the query, and a list of
   * strings describing a row from the CSV: Determine whether, for any of the given query types, the
   * given subject is in the result set returned by the reasoner for that query type. Return true if
   * it is in at least one of these result sets, and false if it is not.
   */
  private boolean executeIndividualQuery(
      OWLNamedIndividual subjectIndividual,
      OWLClassExpression ruleCE,
      List<String> row,
      String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_individual_query(): Called with parameters: "
                + "subjectIndividual: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectIndividual, ruleCE, row, unsplitQueryType));

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query type is unrecognized or not applicable to an individual, inform
    // the user but continue on.
    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.INSTANCE
          || qType == RTypeEnum.DIRECT_INSTANCE
          || qType == RTypeEnum.NOT_INSTANCE) {
        boolean not = false;
        if (qType == RTypeEnum.NOT_INSTANCE) {
          not = true;
        }
        NodeSet<OWLNamedIndividual> instancesFound =
            reasoner.getInstances(ruleCE, qType == RTypeEnum.DIRECT_INSTANCE);
        if (not && !instancesFound.containsEntity(subjectIndividual)
            || !not && instancesFound.containsEntity(subjectIndividual)) {
          return true;
        }
      } else {
        // Spit out an error in this case but continue validating the other rules:
        logger.error(
            String.format(
                "%s validation not possible for OWLNamedIndividual %s.",
                qType.getRuleType(), subjectIndividual));
      }
    }
    return false;
  }

  /**
   * Given a string describing a subject term, a string describing a rule to query that subject term
   * against, a string representing the query types to use when evaluating the results of the query,
   * and a list of strings describing a row from the CSV: Determine whether, for any of the given
   * query types, the given subject is in the result set returned by the reasoner for that query
   * type. Return true if it is in at least one of these result sets, and false if it is not.
   */
  private boolean executeQuery(
      String subject, String rule, List<String> row, String unsplitQueryType) throws Exception {
    logger.debug(
        String.format(
            "execute_query(): Called with parameters: "
                + "subject: \"%s\", "
                + "rule: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subject, rule, row, unsplitQueryType));

    // Get the class expression corresponding to the rule that has been passed:
    OWLClassExpression ruleCE = getClassExpression(rule);
    if (ruleCE == null) {
      report(
          String.format(
              "Unable to parse rule \"%s %s\" at column %d.", unsplitQueryType, rule, colNum + 1));
      return false;
    }

    // Try to extract the label corresponding to the subject term:
    String subjectLabel = getLabelFromTerm(subject);
    if (subjectLabel != null) {
      // Figure out if it is an instance or a class and run the appropriate query
      IRI subjectIri = labelToIRIMap.get(subjectLabel);
      OWLEntity subjectEntity = OntologyHelper.getEntity(ontology, subjectIri);
      try {
        OWLNamedIndividual subjectIndividual = subjectEntity.asOWLNamedIndividual();
        return executeIndividualQuery(subjectIndividual, ruleCE, row, unsplitQueryType);
      } catch (OWLRuntimeException e) {
        try {
          OWLClass subjectClass = subjectEntity.asOWLClass();
          return executeClassQuery(subjectClass, ruleCE, row, unsplitQueryType);
        } catch (OWLRuntimeException ee) {
          // This actually should not happen, since if the subject has a label it should either
          // be a named class or a named individual:
          logger.error(
              String.format(
                  "While validating \"%s\" against \"%s %s\", encountered: %s",
                  subject, unsplitQueryType, rule, ee));
          return false;
        }
      }
    } else {
      // If no label corresponding to the subject term can be found, then try and parse it as a
      // class expression and run a generalised query on it:
      OWLClassExpression subjectCE = getClassExpression(subject);
      if (subjectCE == null) {
        logger.error(String.format("Unable to parse subject \"%s\" at row %d.", subject, rowNum));
        return false;
      }

      try {
        return executeGeneralizedClassQuery(subjectCE, ruleCE, row, unsplitQueryType);
      } catch (UnsupportedOperationException e) {
        logger.error("Generalized class expression queries are not supported by this reasoner.");
        return false;
      }
    }
  }

  /**
   * Given a string describing a term from the ontology, parse it into a class expression expressed
   * in terms of the ontology. If the parsing fails, write a warning statement to the log.
   */
  private OWLClassExpression getClassExpression(String term) {
    OWLClassExpression ce;
    try {
      ce = parser.parse(term);
    } catch (OWLParserException e) {
      // If the parsing fails the first time, try surrounding the term in single quotes:
      try {
        ce = parser.parse("'" + term + "'");
      } catch (OWLParserException ee) {
        logger.warn(
            String.format(
                "Could not determine class expression from \"%s\".\n\t%s.",
                term, e.getMessage().trim()));
        return null;
      }
    }
    return ce;
  }

  /**
   * Create a Cell object based on cell data from the input table.
   *
   * @param column Column that this Cell will go into
   * @param cellData list of strings from the cell
   * @return Cell object for output Table
   */
  private Cell getCell(Column column, List<String> cellData) {
    if (outFormat == null) {
      return new Cell(column, cellData);
    }

    RendererType displayRenderer = outTable.getDisplayRendererType();
    RendererType sortRenderer = outTable.getSortRendererType();
    ShortFormProvider provider = column.getShortFormProvider();

    List<String> display = new ArrayList<>();
    List<String> sort = new ArrayList<>();

    for (String val : cellData) {
      // Try to get IRI based on label
      IRI iri = labelToIRIMap.getOrDefault(val, null);
      OWLClassExpression expr = null;
      if (iri == null) {
        // Try to use parser as a backup if we couldn't get the IRI
        // e.g., if value provided was a CURIE or other short form
        try {
          expr = parser.parse(val);
        } catch (Exception e) {
          // Do nothing
        }
        if (expr == null) {
          // Not a class expression
          display.add(val);
          sort.add(val);
          continue;
        }
        if (!expr.isAnonymous()) {
          iri = expr.asOWLClass().getIRI();
        }
      }

      if (iri != null) {
        // Maybe add HTML link
        if (outFormat.equalsIgnoreCase("html")) {
          display.add(String.format("<a href=\"%s\">%s</a>", iri.toString(), val));
        } else {
          display.add(val);
        }
        sort.add(val);
      } else {
        // No IRI, the expression is anonymous
        // Render based on display/sort renderers and provider
        display.add(ExportOperation.renderManchester(displayRenderer, provider, expr));
        sort.add(ExportOperation.renderManchester(sortRenderer, provider, expr));
      }
    }
    return new Cell(column, display, sort);
  }

  /**
   * Given a string describing one of the classes in the ontology, in either the form of an IRI, an
   * abbreviated IRI, or an rdfs:label, return the rdfs:label for that class.
   */
  private String getLabelFromTerm(String term) {
    if (term == null) {
      return null;
    }

    // Remove any surrounding single quotes from the term:
    term = term.replaceAll("^\'|\'$", "");

    // If the term is already a recognized label, then just send it back:
    if (iriToLabelMap.containsValue(term)) {
      return term;
    }

    // Check to see if the term is a recognized IRI (possibly in short form), and if so return its
    // corresponding label:
    // TODO - short form might not work for everything, rework this
    for (IRI iri : iriToLabelMap.keySet()) {
      if (iri.toString().equals(term) || iri.getShortForm().equals(term)) {
        return iriToLabelMap.get(iri);
      }
    }

    // If the label isn't recognized, just return null:
    return null;
  }

  /**
   * Given a string in the form of a wildcard, and a list of strings representing a row of the CSV,
   * return the rdfs:label contained in the position of the row indicated by the wildcard.
   */
  private String getWildcardContents(String wildcard, List<String> row) throws Exception {
    if (!wildcard.startsWith("%")) {
      logger.error(String.format("Invalid wildcard: \"%s\".", wildcard));
      return null;
    }

    int colIndex = Integer.parseInt(wildcard.substring(1)) - 1;
    if (colIndex >= row.size()) {
      throw new Exception(String.format(columnOutOfRangeError, colNum + 1, wildcard, row.size()));
    }

    String term = row.get(colIndex);
    if (term == null || term.trim().equals("")) {
      logger.info(
          String.format(
              "Failed to retrieve label from wildcard: %s. No term at position %d of this row.",
              wildcard, colIndex + 1));
      return null;
    }

    return term.trim();
  }

  /**
   * Given a string specifying a list of rules of various types, return a map which contains, for
   * each rule type present in the string, the list of rules of that type that have been specified.
   */
  private Map<String, List<String>> parseRules(String ruleString) throws Exception {
    HashMap<String, List<String>> ruleMap = new HashMap<>();
    // Skip over empty strings and strings that start with "##".
    if (!ruleString.trim().equals("") && !ruleString.trim().startsWith("##")) {
      // Rules are separated by semicolons:
      String[] rules = ruleString.split("\\s*;\\s*");
      for (String rule : rules) {
        // Skip any rules that begin with a '#' (these are interpreted as commented out):
        if (rule.trim().startsWith("#")) {
          continue;
        }
        // Each rule is of the form: <rule-type> <rule-content> but for the PRESENCE category, if
        // <rule-content> is left out it is implicitly understood to be "true"
        String[] ruleParts = rule.trim().split("\\s+", 2);
        String ruleType = ruleParts[0].trim();
        String ruleContent;
        if (ruleParts.length == 2) {
          ruleContent = ruleParts[1].trim();
        } else {
          RTypeEnum rTypeEnum = rule_type_to_rtenum_map.get(ruleType);
          if (rTypeEnum != null && rTypeEnum.getRuleCat() == RCatEnum.PRESENCE) {
            ruleContent = "true";
          } else {
            throw new Exception(String.format(malformedRuleError, rule.trim()));
          }
        }

        // Add, to the map, a new empty list for the given ruleType if we haven't seen it before:
        if (!ruleMap.containsKey(ruleType)) {
          ruleMap.put(ruleType, new ArrayList<>());
        }
        // Add the content of the given rule to the list of rules corresponding to its ruleType:
        ruleMap.get(ruleType).add(ruleContent);
      }
    }
    return ruleMap;
  }

  /**
   * Given a list of strings representing a row from the table, return true if any of the cells in
   * the row has non-whitespace content.
   */
  private boolean hasContent(List<String> row) {
    for (String cell : row) {
      if (!cell.trim().equals("")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given a string, possibly containing wildcards, and a list of strings representing a row of the
   * CSV, return a string in which all of the wildcards in the input string have been replaced by
   * the rdfs:labels corresponding to the content in the positions of the row that they indicate.
   */
  private List<String> interpolateRule(String rule, List<String> row) throws Exception {
    // This is what will be returned:
    List<String> interpolatedRules = new ArrayList<>();

    // If the rule only has whitespace in it, return an empty string back to the caller:
    if (rule.trim().equals("")) {
      interpolatedRules.add("");
      return interpolatedRules;
    }

    // Look for wildcards within the given rule. These will be of the form %d where d is the number
    // of the cell the wildcard is pointing to (e.g. %1 is the first cell). Then create a map from
    // wildcard numbers to the terms that they point to, which we extract from the cell
    // indicated by the wildcard number. In general the terms will be split by pipes within a
    // cell. E.g. if the wildcard is %1 and the first cell contains 'term1|term2|term3' then add an
    // entry to the wildcard map like: 1 -> ['term1', 'term2', 'term3'].
    Matcher m = Pattern.compile("%(\\d+)").matcher(rule);
    Map<Integer, String[]> wildCardMap = new HashMap<>();
    while (m.find()) {
      int key = Integer.parseInt(m.group(1));
      if (!wildCardMap.containsKey(key)) {
        String wildcard = getWildcardContents(m.group(), row);
        String[] terms = wildcard != null ? wildcard.split("\\|") : new String[] {null};
        wildCardMap.put(key, terms);
      }
    }

    // If the wildcard map is empty then the rule contained no wildcards. Just return it as it is:
    if (wildCardMap.isEmpty()) {
      interpolatedRules.add(rule);
      return interpolatedRules;
    }

    // Now interpolate the rule using the wildcard map. If any of the wildcards points to a cell
    // with multiple terms, then we duplicate the rule for each term pointed to. Finally we return
    // all of the rules generated.
    for (int i : wildCardMap.keySet()) {
      if (interpolatedRules.isEmpty()) {
        // If we haven't yet interpolated anything, then base the current interpolation on the rule
        // that has been passed as an argument to the function above, and generate an interpolated
        // rule corresponding to every term corresponding to this key in the wildcard map.
        for (String term : wildCardMap.get(i)) {
          String label = getLabelFromTerm(term);
          String interpolatedRule =
              rule.replaceAll(
                  String.format("%%%d", i), label == null ? "(" + term + ")" : "'" + label + "'");
          interpolatedRules.add(interpolatedRule);
        }
      } else {
        // If we have already interpolated some rules, then every string that has been interpolated
        // thus far must be interpolated again for every term corresponding to this key in the
        // wildcard map, and the list of interpolated rules is then replaced with the new list.
        List<String> tmpList = new ArrayList<>();
        for (String term : wildCardMap.get(i)) {
          String label = getLabelFromTerm(term);
          for (String intStr : interpolatedRules) {
            String interpolatedRule =
                intStr.replaceAll(
                    String.format("%%%d", i), label == null ? "(" + term + ")" : "'" + label + "'");
            tmpList.add(interpolatedRule);
          }
        }
        interpolatedRules = tmpList;
      }
    }

    return interpolatedRules;
  }

  /**
   * Given the string `format` and a number of formatting variables, use the formatting variables to
   * fill in the format string in the manner of C's printf function, and write the string to the
   * Writer object (or XLSX workbook, or Jinja context) that belongs to ValidateOperation. If the
   * parameter `showCoords` is true, then include the current row and column number in the output
   * string.
   */
  private void report(String format, Object... positionalArgs) {
    // Any report of error means validation failed
    valid = false;

    // Format the error message
    String outStr = String.format("At %s row %d, column %d: ", currentTable, rowNum, colNum + 1);
    outStr += String.format(format, positionalArgs);
    if (!silent) {
      // Print error if not silent
      System.out.println(outStr);
    }

    if (outFormat != null && !outFormat.equals("txt")) {
      // We want to put formatting on cells with errors
      if (outFormat.equals("xlsx")) {
        // Set the style of the current cell to a red background with a white font:
        currentCell.setFontColor(IndexedColors.WHITE);
        currentCell.setCellPattern(FillPatternType.FINE_DOTS);
        currentCell.setCellColor(IndexedColors.RED);
      } else {
        // Set the HTML class to bg-danger (red background with a white font)
        currentCell.setHTMLClass("bg-danger");
      }
      // Attach a comment to the cell
      // If one for this cell already exists, add new comment to existing comment
      String commentString = String.format(format, positionalArgs);
      String currentComment = currentCell.getComment();
      if (currentComment != null) {
        commentString = currentComment + "; " + commentString;
      }
      currentCell.setComment(commentString);
    } else if (outFormat != null) {
      // Add outStr to messages to be written to file
      messages.add(outStr);
    }
  }

  /**
   * Given a string describing a rule type, return a boolean indicating whether it is one of the
   * rules recognized by ValidateOperation.
   */
  private boolean unknownRuleType(String ruleType) {
    return !rule_type_to_rtenum_map.containsKey(ruleType.split("\\|")[0]);
  }

  /**
   * Given a string describing the content of a rule and a string describing its rule type, return a
   * simple map entry such that the `key` for the entry is the main clause of the rule, and the
   * `value` for the entry is a list of the rule's when-clauses. Each when-clause is itself stored
   * as an array of three strings, including the subject to which the when-clause is to be applied,
   * the rule type for the when clause, and the actual axiom to be validated against the subject.
   */
  private AbstractMap.SimpleEntry<String, List<String[]>> separateRule(String rule, String ruleType)
      throws Exception {

    // Check if there are any when clauses:
    Matcher m = Pattern.compile("(\\(\\s*when\\s+.+\\))(.*)").matcher(rule);
    String whenClauseStr;
    if (!m.find()) {
      // If there is no when clause, then just return back the rule string as it was passed with an
      // empty when clause list:
      logger.debug(String.format("No when-clauses found in rule: \"%s\".", rule));
      return new AbstractMap.SimpleEntry<>(rule, new ArrayList<>());
    }

    // Throw an exception if there is no main clause and this is not a PRESENCE rule:
    if (m.start() == 0 && rule_type_to_rtenum_map.get(ruleType).getRuleCat() != RCatEnum.PRESENCE) {
      throw new Exception(String.format(noMainError, colNum + 1, rule));
    }

    // Extract the actual content of the when-clause.
    whenClauseStr = m.group(1);
    whenClauseStr = whenClauseStr.substring("(when ".length(), whenClauseStr.length() - 1);

    // Don't fail just because there is some extra garbage at the end of the rule, but notify
    // the user about it:
    if (!m.group(2).trim().equals("")) {
      logger.warn(
          String.format("Ignoring string \"%s\" at end of rule \"%s\".", m.group(2).trim(), rule));
    }

    // Within each when clause, multiple subclauses separated by ampersands are allowed. Each
    // subclass must be of the form: <Entity> <Rule-Type> <Axiom>, where: <Entity> is a (not
    // necessarily interpolated) string describing either a label or a generalised DL class
    // expression involving labels, and any label names containing spaces are enclosed within
    // single quotes; <Rule-Type> is a possibly hyphenated alphanumeric string (which corresponds
    // to one of the rule types defined above in RTypeEnum); and <Axiom> can take any form.
    // Here we resolve each sub-clause of the when statement into a list of such triples.
    ArrayList<String[]> whenClauses = new ArrayList<>();
    for (String whenClause : whenClauseStr.split("\\s*&\\s*")) {
      m =
          Pattern.compile("^([^\'\\s()]+|\'[^\']+\'|\\(.+?\\))" + "\\s+([a-z\\-|]+)" + "\\s+(.*)$")
              .matcher(whenClause);

      if (!m.find()) {
        throw new Exception(String.format(malformedWhenClauseError, colNum + 1, whenClause));
      }
      // Add the triple to the list of when clauses:
      whenClauses.add(new String[] {m.group(1), m.group(2), m.group(3)});
    }

    // Now get the main part of the rule (i.e. the part before the when clause):
    m = Pattern.compile("^(.+)\\s+\\(when\\s").matcher(rule);
    if (m.find()) {
      return new AbstractMap.SimpleEntry<>(m.group(1), whenClauses);
    }

    // If no main clause is found, then if this is a PRESENCE rule, implicitly assume that the main
    // clause is "true":
    if (rule_type_to_rtenum_map.get(ruleType).getRuleCat() == RCatEnum.PRESENCE) {
      return new AbstractMap.SimpleEntry<>("true", whenClauses);
    }

    // We should never get here since we have already checked for an empty main clause earlier ...
    logger.error(
        String.format(
            "Encountered unknown error while looking for main clause of rule \"%s\".", rule));
    // Return the rule as passed with an empty when clause list:
    return new AbstractMap.SimpleEntry<>(rule, new ArrayList<>());
  }

  /**
   * Given a string describing a rule, a rule of the type PRESENCE, and a string representing a cell
   * from the CSV, determine whether the cell satisfies the given presence rule (e.g. is-required,
   * is-empty).
   */
  private String validatePresenceRule(String rule, RTypeEnum rType, String cell) throws Exception {

    logger.debug(
        String.format(
            "validate_presence_rule(): Called with parameters: "
                + "rule: \"%s\", "
                + "rule type: \"%s\", "
                + "cell: \"%s\".",
            rule, rType.getRuleType(), cell));

    // Presence-type rules (is-required, is-excluded) must be in the form of a truth value:
    if ((Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1)
        && (Arrays.asList("false", "f", "0", "no", "n").indexOf(rule.toLowerCase()) == -1)) {
      throw new Exception(
          String.format(invalidPresenceRuleError, colNum + 1, rule, rType.getRuleType()));
    }

    // If the restriction isn't "true" then there is nothing to do. Just return:
    if (Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1) {
      logger.debug(
          String.format("Nothing to validate for rule: \"%s %s\"", rType.getRuleType(), rule));
      return null;
    }

    String msg;
    switch (rType) {
      case REQUIRED:
        if (cell.trim().equals("")) {
          msg =
              String.format(
                  "Cell is empty but rule: \"%s %s\" does not allow this.",
                  rType.getRuleType(), rule);
          report(msg);
          return msg;
        }
        break;
      case EXCLUDED:
        if (!cell.trim().equals("")) {
          msg =
              String.format(
                  "Cell is non-empty (\"%s\") but rule: \"%s %s\" does not allow this.",
                  cell, rType.getRuleType(), rule);
          report(msg);
          return msg;
        }
        break;
      default:
        msg =
            String.format(
                "%s validation of rule type: \"%s\" is not yet implemented.",
                rType.getRuleCat(), rType.getRuleType());
        logger.error(msg);
        return msg;
    }
    logger.info(
        String.format("Validated \"%s %s\" against \"%s\".", rType.getRuleType(), rule, cell));
    return null;
  }

  /**
   * Given a string describing a cell from the CSV, a string describing a rule to be applied against
   * that cell, a string describing the type of that rule, and a list of strings describing the row
   * containing the given cell, validate the cell, indicating any validation errors via the output
   * writer (or XLSX workbook).
   */
  private String validateRule(String cell, String rule, List<String> row, String ruleType)
      throws Exception {

    logger.debug(
        String.format(
            "validate_rule(): Called with parameters: "
                + "cell: \"%s\", "
                + "rule: \"%s\", "
                + "row: \"%s\", "
                + "rule type: \"%s\".",
            cell, rule, row, ruleType));

    logger.info(String.format("Validating rule \"%s %s\" against \"%s\".", ruleType, rule, cell));
    if (unknownRuleType(ruleType)) {
      throw new Exception(String.format(unrecognizedRuleTypeError, colNum + 1, ruleType));
    }

    // Separate the given rule into its main clause and optional when clauses:
    AbstractMap.SimpleEntry<String, List<String[]>> separatedRule = separateRule(rule, ruleType);

    // Evaluate and validate any when clauses for this rule first:
    if (!validateWhenClauses(separatedRule.getValue(), row, colNum)) {
      logger.debug("Not all when clauses have been satisfied. Skipping main clause");
      return null;
    }

    // Once all of the when clauses have been validated, get the RTypeEnum representation of the
    // primary rule type of this rule:
    RTypeEnum primRType = rule_type_to_rtenum_map.get(ruleType.split("\\|")[0]);

    // If the primary rule type for this rule is not in the QUERY category, process it at this step
    // and return control to the caller. The further steps below are only needed when queries are
    // going to be sent to the reasoner.
    if (primRType.getRuleCat() != RCatEnum.QUERY) {
      return validatePresenceRule(separatedRule.getKey(), primRType, cell);
    }

    // If the cell contents are empty, just return to the caller silently (if the cell is not
    // expected to be empty, this will have been caught by one of the presence rules in the
    // previous step, assuming such a rule is constraining the column).
    if (cell.trim().equals("")) return null;

    // Get the axiom that the cell will be validated against:
    String axiom = separatedRule.getKey();

    // Send the query to the reasoner:
    // Comment may be null on exception, empty on success, or a non-empty String on validation
    // failure
    // Non-empty strings get added to the Cell
    boolean result = executeQuery(cell, axiom, row, ruleType);
    String msg = null;
    if (!result) {
      msg = String.format("Validation failed for rule: \"%s %s %s\".", cell, ruleType, axiom);
      report(msg);
    } else {
      logger.info(String.format("Validated: \"%s %s %s\".", cell, ruleType, axiom));
    }
    return msg;
  }

  /**
   * Given a list of String arrays describing a list of when-clauses, and a list of Strings
   * describing the row to which these when-clauses belong, validate the when-clauses one by one,
   * returning false if any of them fails to be satisfied, and true if they are all satisfied.
   */
  private boolean validateWhenClauses(List<String[]> whenClauses, List<String> row, int colNum)
      throws Exception {

    for (String[] whenClause : whenClauses) {
      String subject = whenClause[0].trim();
      // If the subject term is blank, then skip this clause:
      if (subject.equals("")) {
        continue;
      }

      // Make sure all of the rule types in the when clause are of the right category:
      String whenRuleType = whenClause[1];
      for (String whenRuleSubType : whenRuleType.split("\\|")) {
        RTypeEnum whenSubRType = rule_type_to_rtenum_map.get(whenRuleSubType);
        if (whenSubRType == null || whenSubRType.getRuleCat() != RCatEnum.QUERY) {
          throw new Exception(
              String.format(
                  invalidWhenTypeError,
                  colNum + 1,
                  String.join(" ", whenClause),
                  query_type_to_rtenum_map.keySet()));
        }
      }

      // Get the axiom to validate and send the query to the reasoner:
      String axiom = whenClause[2];
      if (!executeQuery(subject, axiom, row, whenRuleType)) {
        // If any of the when clauses fail to be satisfied, then we do not need to evaluate any
        // of the other when clauses, or the main clause, since the main clause may only be
        // evaluated when all of the when clauses are satisfied.
        logger.info(
            String.format(
                "When clause: \"%s %s %s\" is not satisfied.", subject, whenRuleType, axiom));
        return false;
      } else {
        logger.info(
            String.format("Validated when clause \"%s %s %s\".", subject, whenRuleType, axiom));
      }
    }
    // If we get to here, then all of the when clauses have been satisfied, so return true:
    return true;
  }

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
    NOT_SUPER("not-superclass-of", RCatEnum.QUERY),
    NOT_DIRECT_SUPER("not-direct-superclass-of", RCatEnum.QUERY),
    SUPER("superclass-of", RCatEnum.QUERY),
    EQUIV("equivalent-to", RCatEnum.QUERY),
    NOT_EQUIV("not-equivalent-to", RCatEnum.QUERY),
    DIRECT_SUB("direct-subclass-of", RCatEnum.QUERY),
    NOT_SUB("not-subclass-of", RCatEnum.QUERY),
    NOT_DIRECT_SUB("not-direct-subclass-of", RCatEnum.QUERY),
    SUB("subclass-of", RCatEnum.QUERY),
    DIRECT_INSTANCE("direct-instance-of", RCatEnum.QUERY),
    NOT_INSTANCE("not-instance-of", RCatEnum.QUERY),
    INSTANCE("instance-of", RCatEnum.QUERY),
    REQUIRED("is-required", RCatEnum.PRESENCE),
    EXCLUDED("is-excluded", RCatEnum.PRESENCE);

    private final String ruleType;
    private final RCatEnum ruleCat;

    RTypeEnum(String ruleType, RCatEnum ruleCat) {
      this.ruleType = ruleType;
      this.ruleCat = ruleCat;
    }

    private String getRuleType() {
      return ruleType;
    }

    private RCatEnum getRuleCat() {
      return ruleCat;
    }
  }
}
