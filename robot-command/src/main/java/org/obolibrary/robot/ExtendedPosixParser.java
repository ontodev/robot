package org.obolibrary.robot;

import java.util.ListIterator;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * A custom CommandLineParser that ignores unrecognized options without throwing an exeception. See
 * http://stackoverflow.com/a/8613949
 */
public class ExtendedPosixParser extends PosixParser {
  /** Flag for ignoring unrecognized options. */
  private boolean ignoreUnrecognizedOption;

  /**
   * Extend constructor to accept ignore option.
   *
   * @param ignoreUnrecognizedOption when true, silently ignore unrecognized options
   */
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
  @Override
  protected void processOption(final String arg, final ListIterator iter) throws ParseException {
    boolean hasOption = getOptions().hasOption(arg);

    if (hasOption || !ignoreUnrecognizedOption) {
      super.processOption(arg, iter);
    }
  }
}
