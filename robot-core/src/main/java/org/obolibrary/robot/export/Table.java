package org.obolibrary.robot.export;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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

  private static final Set<String> basicFormats = Sets.newHashSet("tsv", "csv", "json", "xlsx");

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
    if (format == null || basicFormats.contains(format.toLowerCase())) {
      displayRenderer = RendererType.OBJECT_RENDERER;
    } else if (format.equalsIgnoreCase("html")) {
      displayRenderer = RendererType.OBJECT_HTML_RENDERER;
      sortRenderer = RendererType.OBJECT_RENDERER;
    } else {
      // TODO - unknown format
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
    Map<Integer, String> rules = new HashMap<>();
    for (Column c : columns) {
      String name = c.getDisplayName();
      Cell xlsxCell = headerRow.createCell(colIdx);
      xlsxCell.setCellValue(name);

      String displayRule = c.getDisplayRule();
      if (displayRule != null) {
        rules.put(colIdx, displayRule);
      }
      colIdx++;
    }

    // Maybe add rules
    if (!rules.isEmpty()) {
      org.apache.poi.ss.usermodel.Row rulesRow = sheet.createRow(sheet.getLastRowNum() + 1);
      for (int idx = 0; idx <= colIdx; idx++) {
        if (rules.containsKey(idx)) {
          String rule = rules.get(idx);
          Cell xlsxCell = rulesRow.createCell(idx);
          xlsxCell.setCellValue(rule);
        }
      }
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
  public String toHTML(String split) {
    return toHTML(split, true, false);
  }

  /**
   * Render the Table as an HTML string.
   *
   * @param split character to split multiple cell values on
   * @param standalone if true, include header
   * @param includeJS if true and standalone, include JS script for tooltips
   * @return HTML string
   */
  public String toHTML(String split, boolean standalone, boolean includeJS) {
    StringBuilder sb = new StringBuilder();
    if (standalone) {
      // Add opening tags, style, and maybe js scripts
      sb.append("<head>\n")
          .append("\t<link rel=\"stylesheet\" href=\"")
          .append("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\">\n");
      if (includeJS) {
        sb.append("\t<script src=\"https://code.jquery.com/jquery-3.5.1.slim.min.js\"></script>\n")
            .append(
                "\t<script src=\"https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js\"></script>\n")
            .append(
                "\t<script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js\"></script>\n");
      }
      sb.append("</head>\n").append("<body>\n");
    }
    // Table start
    sb.append("<table class=\"table table-bordered table-striped\">\n")
        .append("<thead class=\"bg-dark text-white header-row\">\n")
        .append("<tr>\n");

    // Add column headers
    Map<Integer, String> rules = new HashMap<>();
    int colIdx = 0;
    for (Column c : columns) {
      sb.append("\t<th>").append(c.getDisplayName()).append("</th>\n");
      String displayRule = c.getDisplayRule();
      if (displayRule != null) {
        rules.put(colIdx, displayRule);
      }
      colIdx++;
    }
    sb.append("</tr>\n").append("</thead>\n");

    // Maybe add rules
    if (!rules.isEmpty()) {
      sb.append("<thead class=\"bg-secondary text-white\">\n").append("<tr>\n");
      for (int idx = 0; idx < colIdx; idx++) {
        if (rules.containsKey(idx)) {
          sb.append("\t<th>").append(rules.get(idx)).append("</th>\n");
        } else {
          sb.append("\t<th></th>\n");
        }
      }
      sb.append("</tr>\n").append("</thead>\n");
    }

    // Add all table rows
    for (Row row : rows) {
      sb.append(row.toHTML(columns, split));
    }
    sb.append("</table>\n");

    if (standalone) {
      // Add closing tag and script to activate tooltips
      sb.append("</body>\n");
      if (includeJS) {
        sb.append("<script>\n")
            .append("\t$(function () {\n")
            .append("\t\t$('[data-toggle=\"tooltip\"]').tooltip()\n")
            .append("\t})")
            .append("</script>\n");
      }
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
}
