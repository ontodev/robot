package org.obolibrary.robot.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.semanticweb.owlapi.model.IRI;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Row {

  // The subject of this row
  private IRI subject;

  // The violation level of this row for Report
  private String violationLevel;

  // List of cells in this row
  private Map<String, Cell> cells = new HashMap<>();

  // For JSON rendering, these should never be arrays
  private static final List<String> singles = Arrays.asList("CURIE", "ID", "IRI");

  /** Init a new Row. */
  public Row() {
    // empty constructor
  }

  /**
   * Init a new Row with a subject.
   *
   * @param subject IRI of subject of row
   */
  public Row(IRI subject) {
    this.subject = subject;
  }

  /**
   * Init a new Row for a Report with a violation level.
   *
   * @param violationLevel String violation level - error, warn, or info
   */
  public Row(String violationLevel) {
    this.violationLevel = violationLevel;
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
   * Add this Row to a Workbook.
   *
   * @param wb Workbook to add a row to
   * @param columns list of Columns
   * @param split character to split multiple cell values on
   */
  public void addToWorkbook(Workbook wb, List<Column> columns, String split) {
    // Create a new XLSX Row
    Sheet sheet = wb.getSheetAt(0);
    int rowIdx = sheet.getLastRowNum() + 1;
    org.apache.poi.ss.usermodel.Row xlsxRow = sheet.createRow(rowIdx);

    // Objects used to add Comments to cells
    CreationHelper factory = wb.getCreationHelper();
    ClientAnchor anchor = factory.createClientAnchor();
    Drawing drawing = sheet.createDrawingPatriarch();

    int cellIdx = 0;
    for (Column c : columns) {
      // Create a new XLSX Cell
      org.apache.poi.ss.usermodel.Cell xlsxCell = xlsxRow.createCell(cellIdx);

      String columnName = c.getDisplayName();
      Cell cell = cells.getOrDefault(columnName, null);

      String value;
      String comment = null;
      CellStyle style = wb.createCellStyle();
      Font font = wb.createFont();
      if (cell != null) {
        List<String> values = cell.getDisplayValues();
        if (values.size() > 1) {
          // If size is greater than 1, escape any split characters with a backslash
          values =
              values.stream().map(x -> x.replace(split, "\\" + split)).collect(Collectors.toList());
        }
        value = String.join(split, values);

        // Maybe set styles
        IndexedColors cellColor = cell.getCellColor();
        if (violationLevel != null) {
          switch (violationLevel.toLowerCase()) {
            case "error":
              cellColor = IndexedColors.ROSE;
              break;
            case "warn":
              cellColor = IndexedColors.LEMON_CHIFFON;
              break;
            case "info":
              cellColor = IndexedColors.LIGHT_CORNFLOWER_BLUE;
          }
        }

        if (cellColor != null) {
          style.setFillForegroundColor(cellColor.getIndex());
        }

        FillPatternType cellPattern = cell.getCellPattern();
        if (cellColor != null && cellPattern == null) {
          cellPattern = FillPatternType.SOLID_FOREGROUND;
        }
        if (cellPattern != null) {
          style.setFillPattern(cellPattern);
        }

        IndexedColors fontColor = cell.getFontColor();
        if (fontColor != null) {
          font.setColor(fontColor.getIndex());
        }

        // Maybe get a comment or null
        comment = cell.getComment();
      } else {
        // Empty value, no styles to set
        value = "";
      }

      // Add value to cell
      xlsxCell.setCellValue(value);

      // Add style to cell
      style.setFont(font);
      xlsxCell.setCellStyle(style);

      // Maybe add a comment
      if (comment != null) {
        Comment xlsxComment = drawing.createCellComment(anchor);
        xlsxComment.setString(factory.createRichTextString(comment));
        xlsxCell.setCellComment(xlsxComment);
      }

      cellIdx++;
    }
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
   * @param split character to split cell values on
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
    String trClass = null;
    if (violationLevel != null) {
      switch (violationLevel.toLowerCase()) {
        case "error":
          trClass = "table-danger";
          break;
        case "warn":
          trClass = "table-warning";
          break;
        case "info":
          trClass = "table-info";
          break;
      }
    }
    StringBuilder sb = new StringBuilder();
    if (trClass != null) {
      sb.append("\t<tr class=\"").append(trClass).append("\">\n");
    } else {
      sb.append("\t<tr>\n");
    }

    // Iterate through columns and get the cell for each
    for (Column c : columns) {
      String columnName = c.getDisplayName();
      Cell cell = cells.getOrDefault(columnName, null);
      String value;
      String htmlClass = null;
      String comment = null;

      if (cell != null) {
        List<String> values = cell.getDisplayValues();
        if (values.size() > 1) {
          // If size is greater than 1, escape any split characters with a backslash
          values =
              values.stream().map(x -> x.replace(split, "\\" + split)).collect(Collectors.toList());
        }
        value = String.join(split, values);
        htmlClass = cell.getHTMLClass();
        comment = cell.getComment();
      } else {
        value = "";
      }

      String tdClass;
      // Set default HTML class
      if (htmlClass == null) {
        tdClass = "";
      } else {
        tdClass = " class=\"" + htmlClass + "\"";
      }
      // Write cell as HTML
      sb.append("\t\t<td").append(tdClass);
      if (comment != null) {
        // If cell has a comment, write into the td element
        sb.append(" data-toggle=\"tooltip\" data-placement=\"right\" title=\"")
            .append(comment.replace("\"", "&quot;"))
            .append("\"");
      }
      sb.append(">").append(value).append("</td>\n");
    }

    // Close table row
    sb.append("\t</tr>\n");
    return sb.toString();
  }

  /**
   * Render the Row as a JsonObject
   *
   * @param columns list of Columns
   * @return JsonObject representation of the row
   */
  public JsonObject toJSON(List<Column> columns) {
    JsonObject row = new JsonObject();
    for (Column c : columns) {
      String columnName = c.getDisplayName();
      Cell cell = cells.getOrDefault(columnName, null);
      if (cell != null) {
        List<String> values = cell.getDisplayValues();
        if (singles.contains(columnName.toUpperCase())) {
          if (values.size() == 1) {
            // Add single value
            row.addProperty(columnName, values.get(0));
          }
        } else {
          if (!values.isEmpty()) {
            // Render everything else as an array
            JsonArray valArray = new JsonArray();
            // Add each value to array
            values.forEach(valArray::add);
            row.add(columnName, valArray);
          }
        }
      }
    }
    return row;
  }
}
