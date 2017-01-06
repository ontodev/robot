package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.obolibrary.robot.exceptions.IncoherentTBoxException;
import org.obolibrary.robot.exceptions.IncoherentRBoxException;
import org.obolibrary.robot.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Provides convenience methods for working with OWL reasoning.
 * 
 * @author cjm
 *
 */
public class ReasonerHelper {

    /**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(ReasonerHelper.class);


    public static void validate(OWLReasoner reasoner) throws IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {

        OWLOntology ont = reasoner.getRootOntology();
        OWLOntologyManager manager = ont.getOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        OWLClass nothing = dataFactory.getOWLNothing();
        OWLClass thing = dataFactory.getOWLThing();
        logger.info("Checking for inconsistencies");
        if (!reasoner.isConsistent()) {
            throw new InconsistentOntologyException();
        }

        logger.info("Checking for unsatisfiable classes...");
        Set<OWLClass> unsatisfiableClasses =
                reasoner.getUnsatisfiableClasses().getEntitiesMinus(nothing);
        if (unsatisfiableClasses.size() > 0) {
            logger.error("There are {} unsatisfiable classes in the ontology.",
                    unsatisfiableClasses.size());
            for (OWLClass cls : unsatisfiableClasses) {
                logger.error("    unsatisfiable: " + cls.getIRI());
            }
            throw new IncoherentTBoxException(unsatisfiableClasses);
        }
        
        logger.info("Checking for unsatisfiable object properties...");

        Set<OWLAxiom>tempAxioms = new HashSet<>();
        Map<OWLClass, OWLObjectProperty> probeFor = new HashMap<>();
        for (OWLObjectProperty p : ont.getObjectPropertiesInSignature(Imports.INCLUDED)) {
            UUID uuid = UUID.randomUUID();
            IRI probeIRI = IRI.create(p.getIRI().toString() + "-" + uuid.toString());
            OWLClass probe = dataFactory.getOWLClass(probeIRI);
            probeFor.put(probe, p);
            tempAxioms.add(dataFactory.getOWLDeclarationAxiom(probe));
            tempAxioms.add(dataFactory.getOWLSubClassOfAxiom(probe, 
                    dataFactory.getOWLObjectSomeValuesFrom(p, thing)));
        }
        manager.addAxioms(ont, tempAxioms);
        reasoner.flush();
 
        Set<OWLClass> unsatisfiableProbeClasses =
                reasoner.getUnsatisfiableClasses().getEntitiesMinus(nothing);
        
        // leave no trace
        manager.removeAxioms(ont, tempAxioms);
        reasoner.flush();
        
         if (unsatisfiableProbeClasses.size() > 0) {
            logger.error("There are {} unsatisfiable properties in the ontology.",
                    unsatisfiableProbeClasses.size());
            Set<OWLObjectProperty> unsatPs = new HashSet<>();
            for (OWLClass cls : unsatisfiableProbeClasses) {
                OWLObjectProperty unsatP = probeFor.get(cls);
                unsatPs.add(unsatP);
                logger.error("    unsatisfiable property: " + unsatP.getIRI());
            }
            throw new IncoherentRBoxException(unsatPs);
        }


    }
    
   

}
