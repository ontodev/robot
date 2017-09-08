package org.obolibrary.robot.query;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Optional;

/**
 * Created by edouglass on 9/7/17.
 *
 * Prints a boolean to a file as a result from an ASK SPARQL query
 */
public class AskResult implements QueryResult {

    private final boolean answer;

    public AskResult(boolean answer) {
        this.answer = answer;
    }

    @Override
    public void writeResults(File outfile, Optional<Lang> format) {
        PrintStream outStream = null;
        try {
            outStream = new PrintStream(new FileOutputStream(outfile));
            outStream.print(answer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(outStream);
        }
    }
}
