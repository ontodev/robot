package org.obolibrary.robot.export;

import java.util.List;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Cell {

  // Column object used to build this cell
  private Column column;

  // List of output values for this cell
  private List<String> outputValues;

  /**
   * Init a new Cell for a Column.
   *
   * @param column Column for the cell
   * @param outputValues List of string output values
   */
  public Cell(Column column, List<String> outputValues) {
    this.column = column;
    this.outputValues = outputValues;
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
  public List<String> getValue() {
    return outputValues;
  }
}
