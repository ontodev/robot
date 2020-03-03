package org.obolibrary.robot.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** @author <a href="mailto@rbca.jackson@gmail.com">Becky Jackson</a> */
public class Cell {

  // Column object used to build this cell
  private Column column;

  // List of output values for this cell
  private List<CellValue> values = new ArrayList<>();

  private List<String> displayValues = new ArrayList<>();
  private String sortValueString;

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
   * Get the column name for a cell. This returns the display name, including any rendering tags.
   *
   * @return String column name
   */
  public String getColumnName() {
    return column.getDisplayName();
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
   * Get the sort value of a cell.
   *
   * @return String value
   */
  public String getSortValueString() {
    return sortValueString;
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
