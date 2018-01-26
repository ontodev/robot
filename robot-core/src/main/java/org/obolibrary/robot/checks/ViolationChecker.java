package org.obolibrary.robot.checks;

import com.bigdata.journal.Options;
import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.obolibrary.robot.UnmergeOperation;
import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViolationChecker {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /**
   * Display number of violations based on severity level to the user as logger errors.
   * If no violations are found, no errors are returned. TODO: Formated reports
   * 
   * @param ontology OWLOntology to report
   * @param queries Set of CheckerQueries
   * @throws Exception on any issue
   */
  public static void getViolations(OWLOntology ontology, Set<CheckerQuery> queries)
      throws Exception {
    File jnl = File.createTempFile("report", ".jnl");
    Properties props = new Properties();
    props.put(Options.BUFFER_MODE, "DiskRW");
    props.put(Options.FILE, jnl.getAbsolutePath());

    BigdataSailRepository repo = new BigdataSailRepository(new BigdataSail(props));
    repo.initialize();

    RepositoryConnection conn = repo.getConnection();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();

    // Load the ontology to the repo
    try {
      // Save in turtle format
      manager.saveOntology(ontology, new TurtleDocumentFormat(), out);
      conn.begin();
      try {
        // Read into the repo
        try (Reader r = new InputStreamReader(new ByteArrayInputStream(out.toByteArray()))) {
          conn.add(r, manager.getOntologyDocumentIRI(ontology).toString(), RDFFormat.TURTLE);
        }
        conn.commit();
      } catch (OpenRDFException e) {
        conn.rollback();
        throw e;
      }
    } finally {
      conn.close();
      out.close();
    }

    // Store severity level violations separately
    Map<String, List<BindingSet>> severity1 = new HashMap<>();
    Map<String, List<BindingSet>> severity2 = new HashMap<>();
    Map<String, List<BindingSet>> severity3 = new HashMap<>();
    Map<String, List<BindingSet>> severity4 = new HashMap<>();
    Map<String, List<BindingSet>> severity5 = new HashMap<>();

    // Track number of violations for each severity level
    Integer count1 = 0;
    Integer count2 = 0;
    Integer count3 = 0;
    Integer count4 = 0;
    Integer count5 = 0;

    conn = repo.getReadOnlyConnection();
    try {
	    for (CheckerQuery query : queries) {
	      List<BindingSet> violations;
	      switch (query.severity) {
	        case 1:
	          violations = getViolations(conn, query);
	          severity1.put(query.title, violations);
	          count1 += violations.size();
	        case 2:
	          violations = getViolations(conn, query);
	          severity2.put(query.title, violations);
	          count2 += violations.size();
	        case 3:
	          violations = getViolations(conn, query);
	          severity3.put(query.title, violations);
	          count3 += violations.size();
	        case 4:
	          violations = getViolations(conn, query);
	          severity4.put(query.title, violations);
	          count4 += violations.size();
	        case 5:
	          violations = getViolations(conn, query);
	          severity5.put(query.title, violations);
	          count5 += violations.size();
	      }
	    }
    } finally {
    	conn.close();
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
   * Returns a list of BindingSets from violation query results.
   * @param  conn open RepositoryConnection
   * @param  query CheckerQuery
   * @return List of BindingSets
   * @throws Exception on any issue
   */
  private static List<BindingSet> getViolations(RepositoryConnection conn, CheckerQuery query)
      throws Exception {
    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.queryString);
    // query.setIncludeInferred(true);
    TupleQueryResult result = tupleQuery.evaluate();

    List<BindingSet> violations = new ArrayList<>();
    while (result.hasNext()) {
      violations.add(result.next());
    }
    return violations;
  }
}
