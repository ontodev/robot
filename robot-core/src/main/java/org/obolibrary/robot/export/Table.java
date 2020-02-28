package org.obolibrary.robot.export;

import java.util.*;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Table {

  private String format;

  // Ordered list of Columns
  private List<Column> columns;

  // Ordered list of Rows
  private List<Row> rows;

  // Ordered list of columns to sort on
  // e.g., the first item in the list will be sorted first
  private List<Column> sortColumns;

  private RendererType displayRenderer = null;
  private RendererType sortRenderer = null;

  /** Init a new Table. */
  public Table(String format) {
    this.format = format;
    columns = new ArrayList<>();
    rows = new ArrayList<>();
    sortColumns = new ArrayList<>();

    // Set renderer types based on format
    if (format.equalsIgnoreCase("tsv") || format.equalsIgnoreCase("csv")) {
      displayRenderer = RendererType.OBJECT_RENDERER;
    } else if (format.equalsIgnoreCase("html")) {
      displayRenderer = RendererType.OBJECT_HTML_RENDERER;
      sortRenderer = RendererType.OBJECT_RENDERER;
    }
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

  public RendererType getDisplayRendererType() {
    return displayRenderer;
  }

  public RendererType getSortRendererType() {
    return sortRenderer;
  }

  /**
   * Get the Columns in a Table.
   *
   * @return List of Columns
   */
  public List<Column> getColumns() {
    return columns;
  }

  public String getFormat() {
    return format;
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
      String sortName = sc.getDisplayName();
      Comparator<Row> rowComparator =
          (r1, r2) -> {
            String v1 = r1.getSortValueString(sortName);
            String v2 = r2.getSortValueString(sortName);

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
   * Render the Table as List of Arrays for writing to CSV/TSV.
   *
   * @param split String to split mulitple cell values on (default |)
   * @return List of String[]
   */
  public List<String[]> toList(String split) {
    List<String[]> table = new ArrayList<>();
    String[] header = new String[columns.size()];
    Iterator<Column> iterator = columns.iterator();
    for (int i = 0; i < header.length; i++) {
      header[i] = iterator.next().getDisplayName();
    }
    table.add(header);

    for (Row row : rows) {
      String[] rowArray = row.toArray(columns, split);
      table.add(rowArray);
    }
    return table;
  }

  /**
   * Render the Table as an HTML string.
   *
   * @param split character to split multiple cell values on
   * @return HTML string
   */
  public String toHTML(String split) {
    StringBuilder sb = new StringBuilder();
    sb.append("<head>\n")
        .append(
            "\t<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\">\n")
        .append("</head>\n")
        .append("<body>\n")
        .append("<table class=\"table table-striped\">\n")
        .append("<tr>\n");

    for (Column c : columns) {
      sb.append("\t<th>").append(c.getDisplayName()).append("</th>\n");
    }

    sb.append("</tr>\n");

    for (Row row : rows) {
      sb.append(row.toHTML(columns, split));
    }
    sb.append("</table>");
    sb.append("</body>");
    return sb.toString();
  }
}
