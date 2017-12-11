package org.obolibrary.robot;

import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.obolibrary.robot.checks.InvalidReferenceViolation.Category;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.obolibrary.robot.reason.EquivalentClassReasoning;
import org.obolibrary.robot.reason.EquivalentClassReasoningMode;
import org.semanticweb.owlapi.search.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import static org.obolibrary.robot.reason.EquivalentClassReasoningMode.ALL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repair an ontology
 */
public class RepairOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(RepairOperation.class);



    /**
     * Return a map from option name to default option value,
     * for all the available reasoner options.
     *
     * @return a map with default values for all available options
     */
    public static Map<String, String> getDefaultOptions() {
        Map<String, String> options = new HashMap<String, String>();
        //options.put("remove-redundant-subclass-axioms", "true");
 
        return options;
    }

 
    public static void repair(OWLOntology ontology) {
        repair(ontology,  getDefaultOptions());

    }
    public static void repair(OWLOntology ontology,
            Map<String, String> options) {

    }

    /**
     * Repairs invalid references
     * 
     * Currently only able to repair references to deprecated classes.
     * 
     * Assumes OBO vocabulary
     * 
     * @param iohelper
     * @param ontology
     * @param violations
     */
    public static void repairInvalidReferences(IOHelper iohelper, OWLOntology ontology, Set<InvalidReferenceViolation> violations) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLEntityRenamer renamer = new OWLEntityRenamer(manager, ontology.getImportsClosure());
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange> ();
        for (InvalidReferenceViolation v : violations) {
            if (v.getCategory().equals(Category.DEPRECATED)) {
                OWLEntity obsObj = v.getReferencedObject();
                IRI replacedBy = null; 
                for (OWLAnnotationAssertionAxiom aaa : ontology.getAnnotationAssertionAxioms(obsObj.getIRI())) {
                    // TODO: use a vocabulary object
                    if (aaa.getProperty().getIRI().equals(IRI.create("http://purl.obolibrary.org/obo/IAO_0100001"))) {
                        OWLAnnotationValue val = aaa.getValue();
                        Optional<IRI> valIRI = val.asIRI();
                        if (valIRI.isPresent()) {
                            replacedBy = valIRI.get();
                        }
                        else {
                            Optional<OWLLiteral> valLit = val.asLiteral();
                            if (valLit.isPresent()) {
                                replacedBy = iohelper.createIRI(valLit.get().getLiteral());
                            }
                        }
                    }
                    if (replacedBy != null) {
                        changes.addAll(renamer.changeIRI(obsObj.getIRI(), replacedBy));
                    }
                }
            }
        }    
        
        // TODO: ensure axioms about original entity are not changed
        manager.applyChanges(changes);
    }

}
