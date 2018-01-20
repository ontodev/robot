package org.obolibrary.robot.checks;

/** @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a> */
public class CheckerQuery {

  public String queryString;
  public String title;
  public Integer severity;

  /**
   * Creates a new CheckerQuery object based on a sparql query file. Expects '## title: ...' and '##
   * severity: ...', followed by '## ---' to denote end of header.
   *
   * @param queryString string of query file contents
   * @throws Exception if header is incorrectly formatted
   */
  public CheckerQuery(String queryString) throws Exception {
    String[] splits = queryString.split("## ---");
    String[] headers = splits[0].split("## ");
    for (String h : headers) {
      if (h.contains("title:")) {
        title = h.split(": ")[1].trim();
      } else if (h.contains("severity:")) {
        severity = Integer.parseInt(h.split(": ")[1].trim());
      }
    }
    this.queryString = splits[1];
    if (this.queryString == null || title == null || severity == null) {
      throw new Exception(
          "The following query string must contain the appropriate header:\n" + queryString);
    }
  }
}
