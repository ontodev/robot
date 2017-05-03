package org.obolibrary.robot;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.rdf.model.*;
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

    @Test
    public void testConstruct() throws IOException, OWLOntologyStorageException {
        OWLOntology ontology = loadOntology("/bot.owl");
        DatasetGraph dsg = QueryOperation.loadOntology(ontology);
        String query =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX part_of: <http://purl.obolibrary.org/obo/BFO_0000050>\n" +
            "CONSTRUCT {\n" +
            "    ?part part_of: ?whole\n" +
            "}\n" +
            "WHERE {\n" +
            "    ?part rdfs:subClassOf [\trdf:type owl:Restriction ;\n" +
            "\t\t\t\t\t\towl:onProperty part_of: ;\n" +
            "\t\t\t\t\t\towl:someValuesFrom ?whole ]\n" +
            "}";
        Model model = QueryOperation.execConstruct(dsg, query);
        Resource s = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000062");
        Property p = ResourceFactory.createProperty("http://purl.obolibrary.org/obo/BFO_0000050");
        RDFNode o = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000467");
        assertTrue(model.contains(s, p, o));
    }

}
