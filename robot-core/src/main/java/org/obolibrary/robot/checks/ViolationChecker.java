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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.obolibrary.robot.UnmergeOperation;
import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get specific violations within an ontology. Currently prints stats through logger.
 * 
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class ViolationChecker {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(UnmergeOperation.class);

  /**
   * Display number of violations based on severity level to the user as logger errors. If no
   * violations are found, no errors are returned. TODO: Formated reports
   *
   * @param ontology OWLOntology to report
   * @param queries Set of CheckerQueries
   * @throws Exception on any issue
   */
  public static void getViolations(OWLOntology ontology, Set<CheckerQuery> queries) throws Exception {
    Properties props = new Properties();
    props.load(ViolationChecker.class.getResourceAsStream("/blazegraph.properties"));
    File jnl = File.createTempFile("report", ".jnl");
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
    Map<CheckerQuery, List<BindingSet>> violations;
    try {
      violations = getViolations(conn, queries);
    } finally {
      conn.close();
    }

    for (Entry<CheckerQuery, List<BindingSet>> violation : violations.entrySet()) {
      CheckerQuery q = violation.getKey();
      List<BindingSet> result = violation.getValue();
      switch (q.severity) {
        case 1:
          severity1.put(q.title, result);
          count1 += result.size();
        case 2:
          severity2.put(q.title, result);
          count2 += result.size();
        case 3:
          severity3.put(q.title, result);
          count3 += result.size();
        case 4:
          severity4.put(q.title, result);
          count4 += result.size();
        case 5:
          severity5.put(q.title, result);
          count5 += result.size();
      }
    }

    Integer violationCount = count1 + count2 + count3 + count4 + count5;
    if (violationCount != 0) {
      logger.error("REPORT FAILED! Violations: " + violationCount);
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
   * Gets violation results through SPARQL querying.
   * 
   * @param conn open RepositoryConnection to repo with ontology to report on
   * @param queries Set of CheckerQueries
   * @return Map of CheckerQuery (key) and the list of BindingSets from the query result
   * @throws InterruptedException if ExecutorService is interrupted
   * @throws ExecutionException if a SparqlTask cannot be executed
   */
  private static Map<CheckerQuery, List<BindingSet>> getViolations(
      RepositoryConnection conn, Set<CheckerQuery> queries) throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newCachedThreadPool();

    List<SparqlTask> tasks = new ArrayList<>();
    for (CheckerQuery query : queries) {
      tasks.add(new SparqlTask(conn, query));
    }
    List<Future<Map<CheckerQuery, List<BindingSet>>>> futures = executor.invokeAll(tasks);
    executor.shutdown();

    Map<CheckerQuery, List<BindingSet>> results = new HashMap<>();
    for (Future<Map<CheckerQuery, List<BindingSet>>> f : futures) {
      results.putAll(f.get());
    }

    return results;
  }
}
