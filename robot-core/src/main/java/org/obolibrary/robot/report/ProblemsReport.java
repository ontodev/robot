package org.obolibrary.robot.report;

import java.util.Set;

import org.obolibrary.robot.checks.CURIEViolation;
import org.obolibrary.robot.checks.ClassMetadataViolation;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.obolibrary.robot.checks.OntologyMetadataViolation;

/**
 * Set of all problems of varying severity
 * 
 * This list will be extended over time
 * 
 * 
 * @author cjm
 *
 */
public class ProblemsReport {

  /**
   * references from axioms to deprecated or non-existent classes
   */
  public Set<InvalidReferenceViolation> invalidReferenceViolations;
  
  /**
   * problems with ontology header
   */
  public Set<OntologyMetadataViolation> ontologyMetadataViolations;
  
  /**
   * problems with metadata on classes
   */
  public Set<ClassMetadataViolation> classMetadataViolations;

  /**
   * problems with CURIEs (ids)
   */
  public Set<CURIEViolation> curieViolations;
}
