package org.obolibrary.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Tests MIREOT extraction.
 *
 * This is a very minimal test with a dumb example
 *
 * @author cjm
 *
 */
public class MireotOperationTest extends CoreTest {

    /**
     * Test MIREOT.
     *
     * @throws IOException on IO problems
     * @throws OWLOntologyCreationException on ontology problems
     */
    @Test
    public void testMireot()
            throws IOException, OWLOntologyCreationException {
        testMireot("/mireot.owl");
    }

    /**
     * Test MIREOT.
     *
     * @param expectedPath the path to a known-good file for comparison
     * @throws IOException on IO problems
     * @throws OWLOntologyCreationException on ontology problems
     */
    public void testMireot(String expectedPath)
            throws IOException, OWLOntologyCreationException {
        OWLOntology inputOntology = loadOntology("/filtered.owl");

        IRI outputIRI = IRI.create("http://purl.obolibrary.org/obo/uberon.owl");

        Set<IRI> upperIRIs = Collections.singleton(
                IRI.create("http://purl.obolibrary.org/obo/UBERON_0001235"));
        Set<IRI> lowerIRIs = upperIRIs;
        //Set<IRI> branchIRIs = upperIRIs;

        List<OWLOntology> outputOntologies = new ArrayList<OWLOntology>();

        outputOntologies.add(
                MireotOperation.getAncestors(inputOntology,
                        upperIRIs, lowerIRIs, null));

        /*
        outputOntologies.add(
                MireotOperation.getDescendants(inputOntology,
                        branchIRIs, null));
        */

        OWLOntology outputOntology = MergeOperation.merge(outputOntologies);

        OntologyHelper.setOntologyIRI(outputOntology, outputIRI, null);

        OWLOntology expected = loadOntology(expectedPath);
        removeDeclarations(expected);
        removeDeclarations(outputOntology);
        assertIdentical(expected, outputOntology);
    }

}
