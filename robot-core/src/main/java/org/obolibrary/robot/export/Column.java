package org.obolibrary.robot.export;

import java.util.List;
import java.util.Map;
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
  private Map<String, List<String>> rules = null;
  private String displayRule = null;

  // Target object (e.g., annotation) for cell values
  // private OWLObject targetObject;

  // Provider to render short form of entities
  private ShortFormProvider shortFormProvider = null;

  // If true, sort on this column
  private int sortOrder = -1;

  // If true, reverse sort
  private boolean reverseSort = false;

  private boolean includeNamed = false;
  private boolean includeAnonymous = false;

  /**
   * Init a new column with a name.
   *
   * @param name Column name
   */
  public Column(String name) {
    this.name = name;
    this.displayName = name;
  }

  /**
   * Init a new column with a name and a display name.
   *
   * @param name Column name
   * @param displayName Column display name
   */
  public Column(String name, String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  /**
   * Init a new Column using an IRI.
   *
   * @param name Column name
   * @param displayName Column display name
   * @param iri IRI of column property
   * @param shortFormProvider ShortFormProvider to use when rendering values
   * @param entitySelect String keyword to set includeNamed and includeAnonymous
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
   * @param entitySelect String keyword to set includeNamed and includeAnonymous
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
   * @param entitySelect String keyword to set includeNamed and includeAnonymous
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

  /**
   * Init a new column with a name and a provider.
   *
   * @param name Column name
   * @param shortFormProvider ShortFormProvider to use when rendering values
   */
  public Column(String name, @Nonnull ShortFormProvider shortFormProvider) {
    this.name = name;
    this.shortFormProvider = shortFormProvider;
    displayName = name;
    includeNamed = true;
    includeAnonymous = true;
  }

  /**
   * Init a new column using one or more rules for validate.
   *
   * @param name Column name
   * @param rules Column rules
   * @param displayRule String raw display rule for output
   * @param shortFormProvider ShortFormProvider for rendering
   */
  public Column(
      String name,
      Map<String, List<String>> rules,
      String displayRule,
      @Nonnull ShortFormProvider shortFormProvider) {
    this.name = name;
    this.displayName = name;
    this.rules = rules;
    this.displayRule = displayRule;
    this.shortFormProvider = shortFormProvider;
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
   * Get the display rule of a column for validation.
   *
   * @return String display rule
   */
  public String getDisplayRule() {
    return displayRule;
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

  /** @return true if including anonymous entities in this column */
  public boolean getIncludeAnonymous() {
    return includeAnonymous;
  }

  /** @return true if including named entities in this column */
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
   * Return the rules used in column for validate, or null.
   *
   * @return map of rules or null
   */
  @Nullable
  public Map<String, List<String>> getRules() {
    return rules;
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

  /**
   * Set the sort order for this column.
   *
   * @param sortOrder int sort order (starting at 0)
   */
  public void setSort(int sortOrder) {
    this.sortOrder = sortOrder;
    this.reverseSort = false;
  }

  /**
   * Set the sort order for this column and if it should be sorted in reverse.
   *
   * @param sortOrder int sort order (starting at 0)
   * @param reverseSort if true, sort in reverse
   */
  public void setSort(int sortOrder, boolean reverseSort) {
    this.sortOrder = sortOrder;
    this.reverseSort = reverseSort;
  }

  /**
   * Set the entity selection values (includeNamed and includeAnonymous) based on the entity select
   * string: NAMED, ANON/ANONYMOUS, or ANY.
   *
   * @param entitySelect entity select string
   */
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
