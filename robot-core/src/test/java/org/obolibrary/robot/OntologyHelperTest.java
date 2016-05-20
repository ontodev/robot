package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests for OntologyHelper.
 */
public class OntologyHelperTest extends CoreTest {
    /**
     * Test changing an ontology IRI.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testSetOntologyIRI() throws IOException {
        OWLOntology simple = loadOntology("/simple.owl");
        OntologyHelper.setOntologyIRI(simple,
                "http://ontology.iri",
                "http://version.iri");

        assertEquals("http://ontology.iri",
                simple.getOntologyID().getOntologyIRI().orNull().toString());
        assertEquals("http://version.iri",
                simple.getOntologyID().getVersionIRI().orNull().toString());
    }

    /**
     * Test getting values.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testGetValues() throws IOException {
        IRI iri = IRI.create(base + "simple.owl#test1");
        OWLOntology simple = loadOntology("/simple.owl");
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
        OWLOntology simple = loadOntology("/simple.owl");
        Map<IRI, String> expected = new HashMap<IRI, String>();
        expected.put(IRI.create(base + "simple.owl#test1"), "Test 1");
        Map<IRI, String> actual = OntologyHelper.getLabels(simple);
        assertEquals(expected, actual);
    }

    /**
     * Test getting a map from labels to IRIs.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testGetLabelIRIs() throws IOException {
        OWLOntology simple = loadOntology("/simple.owl");
        Map<String, IRI> expected = new HashMap<String, IRI>();
        expected.put("Test 1", IRI.create(base + "simple.owl#test1"));
        expected.put("test one", IRI.create(base + "simple.owl#test1"));
        Map<String, IRI> actual = OntologyHelper.getLabelIRIs(simple);
        assertEquals(expected, actual);
    }

    /**
     * Test adding and removing annotations from and ontology.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testOntologyAnnotations() throws IOException {
        IOHelper ioHelper = new IOHelper();

        OWLOntology simple = loadOntology("/simple.owl");
        assertEquals(0, simple.getAnnotations().size());

        // Add plain literal
        OntologyHelper.addOntologyAnnotation(simple,
                ioHelper.createIRI(base + "foo"),
                ioHelper.createLiteral("FOO"));
        assertEquals(1, simple.getAnnotations().size());

        OWLAnnotation annotation = simple.getAnnotations().iterator().next();
        assertEquals(base + "foo",
                annotation.getProperty().getIRI().toString());
        assertEquals("FOO", OntologyHelper.getValue(annotation.getValue()));

        OntologyHelper.removeOntologyAnnotations(simple);
        assertEquals(0, simple.getAnnotations().size());
    }
}
