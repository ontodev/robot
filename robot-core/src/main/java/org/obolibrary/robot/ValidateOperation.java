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

// TODO:
// - ALLOW FOR EXTRA LEADING OR TRAILING WHITESPACE IN CSV CELLS
// - LABEL MATCHING SHOULD BE CASE INSENSITIVE, MAYBE ALSO FOR IRIS (THOUGH THIS IS HARDER)

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
  // DO WE STILL NEED THIS?
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

  private static IRI get_iri_from_short_form(
      String shortIriStr, HashMap<IRI, String> iriToLabelMap) {
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
      writer.write(
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

    // Extract from the ontology a map from rdfs:label strings to IRIs:
    Map<String, IRI> labelToIriMap = OntologyHelper.getLabelIRIs(ontology);
    // Generate the reverse map:
    HashMap<IRI, String> iriToLabelMap = new HashMap();
    for (Map.Entry<String, IRI> entry : labelToIriMap.entrySet()) {
      iriToLabelMap.put(entry.getValue(), entry.getKey());
    }

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
        String childLabel = "";
        IRI child = null;

        // Get the contents of the current cell (the 'child data')
        String cellContents = row.get(colIndex).trim();
        if (cellContents.equals("")) continue;

        if (!colRules.containsKey("type") || colRules.get("type").equals("label")) {
          childLabel = cellContents;
          child = labelToIriMap.get(childLabel);
          if (child == null) {
            writer.write(
                String.format("Child '%s' (%s) is not in label->iri map", childLabel, child));
            continue;
          }
        } else if (colRules.get("type").equals("iri")) {
          String childIriShortForm = cellContents;
          child = get_iri_from_short_form(childIriShortForm, iriToLabelMap);
          if (child == null) {
            writer.write(
                String.format(
                    "Could not determine child IRI from short form: '%s'", childIriShortForm));
            continue;
          }
          childLabel = iriToLabelMap.get(child);
          if (childLabel == null) {
            writer.write(
                String.format(
                    "Could not determine label for child '%s' (not in map)", childIriShortForm));
            continue;
          }
        }

        String parentLabel = "";
        // See if the cell is a subclass of any 'parent':
        if (colRules.containsKey("sc-iri")) {
          // To be implemented
        } else if (colRules.containsKey("sc-label")) {
          String parentCode = colRules.get("sc-label");
          try {
            if (parentCode.equals("@@")) {
              parentLabel = colName;
            } else if (parentCode.startsWith("@")) {
              int parentColIndex = Integer.parseInt(parentCode.substring(1)) - 1;
              parentLabel = header.get(parentColIndex);
            } else if (parentCode.startsWith("%")) {
              int parentColIndex = Integer.parseInt(parentCode.substring(1)) - 1;
              parentLabel = row.get(parentColIndex);
            } else {
              parentLabel = parentCode;
            }
          } catch (Exception e) {
            writer.write("Unable to parse code: " + parentCode);
            // COULD WE DO SOMETHING BETTER HERE?
            e.printStackTrace();
            continue;
          }
        }

        IRI parent = labelToIriMap.get(parentLabel);
        if (parent == null) {
          writer.write(
              String.format("Parent '%s' (%s) is not in label->iri map", parentLabel, parent));
          continue;
        }

        validate_owl(child, childLabel, parent, parentLabel, ontology, reasoner, writer);
      }
    }
    reasoner.dispose();
  }
}
