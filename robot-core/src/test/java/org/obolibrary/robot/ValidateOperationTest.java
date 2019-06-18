package org.obolibrary.robot;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import org.junit.Test;

/** Tests for ValidateOperationTest */
public class ValidateOperationTest extends CoreTest {

  /**
   * A dummy test
   *
   * @throws IOException
   */
  @Test
  public void testDummy() throws IOException {
    InputStream csvStream = this.getClass().getResourceAsStream("/template.csv");
    assert (csvStream != null);
    List<List<String>> csvData = IOHelper.readCSV(csvStream);
    boolean valid = ValidateOperation.validate(csvData, new PrintWriter(System.out));
    assertTrue(valid);
  }
}
