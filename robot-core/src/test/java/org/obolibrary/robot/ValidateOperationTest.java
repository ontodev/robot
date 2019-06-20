package org.obolibrary.robot;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for ValidateOperationTest */
public class ValidateOperationTest extends CoreTest {

  /**
   * A dummy test
   *
   * @throws IOException
   */
  @Test
  public void testDummy() throws IOException {
    IOHelper ioHelper = new IOHelper();

    InputStream csvStream = this.getClass().getResourceAsStream("/template.csv");
    assert (csvStream != null);
    List<List<String>> csvData = ioHelper.readCSV(csvStream);
    assert (csvData != null);

    InputStream owlStream = this.getClass().getResourceAsStream("/template.owl");
    assert (owlStream != null);
    OWLOntology owlData = ioHelper.loadOntology(owlStream);
    assert (owlData != null);

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

    boolean valid =
        ValidateOperation.validate(csvData, owlData, reasonerFactory, new PrintWriter(System.out));
    assertTrue(valid);
  }
}
