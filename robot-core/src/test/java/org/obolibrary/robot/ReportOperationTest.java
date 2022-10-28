package org.obolibrary.robot;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.yaml.snakeyaml.error.YAMLException;

/** Tests for the ReportOperation. */
public class ReportOperationTest extends CoreTest {

  /** Test report produces correct JSON. */
  @Test
  public void testReportProducesValidJson() throws Exception {
    testReportProducesCorrectOutput("json");
  }

  /** Test report produces correct YAML. */
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
          IOUtils.toString(new FileInputStream(outputFile), Charset.defaultCharset()).trim();
      final InputStream expected =
          getClass().getResourceAsStream("/1016-report-json-failure/output." + extension);
      assert expected != null;
      final String expectedOutput =
          IOUtils.toString(expected, StandardCharsets.UTF_8.name()).trim();
      Assert.assertEquals(expectedOutput, output);
    } catch (YAMLException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
}
