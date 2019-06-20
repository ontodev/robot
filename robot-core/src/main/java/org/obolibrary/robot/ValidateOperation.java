package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.semanticweb.owlapi.model.OWLOntology;
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
   * Validates the given jsonObject, writing output to the given writer. Returns true if jsonObject
   * is valid, false otherwise.
   *
   * @param csvData a list of rows extracted from a CSV file to be validated
   * @param owlData the ontology to use for validation
   * @param reasonerFactory the reasoner factory to use for validation
   * @param colToValidate the specific column of the csvData to be validated
   * @param ancestorCol the column of the CSV file containing ancestor information
   * @param writer the Writer instance to write output to
   * @return true if the JSON object is valid, false otherwise
   */
  public static boolean validate(
      List<List<String>> csvData,
      OWLOntology owlData,
      OWLReasonerFactory reasonerFactory,
      String colToValidate,
      String ancestorCol,
      Writer writer)
      throws Exception, IOException {

    // Extract the header row from the CSV data, and get the index numbers for the ancestor column
    // and the column we will be validating against it:
    List<String> headers = csvData.remove(0);
    int ancestorColIndex = headers.indexOf(ancestorCol);
    int colOfInterestIndex = headers.indexOf(colToValidate);
    if ((ancestorColIndex == -1) || (colOfInterestIndex == -1)) {
      String err =
          String.format("\'%s\' or \'%s\' not found in CSV header: ", ancestorCol, colToValidate);
      for (String header : headers) err += (header + ", ");
      throw new Exception(err);
    }

    for (int i = 0; i < csvData.size(); i++) {
      List<String> row = csvData.get(i);
      if (row.size() != headers.size()) {
        writer.write(String.format("Warning: row %d has wrong number of columns; skipping\n", i));
      } else {
        writer.write(row.get(colOfInterestIndex) + " | " + row.get(ancestorColIndex) + "\n");
      }
    }

    return true;
  }
}
