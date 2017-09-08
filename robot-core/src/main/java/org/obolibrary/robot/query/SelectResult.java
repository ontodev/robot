package org.obolibrary.robot.query;

import com.hp.hpl.jena.query.ResultSet;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Optional;

/**
 * Created by edouglass on 9/7/17.
 *
 * Result from a SPARQL SELECT query
 */
public class SelectResult implements QueryResult {

    private ResultSet triplePattern;

    public SelectResult(ResultSet triplePattern) {
        this.triplePattern = triplePattern;
    }

    @Override
    public void writeResults(File outfile, Optional<Lang> format) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outfile);
            ResultSetMgr.write(out, triplePattern, format.orElse(Lang.CSV));
        } catch (FileNotFoundException e) {
            throw new ResultsWritingException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
