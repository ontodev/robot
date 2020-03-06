package org.obolibrary.robot.providers;

import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.NamespaceUtil;
import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * ShortFormProvider to return CURIEs with support for OBO Namespaces. Similar to
 * QNameShortFormProvider.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class OBOShortFormProvider implements ShortFormProvider {

  private final NamespaceUtil namespaceUtil = new NamespaceUtil();

  /**
   * Init a new OBOShortFormProvider
   *
   * @param prefix2Ns Map of prefixes to namespaces
   */
  public OBOShortFormProvider(Map<String, String> prefix2Ns) {
    prefix2Ns.forEach((key, v) -> namespaceUtil.setPrefix(v, key));
  }

  /**
   * Get the short form as a CURIE.
   *
   * @param entity OWLEntity to get short form of
   * @return CURIE
   */
  @Override
  @Nonnull
  public String getShortForm(@Nonnull OWLEntity entity) {
    // First check if this is an OBO prefix
    String strIRI = entity.getIRI().toString();
    if (strIRI.startsWith("http://purl.obolibrary.org/obo/")) {
      // If so, just split some strings to get the namespace
      String shortForm = strIRI.replace("http://purl.obolibrary.org/obo/", "");
      String[] split = shortForm.split("_");
      String prefix;
      if (split.length > 2) {
        prefix = String.join("_", Arrays.copyOf(split, split.length - 1));
      } else {
        prefix = split[0];
      }
      String local = split[split.length - 1];
      return prefix + ":" + local;
    } else {
      // Otherwise, use the QNameShortFormProvider basic method to get short form
      String namespace = entity.getIRI().getNamespace();
      String prefix = namespaceUtil.getPrefix(namespace);
      return entity.getIRI().prefixedBy(prefix + ':');
    }
  }

  /** Dispose of the ShortFormProvider */
  @Override
  public void dispose() {}
}
