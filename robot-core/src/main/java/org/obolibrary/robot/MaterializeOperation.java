package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Materialize SubClassOf axioms using Expression Materializing Reasoner
 *
 * 
 *
 * @see <a href="https://github.com/ontodev/robot/issues/7">issue 7</a>
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 *
 */
public class MaterializeOperation {

    /**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(MaterializeOperation.class);

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

    /**
     * Replace EquivalentClass axioms with weaker SubClassOf axioms.
     *
     * @param ontology The OWLOntology to relax
     * @param options A map of options for the operation
     */
    public static void materialize(OWLOntology ontology,
            OWLReasonerFactory reasonerFactory,
            Set<OWLObjectProperty> properties,
            Map<String, String> options) {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        int seconds;
        long elapsedTime;
        long startTime = System.currentTimeMillis();

        ExpressionMaterializingReasonerFactory merf = new ExpressionMaterializingReasonerFactory(reasonerFactory);
        ExpressionMaterializingReasoner emr = 
                merf.createReasoner(ontology);
        if (!emr.isConsistent()) {
            logger.info("Ontology is not consistent!");
            return;
        }

        startTime = System.currentTimeMillis();

        Set<OWLAxiom> newAxioms = new HashSet<>();

        logger.info("Materializing..."+properties);
        if (properties == null || properties.size() == 0)
            emr.materializeExpressions();
        else
            emr.materializeExpressions(properties);
        
        logger.info("Materialization complete; iterating over classes");

        for (OWLClass c : ontology.getClassesInSignature()) {
            Set<OWLClassExpression> sces = emr.getSuperClassExpressions(c, true);
            for (OWLClassExpression sce : sces) {
                if (!sce.getSignature().contains(dataFactory.getOWLThing())) {
                    OWLAxiom ax = dataFactory.getOWLSubClassOfAxiom(c, sce);
                    newAxioms.add(ax);
                }
            }
        }

        manager.addAxioms(ontology, newAxioms);

        elapsedTime = System.currentTimeMillis() - startTime;
        seconds = (int) Math.ceil(elapsedTime / 1000);
        logger.info("Asserting materialized superclasses took {} seconds.", seconds);
 

    }



}
