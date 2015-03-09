package owltools2;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests for {@link OntologyHelper}.
 */
public class OntologyHelperTest {
    /**
     * Base IRI string for resources files.
     */
    private String base = "https://github.com/"
                        + "ontodev/owltools2-experimental/"
                        + "owltools2-core/"
                        + "src/test/resources/";

    /**
     * Very simple ontology for testing.
     */
    private OWLOntology simple;

    /**
     * Load ontologies for testing.
     *
     * @throws IOException on file problems
     */
    @Before
    public void loadOntologies() throws IOException {
        IOHelper ioh = new IOHelper();
        simple = ioh.loadOntology(
                this.getClass().getResource("/simple.owl").getFile());
    }

    /**
     * Test getting values.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testGetValues() throws IOException {
        IRI iri = IRI.create(base + "simple.owl#test1");
        OWLDataFactory df =
            simple.getOWLOntologyManager().getOWLDataFactory();
        OWLClass test1 = df.getOWLClass(iri);
        Set<OWLAnnotation> annotations =
            test1.getAnnotations(simple, df.getRDFSLabel());

        Set<String> expected = new HashSet<String>();
        expected.add("Test 1");
        expected.add("test one");
        Set<String> actual = OntologyHelper.getValues(annotations);
        assertEquals(expected, actual);

        assertEquals("Test 1", OntologyHelper.getValue(annotations));
    }

    /**
     * Test getting a map of labels.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testGetLabels() throws IOException {
        Map<IRI, String> expected = new HashMap<IRI, String>();
        expected.put(IRI.create(base + "simple.owl#test1"), "Test 1");
        Map<IRI, String> actual = OntologyHelper.getLabels(simple);
        assertEquals(expected, actual);
    }
}
