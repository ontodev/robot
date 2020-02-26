package org.obolibrary.robot.export;

import java.util.*;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Table {

  // Ordered list of Columns
  List<Column> columns;

  // Ordered list of Rows
  List<Row> rows;

  // Ordered list of columns to sort on
  // e.g., the first item in the list will be sorted first
  List<Column> sortColumns;

  /** Init a new Table. */
  public Table() {
    columns = new ArrayList<>();
    rows = new ArrayList<>();
    sortColumns = new ArrayList<>();
  }

  /**
   * Add a Column to the Table. This will be appended to the end of the current list of Columns.
   *
   * @param column Column to add.
   */
  public void addColumn(Column column) {
    columns.add(column);
  }

  /**
   * Add a Row to the Table. This will be appended to the end of the current list of Rows.
   *
   * @param row Row to add
   */
  public void addRow(Row row) {
    rows.add(row);
  }

  /**
   * Get the Columns in a Table.
   *
   * @return List of Columns
   */
  public List<Column> getColumns() {
    return columns;
  }

  /**
   * Once all Columns have been added to a Table, set the columns to sort on. The columns should
   * have consecutive sort order integers which will determine the order in which they are used to
   * sort the Rows.
   */
  public void setSortColumns() {
    // Order sort columns
    Map<Integer, Column> sortColumns = new HashMap<>();
    int maxSort = 0;
    for (Column c : columns) {
      int sortOrder = c.getSortOrder();
      sortColumns.put(sortOrder, c);
      if (sortOrder > maxSort) {
        maxSort = sortOrder;
      }
    }
    int cur = 0;
    while (cur <= maxSort) {
      this.sortColumns.add(sortColumns.get(cur));
      cur++;
    }
  }

  /** Once all Rows have been added to a Table, sort Rows based on the sort Columns. */
  public void sortRows() {
    for (Column sc : sortColumns) {
      // Sort name is used to get the value
      String sortName = sc.getName();
      Comparator<Row> rowComparator =
          (r1, r2) -> {
            List<String> vals1 = r1.get(sortName);
            String v1;
            if (vals1.isEmpty()) {
              v1 = "";
            } else {
              v1 = String.join("|", vals1);
            }

            List<String> vals2 = r2.get(sortName);
            String v2;
            if (vals2.isEmpty()) {
              v2 = "";
            } else {
              v2 = String.join("|", vals2);
            }

            // Make sure empty strings end up last
            if (v1.isEmpty()) return Integer.MAX_VALUE;
            else if (v2.isEmpty()) return Integer.MIN_VALUE;
            else return v1.compareTo(v2);
          };
      if (sc.isReverseSort()) {
        rows.sort(Collections.reverseOrder(rowComparator));
      } else {
        rows.sort(rowComparator);
      }
    }
  }

  /**
   * Render the Table as List of Arrays.
   *
   * @param split String to split mulitple cell values on (default |)
   * @return List of String[]
   */
  public List<String[]> toList(String split) {
    List<String[]> table = new ArrayList<>();
    String[] header = new String[columns.size()];
    Iterator<Column> iterator = columns.iterator();
    for (int i = 0; i < header.length; i++) {
      header[i] = iterator.next().getName();
    }
    table.add(header);

    for (Row row : rows) {
      String[] rowArray = row.toArray(columns, split);
      table.add(rowArray);
    }
    return table;
  }
}
