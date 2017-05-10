package org.obolibrary.robot.reason;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by edouglass on 5/9/17.
 *
 */
public class EquivalentClassReasoning {

    private OWLOntology ontology;

    private EquivalentAxiomReasoningTest reasoningTest;

    private OWLReasoner reasoner;

    private EquivalentClassReasoningMode mode;

    private OWLDataFactory dataFactory;

    private Set<OWLEquivalentClassesAxiom> failingAxioms;

    private Set<OWLEquivalentClassesAxiom> foundEquivalentAxioms;

    public EquivalentClassReasoning(OWLOntology ontology, OWLReasoner reasoner, EquivalentClassReasoningMode mode) {
        this.ontology = ontology;
        this.reasoner = reasoner;
        dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.reasoningTest = createTest(mode, ontology);
        failingAxioms = Sets.newHashSet();
        foundEquivalentAxioms = Sets.newHashSet();
        this.mode = mode;
    }

    public boolean reason() {

        for(OWLClass c : ontology.getClassesInSignature()) {
            Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(c).getEntitiesMinus(c);
            foundEquivalentAxioms.addAll(this.createEquivalenceAxioms(c, equivalentClasses));
        }

        for(OWLEquivalentClassesAxiom axiom : foundEquivalentAxioms) {
            if(reasoningTest.fails(axiom)) {
                failingAxioms.add(axiom);
            }
        }
        return failingAxioms.isEmpty();
    }

    public void logReport(Logger logger) {
        if(!foundEquivalentAxioms.isEmpty()) {
            logger.info("Inferred the following equivalent classes");
            for (OWLEquivalentClassesAxiom anAxiom : foundEquivalentAxioms) {
                logger.info(equivAxiomToString(anAxiom));
            }
        }

        if(!failingAxioms.isEmpty()) {
            logger.error(mode.getExplanation());
            for(OWLEquivalentClassesAxiom axiom : failingAxioms) {
                logger.error(equivAxiomToString(axiom));
            }
        }
    }

    private Set<OWLEquivalentClassesAxiom> createEquivalenceAxioms(OWLClass c, Set<OWLClass> equivalentClasses) {
        Set<OWLEquivalentClassesAxiom> equivalenceAxioms = new HashSet<>();
        for(OWLClass ec : equivalentClasses) {
            equivalenceAxioms.add(dataFactory.getOWLEquivalentClassesAxiom(c, ec));
        }
        return equivalenceAxioms;
    }

    private static EquivalentAxiomReasoningTest createTest(EquivalentClassReasoningMode mode, OWLOntology ontology) {
        EquivalentAxiomReasoningTest test = null;
        switch (mode) {
            case ALL:
                test = axiom -> false;
                break;
            case NONE:
                test = axiom -> true;
                break;
            case ASSERTED_ONLY:
                test = axiom -> !ontology.containsAxiom(axiom, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS);
                break;
        }
        return test;
    }

    private static String equivAxiomToString(OWLEquivalentClassesAxiom axiom) {
        String equivalentClassesString = "Equivalence: ";
        Iterator<OWLClass> classIterator = axiom.getNamedClasses().iterator();
        while (classIterator.hasNext()) {
            equivalentClassesString += classIterator.next().toString();
            if (classIterator.hasNext()) {
                equivalentClassesString += " == ";
            }
        }
        return equivalentClassesString;
    }
}
