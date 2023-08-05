package org.obolibrary.robot;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.jena.query.Dataset;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.obolibrary.robot.checks.Violation;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(Parameterized.class)
public class ReportQueriesTest extends CoreTest {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "missing_label",
            "/1017-empty-label-input.owl",
            new String[] {
              "http://test.org/test#B", "http://test.org/test#C", "http://test.org/test#D"
            }
          }
        });
  }

  private final String inputFile;
  private final String queryName;
  private final String[] failingEntities;

  public ReportQueriesTest(
      final String queryFile, final String inputFile, final String[] failingEntities) {
    this.queryName = queryFile;
    this.inputFile = inputFile;
    this.failingEntities = failingEntities;
  }

  /**
   * Test report queries
   *
   * @throws Exception on any problem
   */
  @Test
  public void testReportQuery() throws Exception {
    final OWLOntology ontology = loadOntology(inputFile);
    final Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, false);

    final InputStream is = getClass().getResourceAsStream("/report_queries/" + queryName + ".rq");
    assert is != null;
    final String query =
        new Scanner(is, "UTF-8").useDelimiter("" + Character.LINE_SEPARATOR).next();
    final List<Violation> violations =
        ReportOperation.getViolations(
            new IOHelper(), dataset, queryName, query, Collections.emptyMap());

    final Set<String> expected = new HashSet<>(Arrays.asList(failingEntities));
    assert violations != null;
    final Set<String> actual =
        violations.stream().map(v -> v.entity.getIRI().toString()).collect(Collectors.toSet());
    Assert.assertEquals(expected, actual);
  }
}
