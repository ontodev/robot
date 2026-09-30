package org.obolibrary.robot.providers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.obolibrary.robot.LanguagePreference;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * A {@link ShortFormProvider} that renders an entity using one of its annotation values (typically
 * an rdfs:label), choosing among multiple values with an ordered list of preferred language tags.
 *
 * <p>The actual selection rules (exact vs. cascade matches, deterministic tie-breaking, and the
 * fallback to any available value) live in {@link LanguagePreference}. This class is the OWL API
 * adapter: it gathers the candidate literal values for an entity across the preferred annotation
 * properties (respecting their priority order) and the imports closure, delegates the choice to
 * {@code LanguagePreference}, and falls back to an alternate short form provider only when the
 * entity has no such annotation at all &mdash; so entities that are genuinely unlabelled render
 * exactly as they did before (their IRI/CURIE).
 */
public class PreferredLanguageShortFormProvider implements ShortFormProvider {

  private final OWLOntologySetProvider ontologySetProvider;
  private final List<OWLAnnotationProperty> annotationProperties;
  private final List<String> preferredLanguages;
  private final ShortFormProvider alternateShortFormProvider;

  /**
   * Construct a preferred-language short form provider.
   *
   * @param ontologySetProvider provides the ontologies whose annotation axioms are searched (the
   *     imports closure of each is included)
   * @param annotationProperties the preferred annotation properties, highest priority first
   * @param preferredLanguages the preferred language tags, highest priority first (the empty string
   *     denotes "no language tag"); see {@link LanguagePreference#parse(String)}
   * @param alternateShortFormProvider used to render an entity that has none of the preferred
   *     annotation properties
   */
  public PreferredLanguageShortFormProvider(
      @Nonnull OWLOntologySetProvider ontologySetProvider,
      @Nonnull List<OWLAnnotationProperty> annotationProperties,
      @Nonnull List<String> preferredLanguages,
      @Nonnull ShortFormProvider alternateShortFormProvider) {
    this.ontologySetProvider = ontologySetProvider;
    this.annotationProperties = annotationProperties;
    this.preferredLanguages = preferredLanguages;
    this.alternateShortFormProvider = alternateShortFormProvider;
  }

  @Nonnull
  @Override
  public String getShortForm(@Nonnull OWLEntity entity) {
    List<LanguagePreference.Candidate> firstNonEmpty = null;
    // Visit the properties in order of preference. A preferred-language match on a higher-priority
    // property wins outright; otherwise the fallback uses the first property that has any value.
    for (OWLAnnotationProperty property : annotationProperties) {
      List<LanguagePreference.Candidate> candidates = getCandidates(entity, property);
      if (candidates.isEmpty()) {
        continue;
      }
      if (firstNonEmpty == null) {
        firstNonEmpty = candidates;
      }
      String preferred = LanguagePreference.selectPreferred(candidates, preferredLanguages);
      if (preferred != null) {
        return preferred;
      }
    }
    if (firstNonEmpty != null) {
      return LanguagePreference.selectFallback(firstNonEmpty);
    }
    // No label at all: preserve the pre-existing IRI/CURIE rendering.
    return alternateShortFormProvider.getShortForm(entity);
  }

  /**
   * Gather the literal values of the given annotation property on the entity, across all provided
   * ontologies and their imports closures.
   *
   * @param entity the entity to gather values for
   * @param property the annotation property to gather
   * @return the candidate literal values (possibly empty)
   */
  private List<LanguagePreference.Candidate> getCandidates(
      OWLEntity entity, OWLAnnotationProperty property) {
    List<LanguagePreference.Candidate> candidates = new ArrayList<>();
    for (OWLOntology ontology : ontologySetProvider.getOntologies()) {
      for (OWLOntology closure : ontology.getImportsClosure()) {
        for (OWLAnnotationAssertionAxiom axiom :
            closure.getAnnotationAssertionAxioms(entity.getIRI())) {
          if (!axiom.getProperty().equals(property)) {
            continue;
          }
          if (axiom.getValue() instanceof OWLLiteral) {
            OWLLiteral literal = (OWLLiteral) axiom.getValue();
            candidates.add(
                new LanguagePreference.Candidate(literal.getLang(), literal.getLiteral()));
          }
        }
      }
    }
    return candidates;
  }

  @Override
  public void dispose() {}
}
