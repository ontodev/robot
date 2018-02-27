package org.obolibrary.robot.checks;

import java.util.List;
import org.apache.commons.lang3.tuple.Triple;

public class Violation {

  private String title;
  private List<Triple<String, String, String>> triples;

  public Violation(CheckerQuery query, List<Triple<String, String, String>> violations) {
    this.triples = violations;
    this.title = query.title;
  }

  public String getTitle() {
    return title;
  }

  public List<Triple<String, String, String>> getTriples() {
    return triples;
  }
}
