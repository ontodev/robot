package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.util.*;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * Tests Template class and class methods.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class TemplateTest extends CoreTest {

  /**
   * Test legacy templating.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testTemplateCSV() throws Exception {
    String path = "/template.csv";
    List<List<String>> rows = TemplateHelper.readCSV(this.getClass().getResourceAsStream(path));
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");

    Template t = new Template(path, rows, simpleParts);
    OWLOntology template = t.generateOutputOntology("http://test.com/template.owl", false, null);
    assertIdentical("/template.owl", template);
  }

  /**
   * Test legacy templating.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testLegacyTemplateCSV() throws Exception {
    String path = "/legacy-template.csv";
    List<List<String>> rows = TemplateHelper.readCSV(this.getClass().getResourceAsStream(path));
    OWLOntology simpleParts = loadOntology("/simple_parts.owl");

    Template t = new Template(path, rows, simpleParts);
    OWLOntology template = t.generateOutputOntology("http://test.com/template.owl", false, null);
    assertIdentical("/template.owl", template);
  }

  /**
   * Test a strange case with no labels and no types.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testNoLabelsNoTypes() throws Exception {
    String path = "/sequence-template.csv";
    List<List<String>> rows = TemplateHelper.readCSV(this.getClass().getResourceAsStream(path));
    Template t = new Template(path, rows);
    t.generateOutputOntology("http://test.com/template.owl", false, null);
  }

  /**
   * Test a case with no labels.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testNoLabels() throws Exception {
    String path = "/workflow-template.csv";
    List<List<String>> rows = TemplateHelper.readCSV(this.getClass().getResourceAsStream(path));
    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix("ex", "http://example.com/");
    Template t = new Template(path, rows, ioHelper);
    OWLOntology template = t.generateOutputOntology("http://test.com/template.owl", false, null);
    assertIdentical("/workflow-template.ttl", template);
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
    path = "/template-logical.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    OWLOntology in = loadOntology("/simple_parts.owl");

    List<OWLOntology> ontologies = new ArrayList<>();
    OWLOntology out;
    for (String table : tables.keySet()) {
      Template t = new Template(table, tables.get(table), in);
      out = t.generateOutputOntology();
      ontologies.add(out);
      in = MergeOperation.merge(Lists.newArrayList(in, out));
    }

    OWLOntology template = MergeOperation.merge(ontologies);
    assertEquals("Count classes", 4, template.getClassesInSignature().size());
    assertEquals("Count logical axioms", 3, template.getLogicalAxiomCount());
    assertEquals("Count all axioms", 11, template.getAxiomCount());
  }

  /**
   * Test multiple templates in legacy format.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testLegacyTemplates() throws Exception {
    Map<String, List<List<String>>> tables = new LinkedHashMap<>();
    String path = "/template-ids.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    path = "/template-labels.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    path = "/legacy-template-logical.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));
    OWLOntology in = loadOntology("/simple_parts.owl");

    List<OWLOntology> ontologies = new ArrayList<>();
    OWLOntology out;
    for (String table : tables.keySet()) {
      Template t = new Template(table, tables.get(table), in);
      out = t.generateOutputOntology();
      ontologies.add(out);
      in = MergeOperation.merge(Lists.newArrayList(in, out));
    }

    OWLOntology template = MergeOperation.merge(ontologies);
    assertEquals("Count classes", 4, template.getClassesInSignature().size());
    assertEquals("Count logical axioms", 3, template.getLogicalAxiomCount());
    assertEquals("Count all axioms", 11, template.getAxiomCount());
  }

  /**
   * Test legacy doc template example.
   *
   * @throws Exception on any issue
   */
  @Test
  public void testLegacyDocsTemplate() throws Exception {
    Map<String, String> options = TemplateOperation.getDefaultOptions();
    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix("ex", "http://example.com/");

    Map<String, List<List<String>>> tables = new LinkedHashMap<>();
    String path = "/docs-template.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));

    OWLOntology template = TemplateOperation.template(null, ioHelper, tables, options);

    OntologyHelper.setOntologyIRI(
        template, IRI.create("https://github.com/ontodev/robot/examples/template.owl"), null);
    assertIdentical("/docs-template.owl", template);
  }

  /**
   * Test parsing a label that contains single quotes.
   *
   * @throws Exception on problem parsing
   */
  @Test
  public void testParseClass() throws Exception {
    OWLDataFactory df = OWLManager.getOWLDataFactory();
    String label = "Streptococcus sp. 'group B'";
    OWLClass expectedClass =
        df.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/NCBITaxon_1319"));

    // Create the checker
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.add(expectedClass, label);

    // Create the parser
    ManchesterOWLSyntaxClassExpressionParser parser =
        new ManchesterOWLSyntaxClassExpressionParser(df, checker);

    OWLClass actualClass =
        TemplateHelper.tryParse("test", checker, parser, label, 0, 0).asOWLClass();
    assert actualClass.getIRI().toString().equals(expectedClass.getIRI().toString());
  }

  /**
   * Test named individual with split property.
   *
   * @throws Exception if entities cannot be found
   */
  @Test
  public void testNamedIndividualSplit() throws Exception {
    Map<String, String> options = TemplateOperation.getDefaultOptions();
    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix("ex", "http://example.com/");

    Map<String, List<List<String>>> tables = new LinkedHashMap<>();
    String path = "/template-individual-split.csv";
    tables.put(path, TemplateHelper.readCSV(this.getClass().getResourceAsStream(path)));

    OWLOntology ontology = TemplateOperation.template(null, ioHelper, tables, options);

    assertEquals("Count individuals", 1, ontology.getIndividualsInSignature().size());
    OWLNamedIndividual namedIndividual = ontology.getIndividualsInSignature().iterator().next();
    assertEquals(
        "Count annotation properties", 1, ontology.getAnnotationPropertiesInSignature().size());
    OWLAnnotationProperty annotationProperty =
        ontology.getAnnotationPropertiesInSignature().iterator().next();
    assertEquals(
        "Count annotation properties of individual",
        2,
        EntitySearcher.getAnnotationObjects(namedIndividual, ontology, annotationProperty).size());
  }
}
