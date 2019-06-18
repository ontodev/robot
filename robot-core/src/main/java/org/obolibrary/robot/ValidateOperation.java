package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
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
  public static boolean validate(List<List<String>> csvData, Writer writer) throws IOException {
    writer.write("You specified an input parameter of:\n" + csvData + "\n");
    writer.write("Note that this parameter was of class: " + csvData.getClass() + "\n");
    return true;
  }
}
