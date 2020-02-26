package org.obolibrary.robot.export;

import java.util.*;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Row {

  // List of cells in this row
  private Map<String, Cell> cells;

  /** Init a new Row. */
  public Row() {
    cells = new HashMap<>();
  }

  /**
   * Add a cell to the row.
   *
   * @param cell Cell to add
   */
  public void add(Cell cell) {
    cells.put(cell.getColumnName(), cell);
  }

  /**
   * Get the Cell value of a column.
   *
   * @param columnName column name
   * @return one or more cell values (List)
   */
  public List<String> get(String columnName) {
    Cell c = cells.getOrDefault(columnName, null);
    return c.getValue();
  }

  /**
   * Render the Row as an array.
   *
   * @param columns list of Columns
   * @return String[] representation of row
   */
  public String[] toArray(List<Column> columns, String split) {
    String[] row = new String[columns.size()];

    int i = 0;
    for (Column c : columns) {
      String columnName = c.getName();
      Cell cell = cells.getOrDefault(columnName, null);
      String value;
      if (cell != null) {
        value = String.join(split, cell.getValue());
      } else {
        value = "";
      }
      row[i] = value;
      i++;
    }
    return row;
  }
}
