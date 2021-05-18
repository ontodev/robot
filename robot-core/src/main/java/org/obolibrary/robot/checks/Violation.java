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
  public Map<OWLEntity, List<OWLEntity>> entityStatements = new HashMap<>();
  // Statements about String subject
  public Map<OWLEntity, List<String>> literalStatements = new HashMap<>();

  // Deprecated statements with string properties
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
   * Create a new Violation object about a String. This is used for blank nodes.
   *
   * @param subject String subject
   */
  public Violation(String subject) {
    this.subject = subject;
  }

  /**
   * Add a statement with an OWLEntity value to the Violation about the subject.
   *
   * @param property OWLEntity property
   * @param value OWLEntity value
   */
  public void addStatement(OWLEntity property, OWLEntity value) {
    List<OWLEntity> values;
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

  /**
   * Add a literal statement to the Violation about the subject.
   *
   * @param property OWLEntity property
   * @param value String literal value
   */
  public void addStatement(OWLEntity property, String value) {
    List<String> values;
    if (literalStatements.get(property) != null) {
      values = new ArrayList<>(literalStatements.get(property));
      values.add(value);
    } else {
      if (value != null) {
        values = Collections.singletonList(value);
      } else {
        values = Collections.emptyList();
      }
    }
    literalStatements.put(property, values);
  }

  /**
   * @param property String property
   * @param value String value
   * @deprecated String properties are no longer recommended; create an OWLEntity for the property
   *     instead
   */
  @Deprecated
  public void addStatement(String property, String value) {
    List<String> values;
    if (statements.get(property) != null) {
      values = new ArrayList<>(statements.get(property));
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

  /**
   * @param property String property
   * @param object OWLObject value
   * @deprecated OWLObjects are no longer supported; use OWLEntity or String literal instead
   */
  @Deprecated
  public void addStatement(OWLEntity property, OWLObject object) {
    // Do nothing
    logger.error(
        String.format(
            "Cannot add OWLObject '%s'; use an OWLEntity or String literal instead.",
            object.toString()));
  }
}
