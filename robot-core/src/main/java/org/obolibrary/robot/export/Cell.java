package org.obolibrary.robot.export;

import java.util.Collections;
import java.util.List;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Cell {

  // Column object used to build this cell
  private Column column;

  // List of output values for this cell
  private List<String> displayValues;

  // TODO - sort values string with split instead of list string

  // List of sort values for this cell (may be different than the display value)
  private List<String> sortValues;

  /**
   * @param column
   * @param displayValues
   */
  public Cell(Column column, List<String> displayValues) {
    this.column = column;
    this.displayValues = displayValues;
    this.sortValues = displayValues;
  }

  /**
   * Init a new Cell for a Column with mulitple values in a cell.
   *
   * @param column Column for the cell
   * @param displayValues List of string output values
   * @param sortValues List of string sort values
   */
  public Cell(Column column, List<String> displayValues, List<String> sortValues) {
    this.column = column;
    this.displayValues = displayValues;
    this.sortValues = sortValues;
  }

  /**
   * Init a Cell for a Column with single value in cell with no sort value.
   *
   * @param column
   * @param displayValue
   */
  public Cell(Column column, String displayValue) {
    this.column = column;
    displayValues = Collections.singletonList(displayValue);
    sortValues = displayValues;
  }

  /**
   * Init a Cell for a Column with single value in cell.
   *
   * @param column
   * @param displayValue
   * @param sortValue
   */
  public Cell(Column column, String displayValue, String sortValue) {
    this.column = column;
    displayValues = Collections.singletonList(displayValue);
    sortValues = Collections.singletonList(sortValue);
  }

  /**
   * Get the column name for a cell.
   *
   * @return String column name
   */
  public String getColumnName() {
    return column.getName();
  }

  /**
   * Get the value of a cell.
   *
   * @return String value
   */
  public List<String> getDisplayValues() {
    return displayValues;
  }

  /**
   * Get the sort value of a cell.
   *
   * @return String value
   */
  public List<String> getSortValues() {
    return sortValues;
  }
}
