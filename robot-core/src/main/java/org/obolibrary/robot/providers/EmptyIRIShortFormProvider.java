package org.obolibrary.robot.providers;

import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.IRIShortFormProvider;

/**
 * Implementation of IRIShortFormProvider that always returns an empty string. For use as an
 * alternative short form provider for the LABEL case in export, where nothing should be returned if
 * a label does not exist.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class EmptyIRIShortFormProvider implements IRIShortFormProvider {

  private static final long serialVersionUID = -2486017661266962136L;

  /**
   * Return an empty string as the short form of an IRI.
   *
   * @param iri IRI to get short form of
   * @return empty string
   */
  @Nonnull
  @Override
  public String getShortForm(@Nonnull IRI iri) {
    return "";
  }
}
