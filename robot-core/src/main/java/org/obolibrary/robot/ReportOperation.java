package org.obolibrary.robot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Report issues with an ontology.
 *
 * <p>Currently this is minimal but we imagine later creating an extensive 'report card' for an
 * ontology, describing ways to make the ontology conform to OBO conventions
 *
 * <p>TODO: decide on report structure. Perhaps JSON-LD? Create vocabulary for report violations?
 */
public class ReportOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReportOperation.class);

  /**
   * Return a map from option name to default option value, for all the available report options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<String, String>();

    return options;
  }

  /**
   * reports ontology
   *
   * @param ontology the OWLOntology to report
   * @param iohelper IOHelper to work with ontology
   */
  public static void report(OWLOntology ontology, IOHelper iohelper) {
    report(ontology, iohelper, getDefaultOptions());
  }

  /**
   * reports ontology
   *
   * @param ontology the OWLOntology to report
   * @param iohelper IOHelper to work with ontology
   * @param options map of report options
   */
  public static void report(OWLOntology ontology, IOHelper iohelper, Map<String, String> options) {

    Set<InvalidReferenceViolation> refViolations =
        InvalidReferenceChecker.getInvalidReferenceViolations(ontology, true);
    for (InvalidReferenceViolation v : refViolations) {
      System.err.println("REFERENCE VIOLATION: " + v);
    }
    int totalViolations = refViolations.size();
    if (totalViolations > 0) {
      System.err.println("REPORT FAILED! Violations: " + totalViolations);
    }
  }
}
