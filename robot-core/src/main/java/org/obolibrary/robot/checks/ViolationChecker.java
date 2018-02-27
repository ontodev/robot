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
   * @return map of severity (key) and list of Violation objects (value)
   */
  public static Map<Integer, List<Violation>> getViolations(
      OWLOntology ontology, Set<CheckerQuery> queries) throws Exception {
    // create violation map
    Map<Integer, List<Violation>> violations;
    {
      violations = new HashMap<Integer, List<Violation>>();
      violations.put(1, new ArrayList<>());
      violations.put(2, new ArrayList<>());
      violations.put(3, new ArrayList<>());
      violations.put(4, new ArrayList<>());
      violations.put(5, new ArrayList<>());
    }

    Integer count1 = 0;
    Integer count2 = 0;
    Integer count3 = 0;
    Integer count4 = 0;
    Integer count5 = 0;

    // add results to map
    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    for (CheckerQuery query : queries) {
      Violation violation;
      switch (query.severity) {
        case 1:
          violation = getViolations(dsg, query);
          updateValues(violations, 1, violation);
          count1 += violation.getTriples().size();
        case 2:
          violation = getViolations(dsg, query);
          updateValues(violations, 2, violation);
          count2 += violation.getTriples().size();
        case 3:
          violation = getViolations(dsg, query);
          updateValues(violations, 3, violation);
          count3 += violation.getTriples().size();
        case 4:
          violation = getViolations(dsg, query);
          updateValues(violations, 4, violation);
          count4 += violation.getTriples().size();
        case 5:
          violation = getViolations(dsg, query);
          updateValues(violations, 5, violation);
          count5 += violation.getTriples().size();
      }
    }
    // get number of violations
    Integer violationCount = count1 + count2 + count3 + count4 + count5;
    if (violationCount != 0) {
      logger.error("REPORT FAILED! Violations: " + violationCount);
      logger.error("Severity 1 violations: " + count1);
      logger.error("Severity 2 violations: " + count2);
      logger.error("Severity 3 violations: " + count3);
      logger.error("Severity 4 violations: " + count4);
      logger.error("Severity 5 violations: " + count5);
    } else {
      logger.info("REPORT PASSED! No violations found.");
    }
    return violations;
  }

  /**
   * Get the violations from one CheckerQuery.
   *
   * @param ontology OWLOntology to report on
   * @param query CheckerQuery object with title and queryString
   * @return Map with query title as key and list of returned triples as value
   * @throws Exception on any problem
   */
  private static Violation getViolations(DatasetGraph dsg, CheckerQuery query) throws Exception {
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
    return new Violation(query, violations);
  }

  /**
   * Convenience function to add a new Violation value to a list of Violation objects associated
   * with an Integer key in a map.
   *
   * @param map the hash map
   * @param key the Integer key
   * @param addValue the Violation value to add
   * @return the map with the new value appended to the list associated with key
   */
  private static Map<Integer, List<Violation>> updateValues(
      Map<Integer, List<Violation>> map, Integer key, Violation addValue) {
    List<Violation> values = map.get(key);
    values.add(addValue);
    map.put(key, values);
    return map;
  }
}
