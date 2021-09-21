package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Tests for CommandLineHelper. */
public class CommandLineHelperTest {

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

    // Expect an Exception here
    assertThrows(
        Exception.class,
        () -> {
          CommandLineHelper.parseArgList("unbalanced 'quotes");
        });
  }
}
