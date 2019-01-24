package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.io.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
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
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testQuery() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/simple.owl");
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology);
    String query = "SELECT * WHERE { ?s ?p ?o }";
    ResultSet results = QueryOperation.execQuery(dataset, query);
    assertEquals(6, QueryOperation.countResults(results));
  }

  /**
   * Tests a simple query with imports loaded.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testQueryWithDefaultGraph() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntologyWithCatalog("/import_test.owl");
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, true);
    String query =
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "SELECT * WHERE {\n"
            + "  ?s ?p ?o .\n"
            + "  FILTER NOT EXISTS { ?s ?p owl:Ontology }\n"
            + "}";
    ResultSet results = QueryOperation.execQuery(dataset, query);
    assertEquals(6, QueryOperation.countResults(results));
  }

  /**
   * Tests a simple query on a named graph.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testQueryWithNamedGraph() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntologyWithCatalog("/import_test.owl");
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, true);
    String query =
        "PREFIX robot: <https://github.com/ontodev/robot/robot-core/src/test/resources/>\n"
            + "SELECT * FROM NAMED robot:simple.owl WHERE {?s ?p ?o}";
    ResultSet results = QueryOperation.execQuery(dataset, query);
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
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology);
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
    Model model = QueryOperation.execConstruct(dataset, query);
    Resource s = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000062");
    Property p = ResourceFactory.createProperty("http://purl.obolibrary.org/obo/BFO_0000050");
    RDFNode o = ResourceFactory.createResource("http://purl.obolibrary.org/obo/UBERON_0000467");
    assertTrue(model.contains(s, p, o));
  }

  /**
   * Tests a verify with violations.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testExecVerifyWithViolations() throws IOException, OWLOntologyStorageException {

    OWLOntology ontology = loadOntology("/simple.owl");
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology);
    String allViolations =
        "SELECT ?s ?p ?o\n" + "WHERE {\n" + "    ?s ?p ?o .\n" + "}\n" + "LIMIT 10";

    ResultSet resultSet = QueryOperation.execQuery(dataset, allViolations);
    ResultSetRewindable copy = ResultSetFactory.copyResults(resultSet);
    int resultSize = copy.size();
    copy.reset();

    ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    QueryOperation.writeResult(copy, Lang.CSV, testOut);

    boolean violations = false;
    if (resultSize > 0) {
      violations = true;
    }

    assertTrue(violations);
    assertEquals(7, Lists.newArrayList(testOut.toString().split("\n")).size());
  }

  /**
   * Tests a verify with no violations.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testExecVerifyNoViolations() throws IOException, OWLOntologyStorageException {

    OWLOntology ontology = loadOntology("/simple.owl");
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology);
    String allViolations = "SELECT ?s ?p ?o\n" + "WHERE {\n" + "    \n" + "}\n" + "LIMIT 0";

    ResultSet resultSet = QueryOperation.execQuery(dataset, allViolations);
    ResultSetRewindable copy = ResultSetFactory.copyResults(resultSet);
    int resultSize = copy.size();
    copy.reset();

    ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    QueryOperation.writeResult(copy, Lang.CSV, testOut);

    boolean violations = false;
    if (resultSize > 0) {
      violations = true;
    }

    assertFalse(violations);
    assertEquals(1, Lists.newArrayList(testOut.toString().split("\n")).size());
  }
}
