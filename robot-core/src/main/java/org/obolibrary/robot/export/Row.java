package org.obolibrary.robot.export;

import java.util.*;
import org.semanticweb.owlapi.model.IRI;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Row {

  // The subject of this row
  private IRI subject;

  // List of cells in this row
  private Map<String, Cell> cells = new HashMap<>();

  /** Init a new Row. */
  public Row(IRI subject) {
    this.subject = subject;
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
  public String getSortValueString(String columnName) {
    Cell c = cells.getOrDefault(columnName, null);
    return c.getSortValueString();
  }

  public IRI getSubject() {
    return subject;
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
      String columnName = c.getDisplayName();
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

  /**
   * Render the Row as a row in an HTML table.
   *
   * @param columns list of Columns
   * @param split character to split multiple values on
   * @return
   */
  public String toHTML(List<Column> columns, String split) {
    StringBuilder sb = new StringBuilder();
    sb.append("\t<tr resource=\"").append(subject.toString()).append("\">");
    for (Column c : columns) {
      String columnName = c.getDisplayName();
      Cell cell = cells.getOrDefault(columnName, null);
      String value;
      if (cell != null) {
        value = String.join(split, cell.getDisplayValues());
      } else {
        value = "";
      }
      sb.append("\t\t<td>").append(value).append("</td>\n");
    }
    return sb.toString();
  }
}
