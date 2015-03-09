package owltools2;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Tests for {@link DiffOperation}.
 */
public class DiffOperationTest {
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
     * Compare one ontology to itself.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testCompareIdentical() throws IOException {
        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(simple, simple, writer);
        System.out.println(writer.toString());
        assertEquals(true, actual);
        assertEquals("Ontologies are identical\n", writer.toString());
    }

    /**
     * Compare one ontology to a modified copy.
     *
     * @throws IOException on file problem
     * @throws OWLOntologyCreationException on ontology problem
     */
    @Test
    public void testCompareModified()
            throws IOException, OWLOntologyCreationException {
        Set<OWLOntology> onts = new HashSet<OWLOntology>();
        onts.add(simple);
        OWLOntologyManager manager = simple.getOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        OWLOntology simple1 = manager.createOntology(
                    IRI.create(base + "simple1.owl"), onts);
        IRI test1 = IRI.create(base + "simple.owl#test1");
        manager.addAxiom(simple1,
                df.getOWLAnnotationAssertionAxiom(
                    df.getRDFSLabel(),
                    test1,
                    df.getOWLLiteral("TEST #1")));

        StringWriter writer = new StringWriter();
        boolean actual = DiffOperation.compare(simple, simple1, writer);
        System.out.println(writer.toString());
        assertEquals(false, actual);
        String expected = "0 axioms in Ontology 1 but not in Ontology 2:\n\n"
                        + "1 axioms in Ontology 2 but not in Ontology 1:\n"
                        + "+ AnnotationAssertion("
                        + "rdfs:label <" + test1.toString() + "> "
                        + "\"TEST #1\"^^xsd:string)\n";
        assertEquals(expected, writer.toString());
    }
}
