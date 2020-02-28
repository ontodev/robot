package org.obolibrary.robot.export;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ShortFormProvider;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Column {

  // Header name for resolving rows
  private String name;

  // Header name for output
  // This may be the same as the name, unless a tag (e.g., [ID]) was specified
  // The display name includes the tag, but the name does not
  private String displayName;

  // Optional header IRI if a property is used in this column
  private IRI iri = null;
  private OWLAnnotationProperty annotationProperty = null;
  private OWLDataProperty dataProperty = null;
  private OWLObjectProperty objectProperty = null;

  // Target object (e.g., annotation) for cell values
  // private OWLObject targetObject;

  // Provider to render short form of entities
  private ShortFormProvider shortFormProvider;

  // If true, sort on this column
  private int sortOrder = -1;

  // If true, reverse sort
  private boolean reverseSort = false;

  public Column(String name, String displayName, IRI iri, ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = displayName;
    this.iri = iri;
    this.shortFormProvider = shortFormProvider;
  }

  public Column(
      String name,
      String displayName,
      OWLAnnotationProperty annotationProperty,
      ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = displayName;
    this.annotationProperty = annotationProperty;
    this.shortFormProvider = shortFormProvider;
  }

  public Column(
      String name,
      String displayName,
      OWLDataProperty dataProperty,
      ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = displayName;
    this.dataProperty = dataProperty;
    this.shortFormProvider = shortFormProvider;
  }

  public Column(
      String name,
      String displayName,
      OWLObjectProperty objectProperty,
      ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = displayName;
    this.objectProperty = objectProperty;
    this.shortFormProvider = shortFormProvider;
  }

  public String getDisplayName() {
    return displayName;
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
   * Return the short form provider for this column
   *
   * @return ShortFormProvider
   */
  public ShortFormProvider getShortFormProvider() {
    return shortFormProvider;
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

  public void setSort(int sortOrder, boolean reverseSort) {
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }
}
