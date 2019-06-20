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
   * @param jsonObject the JSON object to validate
   * @param writer the Writer instance to write output to
   * @return true if the JSON object is valid, false otherwise
   */
  public static boolean validate(
      List<List<String>> csvData,
      OWLOntology owlData,
      OWLReasonerFactory reasonerFactory,
      Writer writer)
      throws IOException {
    writer.write("You specified a csv parameter of:\n" + csvData + "\n");
    writer.write("You specified an owl parameter of:\n" + owlData + "\n");
    return true;
  }
}
