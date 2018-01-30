package org.obolibrary.robot.report;

import org.obolibrary.robot.ReportOperation;

/**
 * The output of a {@link ReportOperation} is a ReportCard. This provides
 * 
 *  - a list of all problems/violations (graded by severity)
 *  - statistics on use of various properties in the ontology (TODO)
 * 
 * 
 * Developer Note: this is designed to be serializable via Jackson; we
 * use mostly POJOs and use jackson annotations to serialize OWL objects
 * using toString
 *
 * @author cjm
 */
public class ReportCard {

  /**
   * All issues (of varying severity) with the ontology
   */
  public ProblemsReport problemsReport;
  
  // TODO: aggregate statistics
}
