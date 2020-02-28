package org.obolibrary.robot.export;

import java.util.*;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Row {

  // TODO - add IRI
  // TODO - Add toHTML function that returns resource="iri" in <tr>

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
  public List<String> getDisplayValues(String columnName) {
    Cell c = cells.getOrDefault(columnName, null);
    return c.getDisplayValues();
  }

  /**
   * Get the Cell sort value of a column.
   *
   * @param columnName column name
   * @return one or more cell values (List)
   */
  public List<String> getSortValues(String columnName) {
    Cell c = cells.getOrDefault(columnName, null);
    return c.getSortValues();
  }

  /**
   * Render the Row as an array. The cells will only include their display values.
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
        value = String.join(split, cell.getDisplayValues());
      } else {
        value = "";
      }
      row[i] = value;
      i++;
    }
    return row;
  }

  public String toHTML(List<Column> columns, String split) {
    return "";
  }

  public String toString(
      List<Column> columns, String split, String leftQuote, String rightQuote, String delimiter) {
    StringBuilder sb = new StringBuilder();
    for (Column c : columns) {
      String columnName = c.getName();
      Cell cell = cells.getOrDefault(columnName, null);
      String value;
      if (cell != null) {
        value = String.join(split, cell.getDisplayValues());
      } else {
        value = "";
      }
      String quotedValue = leftQuote + value + rightQuote + delimiter;
      sb.append(quotedValue);
    }
    return sb.toString();
  }
}
