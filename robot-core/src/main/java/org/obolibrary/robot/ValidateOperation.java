package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for Immune Exposures data
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /**
   * Writes a row of data to the given writer, with a comment appended.
   *
   * @param row the row of data to write (a list of strings)
   * @param comment the comment to append to the end of the row
   * @param writer the Writer instance to write output to
   */
  private static void write_row(List<String> row, String comment, Writer writer)
      throws Exception, IOException {

    // Comma-delimit every cell:
    for (String cell : row) {
      writer.write(String.format("\"%s\",", cell));
    }
    writer.write(String.format("\"%s\"\n", comment));
  }

  private static HashMap<String, String> parse_rules(String ruleString) {
    HashMap<String, String> ruleMap = new HashMap();
    String[] rules = ruleString.split("\\s*;\\s*");
    for (String rule : rules) {
      String[] ruleParts = rule.split("\\s*:\\s*", 2);
      ruleMap.put(ruleParts[0], ruleParts[1]);
    }
    return ruleMap;
  }

  private static ArrayList<String> get_columns_with_given_type(
      Map<String, Map<String, String>> headerToRuleMap, String colType) {

    ArrayList<String> retList = new ArrayList();
    for (String header : headerToRuleMap.keySet()) {
      for (Map.Entry<String, String> entry : headerToRuleMap.get(header).entrySet()) {
        if (entry.getKey().toLowerCase().equals("type") &&
            entry.getValue().toLowerCase().equals(colType.toLowerCase())) {
          retList.add(header);
        }
      }
    }
    return retList;
  }

  // REMOVE
  private static HashMap<String, String> get_column_parents_map() {
    HashMap<String, String> returnMap = new HashMap<>();
    returnMap.put("Exposure Process Reported", "Exposure Process Reported");
    returnMap.put("Exposure Material Reported", "Exposure Material Reported");
    returnMap.put("Disease Reported", "Disease Reported");
    returnMap.put("Disease Ontology ID", "Disease Reported");
    returnMap.put("Disease Stage Reported", "Disease Stage Reported");
    return returnMap;
  }
  
  private static IRI getIriFromShortForm(String shortIriStr, HashMap<IRI, String> iriToLabelMap) {
    for (IRI iri : iriToLabelMap.keySet()) {
      if (iri.getShortForm().equals(shortIriStr)) {
        return iri;
      }
    }
    return null;
  }

  private static void validate_owl(
      IRI child,
      String childLabel,
      IRI parent,
      String parentLabel,
      OWLOntology ontology,
      OWLReasoner reasoner)
      throws Exception {

    // Get the OWLClass corresponding to the parent:
    OWLClass parentClass = OntologyHelper.getEntity(ontology, parent).asOWLClass();

    // Get the OWLClass corresponding to the child, and its super classes:
    OWLClass childClass = OntologyHelper.getEntity(ontology, child).asOWLClass();
    NodeSet<OWLClass> childAncestors = reasoner.getSuperClasses(childClass, false);

    // Check if the child's ancestors include the parent:
    if (!childAncestors.containsEntity(parentClass)) {
      System.out.println(
          String.format(
              "'%s' (%s) is not a descendant of '%s' (%s)",
              childLabel, child.getShortForm(), parentLabel, parent.getShortForm()));
    }
  }


  /**
   * INSERT DESCRIPTION HERE
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   * @param ontology the ontology to use for validation
   * @param reasonerFactory the reasoner factory to use for validation
   * @param writer the Writer instance to write output to
   */
  public static void validate_immexp(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer)
      throws Exception, IOException {

    // Extract the header and rules rows from the CSV data and map the column names to their
    // associated rules:
    List<String> header = csvData.remove(0);
    List<String> rules = csvData.remove(0);
    HashMap<String, Map<String, String>> headerToRuleMap = new HashMap();
    for (int i = 0; i < header.size(); i++) {
      headerToRuleMap.put(header.get(i), parse_rules(rules.get(i)));
    }

    // Based on the rules, determine which columns have labels and which have IRIs:
    ArrayList<String> columnsWithLabels = get_columns_with_given_type(headerToRuleMap, "label");
    ArrayList<String> columnsWithIris = get_columns_with_given_type(headerToRuleMap, "short iri");
    // REMOVE:
    Map<String, String> columnParentsMap = get_column_parents_map();

    // Extract from the ontology a map from (lowercase) rdfs:label strings to IRIs:
    Map<String, IRI> labelToIriMap = OntologyHelper.getLabelIRIs(ontology, true);
    // Generate the reverse map:
    HashMap<IRI, String> iriToLabelMap = new HashMap();
    for (Map.Entry<String, IRI> entry : labelToIriMap.entrySet()) {
      iriToLabelMap.put(entry.getValue(), entry.getKey());
    }

    // Create a new reasoner, from the reasoner factory, based on the ontology data:
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

    // Validate the data rows:
    for (List<String> row : csvData) {
      for (String colName : columnsWithLabels) {
        int colIndex = header.indexOf(colName);
        String childLabel = row.get(colIndex);
        IRI child = labelToIriMap.get(childLabel.toLowerCase());
        if (child == null) {
          System.out.println(
              String.format("Child '%s' (%s) is not in label->iri map", childLabel, child));
          continue;
        }
        // TODO: The template should define, for each column heading, the associated parent. In this
        // case the parent just happens to identical to the column heading, but not always.
        String parentLabel = colName;
        IRI parent = labelToIriMap.get(parentLabel.toLowerCase());
        if (parent == null) {
          System.out.println(
              String.format("Parent '%s' (%s) is not in label->iri map", parentLabel, parent));
          continue;
        }
        validate_owl(child, childLabel, parent, parentLabel, ontology, reasoner);
      }

      for (String colName : columnsWithIris) {
        int colIndex = header.indexOf(colName);
        String childIriShortForm = row.get(colIndex);
        IRI child = getIriFromShortForm(childIriShortForm, iriToLabelMap);
        if (child == null) {
          System.out.println(
              String.format(
                  "Could not determine child IRI from short form: '%s'", childIriShortForm));
          continue;
        }
        String childLabel = iriToLabelMap.get(child);
        if (childLabel == null) {
          System.out.println(
              String.format(
                  "Could not determine label for child '%s' (not in map)", childIriShortForm));
          continue;
        }
        String parentLabel = columnParentsMap.get("Disease Ontology ID");
        IRI parent = labelToIriMap.get(parentLabel.toLowerCase());
        if (parent == null) {
          System.out.println(
              String.format("Parent '%s' (%s) is not in iri->label map", parentLabel, parent));
          continue;
        }
        validate_owl(child, childLabel, parent, parentLabel, ontology, reasoner);
      }
    }
  }

  /* THIS FUNCTION SHOULD BE COMBINED WITH THE OTHER ONE */
  /**
   * Generates a validation report for the given CSV data, writing output to the given writer.
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   * @param ontology the ontology to use for validation
   * @param reasonerFactory the reasoner factory to use for validation
   * @param writer the Writer instance to write output to
   */
  public static void validate_pc(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer)
      throws Exception, IOException {

    // Extract from the ontology a map from rdfs:label strings to IRIs:
    Map<String, IRI> labelToIriMap = OntologyHelper.getLabelIRIs(ontology);

    // Extract the header row from the CSV data:
    List<String> header = csvData.remove(0);
    // Get the index numbers for the parent and child columns:
    String childCol = "ID";
    String parentCol = "Parent IRI";
    int parentColIndex = header.indexOf(parentCol);
    int childColIndex = header.indexOf(childCol);
    if ((parentColIndex == -1) || (childColIndex == -1)) {
      String err = String.format("\'%s\' or \'%s\' not found in CSV header: ", parentCol, childCol);
      for (String column : header) err += (column + ", ");
      throw new Exception(err);
    }

    // The cell in the parent column of the first 'global' row should indicate the ancestor which
    // every other row's parent column should be an instance of.
    List<String> globalRow = csvData.remove(0);
    String ancestor = globalRow.get(parentColIndex);
    // Make sure the ancestor is in the ontology:
    if (!labelToIriMap.containsKey(ancestor)) {
      throw new Exception(String.format("Global ancestor: '%s' not found in ontology", ancestor));
    }

    // Now write the header and global rows to the output file:
    write_row(header, "", writer);
    // The cell in the child column of the global row should be blank, so add a comment about this
    // if the child is non-empty, but don't fail since it is not a showstopper:
    String comment = "";
    String globalChild = globalRow.get(childColIndex);
    if (!globalChild.equals("")) {
      comment =
          String.format(
              ": Non-empty child: %s in column %d of global row", globalChild, childColIndex);
    }
    write_row(globalRow, comment, writer);

    // Get the OWLClass corresponding to the ancestor:
    OWLClass ancestorClass =
        OntologyHelper.getEntity(ontology, labelToIriMap.get(ancestor)).asOWLClass();

    // Create a new reasoner, from the reasoner factory, based on the ontology data:
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    for (List<String> row : csvData) {
      String parent = row.get(parentColIndex);
      if (!labelToIriMap.containsKey(parent)) {
        comment = String.format(": '%s' not found in ontology", parent);
        // In this case, we can't do any further processing for this row, so write what we have and
        // continue:
        write_row(row, comment, writer);
        continue;
      }

      // If the child is not in the ontology, comment on it:
      comment = "";
      String child = row.get(childColIndex);
      if (!labelToIriMap.containsKey(child)) {
        comment += String.format(" : Child: '%s' not found in ontology", child);
      }

      // Get the OWLClass corresponding to the parent, and its super classes:
      OWLClass parentClass =
          OntologyHelper.getEntity(ontology, labelToIriMap.get(parent)).asOWLClass();
      NodeSet<OWLClass> parentAncestors = reasoner.getSuperClasses(parentClass, false);

      // Check if the parent's ancestors include the global ancestor, and comment on it if it isn't:
      if (!parentAncestors.containsEntity(ancestorClass)) {
        comment += String.format(": Parent '%s' is not a descendant of '%s'", parent, ancestor);
      }

      // Write the row with (possibly empty) comment:
      write_row(row, comment, writer);
    }
    reasoner.dispose();
  }
}
