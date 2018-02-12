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
    String fullMsg = trimExceptionMsg(exception);
    String exceptionID = getExceptionID(fullMsg);
    String msg;
    // If there is an exception ID, grab the second part as the message
    if (!exceptionID.equals("")) {
      msg = getExceptionMsg(fullMsg);
      // Otherwise just return the whole thing
    } else {
      msg = fullMsg;
    }
    System.out.println(msg);
    System.out.println("See http://robot.obolibrary.org/" + exceptionID + " for more details");
    // Will only print with --very-very-verbose (DEBUG level)
    StackTraceElement[] trace = exception.getStackTrace();
    for (StackTraceElement t : trace) {
      logger.debug(t.toString());
    }
  }

  /**
   * Given an exception message, return the exception ID when formatted as "{command}#{ID}
   * {message}". Otherwise, return an empty string.
   *
   * @param msg exception message
   * @return exception ID or empty string
   */
  private static String getExceptionID(String msg) {
    String exceptionID = "";
    try {
      exceptionID = msg.substring(0, msg.indexOf(" "));
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
   * Given an exception message, return the message with the ID stripped when formatted as
   * "{command}#{ID} {message}".
   *
   * @param msg exception message
   * @return exception message without ID
   */
  private static String getExceptionMsg(String msg) {
    return msg.substring(msg.indexOf(" ") + 1);
  }

  /**
   * Given a full exception message, trim the Java exception class from the start. If there is no
   * class, just return the message. TODO: Support more throwable classes?
   *
   * @param exception the Exception
   * @return trimmed exception message String
   */
  private static String trimExceptionMsg(Exception exception) {
    String msg = exception.getMessage();
    if (msg.startsWith("java.")) {
      return msg.substring(msg.indexOf(":") + 1).trim();
    } else {
      return msg;
    }
  }
}
