package org.obolibrary.robot.checks;

import java.util.*;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

public class Violation {

  public OWLEntity subject;
  public Map<OWLEntity, List<OWLObject>> statements;

  public Violation(OWLEntity subject) {
    this.subject = subject;
    this.statements = new HashMap<>();
  }

  public void addStatement(OWLEntity property, OWLObject value) {
    List<OWLObject> values;
    if (statements.get(property) != null) {
      // This property already has one value in the map
      // Add this value to the existing list
      values = new ArrayList<>(statements.get(property));
      values.add(value);
    } else {
      // This property is not yet in the map
      // Instantiate a new list
      if (value != null) {
        values = Collections.singletonList(value);
      } else {
        values = Collections.emptyList();
      }
    }
    statements.put(property, values);
  }
}
