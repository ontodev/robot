package org.obolibrary.robot.export;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Column {

  // Header name
  private String name;

  // Target object (e.g., annotation) for cell values
  // private OWLObject targetObject;

  // If true, sort on this column
  private int sortOrder = -1;

  // If true, reverse sort
  private boolean reverseSort = false;

  /**
   * Init a new Column.
   *
   * @param name String name of column
   */
  public Column(String name) {
    this.name = name;
  }

  /**
   * Init a new Column.
   *
   * @param name String name of column
   * @param sortOrder int sort order
   * @param reverseSort boolean if true, sort in reverse
   */
  public Column(String name, int sortOrder, boolean reverseSort) {
    this.name = name;
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
