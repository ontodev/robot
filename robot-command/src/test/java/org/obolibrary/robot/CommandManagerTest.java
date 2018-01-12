package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.commons.cli.Options;
import org.junit.Test;

/** Tests for CommandManager. */
public class CommandManagerTest {
  /** Shared manager. */
  private CommandManager manager = new CommandManager();

  /** Shared options. */
  private Options options = new Options();

  /** Create a new manager with new options and new commands. */
  private void reset() {
    options = new Options();
    options.addOption("h", "help", false, "help option, without value");
    options.addOption("i", "input", true, "input option, with value");

    manager = new CommandManager();
    manager.addCommand("mock1", new MockCommand());
    manager.addCommand("mock2", new MockCommand());
    manager.addCommand("mock3", new MockCommand());
  }

  /**
   * Convenience method for splitting an argument string.
   *
   * @param arg the argument string
   * @return an array of arguments
   * @throws Exception on parse problems
   */
  private String[] split(String arg) throws Exception {
    return CommandLineHelper.parseArgs(arg);
  }

  /**
   * Convenience method for asserting and counting parsed options.
   *
   * @param message a message to pass on test failure
   * @param count an integer with the expected count
   * @param arg a single string of command-line arguments; will be split
   * @throws Exception on any problem
   */
  private void countOptions(String message, int count, String arg) throws Exception {
    List<String> args = CommandLineHelper.parseArgList(arg);
    List<String> used = manager.getOptionArgs(options, args);
    assertEquals(message, count, used.size());
  }

  /**
   * Test the options are being parsed correctly.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testCountOptions() throws Exception {
    reset();
    countOptions("No options", 0, "");
    countOptions("No options", 0, "foo bar");

    countOptions("Short help option", 1, "-h");
    countOptions("Short help option", 1, "-h foo bar");
    countOptions("Long help option", 1, "--help");
    countOptions("Long help option", 1, "--help foo bar");

    countOptions("Short input option", 2, "-i foo");
    countOptions("Short input option", 2, "-i foo bar");
    countOptions("Long input option", 2, "--input foo");
    countOptions("Long input option", 2, "--input foo bar");

    countOptions("Multiple options", 3, "-h --input foo bar");

    countOptions("Unknown option", 0, "-f bar");
    countOptions("Unknown option", 1, "-h --foo bar");
    countOptions("Unknown option", 2, "-i baz --foo bar");
    countOptions("Unknown option", 1, "-h --foo bar");
    countOptions("Unknown option", 3, "-h -i baz --foo bar");
  }

  /**
   * Test that commands are being executed.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExecute() throws Exception {
    reset();
    manager.execute(null, split("--help mock1"));
    manager.execute(null, split("--help mock1 mock2"));
    // manager.execute(null, split("--help mock1 --local foo"));
  }
}
