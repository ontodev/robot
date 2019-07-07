package org.obolibrary.robot;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.obolibrary.robot.exceptions.*;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyze logical axioms in an ontology
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class AnalyzeOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(AnalyzeOperation.class);

  private static final String NS = "analyze#";

  /**
   * Return a map from option name to default option value, for all the available reasoner options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("foo", "false");

    return options;
  }

  /**
   * Given an ontology and a reasoner factory, return the ontology with inferred axioms added after
   * reasoning. Use default options.
   *
   * @param ontology the ontology to reason over
   * @param reasonerFactory the factory to create a reasoner instance from
   * @throws OntologyLogicException if the ontology contains unsatisfiable classes, properties or
   *     inconsistencies
   * @throws OWLOntologyCreationException if ontology cannot be created
   * @throws InvalidReferenceException if the reference checker fails
   */
  public static double analyze(OWLOntology ontology, OWLReasonerFactory reasonerFactory)
      throws Exception {
    return analyze(ontology, reasonerFactory, null, getDefaultOptions());
  }

  /**
   * Given an ontology, a reasoner factory, and a map of options, return the ontology with inferred
   * axioms added after reasoning.
   *
   * @param ontology the ontology to reason over
   * @param reasonerFactory the factory to create a reasoner instance from
   * @param options a map of option strings, or null
   * @throws OntologyLogicException if the ontology contains unsatisfiable classes, properties or
   *     inconsistencies
   * @throws OWLOntologyCreationException if ontology cannot be created
   * @throws InvalidReferenceException if the reference checker fails
   * @throws IOException
   */
  public static double analyze(
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      String outputPath,
      Map<String, String> options)
      throws Exception {
    logger.info("Ontology has {} axioms.", ontology.getAxioms().size());

    List<String> reportLines = new ArrayList<>();

    BufferedWriter bw = null;
    if (outputPath != null) {
      FileWriter fw = new FileWriter(outputPath);
      bw = new BufferedWriter(fw);
    }

    // Check the ontology for reference violations
    // Maybe fail if prevent-invalid-references
    ReasonOperation.checkReferenceViolations(ontology, options);

    // Get the reasoner and run initial reasoning
    // No axioms are asserted in this step
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

    Set<OWLAxiom> removedAxioms = removeRedundantAxioms(reasoner);
    report("# |Removed_redundant_axioms| = " + removedAxioms.size(), bw);
    Set<OWLLogicalAxiom> originalAxioms = ontology.getLogicalAxioms();

    // we are uninterested in trivial inferences, so first relax
    // so that trivial axioms are asserted alreadt
    RelaxOperation.relax(ontology, options);

    Set<OWLLogicalAxiom> infAxioms = getInferredAxioms(reasoner);

    for (OWLAxiom a : infAxioms) {
      logger.info(" I=" + a);
    }

    int n = 0;
    int tPower = 0;
    Set<OWLLogicalAxiom> mostPowerfulAxioms = null;
    int maxPower = 0;
    for (OWLLogicalAxiom axiom : originalAxioms) {
      AxiomType<?> atype = axiom.getAxiomType();
      int power = getLogicalAxiomPower(axiom, reasoner, infAxioms);
      tPower += power;
      report(atype + "\t" + power + "\t" + axiom, bw);
      if (power > maxPower) {
        maxPower = power;
        mostPowerfulAxioms = new HashSet<>();
      }
      if (power > 0 && power == maxPower) {
        mostPowerfulAxioms.add(axiom);
      }
      n++;
    }
    double avgPower = tPower / (double) n;
    report("# Total_power = " + tPower, bw);
    report("# Average_power = " + avgPower, bw);
    report("# Most_powerful_axioms = " + maxPower + " Axioms: " + mostPowerfulAxioms, bw);

    if (bw != null) bw.close();

    return avgPower;
  }

  private static void report(String line, BufferedWriter bw) throws IOException {
    if (bw != null) bw.write(line + "\n");
    else System.out.println(line);
  }

  /**
   * Power = | E(O) - E(O-A) |
   * 
   * The power of a logical axiom is the number of entailments it contributes to
   * 
   * @param axiom
   * @param reasoner
   * @param infAxioms
   * @return axiom power calculation
   */
  public static int getLogicalAxiomPower(
      OWLLogicalAxiom axiom, OWLReasoner reasoner, Set<OWLLogicalAxiom> infAxioms) {
    OWLOntology ontology = reasoner.getRootOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxiom(ontology, axiom);
    reasoner.flush();
    Set<OWLLogicalAxiom> infAxiomsPostRemoval = getInferredAxioms(reasoner);
    SetView<OWLLogicalAxiom> remaining = Sets.difference(infAxioms, infAxiomsPostRemoval);

    for (OWLAxiom a : remaining) {
      logger.info(" R=" + a);
    }
    // restore
    manager.addAxiom(ontology, axiom);
    logger.info("Base=" + infAxioms.size() + " Rem=" + remaining.size());
    return remaining.size();
  }

  public static Set<OWLAxiom> removeRedundantAxioms(OWLReasoner reasoner) {
    OWLOntology ontology = reasoner.getRootOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    Set<OWLAxiom> redundantAxioms = new HashSet<>();
    for (OWLLogicalAxiom axiom : ontology.getLogicalAxioms()) {
      manager.removeAxiom(ontology, axiom);
      reasoner.flush();
      if (isEntailed(reasoner, axiom)) {
        logger.info("Redundant: " + axiom);
        redundantAxioms.add(axiom);
      }
      // restore
      manager.addAxiom(ontology, axiom);
    }
    logger.info("# Removed_redundant_axioms = " + redundantAxioms.size());
    manager.removeAxioms(ontology, redundantAxioms);
    return redundantAxioms;
  }

  public static boolean isEntailed(OWLReasoner reasoner, OWLLogicalAxiom axiom) {
    logger.info("Testing: " + axiom);
    if (axiom instanceof OWLSubClassOfAxiom) {
      OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom) axiom;
      if (sca.getSuperClass().isOWLThing()) {
        return true;
      }
    }

    if (reasoner instanceof ElkReasoner) {
      if (axiom instanceof OWLSubClassOfAxiom) {
        OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom) axiom;
        if (sca.getSuperClass().isAnonymous()) {
          return false;
        } else {
          return reasoner
              .getSuperClasses(sca.getSubClass(), false)
              .containsEntity(sca.getSuperClass().asOWLClass());
        }
      } else {
        return false;
      }
    } else {
      return reasoner.isEntailed(axiom);
    }
  }

  public static Set<OWLLogicalAxiom> getInferredAxioms(OWLReasoner reasoner) {
    // can't seem to get InferredAxiomGenerator to work...
    OWLOntology ontology = reasoner.getRootOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    Set<OWLLogicalAxiom> currentAxioms = ontology.getLogicalAxioms();

    reasoner.flush();
    Set<OWLLogicalAxiom> infAxioms = new HashSet<>();
    for (OWLClass c : ontology.getClassesInSignature()) {
      for (OWLClass sc : reasoner.getSuperClasses(c, true).getFlattened()) {
        OWLSubClassOfAxiom a = df.getOWLSubClassOfAxiom(c, sc);
        if (!currentAxioms.contains(a) && !sc.isOWLThing() && !c.isOWLNothing()) {
          infAxioms.add(a);
        }
      }
    }
    return infAxioms;
  }
}
