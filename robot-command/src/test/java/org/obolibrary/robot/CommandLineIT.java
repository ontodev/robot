package org.obolibrary.robot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Integration tests check the commands in `examples/README.md`.
 */
public class CommandLineIT {

    /**
     * Path to the executable script.
     */
    private String execPath = "../bin/robot";

    /**
     * Path to the examples/ directory.
     */
    private String examplesPath = "../examples/";

    /**
     * Path to the examples README file.
     */
    private String readmePath = examplesPath + "README.md";

    /**
     * Path to the examples/results/ directory.
     */
    private String resultsPath = examplesPath + "results/";

    /**
     * Path to the command output log.
     */
    private String outputPath = "target/integration-tests.txt";

    /**
     * Trim whitespace and trailing backslash from the given string.
     *
     * @param line the line to trim
     * @return the trimmed line
     */
    private String trim(String line) {
        return line.trim().replaceAll("\\\\$", "").trim();
    }

    /**
     * Extract all the robot commands from the example README file.
     * Include only lines that are indented with four spaces and start with
     * "robot"
     *
     * @return a list of example commands
     * @throws IOException if the README cannot be read
     */
    private List<String> extractCommands() throws IOException {
        String content = FileUtils.readFileToString(new File(readmePath));
        List<String> lines = Arrays.asList(
                content.replaceAll("\\r", "").split("\\n"));
        List<String> commands = new ArrayList<String>();

        boolean collecting = false;
        String collected = null;
        for (String line: lines) {
            if (collecting) {
                if (line.startsWith("    ")) {
                    collected += " " + trim(line);
                } else {
                    collecting = false;
                    commands.add(collected);
                    collected = null;
                }
            } else {
                if (line.startsWith("    robot")) {
                    collecting = true;
                    collected = trim(line);
                }
            }
        }

        return commands;
    }

    /**
     * Ensure that the examples/results/ directory exists and is empty.
     *
     * @throws Exception if directory cannot be created or emptied
     */
    private void cleanResults() throws Exception {
        File resultsDir = new File(resultsPath);
        if (resultsDir.exists() && !resultsDir.isDirectory()) {
            throw new Exception(
                    "Could not create directory for integration tests: "
                    + resultsPath);
        }
        FileUtils.deleteDirectory(resultsDir);
        resultsDir.mkdir();
    }

    /**
     * Given a command string starting with 'robot',
     * run that command using the bin/robot script
     * inside the examples/ directory.
     *
     * @param command the command string to execute
     * @throws Exception on if command returns a non-zero exit value
     */
    private void runCommand(String command) throws Exception {
        String header = "============================================\n"
            + "Results from command: "
            + command + "\n\n";

        FileOutputStream outputStream = new FileOutputStream(outputPath, true);
        IOUtils.write(header, outputStream);

        List<String> arguments = CommandLineHelper.parseArgList(command);
        arguments.remove(0);
        arguments.add(0, execPath);

        ProcessBuilder pb = new ProcessBuilder(arguments);
        pb.redirectErrorStream(true);
        pb.directory(new File(examplesPath));

        Process process = pb.start();
        IOUtils.copy(process.getInputStream(), outputStream);
        outputStream.close();

        int exitValue = process.waitFor();
        if (exitValue != 0) {
            throw new Exception("Command '"
                    + command + "' failed; see '"
                    + outputPath + "'");
        }
    }

    /**
     * Compare each file in the example/results/ directory,
     * to the corresponding file in the examples/ directory.
     *
     * @throws Exception on IO problems or file differences
     */
    private void compareResults() throws Exception {
        IOHelper ioHelper = new IOHelper();
        File resultsDir = new File(resultsPath);
        for (File resultFile: resultsDir.listFiles()) {
            if (!resultFile.isFile()) {
                continue;
            }

            // Find the corresponding example file
            String examplePath = examplesPath + resultFile.getName();
            File exampleFile = new File(examplePath);
            if (!exampleFile.exists() || !exampleFile.isFile()) {
                throw new Exception("Integration test file '"
                        + resultsPath + resultFile.getName()
                        + "' does not have a corresponding example file '"
                        + examplePath + "'");
            }

            if (resultFile.getName().endsWith(".owl")) {
                // Compare OWL files using DiffOperation
                OWLOntology exampleOnt = ioHelper.loadOntology(exampleFile);
                OWLOntology resultOnt = ioHelper.loadOntology(resultFile);
                if (!DiffOperation.equals(exampleOnt, resultOnt)) {
                    throw new Exception("Integration test ontology '"
                            + resultsPath + resultFile.getName()
                            + "' is different from example ontology '"
                            + examplePath + "'");
                }
            } else {
                // Compare all other files
                if (!FileUtils.contentEquals(exampleFile, resultFile)) {
                    throw new Exception("Integration test file '"
                            + resultsPath + resultFile.getName()
                            + "' is different from example file '"
                            + examplePath + "'");
                }
            }
        }
    }

    /**
     * Test all commands in the examples/README.md file.
     *
     * @throws Exception on any problem
     */
    @Test
    public void testExecute() throws Exception {
        cleanResults();
        List<String> commands = extractCommands();
        for (String command: commands) {
            runCommand(command);
        }
        // Regression test for dropped axiom:
        //https://github.com/ontodev/robot/issues/98
        runCommand("robot convert -i dropped_axiom.owl -o "
                + "results/dropped_axiom.owl");

        compareResults();
    }

}

