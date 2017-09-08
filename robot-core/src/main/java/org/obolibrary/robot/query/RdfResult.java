package org.obolibrary.robot.query;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;
import java.util.Optional;

/**
 * Result of a SPARQL CONSTRUCT query, which is RDF triples.
 */
public class RdfResult implements QueryResult {

    private Model constructedTriples;

    public RdfResult(Model constructedTriples) {
        this.constructedTriples = constructedTriples;
    }

    @Override
    public void writeResults(File outfile, Optional<Lang> format) {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outfile);
            RDFDataMgr.write(out, constructedTriples, format.orElse(Lang.TTL));
        } catch (FileNotFoundException e) {
            throw new ResultsWritingException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
