package org.obolibrary.robot.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Cell {

  // Column object used to build this cell
  private Column column;

  // List of output values for this cell
  private List<CellValue> values = new ArrayList<>();

  private List<String> displayValues = new ArrayList<>();
  private String sortValueString;

  // Styles for XLSX output
  private IndexedColors cellColor = null;
  private FillPatternType cellPattern = null;
  private IndexedColors fontColor = null;

  // Styles for HTML output
  private String htmlClass = null;

  // Comment can appear as an XLSX Comment or an HTML tooltip
  // This is not required and can be returned null
  private String comment = null;

  /**
   * Init a new Cell for a Column with multiple display values and no sort values.
   *
   * @param column Column for the cell
   * @param displayValues List of string output values
   */
  public Cell(Column column, List<String> displayValues) {
    this.column = column;
    for (String dv : displayValues) {
      values.add(new CellValue(dv, dv));
    }
    sortCellValues();
    setSortAndDisplayValues();
  }

  /**
   * Init a new Cell for a Column with mulitple values in a cell.
   *
   * @param column Column for the cell
   * @param displayValues List of string output values
   * @param sortValues List of string sort values
   */
  public Cell(Column column, List<String> displayValues, List<String> sortValues) {
    this.column = column;
    int idx = 0;
    for (String dv : displayValues) {
      String sv = sortValues.get(idx);
      values.add(new CellValue(dv, sv));
      idx++;
    }
    sortCellValues();
    setSortAndDisplayValues();
  }

  /**
   * Init a Cell for a Column with single value in cell with no sort value.
   *
   * @param column Column for the cell
   * @param displayValue String output value
   */
  public Cell(Column column, String displayValue) {
    this.column = column;
    displayValues.add(displayValue);
    List<String> sortValues = Collections.singletonList(displayValue);
    sortValueString = String.join("|", sortValues);
  }

  /**
   * Init a Cell for a Column with single value in cell.
   *
   * @param column Column for the cell
   * @param displayValue String ouput value
   * @param sortValue String sort value
   */
  public Cell(Column column, String displayValue, String sortValue) {
    this.column = column;
    displayValues.add(displayValue);
    List<String> sortValues = Collections.singletonList(sortValue);
    sortValueString = String.join("|", sortValues);
  }

  /**
   * Get the cell background color for this cell in an XLSX workbook.
   *
   * @return IndexedColors value for cell
   */
  public IndexedColors getCellColor() {
    return cellColor;
  }

  /**
   * Get the cell background pattern for this cell in an XLSX workbook.
   *
   * @return FillPatternType value for cell
   */
  public FillPatternType getCellPattern() {
    return cellPattern;
  }

  /**
   * Get the column name for a cell. This returns the display name, including any rendering tags.
   *
   * @return String column name
   */
  public String getColumnName() {
    return column.getDisplayName();
  }

  /**
   * Get the comment for this Cell.
   *
   * @return String comment, or null
   */
  public String getComment() {
    return comment;
  }

  /**
   * Get the value of a cell.
   *
   * @return String value
   */
  public List<String> getDisplayValues() {
    return displayValues;
  }

  /**
   * Get the HTML class of a cell.
   *
   * @return String HTML bootstrap class, or null
   */
  public String getHTMLClass() {
    return htmlClass;
  }

  /**
   * Get the font color for this cell in an XLSX workbook.
   *
   * @return IndexedColors value for font
   */
  public IndexedColors getFontColor() {
    return fontColor;
  }

  /**
   * Get the sort value of a cell.
   *
   * @return String value
   */
  public String getSortValueString() {
    return sortValueString;
  }

  /**
   * Set the cell background color for this cell in an XLSX workbook.
   *
   * @param cellColor IndexedColors value for cell
   */
  public void setCellColor(IndexedColors cellColor) {
    this.cellColor = cellColor;
  }

  /**
   * Set the cell background pattern for this cell in an XLSX workbook.
   *
   * @param cellPattern FillPatternType value for this cell
   */
  public void setCellPattern(FillPatternType cellPattern) {
    this.cellPattern = cellPattern;
  }

  /**
   * Add a comment to this Cell.
   *
   * @param comment String comment
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * Add an HTML class to this Cell.
   *
   * @param htmlClass String Bootstrap HTML class
   */
  public void setHTMLClass(String htmlClass) {
    this.htmlClass = htmlClass;
  }

  /**
   * Set the font color for this cell in an XLSX workbook.
   *
   * @param fontColor IndexedColors value for font
   */
  public void setFontColor(IndexedColors fontColor) {
    this.fontColor = fontColor;
  }

  /** Set a Cell's display and sort values with the sort value becoming one string to sort on. */
  private void setSortAndDisplayValues() {
    List<String> sortValues = new ArrayList<>();
    for (CellValue cv : values) {
      String dv = cv.getDisplayValue();
      String sv = cv.getSortValue();
      displayValues.add(dv);
      sortValues.add(sv);
    }
    sortValueString = String.join("|", sortValues);
  }

  /** Sort cell values alphabetically. */
  private void sortCellValues() {
    Comparator<CellValue> cvComparator =
        (cv1, cv2) -> {
          String sv1 = cv1.getSortValue();
          String sv2 = cv2.getSortValue();

          // Make sure empty strings end up last
          if (sv1.isEmpty()) return Integer.MAX_VALUE;
          else if (sv2.isEmpty()) return Integer.MIN_VALUE;
          else return sv1.compareTo(sv2);
        };
    values.sort(cvComparator);
  }

  /**
   * Private class to link display and sort values. This allows us to sort within a cell by the sort
   * value and still output the display value.
   */
  private class CellValue {
    String displayValue;
    String sortValue;

    /**
     * Init a new CellValue.
     *
     * @param displayValue String display value
     * @param sortValue String sort value
     */
    public CellValue(String displayValue, String sortValue) {
      this.displayValue = displayValue;
      this.sortValue = sortValue;
    }

    /**
     * Return the single display value.
     *
     * @return String display value
     */
    public String getDisplayValue() {
      return displayValue;
    }

    /**
     * Return the single sort value.
     *
     * @return String sort value
     */
    public String getSortValue() {
      return sortValue;
    }
  }
}
