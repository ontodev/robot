package owltools2;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.semanticweb.owlapi.model.PrefixManager;

/**
 * Tests for {@link IOHelper}.
 */
public class IOHelperTest {
    /**
     * Test getting the default context.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testContext() throws IOException {
        IOHelper ioh = new IOHelper();
        Map<String, String> prefixes = ioh.loadContext();

        assertEquals("Check GO prefix",
                "http://purl.obolibrary.org/obo/GO_",
                prefixes.get("GO"));
    }

    /**
     * Test the default prefix manager.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testPrefixManager() throws IOException {
        IOHelper ioh = new IOHelper();
        PrefixManager pm = ioh.loadPrefixManager();

        assertEquals("Check GO CURIE",
                "http://purl.obolibrary.org/obo/GO_12345",
                pm.getIRI("GO:12345").toString());
    }

}
