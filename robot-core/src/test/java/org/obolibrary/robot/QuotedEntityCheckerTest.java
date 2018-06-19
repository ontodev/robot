package org.obolibrary.robot;

import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * Test QuotedEntityChecker methods.
 *
 * @author <a href="mailto:rctauber@gmail.com">Bekcy Tauber</a>
 */
public class QuotedEntityCheckerTest extends CoreTest {

  /**
   * Test resolving a label from an import.
   *
   * @throws Exception on any issue.
   */
  @Test
  public void testImportLabel() throws Exception {
    OWLOntology ontology = loadOntologyWithCatalog("/import_test.owl");
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(new IOHelper());
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ontology);

    OWLEntity cls = checker.getOWLClass("test one");
    assertTrue(
        "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#test1"
            .equals(cls.getIRI().toString()));
  }
}
