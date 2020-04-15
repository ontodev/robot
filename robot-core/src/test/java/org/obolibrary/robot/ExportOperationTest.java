package org.obolibrary.robot;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.obolibrary.robot.export.Table;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Tests ExportOperation.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class ExportOperationTest extends CoreTest {

  /**
   * Test exporting simple.owl to XLSX.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExportToXLSX() throws Exception {
    OWLOntology ontology = loadOntology("/simple.owl");
    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix(
        "simple", "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#");
    List<String> columns = Arrays.asList("ID", "SubClass Of [ID]");
    Map<String, String> options = ExportOperation.getDefaultOptions();

    // Create the table and render it as a Workbook
    Table t = ExportOperation.createExportTable(ontology, ioHelper, columns, options);
    Workbook wb = t.asWorkbook("|");

    Sheet s = wb.getSheetAt(0);
    // There should be three rows (0, 1, 2)
    assert (s.getLastRowNum() == 2);

    // Validate header
    org.apache.poi.ss.usermodel.Row header = s.getRow(0);
    String v1 = header.getCell(0).getStringCellValue();
    assert (v1.equals("ID"));
    String v2 = header.getCell(1).getStringCellValue();
    assert (v2.equals("SubClass Of [ID]"));

    // Validate last row
    Row r2 = s.getRow(2);
    v1 = r2.getCell(0).getStringCellValue();
    assert (v1.equals("simple:test2"));
    v2 = r2.getCell(1).getStringCellValue();
    assert (v2.equals("simple:test1"));
  }
}
