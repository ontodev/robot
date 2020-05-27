package org.obolibrary.robot.export;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  // Optional header IRI or property used in this column
  // One of these must not be null
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

  private boolean includeNamed = false;
  private boolean includeAnonymous = false;

  /**
   * Init a new Column using an IRI.
   *
   * @param name Column name
   * @param displayName Column display name
   * @param iri IRI of column property
   * @param shortFormProvider ShortFormProvider to use when rendering values
   * @param entitySelect
   */
  public Column(
      String name,
      String displayName,
      @Nonnull IRI iri,
      @Nonnull ShortFormProvider shortFormProvider,
      @Nonnull String entitySelect) {
    this.name = name;
    this.displayName = displayName;
    this.iri = iri;
    this.shortFormProvider = shortFormProvider;
    setEntitySelect(entitySelect);
  }

  /**
   * Init a new Column using an annotation property.
   *
   * @param name Column name
   * @param displayName Column display name
   * @param annotationProperty column OWLAnnotationProperty
   * @param shortFormProvider ShortFormProvider to use when rendering values
   */
  public Column(
      String name,
      String displayName,
      @Nonnull OWLAnnotationProperty annotationProperty,
      @Nonnull ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = displayName;
    this.annotationProperty = annotationProperty;
    this.shortFormProvider = shortFormProvider;
  }

  /**
   * Init a new Column using a data property.
   *
   * @param name Column name
   * @param displayName Column display name
   * @param dataProperty column OWLDataProperty
   * @param shortFormProvider ShortFormProvider to use when rendering values
   * @param entitySelect
   */
  public Column(
      String name,
      String displayName,
      @Nonnull OWLDataProperty dataProperty,
      @Nonnull ShortFormProvider shortFormProvider,
      @Nonnull String entitySelect) {
    this.name = name;
    this.displayName = displayName;
    this.dataProperty = dataProperty;
    this.shortFormProvider = shortFormProvider;
    setEntitySelect(entitySelect);
  }

  /**
   * Init a new column using an object property.
   *
   * @param name Column name
   * @param displayName Column display name
   * @param objectProperty column OWLObjectProperty
   * @param shortFormProvider ShortFormProvider to use when rendering values
   * @param entitySelect
   */
  public Column(
      String name,
      String displayName,
      @Nonnull OWLObjectProperty objectProperty,
      @Nonnull ShortFormProvider shortFormProvider,
      @Nonnull String entitySelect) {
    this.name = name;
    this.displayName = displayName;
    this.objectProperty = objectProperty;
    this.shortFormProvider = shortFormProvider;
    setEntitySelect(entitySelect);
  }

  public Column(String name, @Nonnull ShortFormProvider shortFormProvider) {
    this.name = name;
    this.shortFormProvider = shortFormProvider;
    displayName = name;
    includeNamed = true;
    includeAnonymous = true;
  }

  /**
   * Get the display name of a column.
   *
   * @return String display name
   */
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

  /**
   * Return the OWLProperty used in a column, or null.
   *
   * @return OWLProperty or null
   */
  @Nullable
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

  /** @return */
  @Nonnull
  public boolean getIncludeAnonymous() {
    return includeAnonymous;
  }

  /** @return */
  @Nonnull
  public boolean getIncludeNamed() {
    return includeNamed;
  }

  /**
   * Return the IRI used in a column, or null.
   *
   * @return IRI or null
   */
  @Nullable
  public IRI getIRI() {
    return iri;
  }

  /**
   * Return the short form provider for this column
   *
   * @return ShortFormProvider
   */
  @Nonnull
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

  /** @param entitySelect */
  private void setEntitySelect(String entitySelect) {
    switch (entitySelect.toLowerCase()) {
      case "named":
        this.includeNamed = true;
        this.includeAnonymous = false;
        break;
      case "anon":
      case "anonymous":
        this.includeNamed = false;
        this.includeAnonymous = true;
        break;
      case "any":
      default:
        this.includeNamed = true;
        this.includeAnonymous = true;
    }
  }
}
