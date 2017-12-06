package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests for CommandLineHelper. */
public class CommandLineHelperTest {
  /** The exception to expect, if any. */
  @Rule public ExpectedException exception = ExpectedException.none();

  /**
   * Test command line splitting.
   *
   * @throws Exception on parsing problem
   */
  @Test
  public void testParseArgs() throws Exception {
    String arg = "";
    List<String> args = new ArrayList<String>();
    assertEquals("Empty arg list", CommandLineHelper.parseArgList(arg), args);

    arg = "command";
    args.clear();
    args.add("command");
    assertEquals("Just a command", CommandLineHelper.parseArgList(arg), args);

    arg = "command -i --foo PARAM 'single quoted' \"double quoted\"";
    args.clear();
    args.add("command");
    args.add("-i");
    args.add("--foo");
    args.add("PARAM");
    args.add("single quoted");
    args.add("double quoted");
    assertEquals("Basic command", CommandLineHelper.parseArgList(arg), args);

    arg = "command 'nested \"quotes\" with \\\" escapes and\nnewlines'";
    args.clear();
    args.add("command");
    args.add("nested \"quotes\" with \\\" escapes and\nnewlines");
    assertEquals("Nested quotations", CommandLineHelper.parseArgList(arg), args);

    arg = "unbalanced 'quotes";
    exception.expect(Exception.class);
    CommandLineHelper.parseArgList(arg);
  }
}
