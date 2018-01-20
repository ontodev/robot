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
    List<Map<String, List<Triple>>> severity1 = new ArrayList<>();
    List<Map<String, List<Triple>>> severity2 = new ArrayList<>();
    List<Map<String, List<Triple>>> severity3 = new ArrayList<>();
    List<Map<String, List<Triple>>> severity4 = new ArrayList<>();
    List<Map<String, List<Triple>>> severity5 = new ArrayList<>();

    for (CheckerQuery query : queries) {
      switch (query.severity) {
        case 1:
          severity1.add(getViolations(ontology, query));
        case 2:
          severity2.add(getViolations(ontology, query));
        case 3:
          severity3.add(getViolations(ontology, query));
        case 4:
          severity4.add(getViolations(ontology, query));
        case 5:
          severity5.add(getViolations(ontology, query));
      }
    }

    Integer violations =
        severity1.size()
            + severity2.size()
            + severity3.size()
            + severity4.size()
            + severity5.size();

    if (violations != 0) {
      logger.error("REPORT FAILED! Violations: " + violations);
      logger.error("Severity 1 violations: " + severity1.size());
      logger.error("Severity 2 violations: " + severity2.size());
      logger.error("Severity 3 violations: " + severity3.size());
      logger.error("Severity 4 violations: " + severity4.size());
      logger.error("Severity 5 violations: " + severity5.size());
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
  private static Map<String, List<Triple>> getViolations(OWLOntology ontology, CheckerQuery query)
      throws Exception {
    DatasetGraph dsg = QueryOperation.loadOntology(ontology);
    ResultSet violationSet = QueryOperation.execQuery(dsg, query.queryString);

    List<Triple> violations = new ArrayList<>();
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
    return new HashMap<String, List<Triple>>() {
      {
        put(query.title, violations);
      }
    };
  }
}
