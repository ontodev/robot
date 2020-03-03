package org.obolibrary.robot.providers;

import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * Implementation of ShortFormProvider that always returns an empty string. For use as an
 * alternative short form provider for the LABEL case in export, where nothing should be returned if
 * a label does not exist.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class EmptyShortFormProvider implements ShortFormProvider {

  /**
   * Return an empty string as the short form of an entity.
   *
   * @param owlEntity OWLEntity to get short form of
   * @return empty string
   */
  @Nonnull
  @Override
  public String getShortForm(@Nonnull OWLEntity owlEntity) {
    return "";
  }

  /** Dispose of the provider. */
  @Override
  public void dispose() {}
}
