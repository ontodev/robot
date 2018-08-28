package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add additional SubClassOf axioms that are relaxed forms of equivalence axioms.
 *
 * <p>This is a form of incomplete reasoning that is useful when either axioms are invisible to EL
 * reasoners, or to produce an existential subclass graph that has SomeValuesFrom superclasses
 * materialized.
 *
 * <h2>Motivation</h2>
 *
 * It is frequently convenient to view an ontology without equivalence axioms. This is often for
 * structural reasons. Certain editions of ontologies may come with a guarantee that the existential
 * graph formed by all SubClassOf axioms (between named classes and existential axioms) is both
 * complete (w.r.t graph operations) and non-redundant. Including EquivalentClasses axioms can
 * introduce redundancy at the graph view level. For example, the genus is frequently more general
 * than the inferred superclasses.
 *
 * <p>To ensure that the existential graph is graph-complete it's necessary to write new SubClassOf
 * axioms that are entailed by (but weaker than) Equivalence axioms
 *
 * <h2>Basic Operation</h2>
 *
 * For any equivalence axiom between a name class C and either a single existential X_1 or the class
 * expression IntersectionOf(X_1 ... X_n), generate axioms <code>
 *    C SubClassOf X_1
 *    ...
 *    C SubClassOf X_n
 * </code> Additionally, if X_i is a qualified cardinality constraint, weaken to an existential
 *
 * <h2>Usage</h2>
 *
 * For ELK reasoners, it is recommended to use this prior to the reasoning step, as relaxation can
 * reveal weaker forms of axioms that are outside EL.
 *
 * <p>It is recommended that the reduce operation is executed after relaxation, to remove any
 * redundancies within the subclass graph
 *
 * <p>Note that the materialize operation may seem to make the relax step pointless; materialization
 * produces the most specific existential parent. However, in some cases relaxation should still be
 * performed: - if Elk is used (see above) - if materialization is only performed on a subset of
 * properties
 *
 * <p>The standard sequence is: relax-materialize-reduce, or relax-reason-reduce
 *
 * @see <a href="https://github.com/ontodev/robot/issues/7">issue 7</a>
 * @see <a href="https://github.com/ontodev/robot/issues/135">issue 135</a>
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class RelaxOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RelaxOperation.class);

  /**
   * Return a map from option name to default option value, for all the available reasoner options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    // options.put("remove-redundant-subclass-axioms", "true");
    return new HashMap<>();
  }

  /**
   * Replace EquivalentClass axioms with weaker SubClassOf axioms.
   *
   * @param ontology The OWLOntology to relax
   * @param options A map of options for the operation
   */
  public static void relax(OWLOntology ontology, Map<String, String> options) {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();

    Set<OWLAxiom> newAxioms = new HashSet<>();

    Set<OWLEquivalentClassesAxiom> eqAxioms = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES);

    for (OWLEquivalentClassesAxiom ax : eqAxioms) {
      for (OWLClassExpression x : ax.getClassExpressions()) {

        // we only relax in cases where the equivalence is between one
        // named and one anon expression
        if (!x.isAnonymous()) {
          OWLClass c = (OWLClass) x;
          // ax = EquivalentClasses(x y1 y2 ...)
          for (OWLClassExpression y : ax.getClassExpressionsMinus(c)) {
            // limited structural reasoning:
            //   return (P some Z), if:
            //   - y is of the form (P some Z)
            //   - y is of the form ((P some Z) and ...),
            //     or any level of nesting
            for (OWLObjectSomeValuesFrom svf : getSomeValuesFromAncestor(y, dataFactory)) {
              newAxioms.add(dataFactory.getOWLSubClassOfAxiom(c, svf));
            }
            for (OWLClass z : getNamedAncestors(y)) {
              newAxioms.add(dataFactory.getOWLSubClassOfAxiom(c, z));
            }
          }
        }
      }
    }

    // remove redundant axiom
    for (OWLAxiom ax : newAxioms) {
      logger.info("NEW: " + ax);
      manager.addAxiom(ontology, ax);
    }
  }

  /**
   * Given an OWLClassExpression y, return a set of OWLObjectSomeValuesFrom objects (p some v),
   * where (p some v) is a superclass of y.
   *
   * <p>Not guaranteed to be complete
   *
   * @param x The OWLClassExpression to check.
   * @param dataFactory OWLDataFactory
   * @return the set of OWLObjectSomeValuesFrom objects
   */
  private static Set<OWLObjectSomeValuesFrom> getSomeValuesFromAncestor(
      OWLClassExpression x, OWLDataFactory dataFactory) {
    Set<OWLObjectSomeValuesFrom> svfs = new HashSet<>();
    if (x instanceof OWLObjectSomeValuesFrom) {
      OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) x;
      svfs.add(svf);

    } else if (x instanceof OWLObjectCardinalityRestriction) {
      OWLObjectCardinalityRestriction ocr = (OWLObjectCardinalityRestriction) x;
      OWLClassExpression filler = ocr.getFiller();
      OWLObjectPropertyExpression p = ocr.getProperty();
      if (ocr.getCardinality() > 0) {
        OWLObjectSomeValuesFrom svf = dataFactory.getOWLObjectSomeValuesFrom(p, filler);
        svfs.add(svf);
      }

    } else if (x instanceof OWLObjectIntersectionOf) {
      for (OWLClassExpression op : ((OWLObjectIntersectionOf) x).getOperands()) {
        svfs.addAll(getSomeValuesFromAncestor(op, dataFactory));
      }
    }

    return svfs;
  }

  /**
   * Given an OWLClassExpression y, return a set of named classes c, such that c is a superclass of
   * y,
   *
   * <p>obtained by relaxing/unwinding expression, in a way that is guaranteed valid but not
   * guaranteed complete.
   *
   * <p>This is effectively poor-mans reasoning over IntersectionOf; e.g
   *
   * <pre>
   * C SubClassOf (C and ...)
   * </pre>
   *
   * @param x The OWLClassExpression to unwind.
   * @return the set of OWLClass superclasses
   */
  private static Set<OWLClass> getNamedAncestors(OWLClassExpression x) {
    Set<OWLClass> cs = new HashSet<>();
    if (!x.isAnonymous()) {
      cs.add(x.asOWLClass());
    } else if (x instanceof OWLObjectIntersectionOf) {
      for (OWLClassExpression op : ((OWLObjectIntersectionOf) x).getOperands()) {
        cs.addAll(getNamedAncestors(op));
      }
    }
    return cs;
  }
}
