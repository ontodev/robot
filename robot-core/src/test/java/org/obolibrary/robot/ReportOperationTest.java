package org.obolibrary.robot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.yaml.snakeyaml.error.YAMLException;

/** Tests for the ReportOperation. */
public class ReportOperationTest extends CoreTest {

  private static final String oboInOwl = "http://www.geneontology.org/formats/oboInOwl#";
  private static final String obo = "http://purl.obolibrary.org/obo/";

  /**
   * Test report produces correct JSON.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testReportProducesValidJson() throws Exception {
    testReportProducesCorrectOutput("json");
  }

  /**
   * Test report produces correct YAML.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testReportProducesValidYaml() throws Exception {
    testReportProducesCorrectOutput("yaml");
  }

  private void testReportProducesCorrectOutput(String extension) throws Exception {
    try {
      final OWLOntology ontology = loadOntology("/1016-report-json-failure/input.owl");
      final IOHelper iohelper = new IOHelper();
      final File outputFile =
          File.createTempFile("1016-report-json-failure-output", "." + extension);
      ReportOperation.report(ontology, iohelper, outputFile.toString(), Collections.emptyMap());
      final String output =
          IOUtils.toString(new FileInputStream(outputFile), Charset.defaultCharset())
              .trim()
              .replaceAll("\r\n", "\n");
      final InputStream expected =
          getClass().getResourceAsStream("/1016-report-json-failure/output." + extension);
      assert expected != null;
      final String expectedOutput =
          IOUtils.toString(expected, StandardCharsets.UTF_8.name()).trim().replaceAll("\r\n", "\n");
      Assert.assertEquals(expectedOutput, output);
    } catch (YAMLException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test that equivalent_class_axiom_no_genus reports a restriction on an inverse object property
   * instead of failing. See https://github.com/ontodev/robot/issues/1299.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testNoGenusWithInverseProperty() throws Exception {
    final OWLOntology ontology = loadOntology("/1299-inverse-property-no-genus.ttl");
    final File profile = File.createTempFile("1299-profile", ".txt");
    Files.write(profile.toPath(), "WARN\tequivalent_class_axiom_no_genus\n".getBytes());
    final File outputFile = File.createTempFile("1299-output", ".tsv");
    Map<String, String> options = new HashMap<>();
    options.put("profile", profile.toString());
    options.put("fail-on", "none");
    final boolean passed =
        ReportOperation.report(ontology, new IOHelper(), outputFile.toString(), options);
    Assert.assertTrue(passed);
    final String output = IOUtils.toString(new FileInputStream(outputFile), StandardCharsets.UTF_8);
    Assert.assertTrue(
        output,
        output.contains(
            "WARN\tequivalent_class_axiom_no_genus\turn:probe:C\tinverse(urn:probe:p)\t"
                + "http://www.w3.org/2002/07/owl#Thing"));
  }

  private OWLOntology generateOntologyWithLotsOfViolations() throws OWLOntologyCreationException {
    final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    final OWLDataFactory f = m.getOWLDataFactory();
    final OWLOntology o = m.createOntology(IRI.create(URI.create("https://example.org/o")));

    final OWLAnnotationProperty pLabel = f.getRDFSLabel();
    final OWLAnnotationProperty pHasExactSynonym =
        f.getOWLAnnotationProperty(IRI.create(oboInOwl + "hasExactSynonym"));
    final OWLAnnotationProperty pHasRelatedSynonym =
        f.getOWLAnnotationProperty(IRI.create(oboInOwl + "hasRelatedSynonym"));
    final OWLAnnotationProperty pIAO_0000115 =
        f.getOWLAnnotationProperty(IRI.create(obo + "IAO_0000115"));

    for (int i = 0; i < 5000; i++) {
      final IRI iriC = IRI.create("https://example.org/o/a" + i);
      m.addAxiom(o, f.getOWLAnnotationAssertionAxiom(pLabel, iriC, f.getOWLLiteral(" X\tX")));
      m.addAxiom(
          o, f.getOWLAnnotationAssertionAxiom(pLabel, iriC, f.getOWLLiteral("obsolete X\tX")));
      m.addAxiom(
          o, f.getOWLAnnotationAssertionAxiom(pHasExactSynonym, iriC, f.getOWLLiteral(" X\tX")));
      m.addAxiom(
          o, f.getOWLAnnotationAssertionAxiom(pHasRelatedSynonym, iriC, f.getOWLLiteral(" X\tX")));
      m.addAxiom(o, f.getOWLAnnotationAssertionAxiom(pIAO_0000115, iriC, f.getOWLLiteral("x")));
    }
    return o;
  }

  // disabling, as the test takes a few minutes to run
  // @Test
  public void testReportProducesCorrectOutput() throws Exception {
    try {
      final IOHelper iohelper = new IOHelper();
      final File outputFile = File.createTempFile("1070-codepoint-defaults-output", ".json");
      final OWLOntology o = generateOntologyWithLotsOfViolations();
      ReportOperation.report(o, iohelper, outputFile.toString(), Collections.emptyMap());
    } catch (YAMLException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
}
