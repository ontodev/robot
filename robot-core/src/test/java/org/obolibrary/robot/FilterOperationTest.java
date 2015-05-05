package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
public class FilterOperationTest extends CoreTest {
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
        OWLOntology simple = loadOntology("/simple.owl");
        OWLOntology filtered = FilterOperation.filter(
                simple, properties, simpleIRI);
        assertIdentical("/simple.owl", filtered);
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
        OWLOntology simpleParts = loadOntology("/simple_parts.owl");
        OWLOntology filtered = FilterOperation.filter(
                simpleParts, properties, simpleIRI);
        assertIdentical("/simple.owl", filtered);
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
        OWLOntology simpleParts = loadOntology("/simple_parts.owl");

        OWLOntologyManager manager = simpleParts.getOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        properties.add(
            df.getOWLObjectProperty(IRI.create(base + "simple.owl#part_of")));

        OWLOntology filtered = FilterOperation.filter(
                simpleParts, properties, simpleIRI);

        assertIdentical("/simple_parts.owl", filtered);
    }
}
