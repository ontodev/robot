package org.obolibrary.robot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;

/** Tests for pluggable commands feature. */
public class PluginManagerIT {

  /** Path to the executable script. */
  private String execPath = "../bin/robot";

  /** Path to the build directory of the mock plugin. */
  private String pluginsPath = "../robot-mock-plugin/target";

  /**
   * Search for a motif within a stream.
   *
   * @param stream the stream to search through
   * @param needle the motif to search for
   * @return {@code true} if the motif was found, otherwise {@code false}
   * @throws IOException if any I/O error occurs when reading from the stream
   */
  private boolean searchStream(InputStream stream, String needle) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String line;
    boolean found = false;
    while ((line = reader.readLine()) != null) {
      if (line.contains(needle)) {
        found = true;
      }
    }
    reader.close();

    return found;
  }

  /**
   * Run a commands and check that its standard output contains a given motif.
   *
   * @param program the command to run
   * @param needle the motif to search for
   * @throws Exception if the motif is not found in the command's output
   */
  private void runCommand(String program, String needle) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(program);
    pb.environment().put("ROBOT_PLUGINS_DIRECTORY", pluginsPath);

    Process process = pb.start();
    boolean found = searchStream(process.getInputStream(), needle);
    process.waitFor();

    if (!found) {
      throw new Exception("Pluggable command not available");
    }
  }

  /**
   * Test that the plugin manager finds the pluggable command in the mock plugin.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExecute() throws Exception {
    runCommand(execPath, ":hello");
  }
}
