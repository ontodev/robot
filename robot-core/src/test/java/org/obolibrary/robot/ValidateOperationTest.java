package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ValidateOperationTest */
public class ValidateOperationTest extends CoreTest {

  /**
   * Test of the parent-ancestor validation function
   *
   * @throws IOException
   */
  @Test
  public void testAncestorValidate() throws Exception, IOException {
    IOHelper ioHelper = new IOHelper();

    InputStream csvStream = this.getClass().getResourceAsStream("/nucleus.csv");
    assert (csvStream != null);
    List<List<String>> csvData = ioHelper.readCSV(csvStream);
    assert (csvData != null);

    InputStream owlStream = this.getClass().getResourceAsStream("/nucleus.owl");
    assert (owlStream != null);
    OWLOntology ontology = ioHelper.loadOntology(owlStream);
    assert (ontology != null);

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    StringWriter writer = new StringWriter();
    ValidateOperation.validate(csvData, ontology, reasonerFactory, "ID", "Parent IRI", "true", writer);

    String expectedPath =
        this.getClass().getResource("/nucleus-ancestor-validate-result.csv").getPath();
    assertNotEquals(expectedPath, "");
    String expectedResult =
        FileUtils.readFileToString(new File(expectedPath), StandardCharsets.UTF_8);
    assertEquals(writer.toString(), expectedResult);
  }
}
