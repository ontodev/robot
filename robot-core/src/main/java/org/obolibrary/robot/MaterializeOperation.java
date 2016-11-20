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
     * @param reasonerFactory reasoner factory for the reasoner that is to be wrapped
     * @param properties object properties whose existentials are to be materialized (null materializes all)
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
            logger.error("Ontology is not consistent!");
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

        int i=0;
        for (OWLClass c : ontology.getClassesInSignature()) {
            i++;
            if (i % 100 == 1) {
                logger.info(" Materializing parents of class "+i+"/"+ontology.getClassesInSignature().size());
            }
            if (c.equals(dataFactory.getOWLNothing())) {
                continue;
            }
            Set<OWLClassExpression> sces = emr.getSuperClassExpressions(c, true);
            if (!emr.isSatisfiable(c)) {
                // TODO: see https://github.com/ontodev/robot/issues/40
                logger.error("Ontology is not conherent! Unsatisfiable: "+c);
                return;
            }
            for (OWLClassExpression sce : sces) {
                
                // do not make assertions involving Thing;
                // while valid, these are trivial
                if (!sce.getSignature().contains(dataFactory.getOWLThing())) {
                    
                    // avoid materializing parents with child in signature;
                    // this can happen if a property P is reflexive
                    // -- every class C is a subclass of P some C
                    // while valid, this is trivial, so we avoid asserting
                    if (!sce.getSignature().contains(c)) {
                        OWLAxiom ax = dataFactory.getOWLSubClassOfAxiom(c, sce);
                        newAxioms.add(ax);
                    }
                }
            }
        }

        logger.info("Adding "+newAxioms.size()+" materialized parents");
        manager.addAxioms(ontology, newAxioms);
        emr.dispose();

        elapsedTime = System.currentTimeMillis() - startTime;
        seconds = (int) Math.ceil(elapsedTime / 1000);
        logger.info("Asserting materialized superclasses took {} seconds.", seconds);
 

    }



}
