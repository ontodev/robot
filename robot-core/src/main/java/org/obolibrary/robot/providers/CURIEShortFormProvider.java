package org.obolibrary.robot.providers;

import java.util.*;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShortFormProvider to return CURIEs. Similar to QNameShortFormProvider.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class CURIEShortFormProvider implements ShortFormProvider {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(CURIEShortFormProvider.class);

  private List<Map.Entry<String, String>> sortedPrefixMap;

  /**
   * Init a new CURIEShortFormProvider
   *
   * @param prefix2Ns Map of prefixes to namespaces
   */
  public CURIEShortFormProvider(Map<String, String> prefix2Ns) {
    // Create a list from elements of HashMap
    sortedPrefixMap = new LinkedList<>(prefix2Ns.entrySet());

    // Sort the list, placing longest values (ns) first
    sortedPrefixMap.sort(
        Collections.reverseOrder(Comparator.comparingInt(o -> o.getValue().length())));
  }

  /**
   * Get the short form of an OWLEntity as a CURIE.
   *
   * @param entity OWLEntity to get short form of
   * @return CURIE
   */
  @Override
  @Nonnull
  public String getShortForm(@Nonnull OWLEntity entity) {
    return getShortForm(entity.getIRI());
  }

  /**
   * Get the short form of an IRI as a CURIE.
   *
   * @param iri IRI to get short form of
   * @return CURIE
   */
  public String getShortForm(IRI iri) {
    String iriString = iri.toString();
    // Find the first (longest) match from sorted prefix/ns entries
    for (Map.Entry<String, String> prefix2Ns : sortedPrefixMap) {
      String prefix = prefix2Ns.getKey() + ":";
      String ns = prefix2Ns.getValue();
      if (iriString.startsWith(ns)) {
        return iriString.replace(ns, prefix);
      }
    }
    // Could not match, just return full IRI
    logger.warn("Unable to find namespace for: " + iri);
    return iriString;
  }

  /** Dispose of the ShortFormProvider */
  @Override
  public void dispose() {}
}
