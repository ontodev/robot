package org.obolibrary.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class to handle formatting error messages and returning links to user support.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class ExceptionHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExceptionHelper.class);

  /**
   * Prints the exception details as errors, including help URL (if the exception message contains a
   * valid exception ID). Expects the exception message to be formatted as: "{ID} :: {message}". If
   * message is not formatted, will just log the message.
   *
   * @param exception the exception
   */
  public static void handleException(Exception exception) {
    String msg = trimExceptionClass(exception);
    // Will only print with --very-very-verbose (DEBUG level)
    if (logger.isDebugEnabled()) {
      StackTraceElement[] trace = exception.getStackTrace();
      for (StackTraceElement t : trace) {
        logger.debug(t.toString());
      }
      System.out.println();
    }
    // Print the message
    if (msg != null) {
      String exceptionID = getExceptionID(msg);
      System.out.println(trimExceptionID(msg));
      System.out.println("For details see: http://robot.obolibrary.org/" + exceptionID + "\n");
    }
  }

  /**
   * Given an exception message, return the exception ID when formatted as "command#NAME OF ERROR
   * msg". Otherwise, return an empty string.
   *
   * @param msg exception message
   * @return exception ID or empty string
   */
  private static String getExceptionID(String msg) {
    String exceptionID = "";
    try {
      exceptionID =
          msg.substring(0, msg.indexOf("ERROR") + 5).trim().toLowerCase().replace(" ", "-");
    } catch (NullPointerException e) {
      logger.debug("Malformed exception message: {}", msg);
    }
    if (!exceptionID.contains("#")) {
      logger.debug("Missing exception ID: {}", msg);
      exceptionID = "";
    }
    return exceptionID;
  }

  /**
   * Given a full exception message, trim the Java exception class from the start. If there is no
   * class, just return the message. TODO: Support more throwable classes?
   *
   * @param exception the Exception
   * @return trimmed exception message String
   */
  private static String trimExceptionClass(Exception exception) {
    String msg = exception.getMessage();
    if (msg == null) {
      logger.debug("{} missing exception message.", exception);
      return null;
    }
    if (msg.startsWith("java.")) {
      return msg.substring(msg.indexOf(":") + 1).trim();
    } else {
      return msg;
    }
  }

  /**
   * Given an exception message, remove the error namespace. If no namespace is provided, just
   * return the message.
   *
   * @param msg exception message
   * @return message without the command namespace
   */
  private static String trimExceptionID(String msg) {
    if (msg.contains("#")) {
      return msg.substring(msg.indexOf("#") + 1);
    } else {
      return msg;
    }
  }
}
