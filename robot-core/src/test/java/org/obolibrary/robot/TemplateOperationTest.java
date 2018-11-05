package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.*;
import org.junit.Test;
import org.obolibrary.robot.template.Template;
import org.obolibrary.robot.template.TemplateHelper;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;

/** Tests for TemplateOperation. */
public class TemplateOperationTest extends CoreTest {
  /** Test wrapping and escaping names. */
  @Test
  public void testEscaping() {
    assertEquals("testone", QuotedEntityChecker.wrap("testone"));
    assertEquals("'test one'", QuotedEntityChecker.wrap("test one"));
    assertEquals("5-bromo-2\\'-deoxyuridine", QuotedEntityChecker.wrap("5-bromo-2'-deoxyuridine"));
  }

  /**
   * Test annotation templates.
   *
   * @throws Exception if annotation properties cannot be found
   */
  @Test
  public void testTemplateStrings() throws Exception {
    Set<OWLAnnotation> anns;
    OWLAnnotation ann = null;
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(new IOHelper());

    anns = TemplateHelper.getStringAnnotations(checker, "A rdfs:label", null, "bar");
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"bar\"^^xsd:string)", ann.toString());

    anns = TemplateHelper.getTypedAnnotations(checker, "AT rdfs:label^^xsd:integer", null, "1");
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"1\"^^xsd:integer)", ann.toString());

    anns = TemplateHelper.getLanguageAnnotations(checker, "AL rdfs:label@en", null, "bar");
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"bar\"@en)", ann.toString());

    ann = TemplateHelper.getIRIAnnotation(checker, "AI rdfs:label", IRI.create("http://bar.com"));
    assertEquals("Annotation(rdfs:label <http://bar.com>)", ann.toString());
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
    assertEquals(cls, checker.getOWLClass("test one"));
    assertEquals(cls, checker.getOWLClass("'test one'"));
    assertEquals(cls, checker.getOWLClass("Test 1"));

    IOHelper ioHelper = new IOHelper();
    iri = ioHelper.createIRI("GO:XXXX");
    cls = dataFactory.getOWLClass(iri);
    checker.setIOHelper(ioHelper);
    assertEquals(cls, checker.getOWLClass("GO:XXXX"));

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
    assertEquals(cls, parser.parse("GO:XXXX"));
    // checker.add(cls, "%");
    // assertEquals("", parser.parse("%"));
  }

  /**
   * Test templating.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testTemplateCSV() throws Exception {
    String path = "/template.csv";
    List<List<String>> rows = TemplateHelper.readCSV(this.getClass().getResourceAsStream(path));
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");

    Template t = new Template(path, rows, simpleParts);
    OWLOntology template = t.generateOutputOntology("http://test.com/template.owl");
    assertIdentical("/template.owl", template);
  }

  /**
   * Test multiple templates.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testTemplates() throws Exception {
    Map<String, List<List<String>>> tables = new LinkedHashMap<>();
    String path = "/template-ids.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    path = "/template-labels.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    path = "/template.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");

    List<OWLOntology> ontologies = new ArrayList<>();
    for (String table : tables.keySet()) {
      Template t = new Template(table, tables.get(table), simpleParts);
      ontologies.add(t.generateOutputOntology());
    }

    OWLOntology template = MergeOperation.merge(ontologies);
    for (OWLAxiom cls : template.getAxioms()) {
      System.out.println(cls);
    }
    assertEquals("Count classes", 4, template.getClassesInSignature().size());
    assertEquals("Count logical axioms", 4, template.getLogicalAxiomCount());
    assertEquals("Count all axioms", 12, template.getAxiomCount());
  }
}
