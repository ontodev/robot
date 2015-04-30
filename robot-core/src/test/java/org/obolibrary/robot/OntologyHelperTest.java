package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
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
                        + "ontodev/robot/"
                        + "robot-core/"
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
        Set<String> actual = OntologyHelper.getAnnotationStrings(
                simple, df.getRDFSLabel(), iri);

        Set<String> expected = new HashSet<String>();
        expected.add("Test 1");
        expected.add("test one");
        assertEquals(expected, actual);
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
