package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Method for converting an ontology format.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class ConvertOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ConvertOperation.class);

  /** Class IOHelper. */
  private static final IOHelper ioHelper = new IOHelper();

  /** Namespace for error messages. */
  private static final String NS = "convert#";

  /** Error message when --check is true and the document is not in valid OBO structure */
  private static final String oboStructureError = NS + "OBO STRUCTURE ERROR see details:\n%s\n";

  /**
   * Given an OWLOntology, a format name, an output file, and a boolean determining if OBO frame
   * structure should be checked (if converting to OBO), convert the file format and save in the
   * output file location.
   *
   * @param ontology OWLOntology to convert
   * @param formatName format as string
   * @param outputFile File to save to
   * @param checkOBO boolean to check OBO frame structure
   * @throws Exception on any issue
   */
  public static void convert(
      OWLOntology ontology, String formatName, File outputFile, boolean checkOBO) throws Exception {
    try {
      ioHelper.saveOntology(ontology, IOHelper.getFormat(formatName), outputFile, checkOBO);
    } catch (IOException e) {
      // specific feedback for writing to OBO
      if (e.getMessage().contains("FrameStructureException")) {
        String details = e.getMessage();
        throw new Exception(String.format(oboStructureError, details));
      } else {
        throw e;
      }
    }
  }
}
