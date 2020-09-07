package org.obolibrary.robot.checks;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Violation {

  private final Logger logger = LoggerFactory.getLogger(Violation.class);

  public OWLEntity entity;
  public String subject;

  // Statements about OWLEntity subject
  public Map<OWLEntity, List<OWLObject>> entityStatements = new HashMap<>();
  // Statements about String subject
  public Map<String, List<String>> statements = new HashMap<>();

  /**
   * Create a new Violation object about an OWL entity.
   *
   * @param subject OWLEntity that is the subject of the violation
   */
  public Violation(OWLEntity subject) {
    this.entity = subject;
  }

  /**
   * Add a statement to the Violation about the subject.
   *
   * @param property OWLEntity property
   * @param value OWLObject value
   */
  public void addStatement(OWLEntity property, OWLObject value) {
    List<OWLObject> values;
    if (entityStatements.get(property) != null) {
      // This property already has one value in the map
      // Add this value to the existing list
      values = new ArrayList<>(entityStatements.get(property));
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
    entityStatements.put(property, values);
  }

  /** @param subject String subject */
  public Violation(String subject) {
    this.subject = subject;
  }

  /**
   * @param property String property
   * @param value String value
   */
  public void addStatement(String property, String value) {
    List<String> values;
    if (statements.get(property) != null) {
      values = new ArrayList<String>(statements.get(property));
      values.add(value);
    } else {
      if (value != null) {
        values = Collections.singletonList(value);
      } else {
        values = Collections.emptyList();
      }
    }
    statements.put(property, values);
  }
}
