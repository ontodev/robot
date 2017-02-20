package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import static org.junit.Assert.assertEquals;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Optional;

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
     * Load ontology using guessed catalog
     * 
     * @param path
     * @return the loaded ontology
     * @throws IOException
     */
    public OWLOntology loadOntologyWithCatalog(String path) throws IOException {
        IOHelper ioh = new IOHelper();
        String fullpath = this.getClass().getResource(path).getFile().toString();
        return ioh.loadOntology(fullpath);
    }
    
    /**
     * Load ontology using explicit catalog
     * 
     * @param ontologyIRI
     * @param catalogFile
     * @return the loaded ontology
     * @throws IOException
     * @throws OWLOntologyCreationException 
     */
    public OWLOntology loadOntologyWithCatalog(IRI ontologyIRI, File catalogFile) throws IOException, OWLOntologyCreationException {
        //TODO: move logic here to IOHelper
        //IOHelper ioh = new IOHelper();
        OWLOntologyManager manager =
                OWLManager.createOWLOntologyManager();
        manager.addIRIMapper(new CatalogXmlIRIMapper(catalogFile));
        return manager.loadOntology(ontologyIRI);
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
     * Removes all declaration axioms.
     *
     * Sometimes required for tests that compare an output ontology
     * with a previously generated ontology
     * (saving an ontology can unintentionally introduce declaration axioms)
     *
     * @param ont The target ontology
     */
    protected void removeDeclarations(OWLOntology ont) {
        Set<OWLDeclarationAxiom> decls = ont.getAxioms(AxiomType.DECLARATION);
        ont.getOWLOntologyManager().removeAxioms(ont, decls);
    }

}
