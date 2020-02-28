package org.obolibrary.robot.export;

import org.semanticweb.owlapi.model.*;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Column {

  // Header name
  private String name;

  // Optional header IRI if a property is used in this column
  private IRI iri = null;
  private OWLAnnotationProperty annotationProperty = null;
  private OWLDataProperty dataProperty = null;
  private OWLObjectProperty objectProperty = null;

  // Target object (e.g., annotation) for cell values
  // private OWLObject targetObject;

  // If true, sort on this column
  private int sortOrder = -1;

  // If true, reverse sort
  private boolean reverseSort = false;

  public Column(String name, IRI iri) {
    this.name = name;
    this.iri = iri;
  }

  public Column(String name, OWLAnnotationProperty annotationProperty) {
    this.name = name;
    this.annotationProperty = annotationProperty;
  }

  public Column(String name, OWLDataProperty dataProperty) {
    this.name = name;
    this.dataProperty = dataProperty;
  }

  public Column(String name, OWLObjectProperty objectProperty) {
    this.name = name;
    this.objectProperty = objectProperty;
  }

  public Column(String name, IRI iri, int sortOrder, boolean reverseSort) {
    this.name = name;
    this.iri = iri;
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }

  public Column(
      String name, OWLAnnotationProperty annotationProperty, int sortOrder, boolean reverseSort) {
    this.name = name;
    this.annotationProperty = annotationProperty;
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }

  public Column(String name, OWLDataProperty dataProperty, int sortOrder, boolean reverseSort) {
    this.name = name;
    this.dataProperty = dataProperty;
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }

  public Column(String name, OWLObjectProperty objectProperty, int sortOrder, boolean reverseSort) {
    this.name = name;
    this.objectProperty = objectProperty;
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }

  /**
   * Get the name of a column.
   *
   * @return String name of column
   */
  public String getName() {
    return name;
  }

  public OWLProperty getProperty() {
    if (annotationProperty != null) {
      return annotationProperty;
    } else if (dataProperty != null) {
      return dataProperty;
    } else if (objectProperty != null) {
      return objectProperty;
    }
    return null;
  }

  public IRI getIRI() {
    return iri;
  }

  /**
   * Get the int sort order of the column.
   *
   * @return int sort order
   */
  public int getSortOrder() {
    return sortOrder;
  }

  /**
   * Get reverse sort for column.
   *
   * @return boolean reverse sort
   */
  public boolean isReverseSort() {
    return reverseSort;
  }
}
