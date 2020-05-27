package org.obolibrary.robot.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import java.io.IOException;
import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Table {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(Table.class);

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

  /**
   * Init a new Table.
   *
   * @param format format of the table (tsv, csv, html)
   */
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

  /**
   * Render the Table as a Workbook.
   *
   * @param split character to split multiple cell values on
   * @return Workbook
   */
  public Workbook asWorkbook(String split) {
    Workbook wb = new XSSFWorkbook();
    wb.createSheet();

    // Add headers
    Sheet sheet = wb.getSheetAt(0);
    org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
    int colIdx = 0;
    for (Column c : columns) {
      String name = c.getDisplayName();
      Cell xlsxCell = headerRow.createCell(colIdx);
      xlsxCell.setCellValue(name);
      colIdx++;
    }

    // Add rows
    for (Row row : rows) {
      row.addToWorkbook(wb, columns, split);
    }

    // Set auto sizing
    // TODO - this takes up, relatively, a lot of time.
    /* for (int idx = 0; idx < columns.size(); idx++) {
      sheet.autoSizeColumn(idx);
    } */

    return wb;
  }

  /**
   * Get the type of display renderer
   *
   * @return RendererType
   */
  public RendererType getDisplayRendererType() {
    return displayRenderer;
  }

  /**
   * Get thee type of sort renderer
   *
   * @return RendererType
   */
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

  /**
   * Get the format of this table
   *
   * @return table format (csv, tsv, or html)
   */
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
      logger.info("Sorting on column " + sortName);
      Comparator<Row> rowComparator =
          (r1, r2) -> {
            String o1 = r1.getSortValueString(sortName);
            String o2 = r2.getSortValueString(sortName);
            if (o1.trim().isEmpty() && o2.trim().isEmpty()) return 0;
            else if (o1.trim().isEmpty()) return 1;
            else if (o2.trim().isEmpty()) return -1;
            else return o1.compareTo(o2);
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
  public String toHTML(String split, boolean standalone) {
    StringBuilder sb = new StringBuilder();
    if (!standalone) {
      sb.append("<head>\n")
          .append(
              "\t<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\">\n")
          .append("</head>\n")
          .append("<body>\n");
    }
    sb.append("<table class=\"table table-striped\">\n").append("<tr>\n");

    for (Column c : columns) {
      sb.append("\t<th>").append(c.getDisplayName()).append("</th>\n");
    }

    sb.append("</tr>\n");

    for (Row row : rows) {
      sb.append(row.toHTML(columns, split));
    }
    sb.append("</table>");
    if (!standalone) {
      sb.append("</body>");
    }
    return sb.toString();
  }

  /**
   * Render the Table as a JSON string.
   *
   * @return JSON string
   */
  public String toJSON() {
    JsonArray table = new JsonArray();
    for (Row row : rows) {
      table.add(row.toJSON(columns));
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    return gson.toJson(table);
  }

  /**
   * Render the Table as a YAML string.
   *
   * @return YAML string
   */
  public String toYAML() throws IOException {
    JsonNode jsonNodeTree = new ObjectMapper().readTree(toJSON());
    return new YAMLMapper().writeValueAsString(jsonNodeTree);
  }
}
