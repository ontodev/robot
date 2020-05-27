package org.obolibrary.robot.checks;

import java.util.*;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Violation {

  private final Logger logger = LoggerFactory.getLogger(Violation.class);

  public OWLEntity entity;
  public Map<OWLEntity, List<OWLObject>> statements;

  /**
   * Create a new Violation object about an OWL entity.
   *
   * @param subject OWLEntity that is the subject of the violation
   */
  public Violation(OWLEntity subject) {
    this.entity = subject;
    this.statements = new HashMap<>();
  }

  /**
   * Add a statement to the Violation about the subject.
   *
   * @param property OWLEntity property
   * @param value OWLObject value
   */
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

  @Deprecated
  public String subject;

  /**
   *
   * @param subject String subject
   */
  @Deprecated
  public Violation(String subject) {
    logger.error("new Violation(subject) is no longer supported - please use new Violation(OWLEntity) instead.");
  }

  /**
   *
   * @param property String property
   * @param value String value
   */
  @Deprecated
  public void addStatement(String property, String value) {
    logger.error("addStatement(String, String) is no longer supported - please use addStatement(OWLEntity, OWLObject) instead.");
  }
}
