package org.obolibrary.robot;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
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
    Object jsonObject = loadJSON("/simple.json");
    boolean valid = ValidateOperation.validate(jsonObject, new PrintWriter(System.out));
    assertTrue(valid);
  }
}
