package org.obolibrary.robot;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Test query operation.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 *
 */
public class QueryOperationTest extends CoreTest {

    /**
     * Tests a simple query.
     *
     * @throws IOException on IO error
     * @throws OWLOntologyCreationException on ontology error
     * @throws OWLOntologyStorageException on ontology error
     */
    @Test
    public void testQuery()
            throws IOException, OWLOntologyCreationException,
               OWLOntologyStorageException {
        OWLOntology ontology = loadOntology("/simple.owl");
        DatasetGraph dsg = QueryOperation.loadOntology(ontology);
        String query = "SELECT * WHERE { ?s ?p ?o }";
        ResultSet results = QueryOperation.execQuery(dsg, query);
        assertEquals(6, QueryOperation.countResults(results));
    }

}
