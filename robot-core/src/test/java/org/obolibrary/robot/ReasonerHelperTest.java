package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.obolibrary.robot.exceptions.IncoherentRBoxException;
import org.obolibrary.robot.exceptions.IncoherentTBoxException;
import org.obolibrary.robot.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class ReasonerHelperTest extends CoreTest {

    /**
     * Test checking for incoherent OPs.
     * 
     * See https://github.com/ontodev/robot/issues/104
     *
     * @throws IOException
     * @throws IncoherentTBoxException
     * @throws InconsistentOntologyException
     */
    @Test
    public void testIncoherentRBox() throws IOException, IncoherentTBoxException, InconsistentOntologyException {
        OWLOntology ontology = loadOntology("/incoherent-rbox.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        boolean isCaughtException = false;
        try {
            ReasonerHelper.validate(reasoner);
        }
        catch (IncoherentRBoxException e) {
            isCaughtException = true;
        }
        assertTrue(isCaughtException);
    }
    
    /**
     * Test checking for incoherent classes.
     * 
     * @throws IOException
     * @throws IncoherentTBoxException
     * @throws InconsistentOntologyException
     * @throws IncoherentRBoxException 
     */
    @Test
    public void testIncoherentTBox() throws IOException, IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {
        OWLOntology ontology = loadOntology("/incoherent-tbox.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        boolean isCaughtException = false;
        try {
            ReasonerHelper.validate(reasoner);
        }
        catch (IncoherentTBoxException e) {
            isCaughtException = true;
        }
        assertTrue(isCaughtException);
    }
    
    /**
     * Test checking for inconsistencies.
     * 
     * @throws IOException
     * @throws IncoherentTBoxException
     * @throws InconsistentOntologyException
     * @throws IncoherentRBoxException 
     */
    @Test
    public void testInconsistentOntology() throws IOException, IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {
        OWLOntology ontology = loadOntology("/inconsistent.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        boolean isCaughtException = false;
        try {
            ReasonerHelper.validate(reasoner);
        }
        catch (InconsistentOntologyException e) {
            isCaughtException = true;
        }
        assertTrue(isCaughtException);
    }
    
    /**
     * Test for no false positives in validation
     * 
     * @throws IOException
     * @throws IncoherentTBoxException
     * @throws InconsistentOntologyException
     * @throws IncoherentRBoxException 
     */
    @Test
    public void testNoFalsePositives() throws IOException, IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {
        OWLOntology ontology = loadOntology("/simple.owl");
        OWLReasonerFactory reasonerFactory = new org.semanticweb
                .elk.owlapi.ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        ReasonerHelper.validate(reasoner);
        
        // trivially true, if no exceptions are caught
        assertTrue(true);
    }
}
