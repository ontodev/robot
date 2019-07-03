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
   * Writes a row of data to the given writer, with a comment appended.
   *
   * @param row the row of data to write (a list of strings)
   * @param comment the comment to append to the end of the row
   * @param writer the Writer instance to write output to
   */
  private static void write_row(List<String> row, String comment, Writer writer)
      throws Exception, IOException {

    for (String cell : row) {
      writer.write(String.format("\"%s\",", cell));
    }
    writer.write(String.format("\"%s\"\n", comment));
  }

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
    // other row's parent column should be an instance of.
    List<String> globalRow = csvData.remove(0);
    // Get and check the global ancestor:
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
        // In this case, we can't do any further processing, so write what we have and continue:
        write_row(row, comment, writer);
        continue;
      }

      comment = "";
      String child = row.get(childColIndex);
      if (!labelToIriMap.containsKey(child)) {
        comment += String.format(" : Child: '%s' not found in ontology", child);
      }

      // Get the OWLClass corresponding to the parent, and its super classes:
      OWLClass parentClass =
          OntologyHelper.getEntity(ontology, labelToIriMap.get(parent)).asOWLClass();
      NodeSet<OWLClass> parentAncestors = reasoner.getSuperClasses(parentClass, false);

      // Check if the parent's ancestors include the global ancestor:
      if (!parentAncestors.containsEntity(ancestorClass)) {
        comment += String.format(": Parent '%s' is not a descendant of '%s'", parent, ancestor);
      }

      // Write the row with comment:
      write_row(row, comment, writer);
    }
    reasoner.dispose();
  }
}
