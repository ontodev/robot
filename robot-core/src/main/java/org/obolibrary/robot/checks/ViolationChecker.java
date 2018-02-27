package org.obolibrary.robot.checks;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.obolibrary.robot.QueryOperation;
import org.obolibrary.robot.UnmergeOperation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViolationChecker {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /**
   * Display number of violations by severity as ERROR. If no violations, just provide INFO. TODO:
   * Detailed logging on triples from each query.
   *
   * @param ontology OWLOntology to report on
   * @param queries Set of CheckerQuery objects
   * @throws Exception on any problem
   */
  public static void getViolations(OWLOntology ontology, Set<CheckerQuery> queries)
      throws Exception {
	// Store severity level violations separately
    Map<String, List<Triple<String, String, String>>> severity1 = new HashMap<>();
    Map<String, List<Triple<String, String, String>>> severity2 = new HashMap<>();
    Map<String, List<Triple<String, String, String>>> severity3 = new HashMap<>();
    Map<String, List<Triple<String, String, String>>> severity4 = new HashMap<>();
    Map<String, List<Triple<String, String, String>>> severity5 = new HashMap<>();
    
    // Track number of violations for each severity level
    Integer count1 = 0;
    Integer count2 = 0;
    Integer count3 = 0;
    Integer count4 = 0;
    Integer count5 = 0;

    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    for (CheckerQuery query : queries) {
      List<Triple<String, String, String>> violations;
      switch (query.severity) {
        case 1:
          violations = getViolations(dsg, query);
          severity1.put(query.title, violations);
          count1 += violations.size();
        case 2:
          violations = getViolations(dsg, query);
          severity2.put(query.title, violations);
          count2 += violations.size();
        case 3:
          violations = getViolations(dsg, query);
          severity3.put(query.title, violations);
          count3 += violations.size();
        case 4:
          violations = getViolations(dsg, query);
          severity4.put(query.title, violations);
          count4 += violations.size();
        case 5:
          violations = getViolations(dsg, query);
          severity5.put(query.title, violations);
          count5 += violations.size();
      }
    }

    Integer violations = count1 + count2 + count3 + count4 + count5;
    if (violations != 0) {
      logger.error("REPORT FAILED! Violations: " + violations);
      logger.error("Severity 1 violations: " + count1);
      logger.error("Severity 2 violations: " + count2);
      logger.error("Severity 3 violations: " + count3);
      logger.error("Severity 4 violations: " + count4);
      logger.error("Severity 5 violations: " + count5);
    } else {
      logger.info("No violations found.");
    }
  }

  /**
   * Get the violations from one CheckerQuery.
   *
   * @param ontology OWLOntology to report on
   * @param query CheckerQuery object with title and queryString
   * @return Map with query title as key and list of returned triples as value
   * @throws Exception on any problem
   */
  private static List<Triple<String, String, String>> getViolations(DatasetGraph dsg,
	  CheckerQuery query) throws Exception {
    ResultSet violationSet = QueryOperation.execQuery(dsg, query.queryString);

    List<Triple<String, String, String>> violations = new ArrayList<>();
    while (violationSet.hasNext()) {
      QuerySolution violation = violationSet.next();
      String entity;
      String property;
      String value;
      try {
        entity = violation.get("entity").toString();
      } catch (NullPointerException e) {
        entity = null;
      }
      try {
        property = violation.get("property").toString();
      } catch (NullPointerException e) {
        property = null;
      }
      try {
        value = violation.get("value").toString();
      } catch (NullPointerException e) {
        value = null;
      }
      violations.add(Triple.of(entity, property, value));
    }
    return violations;
  }
}
