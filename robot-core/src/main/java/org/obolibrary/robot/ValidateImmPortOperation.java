package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Validator for Immune Exposures data */
public class ValidateImmPortOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateImmPortOperation.class);

  public static boolean validate(Object jsonObject, Writer writer) throws IOException {
    writer.write("You specified an input parameter of:\n" + jsonObject + "\n");
    writer.write("Note that this parameter was of class: " + jsonObject.getClass() + "\n");
    return true;
  }
}
