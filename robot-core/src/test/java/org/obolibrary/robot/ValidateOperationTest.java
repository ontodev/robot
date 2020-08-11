package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   * @throws IOException on any problem
   */
  @Test
  public void testImmuneExposuresValidation() throws Exception {
    IOHelper ioHelper = new IOHelper();

    InputStream tableStream = this.getClass().getResourceAsStream("/immune_exposures.csv");
    assert (tableStream != null);
    List<List<String>> tableData = IOHelper.readCSV(tableStream);
    assert (tableData != null);

    URL res = this.getClass().getResource("/immune_exposures.csv");
    File file = Paths.get(res.toURI()).toFile();
    String tablePath = file.getAbsolutePath();

    Map<String, List<List<String>>> tables = new HashMap<>();
    tables.put(tablePath, tableData);

    InputStream owlStream = this.getClass().getResourceAsStream("/immune_exposures.owl");
    assert (owlStream != null);
    OWLOntology ontology = ioHelper.loadOntology(owlStream);
    assert (ontology != null);

    // Redirect STDOUT to an OutputStream wrapped in a PrintStream:
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    PrintStream prStream = new PrintStream(outStream);
    System.setOut(prStream);

    // Call validate() with an outputPath of null to send output to STDOUT:
    OWLReasonerFactory reasonerFactory = new ReasonerFactory();
    ValidateOperation.validate(tables, ontology, ioHelper, reasonerFactory, null);

    // Compare the output with the contents of a file in the resources directory which contains
    // the output we expect to get:
    String fileWithExpectedContents =
        this.getClass().getResource("/immune_exposures-result.txt").getPath();
    assertNotEquals(fileWithExpectedContents, "");
    String expectedResult = FileUtils.readFileToString(new File(fileWithExpectedContents));

    assertEquals(outStream.toString(), expectedResult);
  }
}
