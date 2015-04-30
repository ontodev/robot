package org.obolibrary.robot;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Tests for {@link FilterOperation}.
 */
public class FilterOperationTest {
    /**
     * Base IRI string for resources files.
     */
    private static String base = "https://github.com/"
                               + "ontodev/robot/"
                               + "robot-core/"
                               + "src/test/resources/";

    /**
     * IRI of simple ontology.
     */
    private static IRI simpleIRI = IRI.create(base + "simple.owl");

    /**
     * Very simple ontology for testing.
     */
    private OWLOntology simple;

    /**
     * Simple ontology plus part_of relation.
     */
    private OWLOntology simpleParts;

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
        simpleParts = ioh.loadOntology(
                this.getClass().getResource("/simple_parts.owl").getFile());
    }

    /**
     * Filter all object properties from an ontology that has no
     * object properties.
     * Result is identical.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testFilterNothing()
            throws IOException, OWLOntologyCreationException {
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        OWLOntology filtered = FilterOperation.filter(
                simple, properties, simpleIRI);

        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(simple, filtered, writer);
        System.out.println(writer.toString());
        assertEquals(true, actual);
        assertEquals("Ontologies are identical\n", writer.toString());
    }

    /**
     * Filter all object properties from an ontology that has just one
     * object property.
     * Result matches the simple.owl ontology.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testRemoveParts()
            throws IOException, OWLOntologyCreationException {
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        OWLOntology filtered = FilterOperation.filter(
                simpleParts, properties, simpleIRI);

        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(simple, filtered, writer);
        System.out.println(writer.toString());
        assertEquals(true, actual);
        assertEquals("Ontologies are identical\n", writer.toString());
    }

    /**
     * Filter for one object property from an ontology that has just one
     * object property.
     * Result is identical.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testKeepParts()
            throws IOException, OWLOntologyCreationException {
        OWLOntologyManager manager = simple.getOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        properties.add(
            df.getOWLObjectProperty(IRI.create(base + "simple.owl#part_of")));

        OWLOntology filtered = FilterOperation.filter(
                simpleParts, properties, simpleIRI);

        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(simpleParts, filtered, writer);
        System.out.println(writer.toString());
        assertEquals(true, actual);
        assertEquals("Ontologies are identical\n", writer.toString());
    }
}
