package org.obolibrary.robot;

import java.io.IOException;
import org.junit.Test;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

public class CURIEShortFormProviderTest extends CoreTest {

  @Test
  public void testProvider() throws IOException {
    IOHelper ioHelper = new IOHelper();
    CURIEShortFormProvider sfp = new CURIEShortFormProvider(ioHelper.getPrefixes());
    OWLDataFactory df = OWLManager.getOWLDataFactory();

    // Short form 1 should return GO (longer than OBO namespace)
    OWLClass c1 = df.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/GO_123"));
    String sf1 = sfp.getShortForm(c1);
    assert sf1.equals("GO:123");

    // Short form 2 will default to base OBO since UNKNOWN is not registered
    OWLClass c2 = df.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UNKNOWN_123"));
    String sf2 = sfp.getShortForm(c2);
    assert sf2.equals("obo:UNKNOWN_123");

    // The example NS is not in prefixes so full IRI will be returned
    OWLClass c3 = df.getOWLClass(IRI.create("http://example.com/UNKNOWN_456"));
    String sf3 = sfp.getShortForm(c3);
    assert sf3.equals("http://example.com/UNKNOWN_456");
  }
}
