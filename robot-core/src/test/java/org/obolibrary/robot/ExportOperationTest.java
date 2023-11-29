package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.obolibrary.robot.export.Table;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

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

  /**
   * Test exporting to Excel with more than 64,000 exported records.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testLargeExcelExport() throws Exception {

    // how many many axioms to create
    int axiomCount = 64002;

    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix(
        "simple", "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#");

    DefaultPrefixManager pfm = ioHelper.getPrefixManager();

    OWLOntologyManager owlm = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = owlm.createOntology();

    // add axiomCount subclass axioms to ontology
    // first the super class
    OWLClass superClass = CoreTest.dataFactory.getOWLClass("simple:super", pfm);

    // then all the subs and axioms
    for (int i = 0; i < axiomCount; i++) {
      OWLClass subClass = CoreTest.dataFactory.getOWLClass("simple:sub_" + i, pfm);
      OWLSubClassOfAxiom subAxiom =
          CoreTest.dataFactory.getOWLSubClassOfAxiom(subClass, superClass);
      owlm.addAxiom(ontology, subAxiom);
    }

    // export just the classes and their subclasses
    List<String> columns = Arrays.asList("ID", "SubClass Of [ID]");
    Map<String, String> options = ExportOperation.getDefaultOptions();
    options.put("include", "classes");

    // Create the table and render it as a Workbook
    Table t = ExportOperation.createExportTable(ontology, ioHelper, columns, options);
    Workbook wb = t.asWorkbook("|");

    // get the first sheet
    Sheet s = wb.getSheetAt(0);

    // There should be same number of rows as subclass axioms + 1 for the header row
    assert (s.getLastRowNum() == ontology.getAxiomCount(AxiomType.SUBCLASS_OF) + 1);
  }

  /**
   * Test exporting all named headings using simple ontology.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExportNamedHeadingsSimple() throws Exception {
    OWLOntology ontology = loadOntology("/simple_defined_by.owl");
    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix(
        "simple", "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl#");

    // every named header
    List<String> columns =
        Arrays.asList(
            "ID",
            "LABEL",
            "IRI",
            "CURIE",
            "Type",
            "SYNONYMS",
            "rdfs:isDefinedBy",
            "SUBCLASSES",
            "SubClass Of",
            "SubProperty Of",
            "Equivalent Class",
            "Equivalent Property",
            "Disjoint With",
            "Domain",
            "Range");

    // export everything
    Map<String, String> options = ExportOperation.getDefaultOptions();
    options.put("include", "classes individuals properties");

    // Create the table and render it as a Workbook
    Table t = ExportOperation.createExportTable(ontology, ioHelper, columns, options);
    Workbook wb = t.asWorkbook("|");

    // Write test.xlsx for debugging
    // try {
    //   System.out.println("Writing result to robot-core/test.xlsx");
    //   FileOutputStream out = new FileOutputStream("test.xlsx");
    //   wb.write(out);
    //   out.close();
    // } catch (Exception ex) {
    //   System.out.println("Error: " + ex);
    // }

    Sheet s = wb.getSheetAt(0);
    assertEquals(s.getLastRowNum(), 4);

    // Validate header
    // should match size and label
    org.apache.poi.ss.usermodel.Row header = s.getRow(0);

    short minColIx = header.getFirstCellNum();
    short maxColIx = header.getLastCellNum();
    int numCol = maxColIx - minColIx;
    // same width
    assert (numCol == columns.size());

    // column header labels should match
    for (short colIx = minColIx; colIx < maxColIx; colIx++) {
      Cell cell = header.getCell(colIx);
      if (cell == null) {
        continue;
      }
      String foundHeader = cell.getStringCellValue();
      String expectedHeader = columns.get(colIx);
      assert (foundHeader.equals(expectedHeader));
    }

    // rdfs:isDefinedBy should work
    assertEquals(
        s.getRow(4).getCell(6).getStringCellValue(),
        "https://github.com/ontodev/robot/robot-core/src/test/resources/simple.owl");
  }

  /**
   * Test exporting all named headings and object properties seen in the all-axioms ontology.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testExportNamedHeadingsAllAxioms() throws Exception {
    OWLOntology ontology = loadOntology("/axioms.owl");
    Set<OWLEntity> signature = ontology.getSignature();
    // remove datatypes
    signature.removeAll(ontology.getDatatypesInSignature());
    int entityCount = signature.size();

    Set<OWLObjectProperty> properties = ontology.getObjectPropertiesInSignature(Imports.EXCLUDED);

    IOHelper ioHelper = new IOHelper();
    ioHelper.addPrefix("ax", "https://http://robot.obolibrary.org/export_test/");

    // every named header
    List<String> columns = new ArrayList<String>();

    List<String> hdrLabels =
        Arrays.asList(
            "ID",
            "LABEL",
            "Type",
            "IRI",
            "CURIE",
            "SYNONYMS",
            "SUBCLASSES",
            "SubClass Of",
            "SubProperty Of",
            "Equivalent Class",
            "Equivalent Property",
            "Disjoint With",
            "Domain",
            "Range");

    columns.addAll(hdrLabels);

    // add all the object properties to the header
    for (OWLObjectProperty op : properties) {
      String shrt = ioHelper.getPrefixManager().getShortForm(op.getIRI());
      columns.add(shrt);
    }

    // export everything
    Map<String, String> options = ExportOperation.getDefaultOptions();
    options.put("include", "classes individuals properties");

    // Create the table and render it as a Workbook
    Table t = ExportOperation.createExportTable(ontology, ioHelper, columns, options);
    Workbook wb = t.asWorkbook("|");

    Sheet s = wb.getSheetAt(0);
    // There should be same number of rows as class,properties and instances in signature
    assert (s.getLastRowNum() == entityCount);

    // Validate header
    // should match size and label
    org.apache.poi.ss.usermodel.Row header = s.getRow(0);

    short minColIx = header.getFirstCellNum();
    short maxColIx = header.getLastCellNum();
    int numCol = maxColIx - minColIx;
    // should be same width
    assert (numCol == columns.size());

    // column header labels should match workbook
    for (short colIx = minColIx; colIx < maxColIx; colIx++) {
      Cell cell = header.getCell(colIx);
      if (cell == null) {
        continue;
      }
      String foundHeader = cell.getStringCellValue();
      String expectedHeader = columns.get(colIx);
      assert (foundHeader.equals(expectedHeader));
    }
  }
}
