package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.*;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests Template class and class methods.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class TemplateTest extends CoreTest {

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
    assertEquals("Count logical axioms", 3, template.getLogicalAxiomCount());
    assertEquals("Count all axioms", 11, template.getAxiomCount());
  }
}
