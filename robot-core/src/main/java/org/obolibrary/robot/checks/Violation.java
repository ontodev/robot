package org.obolibrary.robot.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Violation {

  public String subject;
  public Map<String, List<String>> statements;

  public Violation(String subject) {
    this.subject = subject;
    this.statements = new HashMap<>();
  }

  public void addStatement(String property, String value) {
    List<String> values = new ArrayList<>();
    if (statements.get(property) != null) {
      values.addAll(statements.get(property));
      values.add(value);
    } else {
      values = Arrays.asList(value);
    }
    statements.put(property, values);
  }
}
