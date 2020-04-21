package org.obolibrary.robot.providers;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * A custom AnnotationValueShortFormProvider that surrounds multi-word values with single quotes
 * when returning the short form when quoting is true.
 */
public class QuotedAnnotationValueShortFormProvider extends AnnotationValueShortFormProvider {

  private boolean quoting = false;

  /**
   * Constructs a quoted annotation value short form provider.
   *
   * @param ontologySetProvider set of ontologies to provide axioms
   * @param alternateShortFormProvider short form provider to generate short forms for entities that
   *     do not have the given annotation
   * @param alternateIRIShortFormProvider IRI short form provider
   * @param annotationProperties preferred annotation properties, with the properties at the start
   *     of the list taking priority over those at the end
   * @param preferredLanguageMap a map of annotation properties to preferred language
   */
  public QuotedAnnotationValueShortFormProvider(
      @Nonnull OWLOntologySetProvider ontologySetProvider,
      @Nonnull ShortFormProvider alternateShortFormProvider,
      @Nonnull IRIShortFormProvider alternateIRIShortFormProvider,
      @Nonnull List<OWLAnnotationProperty> annotationProperties,
      @Nonnull Map<OWLAnnotationProperty, List<String>> preferredLanguageMap) {
    super(
        ontologySetProvider,
        alternateShortFormProvider,
        alternateIRIShortFormProvider,
        annotationProperties,
        preferredLanguageMap);
  }

  /** Turn quoting on or off. */
  public void toggleQuoting() {
    quoting = !quoting;
  }

  /**
   * Given an OWLEntity, return the short form based on annotation properties. If the annotation
   * does not exist, use the alternateShortFormProvider. Maybe surround the short form in single
   * quotes if there is more than one word.
   *
   * @param entity OWLEntity to get short form of
   * @return short form, maybe quoted
   */
  @Nonnull
  public String getShortForm(@Nonnull OWLEntity entity) {
    String shortForm = super.getShortForm(entity);
    if (shortForm.contains(" ") && quoting) {
      return String.format("'%s'", shortForm);
    }
    return shortForm;
  }
}
