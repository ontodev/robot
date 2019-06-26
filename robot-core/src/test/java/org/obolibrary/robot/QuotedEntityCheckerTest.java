package org.obolibrary.robot;

import static junit.framework.TestCase.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * Test QuotedEntityChecker methods.
 *
 * @author <a href="mailto:rctauber@gmail.com">Bekcy Tauber</a>
 */
public class QuotedEntityCheckerTest extends CoreTest {

  /** Test wrapping and escaping names. */
  @Test
  public void testEscaping() {
    Assert.assertEquals("testone", QuotedEntityChecker.wrap("testone"));
    Assert.assertEquals("'test one'", QuotedEntityChecker.wrap("test one"));
    Assert.assertEquals(
        "5-bromo-2\\'-deoxyuridine", QuotedEntityChecker.wrap("5-bromo-2'-deoxyuridine"));
  }

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
    if (cls != null) {
      assertEquals(
          "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#test1",
          cls.getIRI().toString());
    } else {
      throw new Exception("Class 'test one' does not exist.");
    }
  }

  /**
   * Test the QuotedEntityChecker.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testChecker() throws Exception {
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(simpleParts);

    IRI iri = IRI.create(base + "simple.owl#test1");
    OWLClass cls = dataFactory.getOWLClass(iri);
    Assert.assertEquals(cls, checker.getOWLClass("test one"));
    Assert.assertEquals(cls, checker.getOWLClass("'test one'"));
    Assert.assertEquals(cls, checker.getOWLClass("Test 1"));
    Assert.assertEquals(iri, checker.getIRI("test one", false));

    IOHelper ioHelper = new IOHelper();
    iri = ioHelper.createIRI("GO:XXXX");
    cls = dataFactory.getOWLClass(iri);
    checker.setIOHelper(ioHelper);
    Assert.assertEquals(cls, checker.getOWLClass("GO:XXXX"));

    System.out.println("PARSER");
    ManchesterOWLSyntaxClassExpressionParser parser =
        new ManchesterOWLSyntaxClassExpressionParser(
            dataFactory, checker
            // new org.semanticweb.owlapi.expression.ShortFormEntityChecker(
            //    new org.semanticweb.owlapi.util.
            //        BidirectionalShortFormProviderAdapter(
            //            ioHelper.getPrefixManager()))
            );
    // assertEquals(cls, parser.parse("'test one'"));
    Assert.assertEquals(cls, parser.parse("GO:XXXX"));
    // checker.add(cls, "%");
    // assertEquals("", parser.parse("%"));
  }
}
