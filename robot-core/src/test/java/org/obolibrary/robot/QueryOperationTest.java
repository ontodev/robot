package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Test query operation.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
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
      throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/simple.owl");
    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    String query = "SELECT * WHERE { ?s ?p ?o }";
    ResultSet results = QueryOperation.execQuery(dsg, query);
    assertEquals(6, QueryOperation.countResults(results));
  }

  /**
   * Tests a construct query.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testConstruct() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/bot.owl");
    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    String query =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX part_of: <http://purl.obolibrary.org/obo/BFO_0000050>\n"
            + "CONSTRUCT {\n"
            + "    ?part part_of: ?whole\n"
            + "}\n"
            + "WHERE {\n"
            + "    ?part rdfs:subClassOf [\trdf:type owl:Restriction ;\n"
            + "\t\t\t\t\t\towl:onProperty part_of: ;\n"
            + "\t\t\t\t\t\towl:someValuesFrom ?whole ]\n"
            + "}";
    Model model = QueryOperation.execConstruct(dsg, query);
    Resource s = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000062");
    Property p = ResourceFactory.createProperty("http://purl.obolibrary.org/obo/BFO_0000050");
    RDFNode o = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000467");
    assertTrue(model.contains(s, p, o));
  }

  @Test
  public void testExecVerifyWithViolations() throws Exception {

    OWLOntology ontology = loadOntology("/simple.owl");
    DatasetGraph graph = QueryOperation.loadOntology(ontology);
    String allViolations =
        "SELECT ?s ?p ?o\n" + "WHERE {\n" + "    ?s ?p ?o .\n" + "}\n" + "LIMIT 10";
    ResultSet resultSet = QueryOperation.execQuery(graph, allViolations);
    ResultSetRewindable copy = ResultSetFactory.copyResults(resultSet);
    Map<File, Tuple<ResultSetRewindable, OutputStream>> testResults = new HashMap<>();
    ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    testResults.put(new File("/path/to/rule.sparql"), new Tuple<>(copy, testOut));
    boolean violations = QueryOperation.execVerify(testResults);

    assertTrue(violations);
    assertEquals(7, Lists.newArrayList(testOut.toString().split("\n")).size());
  }

  @Test
  public void testExecVerifyNoViolations() throws Exception {
    OWLOntology ontology = loadOntology("/simple.owl");
    DatasetGraph graph = QueryOperation.loadOntology(ontology);
    String allViolations = "SELECT ?s ?p ?o\n" + "WHERE {\n" + "    \n" + "}\n" + "LIMIT 0";
    ResultSet resultSet = QueryOperation.execQuery(graph, allViolations);
    ResultSetRewindable copy = ResultSetFactory.copyResults(resultSet);
    Map<File, Tuple<ResultSetRewindable, OutputStream>> testResults = new HashMap<>();
    ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    testResults.put(new File("/path/to/rule.sparql"), new Tuple<>(copy, testOut));
    boolean violations = QueryOperation.execVerify(testResults);

    assertFalse(violations);
    assertEquals(1, Lists.newArrayList(testOut.toString().split("\n")).size());
  }
}
