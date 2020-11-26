package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Tests ExplainOperation.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class ExplainOperationTest extends CoreTest {

  private final String ONT_UNSAT = "/uvula_multiple_unsat.ofn";
  private final String ONT_INCONSISTENT = "/uvula_inconsistent.ofn";

  /**
   * Test explaining unsatisfiable classes.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExplainUnsatisfiableClasses() throws Exception {
    OWLOntology ontology = loadOntology(ONT_UNSAT);
    OWLReasonerFactory factory = new ReasonerFactory();
    OWLReasoner r = factory.createReasoner(ontology);
    Set<Explanation<OWLAxiom>> explanations =
        ExplainOperation.explainUnsatisfiableClasses(ontology, r, factory, 1);
    assertEquals(explanations.size(), 6);
  }

  /**
   * Test explaining inconsistent ontology.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExplainInconsistentOntology() throws Exception {
    OWLOntology ontology = loadOntology(ONT_INCONSISTENT);
    OWLReasonerFactory factory = new ReasonerFactory();
    Set<Explanation<OWLAxiom>> explanations =
        ExplainOperation.explainInconsistent(ontology, factory, 1);
    assertEquals(explanations.size(), 1);
  }

  /**
   * Test explaining inconsistent ontology.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExplainRootUnsatisfiableClasses() throws Exception {
    OWLOntology ontology = loadOntology(ONT_UNSAT);
    OWLReasonerFactory factory = new ReasonerFactory();
    OWLReasoner r = factory.createReasoner(ontology);
    Set<Explanation<OWLAxiom>> explanations =
        ExplainOperation.explainRootUnsatisfiableClasses(ontology, r, factory, 1);
    assertEquals(explanations.size(), 4);
  }

  /**
   * Test explaining entailment.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExplainEntailment() throws Exception {
    OWLOntology ontology = loadOntology(ONT_UNSAT);
    OWLReasonerFactory factory = new ReasonerFactory();
    OWLDataFactory df = OWLManager.getOWLDataFactory();
    OWLAxiom ax =
        df.getOWLSubClassOfAxiom(
            df.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0010235")),
            df.getOWLNothing());
    Set<Explanation<OWLAxiom>> explanations = ExplainOperation.explain(ax, ontology, factory, 2);
    assertEquals(explanations.size(), 2);
  }
}
