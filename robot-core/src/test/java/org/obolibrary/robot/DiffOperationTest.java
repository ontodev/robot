package org.obolibrary.robot;

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests for DiffOperation. */
public class DiffOperationTest extends CoreTest {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(DiffOperationTest.class);

  /**
   * Compare one ontology to itself.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testCompareIdentical() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    assertIdentical(simple, simple);
  }

  /**
   * Compare one ontology to a modified copy.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testCompareModified() throws IOException, OWLOntologyCreationException {
    OWLOntology simple = loadOntology("/simple.owl");
    Set<OWLOntology> onts = new HashSet<>();
    onts.add(simple);
    OWLOntologyManager manager = simple.getOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    OWLOntology simple1 = manager.createOntology(IRI.create(base + "simple1.owl"), onts);
    IRI test1 = IRI.create(base + "simple.owl#test1");
    manager.addAxiom(
        simple1,
        df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), test1, df.getOWLLiteral("TEST #1")));

    StringWriter writer = new StringWriter();
    boolean actual = DiffOperation.compare(simple, simple1, writer);
    logger.debug(writer.toString());
    assertFalse(actual);
    String expected =
        IOUtils.toString(
            this.getClass().getResourceAsStream("/simple1.diff"), Charset.defaultCharset());
    assertEquals(expected, writer.toString());
  }

  /**
   * Compare one ontology to a modified copy with labels in output.
   *
   * @throws IOException on file problem
   */
  @Test
  public void testCompareModifiedWithLabels() throws IOException {
    OWLOntology simple = loadOntology("/simple.owl");
    OWLOntology elk = loadOntology("/simple_elk.owl");

    StringWriter writer = new StringWriter();
    Map<String, String> options = new HashMap<>();
    options.put("labels", "true");
    boolean actual = DiffOperation.compare(simple, elk, new IOHelper(), writer, options);
    logger.debug(writer.toString());
    assertFalse(actual);
    String expected =
        IOUtils.toString(
            this.getClass().getResourceAsStream("/simple.diff"), Charset.defaultCharset());
    assertEquals(expected, writer.toString());
  }

  /**
   * OWL API ontology equality only compares the ontology ID. This test confirms this and verifies
   * that we can use an identity-based set for collections of ontologies when needed.
   *
   * @throws OWLOntologyCreationException if test ontology cannot be created
   */
  @Test
  public void testOntologyEquality() throws OWLOntologyCreationException {
    OWLDataFactory f = OWLManager.getOWLDataFactory();
    OWLOntologyManager mgr1 = OWLManager.createOWLOntologyManager();
    OWLOntologyManager mgr2 = OWLManager.createOWLOntologyManager();
    OWLClass a = f.getOWLClass(IRI.create("http://example.org/A"));
    OWLClass b = f.getOWLClass(IRI.create("http://example.org/B"));
    OWLOntology ont1 =
        mgr1.createOntology(
            Collections.singleton(f.getOWLDeclarationAxiom(a)),
            IRI.create("http://example.org/ontology"));
    OWLOntology ont2 =
        mgr2.createOntology(
            Collections.singleton(f.getOWLDeclarationAxiom(b)),
            IRI.create("http://example.org/ontology"));
    Set<OWLOntology> normalSet = new HashSet<>();
    Set<OWLOntology> identitySet = Sets.newIdentityHashSet();
    normalSet.add(ont1);
    normalSet.add(ont2);
    identitySet.add(ont1);
    identitySet.add(ont2);
    assertEquals(1, normalSet.size());
    assertEquals(2, identitySet.size());
  }
}
