package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.checks.CheckViolation;
import org.obolibrary.robot.checks.ClassMetadataViolation;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.obolibrary.robot.checks.MetadataChecker;
import org.obolibrary.robot.checks.OntologyMetadataViolation;
import org.obolibrary.robot.report.ProblemsReport;
import org.obolibrary.robot.report.ReportCard;
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
   * @return report
   */
  public static ReportCard report(OWLOntology ontology, IOHelper iohelper) {
    return report(ontology, iohelper, getDefaultOptions());
  }

  /**
   * reports ontology
   *
   * @param ontology the OWLOntology to report
   * @param iohelper IOHelper to work with ontology
   * @param options map of report options
   * @return report
   */
  public static ReportCard report(
      OWLOntology ontology, IOHelper iohelper, Map<String, String> options) {

    ReportCard reportCard = new ReportCard();
    ProblemsReport problemsReport = new ProblemsReport();
    reportCard.problemsReport = problemsReport;

    Set<CheckViolation> violations = new HashSet<>();
    Map<Integer, Set<CheckViolation>> violationsBySeverity = new HashMap<>();

    Set<InvalidReferenceViolation> refViolations =
        InvalidReferenceChecker.getInvalidReferenceViolations(ontology, false);
    for (InvalidReferenceViolation v : refViolations) {
      logger.warn("REFERENCE VIOLATION: " + v);
    }
    violations.addAll(refViolations);
    problemsReport.invalidReferenceViolations = refViolations;

    Set<ClassMetadataViolation> classMetadataViolations =
        MetadataChecker.getClassMetadataViolations(ontology);
    if (classMetadataViolations.size() > 0) {
      for (ClassMetadataViolation v : classMetadataViolations) {
        logger.warn("Ontology Metadata violation: " + v);
      }
    }
    violations.addAll(classMetadataViolations);
    problemsReport.classMetadataViolations = classMetadataViolations;

    Set<OntologyMetadataViolation> ontologyMetadataViolations =
        MetadataChecker.getOntologyMetadataViolations(ontology);
    if (ontologyMetadataViolations.size() > 0) {
      for (OntologyMetadataViolation v : ontologyMetadataViolations) {
        logger.warn("Class Metadata violation: " + v);
      }
    }
    violations.addAll(ontologyMetadataViolations);
    problemsReport.ontologyMetadataViolations = ontologyMetadataViolations;

    if (violations.size() > 0) {
      for (int s = 1; s <= 5; s++) {
        violationsBySeverity.put(s, new HashSet<>());
      }
      logger.error("REPORT FAILED! Violations: " + violations.size());

      for (CheckViolation v : violations) {
        int s = v.getSeverity();
        violationsBySeverity.get(s).add(v);
      }

      for (int s = 1; s <= 5; s++) {
        logger.error("Severity " + s + " violations: " + violationsBySeverity.get(s).size());
      }
    }
    return reportCard;
  }
}
