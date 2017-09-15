package org.obolibrary.robot.query;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

/**
 * Created by edouglass on 9/14/17.
 *
 * Tests RdfResult
 */
public class RdfResultTest {

    private RdfResult result;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() {
        InputStream simpleRdf = this.getClass().getResourceAsStream("/simpleresults.ttl");
        DatasetGraph mem = readStreamAsRdf(simpleRdf);
        result = new RdfResult(ModelFactory.createModelForGraph(mem.getDefaultGraph()));
    }

    @Test
    public void testWrite() throws IOException {
        File testOutFile = temp.newFile("testOut");
        result.writeResults(testOutFile, Optional.of(Lang.TURTLE));

        DatasetGraph graph = readStreamAsRdf(new FileInputStream(testOutFile));
        DatasetGraph expectedGraph = readStreamAsRdf(this.getClass().getResourceAsStream("/simpleresults.ttl"));
        assertTrue(graph.getDefaultGraph().isIsomorphicWith(expectedGraph.getDefaultGraph()));
    }

    private DatasetGraph readStreamAsRdf(InputStream turtleStream) {
        DatasetGraph graph = DatasetGraphFactory.createMem();
        RDFDataMgr.read(graph.getDefaultGraph(), turtleStream, Lang.TURTLE);
        return graph;
    }

}
