package org.obolibrary.robot.providers;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.obolibrary.robot.LanguagePreference;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/** Tests for {@link PreferredLanguageShortFormProvider}. */
public class PreferredLanguageShortFormProviderTest {

  private static final String BASE = "http://example.org/";
  private static final OWLDataFactory DF = OWLManager.getOWLDataFactory();

  private final OWLClass dog = DF.getOWLClass(IRI.create(BASE + "dog"));
  private final OWLClass fox = DF.getOWLClass(IRI.create(BASE + "fox"));
  private final OWLClass cat = DF.getOWLClass(IRI.create(BASE + "cat"));

  private void addLabel(
      OWLOntologyManager m, OWLOntology o, OWLClass c, String value, String lang) {
    m.addAxiom(
        o,
        DF.getOWLAnnotationAssertionAxiom(
            DF.getRDFSLabel(), c.getIRI(), DF.getOWLLiteral(value, lang)));
  }

  /**
   * Build, in a fresh manager, an ontology in which "dog" has four labels (de, en, en-GB, and an
   * untagged one), "fox" has only a de and an en-GB label (no bare en), and "cat" has none.
   */
  private OWLOntology ontology() throws OWLOntologyCreationException {
    OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    OWLOntology o = m.createOntology(IRI.create(BASE + "lang.owl"));
    m.addAxiom(o, DF.getOWLDeclarationAxiom(dog));
    m.addAxiom(o, DF.getOWLDeclarationAxiom(fox));
    m.addAxiom(o, DF.getOWLDeclarationAxiom(cat));
    addLabel(m, o, dog, "Hund", "de");
    addLabel(m, o, dog, "dog", "en");
    addLabel(m, o, dog, "hound", "en-GB");
    // A plain (untagged) literal has an empty language tag.
    m.addAxiom(
        o,
        DF.getOWLAnnotationAssertionAxiom(
            DF.getRDFSLabel(), dog.getIRI(), DF.getOWLLiteral("plainlabel")));
    addLabel(m, o, fox, "Fuchs", "de");
    addLabel(m, o, fox, "fox (GB)", "en-GB");
    return o;
  }

  private PreferredLanguageShortFormProvider provider(String prefs)
      throws OWLOntologyCreationException {
    OWLOntology o = ontology();
    List<OWLAnnotationProperty> properties = Collections.singletonList(DF.getRDFSLabel());
    return new PreferredLanguageShortFormProvider(
        o.getOWLOntologyManager(),
        properties,
        LanguagePreference.parse(prefs),
        new SimpleShortFormProvider());
  }

  /** An exact language match wins, and beats an available cascade candidate. */
  @Test
  public void testExactPreferred() throws OWLOntologyCreationException {
    assertEquals("dog", provider("en,fr").getShortForm(dog));
    // "en" is exact for "dog"; it must not cascade to the en-GB "hound".
    assertEquals("dog", provider("en").getShortForm(dog));
  }

  /** A specific tag selects the matching regional label. */
  @Test
  public void testRegionalTag() throws OWLOntologyCreationException {
    assertEquals("hound", provider("en-GB").getShortForm(dog));
  }

  /** A broad preference cascades to a regional label when no exact match exists. */
  @Test
  public void testCascade() throws OWLOntologyCreationException {
    // "fox" has no bare-en label, so a preference of "en" cascades to its en-GB label.
    assertEquals("fox (GB)", provider("en").getShortForm(fox));
  }

  /** The no-lang token selects an untagged literal. */
  @Test
  public void testNoLangToken() throws OWLOntologyCreationException {
    assertEquals("plainlabel", provider("none").getShortForm(dog));
  }

  /** With no matching preferred language, fall back to the alphanumerically-first label. */
  @Test
  public void testFallbackToAnyLabel() throws OWLOntologyCreationException {
    // "es" matches nothing; among {Hund, dog, hound, plainlabel} the alpha-first is "Hund".
    assertEquals("Hund", provider("es").getShortForm(dog));
  }

  /** An entity with no label defers to the alternate provider (its short form / IRI fragment). */
  @Test
  public void testMissingLabelUsesAlternate() throws OWLOntologyCreationException {
    assertEquals("cat", provider("en,fr").getShortForm(cat));
  }
}
