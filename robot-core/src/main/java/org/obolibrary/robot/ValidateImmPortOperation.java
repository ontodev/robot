package org.obolibrary.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SAY SOMETHING HERE */
public class ValidateImmPortOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateImmPortOperation.class);

  public static boolean equals(String won, String too) {
    return won.compareTo(too) == 0;
  }
}
