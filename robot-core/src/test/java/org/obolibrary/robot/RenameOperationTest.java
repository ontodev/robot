package org.obolibrary.robot;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;

/** Tests for RenameOperation. */
public class RenameOperationTest extends CoreTest {

  /**
   * Test renaming of full IRIs.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testFullRename() throws Exception {
    OWLOntology ont = loadOntology("/simple.owl");
    Map<String, String> mappings = new HashMap<>();
    mappings.put(
        "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#test1",
        "http://foo.bar/test1");

    RenameOperation.renameFull(ont, new IOHelper(), mappings);

    assertIdentical("/rename_full.owl", ont);
  }

  /**
   * Test renaming of partial IRIs.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testPartialRename() throws Exception {
    OWLOntology ont = loadOntology("/simple.owl");
    Map<String, String> mappings = new HashMap<>();
    mappings.put(
        "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#",
        "http://foo.bar/");

    RenameOperation.renamePrefixes(ont, new IOHelper(), mappings);

    assertIdentical("/rename_partial.owl", ont);
  }
}
