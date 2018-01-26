package org.obolibrary.robot.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

/**
 * Task for concurrency in SPARQL querying.
 * @author <a href="mailto:rctauber@gmail.com>Becky Tauber</a>
 */
public class SparqlTask implements Callable<Map<CheckerQuery, List<BindingSet>>> {

  private RepositoryConnection conn;
  private CheckerQuery query;

  /**
   * Constructor.
   * 
   * @param conn open RespositoryConnection
   * @param query CheckerQuery object
   */
  public SparqlTask(RepositoryConnection conn, CheckerQuery query) {
    this.conn = conn;
    this.query = query;
  }

  /**
   * SPARQL task execution.
   * 
   * @return Map of CheckerQuery (key) and list of BindingSets from query result
   */
  @Override
  public Map<CheckerQuery, List<BindingSet>> call() throws Exception {
    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.queryString);
    TupleQueryResult result = tupleQuery.evaluate();
    List<BindingSet> bindingSet = new ArrayList<>();
    while (result.hasNext()) {
      bindingSet.add(result.next());
    }
    return new HashMap<CheckerQuery, List<BindingSet>>() {
      {
        put(query, bindingSet);
      }
    };
  }
}
