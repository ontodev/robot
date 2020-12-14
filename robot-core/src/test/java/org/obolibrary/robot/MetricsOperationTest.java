package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.obolibrary.robot.metrics.MetricsLabels;
import org.obolibrary.robot.metrics.MetricsResult;
import org.semanticweb.elk.io.FileUtils;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Test metrics operation.trea
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MetricsOperationTest extends CoreTest {

  String TESTONTOLOGY = "uberon.owl";

  /**
   * Tests essential metrics generation.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testEssentialMetrics() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "essential");
    assertEquals(313, result.getSimpleMetricValue(MetricsLabels.AXIOM_COUNT));
  }

  /**
   * Tests extended metrics generation.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testExtendedMetrics() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "extended");
    assertEquals("SI(D)", result.getSimpleMetricValue(MetricsLabels.EXPRESSIVITY));
  }

  /**
   * Tests all metrics generation.
   *
   * @throws IOException on IO error
   * @throws OWLOntologyStorageException on ontology error
   */
  @Test
  public void testAllMetrics() throws IOException, OWLOntologyStorageException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "all");
    assertEquals(0, result.getSimpleMetricValue(MetricsLabels.GCI_COUNT_INCL));
  }

  /**
   * Tests all metrics generation.
   *
   * @throws IOException on IO error
   */
  @Test
  public void writeAllMetricsToTSV() throws IOException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    OWLReasonerFactory rf = new ElkReasonerFactory();
    MetricsResult result = MetricsOperation.runMetrics(ontology, rf, "all-reasoner");
    File testFile = new File("test.tsv");
    MetricsOperation.maybeWriteResult(result, "tsv", testFile);
    assertTrue(testFile.exists());
    FileUtils.deleteRecursively(testFile);
  }

  /**
   * Tests all metrics generation.
   *
   * @throws IOException on IO error
   */
  @Test
  public void writeAllMetricsToCSV() throws IOException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "all");
    File testFile = new File("test.csv");
    MetricsOperation.maybeWriteResult(result, "csv", testFile);
    assertTrue(testFile.exists());
    FileUtils.deleteRecursively(testFile);
  }

  /**
   * Tests all metrics generation.
   *
   * @throws IOException on IO error
   */
  @Test
  public void writeAllMetricsToJSON() throws IOException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "all");
    File testFile = new File("test.json");
    MetricsOperation.maybeWriteResult(result, "json", testFile);
    assertTrue(testFile.exists());
    FileUtils.deleteRecursively(testFile);
  }

  /**
   * Tests all metrics generation.
   *
   * @throws IOException on IO error
   */
  @Test
  public void writeAllMetricsToYAML() throws IOException {
    OWLOntology ontology = loadOntology("/" + TESTONTOLOGY);
    MetricsResult result = MetricsOperation.runMetrics(ontology, "all");
    File testFile = new File("test.yml");
    MetricsOperation.maybeWriteResult(result, "yaml", testFile);
    assertTrue(testFile.exists());
    FileUtils.deleteRecursively(testFile);
  }
}
