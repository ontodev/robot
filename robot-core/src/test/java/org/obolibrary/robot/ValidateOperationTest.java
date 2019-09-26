package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ValidateOperationTest */
public class ValidateOperationTest extends CoreTest {

  /**
   * Test of the validation operation on a sample 'immune exposures' CSV file.
   *
   * @throws IOException
   */
  @Test
  public void testImmuneExposuresValidation() throws Exception, IOException {
    IOHelper ioHelper = new IOHelper();

    InputStream csvStream = this.getClass().getResourceAsStream("/immune_exposures.csv");
    assert (csvStream != null);
    List<List<String>> csvData = ioHelper.readCSV(csvStream);
    assert (csvData != null);

    InputStream owlStream = this.getClass().getResourceAsStream("/immune_exposures.owl");
    assert (owlStream != null);
    OWLOntology ontology = ioHelper.loadOntology(owlStream);
    assert (ontology != null);

    StringWriter writer = new StringWriter();
    OWLReasonerFactory reasonerFactory = new ReasonerFactory();
    ValidateOperation.validate(csvData, ontology, reasonerFactory, writer);

    String expectedPath = this.getClass().getResource("/immune_exposures-result.txt").getPath();
    assertNotEquals(expectedPath, "");
    String expectedResult = FileUtils.readFileToString(new File(expectedPath));
    assertEquals(writer.toString(), expectedResult);
  }
}
