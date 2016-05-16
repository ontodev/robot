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
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add additional SubClassOf axioms that are relaxed forms of equivalence axioms
 * 
 * <h2>Motivation</h2>
 *
 * It is frequently convenient to view an ontology without equivalence axioms. This is often for structural reasons. Certain editions of ontologies may come with a guarantee that the existential graph formed by all SubClassOf axioms (between named classes and existential axioms) is both complete (w.r.t graph operations) and non-redundant. Including EquivalentClasses axioms can introduce redundancy at the graph view level. For example, the genus is frequently more general than the inferred superclasses.
 *
 * To ensure that the existential graph is graph-complete it's necessary to write new SubClassOf axioms that are entailed by (but weaker than) Equivalence axioms
 * 
 * <h2>Basic Operation</h2>
 *	
 * For any equivalence axiom between a name class C and either a single existential X_1 or the class expression IntersectionOf(X_1 ... X_n), generate axioms
 * <code>
 *    C SubClassOf X_1
 *    ...
 *    C SubClassOf X_n
 * </code>
 * 
 * 
 * 
 * @see <a href="https://github.com/ontodev/robot/issues/7">issue 7</a>
 * 
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 *
 */
public class RelaxOperation {

	private static final Logger logger =
			LoggerFactory.getLogger(RelaxOperation.class);

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


	public static void relax(OWLOntology ontology,
			Map<String, String> options) {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();


		Set<OWLAxiom> newAxioms = new HashSet<>();

		Set<OWLEquivalentClassesAxiom> eqAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);

		for (OWLEquivalentClassesAxiom ax : eqAxioms) {
			for (OWLClassExpression x : ax.getClassExpressions()) {
				if (!x.isAnonymous()) {
					OWLClass c = (OWLClass)x;
					for (OWLClassExpression y : ax.getClassExpressionsMinus(c)) {
						for (OWLObjectSomeValuesFrom svf : getSomeValuesFromAncestor(y)) {
							newAxioms.add(dataFactory.getOWLSubClassOfAxiom(c, svf));
						}
					}
				}
			}
		}


		// remove redundant axiom
		for (OWLAxiom ax : newAxioms) {
			logger.info("NEW: "+ax);
			manager.addAxiom(ontology, ax);
		}

	}

	private static Set<OWLObjectSomeValuesFrom> getSomeValuesFromAncestor(OWLClassExpression x) {
		Set<OWLObjectSomeValuesFrom> svfs = new HashSet<>();
		if (x instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) x;
			svfs.add(svf);
		}
		else if (x instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression op : ((OWLObjectIntersectionOf) x).getOperands()) {
				svfs.addAll(getSomeValuesFromAncestor(op));
			}
		}
		return svfs;
	}


	private static OWLClass mapClass(OWLDataFactory dataFactory, 
			Map<OWLClassExpression,OWLClass> rxmap, OWLClassExpression x) {
		if (!rxmap.containsKey(x)) {
			if (x.isAnonymous()) {
				UUID uuid = UUID.randomUUID();
				OWLClass c = dataFactory.getOWLClass(IRI.create("urn:uuid"+uuid.toString()));
				logger.info(c + " ==> "+x);
				rxmap.put(x, c);
			}
			else {
				rxmap.put(x, (OWLClass) x);
			}

		}
		return rxmap.get(x);
	}



}
