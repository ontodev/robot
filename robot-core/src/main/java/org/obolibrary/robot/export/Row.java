package org.obolibrary.robot.export;

import java.util.*;
import java.util.stream.Collectors;
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
   * Get the Cell sort value of a column.
   *
   * @param columnName column name
   * @return one or more cell values (List)
   */
  public String getSortValueString(String columnName) {
    Cell c = cells.getOrDefault(columnName, null);
    return c.getSortValueString();
  }

  /**
   * Return the subject of this row as an IRI.
   *
   * @return IRI of the subject
   */
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
        List<String> values = cell.getDisplayValues();
        if (values.size() > 1) {
          // If size is greater than 1, escape any split characters with a backslash
          values =
              values.stream().map(x -> x.replace(split, "\\" + split)).collect(Collectors.toList());
        }
        value = String.join(split, values);
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
   * @return HTML string rendering of row
   */
  public String toHTML(List<Column> columns, String split) {
    StringBuilder sb = new StringBuilder();
    sb.append("\t<tr>\n");
    for (Column c : columns) {
      String columnName = c.getDisplayName();
      Cell cell = cells.getOrDefault(columnName, null);
      String value;
      if (cell != null) {
        List<String> values = cell.getDisplayValues();
        if (values.size() > 1) {
          // If size is greater than 1, escape any split characters with a backslash
          values =
              values.stream().map(x -> x.replace(split, "\\" + split)).collect(Collectors.toList());
        }
        value = String.join(split, values);
      } else {
        value = "";
      }
      sb.append("\t\t<td>").append(value).append("</td>\n");
    }
    sb.append("\t</tr>\n");
    return sb.toString();
  }
}
