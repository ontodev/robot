package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements the validate operation for a given CSV file and ontology.
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /**
   * INSERT DOC HERE
   */
  private static Map<String, IRI> reverse_iri_label_map(Map<IRI, String> iriToLabelMap) {
    HashMap<String, IRI> labelToIriMap = new HashMap();
    for (Map.Entry<IRI, String> entry : iriToLabelMap.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (labelToIriMap.containsKey(reverseKey)) {
        logger.warn(
            String.format(
                "Duplicate rdfs:label '%s'. Overwriting value '%s' with '%s'",
                reverseKey, labelToIriMap.get(reverseKey), reverseValue));
      }
      labelToIriMap.put(reverseKey, reverseValue);
    }
    return labelToIriMap;
  }

  /**
   * INSERT DOC HERE
   */
  private static Map<String, String> parse_rules(String ruleString) {
    HashMap<String, String> ruleMap = new HashMap();
    String[] rules = ruleString.split("\\s*;\\s*");
    for (String rule : rules) {
      String[] ruleParts = rule.split("\\s*:\\s*", 2);
      ruleMap.put(ruleParts[0].trim(), ruleParts[1].trim());
    }
    return ruleMap;
  }

  /**
   * INSERT DOC HERE
   */
  private static String get_label_from_term(
      String term,
      Map<String, IRI> labelToIriMap,
      Map<IRI, String> iriToLabelMap) {

    // If the term is already a recognised label, then just send it back:
    if (labelToIriMap.containsKey(term)) {
      return term;
    }

    // Check to see if the term is a recognised IRI (possibly in short form), and if so return its
    // corresponding label:
    for (IRI iri : iriToLabelMap.keySet()) {
      if (iri.toString().equals(term) || iri.getShortForm().equals(term)) {
        return iriToLabelMap.get(iri);
      }
    }

    // If the label isn't recognised, just return null:
    return null;
  }

  /**
   * INSERT DOC HERE
   */
  private static String find_parent_label_from_rule(
      String parentRule,
      List<String> row,
      Map<String, IRI> labelToIriMap,
      Map<IRI, String> iriToLabelMap) {

    String parentTerm = null;
    if (parentRule.startsWith("%")) {
      int parentColIndex = Integer.parseInt(parentRule.substring(1)) - 1;
      parentTerm = row.get(parentColIndex).trim();
    }
    else {
      parentTerm = parentRule;
    }

    return (parentTerm != null && !parentTerm.equals("")) ?
        get_label_from_term(parentTerm, labelToIriMap, iriToLabelMap) : null;
  }

  /**
   * INSERT DOC HERE
   */
  private static void validate_ancestry(
      IRI child,
      String childLabel,
      IRI parent,
      String parentLabel,
      OWLOntology ontology,
      OWLReasoner reasoner,
      Writer writer)
      throws Exception {

    // Get the OWLClass corresponding to the parent:
    OWLClass parentClass = OntologyHelper.getEntity(ontology, parent).asOWLClass();

    // Get the OWLClass corresponding to the child, and its super classes:
    OWLClass childClass = OntologyHelper.getEntity(ontology, child).asOWLClass();
    NodeSet<OWLClass> childAncestors = reasoner.getSuperClasses(childClass, false);

    // Check if the child's ancestors include the parent:
    if (!childAncestors.containsEntity(parentClass)) {
      logger.error(
          String.format(
              "'%s' (%s) is not a descendant of '%s' (%s)\n",
              childLabel, child.getShortForm(), parentLabel, parent.getShortForm()));
    }
    System.out.println(
        String.format("Relationship between '%s' and '%s' is valid.", childLabel, parentLabel));
  }

  /**
   * INSERT DOC HERE
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   * @param ontology the ontology to use for validation
   * @param reasonerFactory the reasoner factory to use for validation
   * @param writer the Writer instance to write output to
   */
  public static void validate(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      Writer writer)
      throws Exception, IOException {

    // Extract from the ontology two maps from rdfs:labels to IRIs and vice versa:
    Map<IRI, String> iriToLabelMap = OntologyHelper.getIRILabels(ontology);
    Map<String, IRI> labelToIriMap = reverse_iri_label_map(iriToLabelMap);

    // Create a new reasoner, from the reasoner factory, based on the ontology data:
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

    // Extract the header and rules rows from the CSV data and map the column names to their
    // associated rules:
    List<String> header = csvData.remove(0);
    List<String> allRules = csvData.remove(0);
    HashMap<String, Map<String, String>> headerToRuleMap = new HashMap();
    for (int i = 0; i < header.size(); i++) {
      headerToRuleMap.put(header.get(i), parse_rules(allRules.get(i)));
    }

    // Validate the data rows:
    for (List<String> row : csvData) {
      for (String colName : header) {
        int colIndex = header.indexOf(colName);
        Map<String, String> colRules = headerToRuleMap.get(colName);

        // Get the contents of the current cell (the 'child data')
        String childCell = row.get(colIndex).trim();
        if (childCell.equals("")) continue;

        // Get the rdfs:label and IRI corresponding to the child:
        String childLabel = get_label_from_term(childCell, labelToIriMap, iriToLabelMap);
        if (childLabel == null) {
          logger.error("Could not find '" + childCell + "' in ontology.");
          continue;
        }
        IRI child = labelToIriMap.get(childLabel);
        System.out.println("Found child: " + child.toString() + " with label: " + childLabel);

        // If the child is supposed to be a subclass of something, get the rdfs:label and IRI of
        // the indicated parent:
        if (colRules.containsKey("sc")) {
          String parentLabel = find_parent_label_from_rule(
              colRules.get("sc"), row, labelToIriMap, iriToLabelMap);
          if (parentLabel == null) {
            logger.error("Could not determine parent from rule '" + colRules.get("sc") + "'");
            continue;
          }
          IRI parent = labelToIriMap.get(parentLabel);
          System.out.println("Found parent: " + parent.toString() + " with label: " + parentLabel);
          // Validate the parent-child relationship:
          validate_ancestry(child, childLabel, parent, parentLabel, ontology, reasoner, writer);
        }
      }
    }
    reasoner.dispose();
  }
}
