package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reason over an ontology and remove redundant SubClassOf axioms
 *
 * Every axiom <code>A = SubClassOf(C D)</code> is tested
 * (C or D are permitted to be anonymous, e.g. SomeValuesFrom)
 *
 * If there already exists an axiom <code>SubClassOf(C Z)</code>,
 * where Z is entailed to be a proper SubClassOf of D (direct or indirect),
 * then A is redundant, and removed.
 *
 * <h2>Implementation</h2>
 *
 * Because an OWL reasoner will only return named (non-anonymous) superclasses,
 * we add a pre-processing step, where for each class C
 * appearing in either LHS or RHS of a SubClassOf expression,
 * if C is anonymous, we create a named class C' and add
 * a temporary axioms <code>EquivalentClasses(C' C)</code>, which is later
 * removed as a post-processing step. When performing reasoner tests,
 * we can then substitute C for C'
 *
 * <h2>GENERAL CLASS INCLUSION AXIOMS</h2>
 *
 * We make a special additional case of redunancy, as in the following example:
 *
 * <code>
 * 1. (hand and part-of some human) SubClassOf part-of some forelimb
 * 2. hand SubClassOf part-of some forelimb
 * </code>
 *
 * Here we treat axiom 1 as redundant,
 * but this is not detected by the algorithm above,
 * because there is no explicit SubClassOf axiom
 * between the GCI LHS and 'human'.
 * We therefore extend the test above and
 * first find all superclasses of anonymous LHSs,
 * and then test for these
 *
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 *
 */
public class ReduceOperation {

    /**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(ReduceOperation.class);

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
     * Remove redundant SubClassOf axioms.
     *
     * @param ontology The ontology to reduce.
     * @param reasonerFactory The reasoner factory to use.
     */
    public static void reduce(OWLOntology ontology,
            OWLReasonerFactory reasonerFactory) {
        reduce(ontology, reasonerFactory, getDefaultOptions());
    }

    /**
     * Remove redundant SubClassOf axioms.
     *
     * @param ontology The ontology to reduce.
     * @param reasonerFactory The reasoner factory to use.
     * @param options A map of options for the operation.
     */
    public static void reduce(OWLOntology ontology,
            OWLReasonerFactory reasonerFactory,
            Map<String, String> options) {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();


        Map<OWLClass, Set<OWLClass>> subClassMap = new HashMap<>();
        Set<OWLSubClassOfAxiom> subClassAxioms =
            ontology.getAxioms(AxiomType.SUBCLASS_OF);
        Set<OWLClassExpression> exprs = new HashSet<>();
        //Map<OWLClass, OWLClassExpression> xmap = new HashMap<>();
        Map<OWLClassExpression, OWLClass> rxmap = new HashMap<>();

        for (OWLSubClassOfAxiom ax : subClassAxioms) {

            OWLClass subClass =
                mapClass(dataFactory, rxmap, ax.getSubClass());
            OWLClass superClass =
                mapClass(dataFactory, rxmap, ax.getSuperClass());
            if (!subClassMap.containsKey(subClass)) {
                subClassMap.put(subClass, new HashSet<OWLClass>());
            }
            subClassMap.get(subClass).add(superClass);

            // DEP
            if (ax.getSubClass().isAnonymous()) {
                exprs.add(ax.getSubClass());
            }
            if (ax.getSuperClass().isAnonymous()) {
                exprs.add(ax.getSuperClass());
            }
        }
        Set<OWLAxiom> tempAxioms = new HashSet<>();
        for (OWLClassExpression x : rxmap.keySet()) {
            OWLAxiom ax =
                dataFactory.getOWLEquivalentClassesAxiom(rxmap.get(x), x);
            manager.addAxiom(ontology, ax);
            tempAxioms.add(ax);
        }


        // TO DO: DRY - move to ReasonerOperation module
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        if (!reasoner.isConsistent()) {
            logger.info("Ontology is not consistent!");
            return;
        }

        Node<OWLClass> unsatisfiableClasses =
                reasoner.getUnsatisfiableClasses();
        if (unsatisfiableClasses.getSize() > 1) {
            logger.info("There are {} unsatisfiable classes in the ontology.",
                    unsatisfiableClasses.getSize());
            for (OWLClass cls : unsatisfiableClasses) {
                if (!cls.isOWLNothing()) {
                    logger.info("    unsatisfiable: " + cls.getIRI());
                }
            }
        }

        Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<>();
        for (OWLSubClassOfAxiom ax : subClassAxioms) {

            // TO DO: make configurable
            if (ax.getAnnotations().size() > 0) {
                logger.debug("Protecting: " + ax);
                continue;
            }

            logger.debug("Testing: " + ax);
            OWLClassExpression subClassExpr = ax.getSubClass();
            OWLClassExpression superClassExpr = ax.getSuperClass();
            OWLClass subClass = rxmap.get(subClassExpr);
            OWLClass superClass = rxmap.get(superClassExpr);
            boolean isRedundant = false;

            for (OWLClass assertedSuper : subClassMap.get(subClass)) {
                if (reasoner.getSuperClasses(assertedSuper, false)
                        .containsEntity(superClass)) {
                    isRedundant = true;
                    break;
                }
            }

            // Special case for GCIs
            if (subClassExpr.isAnonymous()) {
                logger.debug("GCI:" + subClassExpr);
                for (OWLClass intermediateParent: reasoner
                        .getSuperClasses(subClass, false).getFlattened()) {
                    if (subClassMap.containsKey(intermediateParent)) {
                        logger.debug(
                            "GCI intermediate parent:" + intermediateParent);
                        if (reasoner.getSuperClasses(intermediateParent, false)
                                .containsEntity(superClass)) {
                            isRedundant = true;
                            break;
                        }
//                   for (OWLClass assertedSuper
//                           : subClassMap.get(intermediateParent)) {
//                       logger.info("  DOES: " + assertedSuper
//                           + "  CONTAIN:"+superClass);
//                       logger.info("   SUPES="
//                           + reasoner.getSuperClasses(assertedSuper, false));
//                       if (reasoner.getSuperClasses(assertedSuper, false)
//                             .containsEntity(superClass)) {
//                           isRedundant = true;
//                           break;
//                       }
//                   }
                    }
                }
            }

            if (isRedundant) {
                logger.info("REMOVING REDUNDANT: " + ax);
                rmAxioms.add(ax);
            }
        }

        // post-processing step: remove temporary equivalence axioms
        // for anonymous expressions
        for (OWLAxiom ax : tempAxioms) {
            manager.removeAxiom(ontology, ax);
        }

        // remove redundant axiom
        for (OWLAxiom ax : rmAxioms) {
            manager.removeAxiom(ontology, ax);
        }

    }

    /**
     * Map a class expression to a class.
     *
     * @param dataFactory A datafactory for creating the mapped class expression
     * @param rxmap A map from class expressions to classes
     * @param x The OWLClassExpression to map
     * @return the mapped OWLClass
     */
    private static OWLClass mapClass(OWLDataFactory dataFactory,
            Map<OWLClassExpression, OWLClass> rxmap, OWLClassExpression x) {
        if (!rxmap.containsKey(x)) {
            if (x.isAnonymous()) {
                UUID uuid = UUID.randomUUID();
                OWLClass c = dataFactory.getOWLClass(
                        IRI.create("urn:uuid" + uuid.toString()));
                logger.debug(c + " ==> " + x);
                rxmap.put(x, c);
            } else {
                rxmap.put(x, (OWLClass) x);
            }

        }
        return rxmap.get(x);
    }



}
