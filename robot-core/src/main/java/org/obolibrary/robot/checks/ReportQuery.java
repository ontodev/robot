package org.obolibrary.robot.checks;

import java.util.ArrayList;
import java.util.List;

public class ReportQuery {

  private final String ruleName;
  private final String ruleURL;
  private final String query;
  private final String level;

  private final List<Violation> violations = new ArrayList<>();

  /**
   * Create a new ReportQuery object.
   *
   * @param ruleName String name of rule
   * @param query String query contents
   * @param level String violation level
   */
  public ReportQuery(String ruleName, String query, String level) {
    this.ruleName = ruleName;
    this.ruleURL = null;
    this.query = query;
    this.level = level;
  }

  /**
   * Create a new ReportQuery object.
   *
   * @param ruleName String name of rule
   * @param ruleURL String link to rule documentation
   * @param query String query contents
   * @param level String violation level
   */
  public ReportQuery(String ruleName, String ruleURL, String query, String level) {
    this.ruleName = ruleName;
    this.ruleURL = ruleURL;
    this.query = query;
    this.level = level;
  }

  /**
   * Add multiple violations to this ReportQuery.
   *
   * @param vs list of Violations
   */
  public void addViolations(List<Violation> vs) {
    violations.addAll(vs);
  }

  /**
   * Return the violation level of this ReportQuery.
   *
   * @return String level
   */
  public String getLevel() {
    return level;
  }

  /**
   * Return the query contents of this ReportQuery.
   *
   * @return String SPARQL query contents
   */
  public String getQuery() {
    return query;
  }

  /**
   * Return the name of the rule for this ReportQuery.
   *
   * @return String rule name
   */
  public String getRuleName() {
    return ruleName;
  }

  /**
   * Return the URL for rule documentation (or null) for this ReportQuery.
   *
   * @return String rule documentation URL
   */
  public String getRuleURL() {
    return ruleURL;
  }

  /**
   * Return the collection of violations for this ReportQuery.
   *
   * @return List of Violations
   */
  public List<Violation> getViolations() {
    return violations;
  }
}
