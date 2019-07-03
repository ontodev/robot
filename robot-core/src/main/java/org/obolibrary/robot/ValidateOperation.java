package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
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
 * Validator for Immune Exposures data
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /**
   * Generates a validation report for the given CSV data, writing output to the given writer.
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   * @param ontology the ontology to use for validation
   * @param reasonerFactory the reasoner factory to use for validation
   * @param childCol the column of the CSV file containing child information
   * @param parentCol the column of the CSV file containing parent information
   * @param writer the Writer instance to write output to
   */
  public static void validate(
      List<List<String>> csvData,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      String childCol,
      String parentCol,
      Writer writer)
      throws Exception, IOException {

    // Extract from the ontology a map from rdfs:label strings to IRIs:
    Map<String, IRI> labelToIriMap = OntologyHelper.getLabelIRIs(ontology);

    // Extract the header row from the CSV data:
    List<String> header = csvData.remove(0);
    // Get the index numbers for the parent column and the column to validate:
    int parentColIndex = header.indexOf(parentCol);
    int childColIndex = header.indexOf(childCol);
    if ((parentColIndex == -1) || (childColIndex == -1)) {
      String err = String.format("\'%s\' or \'%s\' not found in CSV header: ", parentCol, childCol);
      for (String column : header) err += (column + ", ");
      throw new Exception(err);
    }

    // The cell in the parent column of the first row should indicate the ancestor which every
    // other row's parent column should be an instance of. The cell in the child column should be
    // blank.
    List<String> global_row = csvData.remove(0);
    String globalChild = global_row.get(childColIndex);
    if (!globalChild.equals("")) {
      writer.write(
          String.format(
              "Warning: non-empty child: %s in column %d of global row\n",
              globalChild, childColIndex));
    }
    String ancestor = global_row.get(parentColIndex);
    writer.write(String.format("Ancestor for this file is: %s\n", ancestor));

    // Make sure the ancestor is in the ontology:
    if (!labelToIriMap.containsKey(ancestor)) {
      throw new Exception(String.format("Global ancestor: '%s' not found in ontology", ancestor));
    }

    // Get the OWLClass corresponding to the ancestor:
    OWLClass ancestorClass =
        OntologyHelper.getEntity(ontology, labelToIriMap.get(ancestor)).asOWLClass();

    // Create a new reasoner, from the reasoner factory, based on the ontology data:
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    for (int i = 0; i < csvData.size(); i++) {
      List<String> row = csvData.get(i);
      if (row.size() != header.size()) {
        writer.write(
            String.format(
                "Warning: row %d has wrong number of columns; "
                    + "I'm going to need to report this!\n",
                i));
        continue;
      }
      String parent = row.get(parentColIndex);
      if (!labelToIriMap.containsKey(parent)) {
        writer.write(
            String.format(
                "On row %d, parent: '%s' not found in ontology. I'm going to "
                    + "need to report this!\n",
                i, parent));
        continue;
      }
      String child = row.get(childColIndex);
      if (!labelToIriMap.containsKey(child)) {
        writer.write(
            String.format(
                "On row %d, child: '%s' not found in ontology. I'm going to "
                    + "need to report this!\n",
                i, child));
        // Don't need to continue in this case since the child is not needed for further processing
      }

      // Get the OWLClass corresponding to the parent, and its super classes:
      OWLClass parentClass =
          OntologyHelper.getEntity(ontology, labelToIriMap.get(parent)).asOWLClass();
      NodeSet<OWLClass> parentAncestors = reasoner.getSuperClasses(parentClass, false);

      // Check if the parent's ancestors include the global ancestor:
      if (!parentAncestors.containsEntity(ancestorClass)) {
        writer.write(
            String.format(
                "On row %d, parent '%s' is not a descendant of '%s'. I'm "
                    + "going to need to report this!\n",
                i, parent, ancestor));
      }
    }
    reasoner.dispose();
  }
}
