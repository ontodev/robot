package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.obolibrary.robot.report.ReportCard;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/** Tests for ReportOperation. */
public class ReportOperationTest extends CoreTest {

  /**
   * Test reporting on an ontology
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testReport() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/need-of-repair.owl");
    IOHelper iohelper = new IOHelper();
    ReportCard reportCard = ReportOperation.report(ontology, iohelper);
    assertEquals(1, reportCard.problemsReport.invalidReferenceViolations.size());
    assertEquals(4, reportCard.problemsReport.classMetadataViolations.size());
    writeReport( new ObjectMapper(new JsonFactory()),
        reportCard);
    writeReport( new ObjectMapper(new YAMLFactory()),
        reportCard);
  }
  
  /**
   * See https://github.com/owlcs/owlapi/issues/317#issuecomment-359025467
   * 
   * https://github.com/Phenomics/hpo-obo-qc/issues/1
   * 
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testOboAltIds() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/obo_altid_test.obo");
    IOHelper iohelper = new IOHelper();
    ReportCard reportCard = ReportOperation.report(ontology, iohelper);
    assertEquals(1, reportCard.problemsReport.invalidReferenceViolations.size());
    writeReport( new ObjectMapper(new YAMLFactory()),
        reportCard);
  }

  /**
   * 
   * 
   * @throws IOException
   * @throws OWLOntologyCreationException
   */
  @Test
  public void testCURIEchecks() throws IOException, OWLOntologyCreationException {
    OWLOntology ontology = loadOntology("/bad_dbxref_test.obo");
    IOHelper iohelper = new IOHelper();
    ReportCard reportCard = ReportOperation.report(ontology, iohelper);
    writeReport( new ObjectMapper(new YAMLFactory()),
        reportCard);
    assertEquals(3, reportCard.problemsReport.curieViolations.size());
  }
  
  public void writeReport(ObjectMapper mapper, ReportCard reportCard) throws JsonProcessingException {
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    System.out.println(writer.writeValueAsString(reportCard));
  }
  
}
