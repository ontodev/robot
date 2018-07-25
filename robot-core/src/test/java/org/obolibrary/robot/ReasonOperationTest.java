package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.junit.Test;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;

/** Tests for ReasonOperation. */
public class ReasonOperationTest extends CoreTest {
  /**
   * Test reasoning with StructuralReasoner.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testStructural()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory =
        new org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory();
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertIdentical("/simple_structural.owl", reasoned);
  }

  /**
   * Test reasoning with ELK.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testELK()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertIdentical("/simple_elk.owl", reasoned);
  }

  /**
   * Test reasoning with HermiT.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testHermit()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertEquals(6, reasoned.getAxiomCount());
    assertIdentical("/simple_hermit.owl", reasoned);
  }

  /**
   * Test reasoning with JFact.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testJFact()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory = new JFactFactory();
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertEquals(6, reasoned.getAxiomCount());
    assertIdentical("/simple_jfact.owl", reasoned);
  }

  /**
   * Test inferring into new ontology.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testInferIntoNewOntology()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
    Map<String, String> opts = new HashMap<>();
    // see https://github.com/ontodev/robot/issues/80
    opts.put("create-new-ontology", "true");
    // see https://github.com/ontodev/robot/issues/80
    opts.put("annotate-inferred-axioms", "true");
    ReasonOperation.reason(reasoned, reasonerFactory, opts);
    assertEquals(2, reasoned.getAxiomCount());
    // assertIdentical("/simple_hermit.owl", reasoned);
  }

  /**
   * Test inferring into new ontology.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testInferIntoNewOntologyNonTrivial()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
    Map<String, String> opts = new HashMap<>();

    // see https://github.com/ontodev/robot/issues/80
    opts.put("create-new-ontology", "true");
    opts.put("annotate-inferred-axioms", "true");

    ReasonOperation.reason(reasoned, reasonerFactory, opts);

    // note that some of the inferred axioms are trivial
    // involving owl:Thing
    assertEquals(15, reasoned.getAxiomCount());
    // assertIdentical("/simple_hermit.owl", reasoned);
  }

  /**
   * Test inferring into new ontology, excluding duplicates.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testInferIntoNewOntologyNoDupes()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
    Map<String, String> opts = new HashMap<>();
    opts.put("create-new-ontology", "true");
    opts.put("annotate-inferred-axioms", "true");
    opts.put("exclude-duplicate-axioms", "true");
    ReasonOperation.reason(reasoned, reasonerFactory, opts);
    assertEquals(5, reasoned.getAxiomCount());
    // assertIdentical("/simple_hermit.owl", reasoned);
  }

  /**
   * Test removing redundant subclass axioms.
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testRemoveRedundantSubClassAxioms()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/redundant_subclasses.owl");
    OWLReasonerFactory reasonerFactory = new org.semanticweb.elk.owlapi.ElkReasonerFactory();
    ReasonOperation.reason(reasoned, reasonerFactory, Collections.emptyMap());
    assertIdentical("/redundant_subclasses.owl", reasoned);

    Map<String, String> options = new HashMap<String, String>();
    options.put("remove-redundant-subclass-axioms", "true");

    reasoned = loadOntology("/redundant_subclasses.owl");
    ReasonOperation.reason(reasoned, reasonerFactory, options);
    assertIdentical("/without_redundant_subclasses.owl", reasoned);
  }

  /**
   * Test reasoning with Expression Materializing Reasoner.
   *
   * <p>This test should return the same results as running any other reasoner
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testEMRBasic()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/simple.owl");
    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    OWLReasonerFactory reasonerFactory =
        new ExpressionMaterializingReasonerFactory(coreReasonerFactory);
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertIdentical("/simple_elk.owl", reasoned);
  }

  /**
   * Test reasoning with Expression Materializing Reasoner.
   *
   * <p>This test effectively relaxes an equivalence axiom
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testEMRRelax()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/relax_equivalence_axioms_test.obo");
    OWLReasonerFactory coreReasonerFactory = new ElkReasonerFactory();
    OWLReasonerFactory reasonerFactory =
        new ExpressionMaterializingReasonerFactory(coreReasonerFactory);
    Map<String, String> opts = ReasonOperation.getDefaultOptions();
    opts.put("exclude-owl-thing", "true");
    ReasonOperation.reason(reasoned, reasonerFactory, opts);
    assertIdentical("/relax_equivalence_axioms_expressions_materialized.obo", reasoned);
  }

  /**
   * Test reasoning with intersection axioms
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testIntersection()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology reasoned = loadOntology("/intersection.omn");
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    ReasonOperation.reason(reasoned, reasonerFactory);
    assertTrue(checkContains(reasoned, "SubClassOf(<http://x.org/XA> <http://x.org/XB>)"));
  }

  /**
   * Test reasoning with external ontologies.
   *
   * <p>Depending on user option, inferred axioms that refer solely to external ontology classes
   * (i.e. those in the import chain) should not be asserted
   *
   * @throws IOException on file problem
   * @throws OWLOntologyCreationException on ontology problem
   * @throws OntologyLogicException on logic error
   * @throws InvalidReferenceException
   */
  @Test
  public void testExternal()
      throws IOException, OWLOntologyCreationException, OntologyLogicException,
          InvalidReferenceException {
    OWLOntology importOnt1 = loadOntology("/intersection.omn");
    IRI oiri = importOnt1.getOntologyID().getOntologyIRI().get();
    OWLOntology mainOnt = loadOntology("/simple.owl");
    OWLOntologyManager mgr = mainOnt.getOWLOntologyManager();
    OWLOntology importOnt = mgr.createOntology(oiri);
    mgr.addAxioms(importOnt, importOnt1.getAxioms());

    OWLImportsDeclaration importsDecl = mgr.getOWLDataFactory().getOWLImportsDeclaration(oiri);
    AddImport ch = new AddImport(mainOnt, importsDecl);
    mgr.applyChange(ch);
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

    Map<String, String> opts = new HashMap<>();
    opts.put("exclude-external-entities", "true");
    ReasonOperation.reason(mainOnt, reasonerFactory, opts);
    assertFalse(checkContains(mainOnt, "SubClassOf(<http://x.org/XA> <http://x.org/XB>)"));
    opts.put("exclude-external-entities", "false");
    ReasonOperation.reason(mainOnt, reasonerFactory, opts);
    assertTrue(checkContains(mainOnt, "SubClassOf(<http://x.org/XA> <http://x.org/XB>)"));
  }

  private boolean checkContains(OWLOntology reasoned, String axStr) {
    for (OWLAxiom a : reasoned.getLogicalAxioms()) {
      if (a.toString().equals(axStr)) return true;
    }
    return false;
  }
}
