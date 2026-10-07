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
                this.getClass().getResourceAsStream("/simple1.diff"), Charset.defaultCharset())
            .replaceAll("\r\n", "\n");
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
                this.getClass().getResourceAsStream("/simple.diff"), Charset.defaultCharset())
            .replaceAll("\r\n", "\n");
    assertEquals(expected, writer.toString());
  }

  /**
   * Compare two ontologies with the pretty format and a language preference, confirming the option
   * is threaded through to the label short form provider.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testCompareWithLanguagePreference() throws IOException, OWLOntologyCreationException {
    String base = "http://example.org/";
    OWLClass dog = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(base + "dog"));
    OWLClass puppy = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(base + "puppy"));

    OWLOntology left = buildLangOntology(base + "lang-left.owl", dog, puppy, true);
    OWLOntology right = buildLangOntology(base + "lang-right.owl", dog, puppy, false);

    // German preference selects "Hund".
    StringWriter deWriter = new StringWriter();
    Map<String, String> deOptions = new HashMap<>();
    deOptions.put("format", "pretty");
    deOptions.put(LanguagePreference.OPTION_NAME, "de");
    DiffOperation.compare(left, right, new IOHelper(), deWriter, deOptions);
    String deOutput = deWriter.toString();
    assertTrue("expected German label in output:\n" + deOutput, deOutput.contains("Hund"));
    assertFalse("did not expect English label:\n" + deOutput, deOutput.contains("Canine"));

    // English preference selects "Canine".
    StringWriter enWriter = new StringWriter();
    Map<String, String> enOptions = new HashMap<>();
    enOptions.put("format", "pretty");
    enOptions.put(LanguagePreference.OPTION_NAME, "en");
    DiffOperation.compare(left, right, new IOHelper(), enWriter, enOptions);
    String enOutput = enWriter.toString();
    assertTrue("expected English label in output:\n" + enOutput, enOutput.contains("Canine"));
    assertFalse("did not expect German label:\n" + enOutput, enOutput.contains("Hund"));
  }

  /**
   * Confirm that a language preference is honored when labels are requested via {@code --labels
   * true} (with the default "plain" format) rather than by explicitly selecting the "pretty"
   * format. Internally {@code labels=true} upgrades "plain" to "pretty", so the language preference
   * must take effect the same way.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   */
  @Test
  public void testCompareWithLanguagePreferenceViaLabelsOption()
      throws IOException, OWLOntologyCreationException {
    String base = "http://example.org/";
    OWLClass dog = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(base + "dog"));
    OWLClass puppy = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(base + "puppy"));

    OWLOntology left = buildLangOntology(base + "lang-left.owl", dog, puppy, true);
    OWLOntology right = buildLangOntology(base + "lang-right.owl", dog, puppy, false);

    // Request labels with --labels true (no explicit --format); German preference selects "Hund".
    StringWriter deWriter = new StringWriter();
    Map<String, String> deOptions = new HashMap<>();
    deOptions.put("labels", "true");
    deOptions.put(LanguagePreference.OPTION_NAME, "de");
    DiffOperation.compare(left, right, new IOHelper(), deWriter, deOptions);
    String deOutput = deWriter.toString();
    assertTrue("expected German label in output:\n" + deOutput, deOutput.contains("Hund"));
    assertFalse("did not expect English label:\n" + deOutput, deOutput.contains("Canine"));

    // The same input with an English preference selects "Canine".
    StringWriter enWriter = new StringWriter();
    Map<String, String> enOptions = new HashMap<>();
    enOptions.put("labels", "true");
    enOptions.put(LanguagePreference.OPTION_NAME, "en");
    DiffOperation.compare(left, right, new IOHelper(), enWriter, enOptions);
    String enOutput = enWriter.toString();
    assertTrue("expected English label in output:\n" + enOutput, enOutput.contains("Canine"));
    assertFalse("did not expect German label:\n" + enOutput, enOutput.contains("Hund"));
  }

  /**
   * Build a small ontology whose "dog" class carries a German and an English label. When {@code
   * withSubClass} is true, "puppy" is asserted to be a subclass of "dog" (the axiom that will
   * differ between the two ontologies).
   */
  private OWLOntology buildLangOntology(
      String iri, OWLClass dog, OWLClass puppy, boolean withSubClass)
      throws OWLOntologyCreationException {
    OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    OWLDataFactory df = m.getOWLDataFactory();
    OWLOntology o = m.createOntology(IRI.create(iri));
    m.addAxiom(o, df.getOWLDeclarationAxiom(dog));
    m.addAxiom(o, df.getOWLDeclarationAxiom(puppy));
    m.addAxiom(
        o,
        df.getOWLAnnotationAssertionAxiom(
            df.getRDFSLabel(), dog.getIRI(), df.getOWLLiteral("Hund", "de")));
    m.addAxiom(
        o,
        df.getOWLAnnotationAssertionAxiom(
            df.getRDFSLabel(), dog.getIRI(), df.getOWLLiteral("Canine", "en")));
    if (withSubClass) {
      m.addAxiom(o, df.getOWLSubClassOfAxiom(puppy, dog));
    }
    return o;
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
