package org.obolibrary.robot.reason;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;

/**
 * Created by edouglass on 5/9/17.
 *
 * <p>All the pieces that are needed to perform reasoning on equivalent classes in an OWLOntology
 */
public class EquivalentClassReasoning {

  /** The OWLOntolgoy to reason over */
  private OWLOntology ontology;

  /** The particular fail mode for dealing with an equivalent class. */
  private EquivalentAxiomReasoningTest reasoningTest;

  /** Reasoner to use on the OWLOntolgoy in finding equivalent classes */
  private OWLReasoner reasoner;

  /** The mode that represents a particular equivalent class reasoning strategy, once found */
  private EquivalentClassReasoningMode mode;

  private OWLDataFactory dataFactory;

  /**
   * Growable Set of Equivalent Class Axioms that are found by the reasoner that also fail the test
   * in the above reasoningTest. These are Equivalent Classes that are not allowed.
   */
  private Set<OWLEquivalentClassesAxiom> failingAxioms;

  /**
   * Growable set of found Equivalenct Class Axioms. As the reasoner finds equivalent classes, they
   * are added here
   */
  private Set<OWLEquivalentClassesAxiom> foundEquivalentAxioms;

  /**
   * Creates a new instance that will reason over the given ontology with the strategy specified by
   * the EquivalentClassReasoningMode.
   *
   * @param ontology OWLOntology to reason equivalence classes over
   * @param reasoner OWLReasoner that will reason over the ontology
   * @param mode One of ALL, NONE, or ASSERTED_ONLY, representing a strategy to deal with found
   *     equivalent classes
   */
  public EquivalentClassReasoning(
      OWLOntology ontology, OWLReasoner reasoner, EquivalentClassReasoningMode mode) {
    this.ontology = ontology;
    this.reasoner = reasoner;
    dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    this.reasoningTest = createTest(mode, ontology);
    failingAxioms = Sets.newHashSet();
    foundEquivalentAxioms = Sets.newHashSet();
    this.mode = mode;
  }

  /**
   * Uses the reasoner to find all equivalent classes in the ontology, and then adds the axioms that
   * are not allowed the the list of failed axioms. Once reasoning has occurred, a report can be
   * logged with #logReport
   *
   * @return True if there were no failing equivalence axioms found, False if any failed the test
   */
  public boolean reason() {

    for (OWLClass c : ontology.getClassesInSignature()) {
      Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(c).getEntitiesMinus(c);
      foundEquivalentAxioms.addAll(this.createEquivalenceAxioms(c, equivalentClasses));
    }

    for (OWLEquivalentClassesAxiom axiom : foundEquivalentAxioms) {
      if (reasoningTest.fails(axiom)) {
        failingAxioms.add(axiom);
      }
    }
    return failingAxioms.isEmpty();
  }

  /**
   * Uses the provided logger to print out the results of the reasoning operation.
   *
   * @param logger an SLF4J logger instance
   */
  public void logReport(Logger logger) {
    if (!foundEquivalentAxioms.isEmpty()) {
      logger.info("Inferred the following equivalent classes");
      for (OWLEquivalentClassesAxiom anAxiom : foundEquivalentAxioms) {
        logger.info(equivAxiomToString(anAxiom));
      }
    }

    if (!failingAxioms.isEmpty()) {
      logger.error(mode.getExplanation());
      for (OWLEquivalentClassesAxiom axiom : failingAxioms) {
        logger.error(equivAxiomToString(axiom));
      }
    }
  }

  private Set<OWLEquivalentClassesAxiom> createEquivalenceAxioms(
      OWLClass c, Set<OWLClass> equivalentClasses) {
    Set<OWLEquivalentClassesAxiom> equivalenceAxioms = new HashSet<>();
    for (OWLClass ec : equivalentClasses) {
      equivalenceAxioms.add(dataFactory.getOWLEquivalentClassesAxiom(c, ec));
    }
    return equivalenceAxioms;
  }

  /**
   * This is how we define the failure behavior given a Reasoning Mode.
   *
   * @param mode one of the enum instances that define how reasoning behavior should be over
   *     equivalent axioms
   * @param ontology Ontology to reason over
   * @return The EquivalentAxiomReasoningTest instance with the implementation of how to detect a
   *     failing Equivalence Axiom
   */
  private static EquivalentAxiomReasoningTest createTest(
      EquivalentClassReasoningMode mode, OWLOntology ontology) {
    EquivalentAxiomReasoningTest test = null;
    switch (mode) {
      case ALL:
        test = axiom -> false;
        break;
      case NONE:
        test = axiom -> true;
        break;
      case ASSERTED_ONLY:
        test =
            axiom ->
                !ontology.containsAxiom(
                    axiom, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS);
        break;
    }
    return test;
  }

  private static String equivAxiomToString(OWLEquivalentClassesAxiom axiom) {
    StringBuilder equivalentClassesString = new StringBuilder("Equivalence: ");
    Iterator<OWLClass> classIterator = axiom.getNamedClasses().iterator();
    while (classIterator.hasNext()) {
      equivalentClassesString.append(classIterator.next().toString());
      if (classIterator.hasNext()) {
        equivalentClassesString.append(" == ");
      }
    }
    return equivalentClassesString.toString();
  }
}
