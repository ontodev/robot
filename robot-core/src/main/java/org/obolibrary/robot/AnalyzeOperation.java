package org.obolibrary.robot;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.ReasonOperation;
import org.obolibrary.robot.RelaxOperation;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyze logical axioms in an ontology.
 *
 * <p>For complete documentation, see http://robot.obolibrary.org/analyze
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
   * <code>Power = | E(O) - E(O-A) |</code>
   *
   * <p>The power of a logical axiom is the number of entailments it contributes to
   *
   * <p>Algorithm:
   *
   * <ul>
   * <li> E(O) is assumed to be calculated in advance. 
   * <li> A is removed from O - Each axiom in E(O) is tested to see if it can still be entailed 
   * <li> if it can, it is added to E(O-A) - A is added back to O
   * </ul>
   *
   * @param axiom - axiom to be tested
   * @param reasoner
   * @param infAxioms - axioms inferred from whole ontology, i.e. E(O)
   * @return axiom power calculation
   */
  public static int getLogicalAxiomPower(
      OWLLogicalAxiom axiom, OWLReasoner reasoner, Set<OWLLogicalAxiom> infAxioms) {
    OWLOntology ontology = reasoner.getRootOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    manager.removeAxiom(ontology, axiom);
    reasoner.flush();

    Set<OWLLogicalAxiom> infAxiomsPostRemoval = new HashSet<>();
    for (OWLLogicalAxiom a : infAxioms) {
      if (isEntailed(reasoner, a)) {
        infAxiomsPostRemoval.add(a);
      }
    }

    SetView<OWLLogicalAxiom> remaining = Sets.difference(infAxioms, infAxiomsPostRemoval);

    // for (OWLAxiom a : remaining) {
    //  logger.info(" R=" + a);
    // }
    
    // restore
    manager.addAxiom(ontology, axiom);
    logger.info("Base=" + infAxioms.size() + " Rem=" + remaining.size()+" Axiom:"+axiom);
    return remaining.size();
  }

  /**
   * Remove any logical axioms that can be inferred, if they were to be removed
   * 
   * @param reasoner
   * @return removed axioms
   */
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

  /**
   * 
   * Note that the owlapi reasoner API provides this method, but Elk does not implement.
   * 
   * Here we wrap that API, but if the reasoner is Elk, we only test for SubClassOf
   * 
   * @param reasoner
   * @param axiom
   * @return true if axiom is entailed
   */
  public static boolean isEntailed(OWLReasoner reasoner, OWLLogicalAxiom axiom) {
    logger.debug("Testing: " + axiom);
    
    // test for trivial case, no need to invoke reasoner
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

  /**
   * Gets non-redundant logical axioms that are inferred by a reasoner, excluding
   * those that are currently asserted
   * 
   * Currently only implemented for SubClassOf
   * 
   * This excludes trivial axioms: SubClassOf(?x, Thing) and SubClassOf(Nothing ?x)
   * 
   * Note: in theory the owlapi InferredAxiomGenerator can provide this functionality,
   * but I was not able to get this to work to my satisfaction
   * 
   * @param reasoner
   * @return all inferred axioms (currently SubClassOf only)
   */
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
