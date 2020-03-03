package org.obolibrary.robot.providers;

import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * A short form provider that returns full IRIs.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class IRIValueShortFormProvider implements ShortFormProvider {

  /** Create a new IRIValueShortFormProvider */
  public IRIValueShortFormProvider() {}

  /**
   * Return the IRI of the entity as the short form.
   *
   * @param entity OWLEntity to get short form of
   * @return String IRI
   */
  @Nonnull
  @Override
  public String getShortForm(@Nonnull OWLEntity entity) {
    return entity.getIRI().toString();
  }

  /** Dispose of the short form provider. */
  public void dispose() {}
}
