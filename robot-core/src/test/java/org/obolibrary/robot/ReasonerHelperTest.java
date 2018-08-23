package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import org.obolibrary.robot.exceptions.IncoherentRBoxException;
import org.obolibrary.robot.exceptions.IncoherentTBoxException;
import org.obolibrary.robot.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests convenience operations using reasoner.
 *
 * <p>So far thus encompasses only logic validation tests (incoherency, inconsistency)
 *
 * @author cjm
 */
public class ReasonerHelperTest extends CoreTest {

  /**
   * Test checking for incoherent OPs.
   *
   * <p>See https://github.com/ontodev/robot/issues/104
   *
   * @throws IOException if file error
   * @throws IncoherentTBoxException if has unsatisfiable classes
   * @throws InconsistentOntologyException if has inconsistencies
   */
  @Test
  public void testIncoherentRBox()
      throws IOException, IncoherentTBoxException, InconsistentOntologyException {
    OWLOntology ontology = loadOntology("/incoherent-rbox.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    boolean isCaughtException = false;
    try {
      ReasonerHelper.validate(reasoner);

    } catch (IncoherentRBoxException e) {
      isCaughtException = true;
    }
    assertTrue(isCaughtException);
  }

  /**
   * Test checking for incoherent classes.
   *
   * @throws IOException if file error
   * @throws InconsistentOntologyException if has inconsistencies
   * @throws IncoherentRBoxException if has unsatisfiable properties
   */
  @Test
  public void testIncoherentTBox()
      throws IOException, InconsistentOntologyException, IncoherentRBoxException {
    OWLOntology ontology = loadOntology("/incoherent-tbox.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    boolean isCaughtException = false;
    try {
      ReasonerHelper.validate(reasoner);

    } catch (IncoherentTBoxException e) {
      isCaughtException = true;
    }
    assertTrue(isCaughtException);
  }

  /**
   * Test creating an unsatisfiable module
   *
   * @throws Exception on any problem
   */
  @Test
  public void testCreateUnsatisfiableModule() throws Exception {
    OWLOntology ontologyMain = loadOntology("/incoherent-tbox.owl");
    IRI iri = ontologyMain.getOntologyID().getOntologyIRI().orNull();
    if (iri == null) {
      throw new Exception("Ontology 'incoherent-tbox.owl' does not have an IRI");
    }
    OWLOntology ontology = ontologyMain.getOWLOntologyManager().createOntology();
    OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
    AddImport ai = new AddImport(ontology, factory.getOWLImportsDeclaration(iri));
    ontology.getOWLOntologyManager().applyChange(ai);
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    boolean isCaughtException = false;
    IOHelper ioHelper = new IOHelper();
    String PATH = "target/unsat.owl";
    try {
      ReasonerHelper.validate(reasoner, PATH, ioHelper);
    } catch (IncoherentTBoxException e) {
      isCaughtException = true;
      IOHelper ioh = new IOHelper();
      OWLOntology unsatisfiableOntology = ioh.loadOntology(PATH);
      Set<OWLSubClassOfAxiom> scas = unsatisfiableOntology.getAxioms(AxiomType.SUBCLASS_OF);

      // we expect each axiom to have exactly one annotation, which is an rdfs:isDefinedBy
      // pointing at the ontology in which the axiom came from
      for (OWLSubClassOfAxiom sca : scas) {
        assertEquals(1, sca.getAnnotations().size());
        OWLAnnotation ann = sca.getAnnotations().iterator().next();
        assertEquals(factory.getRDFSIsDefinedBy(), ann.getProperty());
        OWLLiteral v = ann.getValue().asLiteral().orNull();
        if (v == null) {
          throw new Exception(String.format("OWLAnnotation '%s' has no value.", ann.toString()));
        }
        String val = v.getLiteral();
        String originalOntIdVal = ontologyMain.getOntologyID().toString();
        assertEquals(originalOntIdVal, val);
      }
      assertEquals(2, scas.size());
    }
    assertTrue(isCaughtException);
  }

  /**
   * Test checking for inconsistencies.
   *
   * @throws IOException if file error
   * @throws IncoherentTBoxException if has unsatisfiable classes
   * @throws IncoherentRBoxException if has unsatisfiable properties
   */
  @Test
  public void testInconsistentOntology()
      throws IOException, IncoherentTBoxException, IncoherentRBoxException {
    OWLOntology ontology = loadOntology("/inconsistent.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    boolean isCaughtException = false;
    try {
      ReasonerHelper.validate(reasoner);

    } catch (InconsistentOntologyException e) {
      isCaughtException = true;
    }
    assertTrue(isCaughtException);
  }

  /**
   * Test for no false positives in validation.
   *
   * @throws IOException if file error
   * @throws IncoherentTBoxException if has unsatisfiable classes
   * @throws InconsistentOntologyException if has inconsistencies
   * @throws IncoherentRBoxException if has unsatisfiable properties
   */
  @Test
  public void testNoFalsePositives()
      throws IOException, IncoherentTBoxException, InconsistentOntologyException,
          IncoherentRBoxException {
    OWLOntology ontology = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    ReasonerHelper.validate(reasoner);

    // trivially true, if no exceptions are caught
    assertTrue(true);
  }
}
