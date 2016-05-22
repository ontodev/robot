package org.obolibrary.robot;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import static org.junit.Assert.assertEquals;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Helper methods for core tests.
 */
public class CoreTest {
    /**
     * Base IRI string for resources files.
     */
    protected static String base = "https://github.com/"
                               + "ontodev/robot/"
                               + "robot-core/"
                               + "src/test/resources/";

    /**
     * IRI of simple ontology.
     */
    protected static IRI simpleIRI = IRI.create(base + "simple.owl");

    /**
     * Shared data factory.
     */
    protected static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

    /**
     * Load an ontology from a resource path.
     *
     * @param path the resource path for the ontology
     * @return the loaded ontology
     * @throws IOException on file problems
     */
    public OWLOntology loadOntology(String path) throws IOException {
        IOHelper ioh = new IOHelper();
        return ioh.loadOntology(this.getClass().getResourceAsStream(path));
    }

    /**
     * Given an ontology path and an ontology, assert that they are the same,
     * and print a message to the test log.
     *
     * @param leftPath the resource path for the first ontology to compare
     * @param right the second ontology to compare
     * @throws IOException on any problem
     */
    public void assertIdentical(String leftPath, OWLOntology right)
            throws IOException {
        OWLOntology left = loadOntology(leftPath);
        assertIdentical(left, right);
    }

    /**
     * Given two ontologies, assert that they are the same,
     * and print a message to the test log.
     *
     * @param left the first ontology to compare
     * @param right the second ontology to compare
     * @throws IOException on any problem
     */
    public void assertIdentical(OWLOntology left, OWLOntology right)
            throws IOException {
        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(left, right, writer);
        System.out.println(writer.toString());
        assertEquals(true, actual);
        assertEquals("Ontologies are identical\n", writer.toString());
    }
    
    /**
     * Removes all declaration axioms
     * 
     * Sometimes required for tests that compare an output ontology
     * with a previously generated ontology (saving an ontology can unintentionally
     * introduce declaration axioms)
     * 
     * @param ont
     */
    protected void removeDeclarations(OWLOntology ont) {
		Set<OWLDeclarationAxiom> decls = ont.getAxioms(AxiomType.DECLARATION);
		ont.getOWLOntologyManager().removeAxioms(ont, decls);

	}

}
