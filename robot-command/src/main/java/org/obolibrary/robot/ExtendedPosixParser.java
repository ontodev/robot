package org.obolibrary.robot;

import java.util.ListIterator;
import org.apache.commons.cli.*;

/**
 * A custom CommandLineParser that ignores unrecognized options without throwing an exception. See
 * http://stackoverflow.com/a/8613949
 */
@Deprecated
@SuppressWarnings("deprecation")
public class ExtendedPosixParser extends PosixParser {
  /** Flag for ignoring unrecognized options. */
  private boolean ignoreUnrecognizedOption;

  /**
   * Extend constructor to accept ignore option.
   *
   * @param ignoreUnrecognizedOption when true, silently ignore unrecognized options
   */
  @Deprecated
  public ExtendedPosixParser(final boolean ignoreUnrecognizedOption) {
    this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
  }

  /**
   * Given an argument string and an iterator, try to process the option, respecting the
   * ignoreUnrecognizedOption flag.
   *
   * @param arg the argument to process
   * @param iter the iterator for the super class to deal with
   * @throws ParseException on any problems
   */
  @Deprecated
  @Override
  protected void processOption(final String arg, final ListIterator<String> iter)
      throws ParseException {
    boolean hasOption = getOptions().hasOption(arg);

    if (hasOption || !ignoreUnrecognizedOption) {
      super.processOption(arg, iter);
    }
  }
}
