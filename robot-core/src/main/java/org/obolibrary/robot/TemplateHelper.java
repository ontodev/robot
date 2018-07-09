package org.obolibrary.robot;

import com.google.common.collect.Sets;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/** Convenience methods for working with templates. */
public class TemplateHelper {

  /** Shared DataFactory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** Namespace for error messages. */
  private static final String NS = "template#";

  /**
   * Error message when annotation property cannot be resolved. Expects: annotation property name.
   */
  private static final String annotationPropertyError =
      NS + "ANNOTATION PROPERTY ERROR could not handle annotation property: %s";

  /** Error message when datatype cannot be resolved. Expects: datatype name. */
  private static final String datatypeError = NS + "DATATYPE ERROR could not find datatype: %s";

  /** Error message when template file type is not CSV, TSV, or TAB. Expects: file name. */
  private static final String fileTypeError = NS + "FILE TYPE ERROR unrecognized file type for: %s";

  /** Error message when the template does not have an ID column. Expects: table name. */
  private static final String idError = NS + "ID ERROR an \"ID\" column is required in table %s";

  /** Error message when the IRI in an IRI annotation cannot be resolved. Expects: value. */
  private static final String iriError = NS + "IRI ERROR could not create IRI annotation: %s";

  /**
   * Error message when a language annotation string does not include "@{lang}". Expects: template
   * string
   */
  private static final String languageFormatError =
      NS + "LANGUAGE FORMAT ERROR invalid language annotation template string: %s";

  /** Error message when the template file does not exist. Expects: file name. */
  private static final String templateFileError =
      NS + "TEMPLATE FILE ERROR template %s does not exist";

  /**
   * Error message when a language annotation string does not include "^^{type}". Expects: template
   * string
   */
  private static final String typedFormatError =
      NS + "TYPED FORMAT ERROR invalid typed annotation string: %s";

  /** OWL Namespace. */
  private static String owl = "http://www.w3.org/2002/07/owl#";

  /**
   * Given a set of rows, the row number, and the column number, get the content in the column for
   * the row. If there are any issues, return an empty string. If the cell is empty, return null.
   *
   * @param rows list of rows (lists of strings)
   * @param row row number to get ID of
   * @param column column number
   * @return content, null, or empty string.
   */
  public static String getCellContent(List<List<String>> rows, int row, Integer column) {
    String id = null;
    if (column != null && column != -1) {
      try {
        id = rows.get(row).get(column);
      } catch (IndexOutOfBoundsException e) {
        return "";
      }
      if (id == null || id.trim().isEmpty()) {
        return "";
      }
    }
    return id;
  }

  /**
   * Create an OWLAnnotation based on the template string and cell value.
   *
   * @param checker used to resolve the annotation property
   * @param ioHelper IOHelper used to create IRIs from values
   * @param template the template string
   * @param value the value for the annotation
   * @return OWLAnnotation, or null if template string is not supported
   * @throws Exception if annotation property cannot be found
   */
  public static OWLAnnotation getAnnotation(
      QuotedEntityChecker checker, IOHelper ioHelper, String template, String value)
      throws Exception {
    if (template.startsWith("A ")) {
      return getStringAnnotation(checker, template, value);
    } else if (template.startsWith("AT ")) {
      if (template.indexOf("^^") > -1) {
        return getTypedAnnotation(checker, template, value);
      } else {
        throw new Exception(String.format(typedFormatError, template));
      }
    } else if (template.startsWith("AL ")) {
      if (template.indexOf("@") > -1) {
        return getLanguageAnnotation(checker, template, value);
      } else {
        throw new Exception(String.format(languageFormatError, template));
      }
    } else if (template.startsWith("AI ")) {
      IRI iri = ioHelper.createIRI(value);
      if (iri != null) {
        return getIRIAnnotation(checker, template, iri);
      } else {
        throw new Exception(String.format(iriError, value));
      }
    } else {
      return null;
    }
  }

  /**
   * Get a set of annotation axioms for an OWLEntity. Supports axiom annotations and axiom
   * annotation annotations.
   *
   * @param entity OWLEntity to annotation
   * @param annotations Set of OWLAnnotations
   * @param nested Map with top-level OWLAnnotation as key and another map (axiom OWLAnnotation, set
   *     of axiom annotation OWLAnnotations) as value
   * @return Set of OWLAnnotationAssertionAxioms
   */
  public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
      OWLEntity entity,
      Set<OWLAnnotation> annotations,
      Map<OWLAnnotation, Map<OWLAnnotation, Set<OWLAnnotation>>> nested) {
    Set<OWLAnnotationAssertionAxiom> axioms = new HashSet<>();
    // Create basic annotations
    for (OWLAnnotation annotation : annotations) {
      axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(entity.getIRI(), annotation));
    }
    // Create annotations with axiom annotations
    for (Entry<OWLAnnotation, Map<OWLAnnotation, Set<OWLAnnotation>>> annotationPlusAxioms :
        nested.entrySet()) {
      OWLAnnotation annotation = annotationPlusAxioms.getKey();
      Set<OWLAnnotation> axiomAnnotations = new HashSet<>();
      // For each annotation with its axiom annotations ...
      for (Entry<OWLAnnotation, Set<OWLAnnotation>> nestedSet :
          annotationPlusAxioms.getValue().entrySet()) {
        OWLAnnotation axiomAnnotation = nestedSet.getKey();
        // Check if there are annotations on the axiom annotations, and add those
        Set<OWLAnnotation> axiomAnnotationAnnotations = nestedSet.getValue();
        if (axiomAnnotationAnnotations.isEmpty()) {
          axiomAnnotations.add(axiomAnnotation);
        } else {
          OWLAnnotationProperty property = axiomAnnotation.getProperty();
          OWLAnnotationValue value = axiomAnnotation.getValue();
          axiomAnnotations.add(
              dataFactory.getOWLAnnotation(property, value, axiomAnnotationAnnotations));
        }
      }
      axioms.add(
          dataFactory.getOWLAnnotationAssertionAxiom(
              entity.getIRI(), annotation, axiomAnnotations));
    }
    return axioms;
  }

  /**
   * Get a set of OWLAxioms (subclass or equivalent) for an OWLClass. Supports axiom annotations.
   *
   * @param cls OWLClass to add axioms to
   * @param classType subclass or equivalent
   * @param classExpressions Set of OWLClassExpressions
   * @param annotatedExpressions Map of annotated OWLClassExpressions and the Set of OWLAnnotations
   * @return Set of OWLAxioms, or null if classType is not subclass or equivalent
   * @throws Exception if classType is not subclass or equivalent
   */
  public static Set<OWLAxiom> getLogicalAxioms(
      OWLClass cls,
      String classType,
      Set<OWLClassExpression> classExpressions,
      Map<OWLClassExpression, Set<OWLAnnotation>> annotatedExpressions)
      throws Exception {
    Set<OWLAxiom> axioms = new HashSet<>();
    if (classType.equals("subclass")) {
      for (OWLClassExpression expression : classExpressions) {
        axioms.add(dataFactory.getOWLSubClassOfAxiom(cls, expression));
      }
      for (Entry<OWLClassExpression, Set<OWLAnnotation>> annotatedEx :
          annotatedExpressions.entrySet()) {
        axioms.add(
            dataFactory.getOWLSubClassOfAxiom(cls, annotatedEx.getKey(), annotatedEx.getValue()));
      }
      return axioms;
    } else if (classType.equals("equivalent")) {
      // Since it's an intersection, all annotations will be added to the same axiom
      Set<OWLAnnotation> annotations = new HashSet<>();
      for (Entry<OWLClassExpression, Set<OWLAnnotation>> annotatedEx :
          annotatedExpressions.entrySet()) {
        classExpressions.add(annotatedEx.getKey());
        annotations.addAll(annotatedEx.getValue());
      }
      OWLObjectIntersectionOf intersection =
          dataFactory.getOWLObjectIntersectionOf(classExpressions);
      OWLAxiom axiom;
      if (!annotations.isEmpty()) {
        axiom = dataFactory.getOWLEquivalentClassesAxiom(cls, intersection, annotations);
      } else {
        axiom = dataFactory.getOWLEquivalentClassesAxiom(cls, intersection);
      }
      return Sets.newHashSet(axiom);
    } else {
      return null;
    }
  }

  /**
   * Find an annotation property with the given name or create one.
   *
   * @param checker used to search by rdfs:label (for example)
   * @param name the name to search for
   * @return an annotation property
   * @throws Exception if the name cannot be resolved
   */
  public static OWLAnnotationProperty getAnnotationProperty(
      QuotedEntityChecker checker, String name) throws Exception {
    OWLAnnotationProperty property = checker.getOWLAnnotationProperty(name, true);
    if (property != null) {
      return property;
    }
    throw new Exception(String.format(annotationPropertyError, name));
  }

  /**
   * Find a datatype with the given name or create one.
   *
   * @param checker used to search by rdfs:label (for example)
   * @param name the name to search for
   * @return a datatype
   * @throws Exception if the name cannot be resolved
   */
  public static OWLDatatype getDatatype(QuotedEntityChecker checker, String name) throws Exception {
    OWLDatatype datatype = checker.getOWLDatatype(name, true);
    if (datatype != null) {
      return datatype;
    }
    throw new Exception(String.format(datatypeError, name));
  }

  /**
   * Return a string annotation for the given template string and value.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation with property and string literal value
   * @throws Exception if the annotation property cannot be found
   */
  public static OWLAnnotation getStringAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    String name = template.substring(1).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value));
  }

  /**
   * Return a typed annotation for the given template string and value. The template string format
   * is "AT [name]^^[datatype]" and the value is any string.
   *
   * @param checker used to resolve the annotation property and datatype
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation axiom with property and typed literal value
   * @throws Exception if the annotation property cannot be found
   */
  public static OWLAnnotation getTypedAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    template = template.substring(2).trim();
    String name = template.substring(0, template.indexOf("^^")).trim();
    String typeName = template.substring(template.indexOf("^^") + 2, template.length()).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);
    OWLDatatype datatype = getDatatype(checker, typeName);
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value, datatype));
  }

  /**
   * Return a language tagged annotation for the given template and value. The template string
   * format is "AL [name]@[lang]" and the value is any string.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation axiom with property and language tagged literal
   * @throws Exception if the annotation property cannot be found
   */
  public static OWLAnnotation getLanguageAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    template = template.substring(2).trim();
    String name = template.substring(0, template.indexOf("@")).trim();
    String lang = template.substring(template.indexOf("@") + 1, template.length()).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value, lang));
  }

  /**
   * Return an IRI annotation for the given template string and value. The template string format is
   * "AI [name]" and the value is a string that can be interpreted as an IRI.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the IRI value for the annotation
   * @return a new annotation axiom with property and an IRI value
   * @throws Exception if the annotation property cannot be found
   */
  public static OWLAnnotation getIRIAnnotation(
      QuotedEntityChecker checker, String template, IRI value) throws Exception {
    String name = template.substring(2).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);
    return dataFactory.getOWLAnnotation(property, value);
  }

  /**
   * Use type, id, and label information to get an entity from the data in a row. Requires either:
   * an id (default type is owl:Class); an id and type; or a label.
   *
   * @param checker for looking up labels
   * @param type the IRI of the type for this entity, or null
   * @param id the ID for this entity, or null
   * @param label the label for this entity, or null
   * @return the entity
   * @throws Exception if the entity cannot be created
   */
  public static OWLEntity getEntity(QuotedEntityChecker checker, IRI type, String id, String label)
      throws Exception {

    IOHelper ioHelper = checker.getIOHelper();

    if (id != null && ioHelper != null) {
      IRI iri = ioHelper.createIRI(id);
      if (type == null) {
        type = IRI.create(owl + "Class");
      }
      String t = type.toString();
      if (t.equals(owl + "Class")) {
        return dataFactory.getOWLClass(iri);
      } else if (t.equals(owl + "AnnotationProperty")) {
        return dataFactory.getOWLAnnotationProperty(iri);
      } else if (t.equals(owl + "ObjectProperty")) {
        return dataFactory.getOWLObjectProperty(iri);
      } else if (t.equals(owl + "DatatypeProperty")) {
        return dataFactory.getOWLDataProperty(iri);
      } else if (t.equals(owl + "Datatype")) {
        return dataFactory.getOWLDatatype(iri);
      } else {
        return dataFactory.getOWLNamedIndividual(iri);
      }
    }

    if (label != null && type != null) {
      String t = type.toString();
      if (t.equals(owl + "Class")) {
        return checker.getOWLClass(label);
      } else if (t.equals(owl + "AnnotationProperty")) {
        return checker.getOWLAnnotationProperty(label);
      } else if (t.equals(owl + "ObjectProperty")) {
        return checker.getOWLObjectProperty(label);
      } else if (t.equals(owl + "DatatypeProperty")) {
        return checker.getOWLDataProperty(label);
      } else if (t.equals(owl + "Datatype")) {
        return checker.getOWLDatatype(label);
      } else {
        return checker.getOWLIndividual(label);
      }
    }

    if (label != null) {
      return checker.getOWLEntity(label);
    }

    return null;
  }

  /**
   * Get a list of the IRIs defined in a set of template tables.
   *
   * @param tables a map from table names to tables
   * @param ioHelper used to find entities by name
   * @return a list of IRIs
   * @throws Exception when names or templates cannot be handled
   */
  public static List<IRI> getIRIs(Map<String, List<List<String>>> tables, IOHelper ioHelper)
      throws Exception {
    List<IRI> iris = new ArrayList<IRI>();
    for (Map.Entry<String, List<List<String>>> table : tables.entrySet()) {
      String tableName = table.getKey();
      List<List<String>> rows = table.getValue();
      iris.addAll(getIRIs(tableName, rows, ioHelper));
    }
    return iris;
  }

  /**
   * Get a list of the IRIs defined in a template table.
   *
   * @param tableName the name of the table
   * @param rows the table of data
   * @param ioHelper used to find entities by name
   * @return a list of IRIs
   * @throws Exception when names or templates cannot be handled
   */
  public static List<IRI> getIRIs(String tableName, List<List<String>> rows, IOHelper ioHelper)
      throws Exception {
    // Find the ID column.
    List<String> templates = rows.get(1);
    int idColumn = -1;
    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      if (template == null) {
        continue;
      }
      template = template.trim();
      if (template.equals("ID")) {
        idColumn = column;
      }
    }
    if (idColumn == -1) {
      throw new Exception(String.format(idError, tableName));
    }

    List<IRI> iris = new ArrayList<IRI>();
    for (int row = 2; row < rows.size(); row++) {
      String id = null;
      try {
        id = rows.get(row).get(idColumn);
      } catch (IndexOutOfBoundsException e) {
        continue;
      }
      if (id == null || id.trim().isEmpty()) {
        continue;
      }
      IRI iri = ioHelper.createIRI(id);
      if (iri == null) {
        continue;
      }
      iris.add(iri);
    }

    return iris;
  }

  /**
   * Read comma-separated values from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(String path) throws IOException {
    return readCSV(new FileReader(path));
  }

  /**
   * Read comma-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(InputStream stream) throws IOException {
    return readCSV(new InputStreamReader(stream));
  }

  /**
   * Read comma-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(Reader reader) throws IOException {
    CSVReader csv = new CSVReader(reader);
    List<List<String>> rows = new ArrayList<List<String>>();
    String[] nextLine;
    while ((nextLine = csv.readNext()) != null) {
      rows.add(new ArrayList<String>(Arrays.asList(nextLine)));
    }
    csv.close();
    return rows;
  }

  /**
   * Read a table from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTable(String path) throws IOException {
    File file = new File(path);
    if (!file.exists()) {
      throw new IllegalArgumentException(String.format(templateFileError, file.getName()));
    }
    String extension = FilenameUtils.getExtension(file.getName());
    extension = extension.trim().toLowerCase();
    if (extension.equals("csv")) {
      return readCSV(new FileReader(path));
    } else if (extension.equals("tsv") || extension.equals("tab")) {
      return readTSV(new FileReader(path));
    } else {
      throw new IOException(String.format(fileTypeError, path));
    }
  }

  /**
   * Read tab-separated values from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(String path) throws IOException {
    return readTSV(new FileReader(path));
  }

  /**
   * Read tab-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(InputStream stream) throws IOException {
    return readTSV(new InputStreamReader(stream));
  }

  /**
   * Read tab-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(Reader reader) throws IOException {
    CSVReader csv = new CSVReader(reader, '\t');
    List<List<String>> rows = new ArrayList<List<String>>();
    String[] nextLine;
    while ((nextLine = csv.readNext()) != null) {
      rows.add(new ArrayList<String>(Arrays.asList(nextLine)));
    }
    csv.close();
    return rows;
  }

  /**
   * Given a template string, a cell value, and an empty list, fill the list with any number of
   * values based on a SPLIT character, then return the template string without SPLIT. If there are
   * no SPLITs, only add the original cell to the values.
   *
   * @param template template string
   * @param cell cell contents
   * @param values empty list to fill
   * @return template string without SPLIT
   */
  public static String processSplit(String template, String cell, List<String> values) {
    // If the template contains SPLIT=X,
    // then split the cell value
    // and remove that string from the template.
    Pattern splitter = Pattern.compile("SPLIT=(\\S+)");
    Matcher matcher = splitter.matcher(template);
    if (matcher.find()) {
      Pattern split = Pattern.compile(Pattern.quote(matcher.group(1)));
      values.addAll(Arrays.asList(split.split(cell)));
      template = matcher.replaceAll("").trim();
    } else {
      values.add(cell);
    }
    return template;
  }
}
