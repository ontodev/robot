package org.obolibrary.robot;

import com.google.common.collect.Sets;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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
import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Convenience methods for working with templates.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class TemplateHelper {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(TemplateHelper.class);

  /** Shared DataFactory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /* Error messages */

  /** Namespace for error messages. */
  private static final String NS = "template#";

  /**
   * Error message when annotation property cannot be resolved. Expects: annotation property name.
   */
  private static final String annotationPropertyError =
      NS + "ANNOTATION PROPERTY ERROR could not handle annotation property: %s";

  /** Error message when a given class expression is not able to be parsed. */
  protected static final String manchesterParseError =
      NS + "MANCHESTER PARSE ERROR the expression '%s' at row %d, column %d cannot be parsed: %s";

  /** Error message when the CLASS_TYPE is not subclass or equivalent. */
  private static final String classTypeError =
      NS + "CLASS TYPE ERROR '%s' is not a valid CLASS_TYPE";

  /** Error message when datatype cannot be resolved. Expects: datatype name. */
  private static final String datatypeError = NS + "DATATYPE ERROR could not find datatype: %s";

  /** Error message when template file type is not CSV, TSV, or TAB. Expects: file name. */
  private static final String fileTypeError = NS + "FILE TYPE ERROR unrecognized file type for: %s";

  /** Error message when the template does not have an ID column. Expects: table name. */
  private static final String idError = NS + "ID ERROR an \"ID\" column is required in table %s";

  /** Error message when the IRI in an IRI annotation cannot be resolved. Expects: value. */
  private static final String iriError =
      NS + "IRI ERROR could not create IRI annotation at row %d, column %d: %s";

  /**
   * Error message when a language annotation string does not include "@{lang}". Expects: template
   * string
   */
  private static final String languageFormatError =
      NS + "LANGUAGE FORMAT ERROR invalid language annotation template string at column %d: %s";

  /** Error message when the template file does not exist. Expects: file name. */
  private static final String templateFileError =
      NS + "TEMPLATE FILE ERROR template %s does not exist";

  /**
   * Error message when a language annotation string does not include "^^{type}". Expects: template
   * string
   */
  private static final String typedFormatError =
      NS + "TYPED FORMAT ERROR invalid typed annotation string at column %d: %s";

  /* OWL entity type IRIs */

  private static final String OWL = "http://www.w3.org/2002/07/owl#";

  private static final String OWL_ANNOTATION_PROPERTY = OWL + "AnnotationProperty";

  private static final String OWL_CLASS = OWL + "Class";

  private static final String OWL_DATA_PROPERTY = OWL + "DatatypeProperty";

  private static final String OWL_DATATYPE = OWL + "Datatype";

  private static final String OWL_OBJECT_PROPERTY = OWL + "ObjectProperty";

  /**
   * Given a QuotedEntityChecker, a string value, and a character to split the value string on (or
   * null), return the value or values as a set of OWLAnnotationProperties.
   *
   * @param checker QuotedEntityChecker to get properties
   * @param value value or values to parse to properties
   * @param split character to split value on or null
   * @return set of OWLAnnotationProperties
   * @throws Exception if property cannot be found or created
   */
  public static Set<OWLAnnotationProperty> getAnnotationProperties(
      QuotedEntityChecker checker, String value, String split) throws Exception {
    List<String> allValues = getAllValues(value, split);

    Set<OWLAnnotationProperty> properties = new HashSet<>();
    for (String v : allValues) {
      String content = QuotedEntityChecker.wrap(v);
      properties.add(getAnnotationProperty(checker, content));
    }

    return properties;
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
    OWLAnnotationProperty property = checker.getOWLAnnotationProperty(name);
    if (property != null) {
      return property;
    }
    throw new Exception(String.format(annotationPropertyError, name));
  }

  /**
   * Create an OWLAnnotation based on the template string and cell value.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return OWLAnnotation, or null if template string is not supported
   * @throws Exception if annotation cannot be created
   */
  @Deprecated
  public static OWLAnnotation getAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    if (template.startsWith("A ")) {
      return getStringAnnotation(checker, template, value);
    } else if (template.startsWith("AT ")) {
      if (template.contains("^^")) {
        return getTypedAnnotation(checker, template, value);
      } else {
        throw new Exception(
            String.format("TYPED FORMAT ERROR invalid typed annotation string: %s", template));
      }
    } else if (template.startsWith("AL ")) {
      if (template.contains("@")) {
        return getLanguageAnnotation(checker, template, value);
      } else {
        throw new Exception(
            String.format(
                "LANGUAGE FORMAT ERROR invalid language annotation template string: %s", template));
      }
    } else if (template.startsWith("AI ")) {
      return getIRIAnnotation(checker, template, value, 0, 0);
    } else {
      return null;
    }
  }

  /**
   * Create an OWLAnnotation based on the template string and cell value. Replaced by
   * getAnnotation(QuotedEntityChecker checker, String template, String value).
   *
   * @param checker used to resolve the annotation property
   * @param ioHelper IOHelper used to create IRIs from values
   * @param template the template string
   * @param value the value for the annotation
   * @return OWLAnnotation, or null if template string is not supported
   * @throws Exception if annotation property cannot be found
   */
  @Deprecated
  public static OWLAnnotation getAnnotation(
      QuotedEntityChecker checker, IOHelper ioHelper, String template, String value)
      throws Exception {
    if (template.startsWith("A ")) {
      return getStringAnnotation(checker, template, value);
    } else if (template.startsWith("AT ")) {
      if (template.contains("^^")) {
        return getTypedAnnotation(checker, template, value);
      } else {
        throw new Exception(
            String.format("TYPED FORMAT ERROR invalid typed annotation string: %s", template));
      }
    } else if (template.startsWith("AL ")) {
      if (template.contains("@")) {
        return getLanguageAnnotation(checker, template, value);
      } else {
        throw new Exception(
            String.format(
                "LANGUAGE FORMAT ERROR invalid language annotation template string: %s", template));
      }
    } else if (template.startsWith("AI ")) {
      IRI iri = ioHelper.createIRI(value);
      if (iri != null) {
        return getIRIAnnotation(checker, template, iri);
      } else {
        throw new Exception(String.format(iriError, 0, 0, value));
      }
    } else {
      return null;
    }
  }

  private static String getSplit(String template) {
    if (template.contains("SPLIT=")) {
      return template.substring(template.indexOf("SPLIT=") + 6).trim();
    }
    return null;
  }

  private static String getTemplate(String template) {
    if (template.contains("SPLIT=")) {
      return template.substring(0, template.indexOf("SPLIT="));
    }
    return template;
  }

  /**
   * Create an OWLAnnotation based on the template string and cell value. Replaced by
   * getAnnotation(QuotedEntityChecker checker, String template, String value).
   *
   * @param checker used to resolve the annotation property and IRIs
   * @param template the template string
   * @param value the value for the annotation
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return OWLAnnotation, or null if template string is not supported
   * @throws Exception if annotation property cannot be found
   */
  public static Set<OWLAnnotation> getAnnotations(
      QuotedEntityChecker checker, String template, String value, int rowNum, int column)
      throws Exception {
    String split = getSplit(template);
    template = getTemplate(template);

    // Trim the > if it hasn't been trimmed yet
    if (template.startsWith(">")) {
      template = template.substring(1);
    }

    if (template.startsWith("A ") || template.startsWith("C ")) {
      return getStringAnnotations(checker, template, split, value);
    } else if (template.startsWith("AT ") || template.startsWith("CT ")) {
      if (template.contains("^^")) {
        return getTypedAnnotations(checker, template, split, value);
      } else {
        throw new Exception(String.format(typedFormatError, column, template));
      }
    } else if (template.startsWith("AL ") || template.startsWith("CL ")) {
      if (template.contains("@")) {
        return getLanguageAnnotations(checker, template, split, value);
      } else {
        throw new Exception(String.format(languageFormatError, column, template));
      }
    } else if (template.startsWith("AI ") || template.startsWith("CI ")) {
      Set<OWLAnnotation> annotations = new HashSet<>();
      if (split != null) {
        String[] values = value.split(Pattern.quote(split));
        for (String v : values) {
          annotations.add(maybeGetIRIAnnotation(checker, template, v, rowNum, column));
        }
      } else {
        annotations.add(maybeGetIRIAnnotation(checker, template, value, rowNum, column));
      }
      return annotations;
    } else {
      return new HashSet<>();
    }
  }

  /**
   * Given a Manchester Syntax parser, a template string, and a template value, insert the value
   * into the template string and parse to a set of class expressions. If the template string
   * contains 'SPLIT=', the value will be split on the provided character. Otherwise, the set will
   * only have one entry.
   *
   * @param parser ManchesterOWLSyntaxClassExpressionParser to parse expression
   * @param template template string
   * @param value value to replace '%' in template string
   * @param rowNum the line number
   * @param col the column number
   * @return set of OWLClassExpressions
   */
  public static Set<OWLClassExpression> getClassExpressions(
      ManchesterOWLSyntaxClassExpressionParser parser,
      String template,
      String value,
      int rowNum,
      int col)
      throws Exception {
    String split = getSplit(template);
    template = getTemplate(template);

    Set<OWLClassExpression> expressions = new HashSet<>();
    if (template.startsWith("CI")) {
      if (split != null) {
        String[] values = value.split(Pattern.quote(split));
        for (String v : values) {
          String content = QuotedEntityChecker.wrap(v);
          expressions.add(tryParse(parser, content, rowNum, col));
        }
      } else {
        String content = QuotedEntityChecker.wrap(value);
        expressions.add(tryParse(parser, content, rowNum, col));
      }
    } else if (template.startsWith("C ")) {
      if (split != null) {
        String[] values = value.split(Pattern.quote(split));
        for (String v : values) {
          String content = QuotedEntityChecker.wrap(v);
          String sub = template.substring(2).trim().replaceAll("%", content);
          expressions.add(tryParse(parser, sub, rowNum, col));
        }
      } else {
        String content = QuotedEntityChecker.wrap(value);
        String sub = template.substring(2).trim().replaceAll("%", content);
        expressions.add(tryParse(parser, sub, rowNum, col));
      }
    }
    return expressions;
  }

  /**
   * Given a QuotedEntityChecker, a template string, and a template value, return a set of data
   * property expressions based on the value or values (if SPLIT is included in the template
   * string). Note that a data property expression can ONLY be another data property, but this
   * allows support for future data property expressions.
   *
   * @param checker QuotedEntityChecker to resolve entities
   * @param template template string
   * @param value template value or values
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return set of OWLDataPropertyExpressions
   */
  public static Set<OWLDataPropertyExpression> getDataPropertyExpressions(
      QuotedEntityChecker checker, String template, String value, int rowNum, int column)
      throws Exception {
    String split = getSplit(template);
    template = getTemplate(template);

    // Create a parser
    ManchesterOWLSyntaxParser parser =
        new ManchesterOWLSyntaxParserImpl(
            OWLOntologyLoaderConfiguration::new, OWLManager.getOWLDataFactory());
    parser.setOWLEntityChecker(checker);

    // Maybe split values
    List<String> allValues = getAllValues(value, split);

    Set<OWLDataPropertyExpression> expressions = new HashSet<>();
    if (template.startsWith("PI")) {
      for (String v : allValues) {
        String content = QuotedEntityChecker.wrap(v);
        OWLDataProperty property = checker.getOWLDataProperty(content);
        expressions.add(property);
      }
    } else if (template.startsWith("P ")) {
      for (String v : allValues) {
        String content = QuotedEntityChecker.wrap(v);
        String sub = template.substring(2).trim().replaceAll("%", content);
        parser.setStringToParse(sub);
        logger.info("Parsing expression '%s'", sub);
        try {
          expressions.addAll(parser.parseDataPropertyList());
        } catch (OWLParserException e) {
          String cause = getManchesterErrorCause(e);
          throw new Exception(String.format(manchesterParseError, sub, rowNum, column, cause));
        }
      }
    }

    return expressions;
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
    OWLDatatype datatype = checker.getOWLDatatype(name);
    if (datatype != null) {
      return datatype;
    }
    throw new Exception(String.format(datatypeError, name));
  }

  /**
   * Given a QuotedEntityChecker, a string value, and a character to split the value string on (or
   * null), return the value or values as a set of OWLDatatypes.
   *
   * @param checker QuotedEntityChecker to get OWLDatatypes
   * @param value value or values to parse to datatypes
   * @param split character to split value on or null
   * @return set of OWLDatatypes
   * @throws Exception if datatype cannot be found or created
   */
  public static Set<OWLDatatype> getDatatypes(
      QuotedEntityChecker checker, String value, String split) throws Exception {
    List<String> allValues = getAllValues(value, split);

    Set<OWLDatatype> datatypes = new HashSet<>();
    for (String v : allValues) {
      String content = QuotedEntityChecker.wrap(v);
      datatypes.add(getDatatype(checker, content));
    }

    return datatypes;
  }

  /**
   * Given a QuotedEntityChecker, a string value (maybe separated by a split character), and a split
   * character (or null), return the value or values as a set of OWLIndividuals.
   *
   * @param checker QuotedEntityChecker to get individuals by label
   * @param value string of individual or individuals by label or ID
   * @param split character to split value string on
   * @return set of OWLIndividuals
   */
  public static Set<OWLIndividual> getIndividuals(
      QuotedEntityChecker checker, String value, String split) {
    if (value == null || value.trim().isEmpty()) {
      return new HashSet<>();
    }

    Set<OWLIndividual> individuals = new HashSet<>();
    List<String> allValues = getAllValues(value, split);

    for (String v : allValues) {
      individuals.add(checker.getOWLIndividual(v));
    }

    return individuals;
  }

  /**
   * Return an IRI annotation for the given template string and string value. The template string
   * format is "AI [name]" and the value is the name of an entity or an IRI.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return a new annotation axiom with property and an IRI value
   * @throws Exception if the annotation property cannot be found or the IRI cannot be created
   */
  public static OWLAnnotation getIRIAnnotation(
      QuotedEntityChecker checker, String template, String value, int rowNum, int column)
      throws Exception {
    OWLAnnotation iriAnnotation = getIRIAnnotation(checker, template, value);
    if (iriAnnotation == null) {
      throw new Exception(String.format(iriError, rowNum, column, value));
    }
    return iriAnnotation;
  }

  /**
   * Return an IRI annotation for the given template string and string value. The template string
   * format is "AI [name]" and the value is the name of an entity or an IRI.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation axiom with property and an IRI value
   * @throws Exception if the annotation property cannot be found or the IRI cannot be created
   */
  public static OWLAnnotation getIRIAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    IRI iri = checker.getIRI(value, true);
    if (iri == null) {
      return null;
    }
    return getIRIAnnotation(checker, template, iri);
  }

  /**
   * Return an IRI annotation for the given template string and IRI value. The template string
   * format is "AI [name]" and the value is an IRI.
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
   * Get a list of the IRIs defined in a set of template tables.
   *
   * @param tables a map from table names to tables
   * @param ioHelper used to find entities by name
   * @return a list of IRIs
   * @throws Exception when names or templates cannot be handled
   */
  public static List<IRI> getIRIs(Map<String, List<List<String>>> tables, IOHelper ioHelper)
      throws Exception {
    List<IRI> iris = new ArrayList<>();
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

    List<IRI> iris = new ArrayList<>();
    for (int row = 2; row < rows.size(); row++) {
      String id;
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
   * Return a set of language tagged annotations for the given template and value. The template
   * string format is "AL [name]@[lang]" and the value is any string. Replaced by sets of
   * annotations to support splits.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation with property and language tagged literal
   * @throws Exception if the annotation property cannot be found
   */
  @Deprecated
  public static OWLAnnotation getLanguageAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    OWLAnnotationProperty property = getAnnotationProperty(checker, template, "@");
    String lang = template.substring(template.indexOf("@") + 1).trim();
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value, lang));
  }

  /**
   * Return a set of language tagged annotations for the given template and value(s). The template
   * string format is "AL [name]@[lang]" and the value is any string.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param split the character to split values on
   * @param value the value for the annotation
   * @return a set of new annotation(s) with property and language tagged literal
   * @throws Exception if the annotation property cannot be found
   */
  public static Set<OWLAnnotation> getLanguageAnnotations(
      QuotedEntityChecker checker, String template, String split, String value) throws Exception {
    OWLAnnotationProperty property = getAnnotationProperty(checker, template, "@");
    String lang = template.substring(template.indexOf("@") + 1).trim();

    Set<OWLAnnotation> annotations = new HashSet<>();
    List<String> allValues = getAllValues(value, split);

    for (String v : allValues) {
      annotations.add(dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(v, lang)));
    }

    return annotations;
  }

  /**
   * Given a QuotedEntityChecker, a string value (maybe separated by a split character), and a split
   * character (or null), return the value or values as a set of OWLLiterals.
   *
   * @param checker QuotedEntityChecker to get datatypes
   * @param value string of literal or literals
   * @param split character to split value string on
   * @return set of OWLLiterals
   */
  public static Set<OWLLiteral> getLiterals(QuotedEntityChecker checker, String value, String split)
      throws Exception {
    Set<OWLLiteral> literals = new HashSet<>();
    List<String> allValues = getAllValues(value, split);

    for (String v : allValues) {
      if (v.contains("^^")) {
        String datatype = v.substring(v.indexOf("^^") + 2);
        v = v.substring(0, v.indexOf("^^"));
        OWLDatatype dt = getDatatype(checker, datatype);
        literals.add(dataFactory.getOWLLiteral(v.trim(), dt));
      } else {
        literals.add(dataFactory.getOWLLiteral(v.trim()));
      }
    }

    return literals;
  }

  /**
   * Given a QuotedEntityChecker, a template string, and a template value, return a set of object
   * property expressions based on the value or values (if SPLIT is included in the template
   * string). Note that an object property expression can ONLY be another object property or
   * 'inverse', but this allows support for future data property expressions.
   *
   * @param checker QuotedEntityChecker to resolve entities
   * @param template template string
   * @param value template value or values
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return set of OWLDataPropertyExpressions
   */
  public static Set<OWLObjectPropertyExpression> getObjectPropertyExpressions(
      QuotedEntityChecker checker, String template, String value, int rowNum, int column)
      throws Exception {
    String split = getSplit(template);
    template = getTemplate(template);

    // Create a parser

    ManchesterOWLSyntaxParser parser =
        new ManchesterOWLSyntaxParserImpl(
            new OWLAPIConfigProvider(), OWLManager.getOWLDataFactory());
    parser.setOWLEntityChecker(checker);

    // Maybe split values
    List<String> allValues = getAllValues(value, split);

    Set<OWLObjectPropertyExpression> expressions = new HashSet<>();
    if (template.startsWith("PI")) {
      for (String v : allValues) {
        String content = QuotedEntityChecker.wrap(v);
        OWLObjectProperty property = checker.getOWLObjectProperty(content);
        expressions.add(property);
      }
    } else if (template.startsWith("P ")) {
      for (String v : allValues) {
        String content = QuotedEntityChecker.wrap(v);
        String sub = template.substring(2).trim().replaceAll("%", content);
        parser.setStringToParse(sub);
        logger.info("Parsing expression '%s'", sub);
        try {
          expressions.addAll(parser.parseObjectPropertyList());
        } catch (OWLParserException e) {
          String cause = getManchesterErrorCause(e);
          throw new Exception(String.format(manchesterParseError, sub, rowNum, column, cause));
        }
      }
    }

    return expressions;
  }

  /**
   * Return a string annotation for the given template string and value. Replaced by sets of
   * annotations to support splits.
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation with property and string literal value
   * @throws Exception if the annotation property cannot be found
   */
  @Deprecated
  public static OWLAnnotation getStringAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    String name = template.substring(1).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value));
  }

  /**
   * Return a set of string annotations for the given template string and value(s).
   *
   * @param checker used to resolve the annotation property
   * @param template the template string
   * @param split the character to split values on
   * @param value the value for the annotation
   * @return a set of new annotation(s) with property and string literal value
   * @throws Exception if the annotation property cannot be found
   */
  public static Set<OWLAnnotation> getStringAnnotations(
      QuotedEntityChecker checker, String template, String split, String value) throws Exception {

    String name = template.substring(1).trim();
    OWLAnnotationProperty property = getAnnotationProperty(checker, name);

    Set<OWLAnnotation> annotations = new HashSet<>();
    if (split != null) {
      String[] values = value.split(Pattern.quote(split));
      for (String v : values) {
        annotations.add(dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(v)));
      }
    } else {
      annotations.add(dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value)));
    }

    return annotations;
  }

  /**
   * Return a set of typed annotations for the given template string and value. The template string
   * format is "AT [name]^^[datatype]" and the value is any string. Replaced by sets of annotations
   * to support splits.
   *
   * @param checker used to resolve the annotation property and datatype
   * @param template the template string
   * @param value the value for the annotation
   * @return a new annotation with property and typed literal value
   * @throws Exception if the annotation property cannot be found
   */
  @Deprecated
  public static OWLAnnotation getTypedAnnotation(
      QuotedEntityChecker checker, String template, String value) throws Exception {
    OWLAnnotationProperty property = getAnnotationProperty(checker, template, "^^");
    String typeName = template.substring(template.indexOf("^^") + 2).trim();
    OWLDatatype datatype = getDatatype(checker, typeName);
    return dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value, datatype));
  }

  /**
   * Return a set of typed annotations for the given template string and value(s). The template
   * string format is "AT [name]^^[datatype]" and the value is any string.
   *
   * @param checker used to resolve the annotation property and datatype
   * @param template the template string
   * @param split the character to split values on
   * @param value the value for the annotation
   * @return a set of new annotation(s) with property and typed literal value
   * @throws Exception if the annotation property cannot be found
   */
  public static Set<OWLAnnotation> getTypedAnnotations(
      QuotedEntityChecker checker, String template, String split, String value) throws Exception {
    OWLAnnotationProperty property = getAnnotationProperty(checker, template, "^^");
    String typeName = template.substring(template.indexOf("^^") + 2).trim();
    OWLDatatype datatype = getDatatype(checker, typeName);

    Set<OWLAnnotation> annotations = new HashSet<>();
    if (split != null) {
      String[] values = value.split(Pattern.quote(split));
      for (String v : values) {
        annotations.add(
            dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(v, datatype)));
      }
    } else {
      annotations.add(
          dataFactory.getOWLAnnotation(property, dataFactory.getOWLLiteral(value, datatype)));
    }

    return annotations;
  }

  /**
   * Given an entity type or character-separated entity types, a character to split the type string
   * on, and a default type, return a list of the types. If type is null, set to the default type.
   *
   * @param type type or types to include in list
   * @param typeSplit character to split type string on
   * @param defaultType default type if type is null
   * @return list of types
   */
  public static List<String> getTypes(String type, String typeSplit, String defaultType) {
    List<String> types = new ArrayList<>();
    if (type == null || type.trim().isEmpty()) {
      types.add(defaultType);
    } else {
      if (typeSplit != null) {
        String[] ts = type.split(Pattern.quote(typeSplit));
        for (String t : ts) {
          types.add(t.trim().toLowerCase());
        }
      } else {
        types.add(type.trim().toLowerCase());
      }
    }
    return types;
  }

  /**
   * Given a QuotedEntityChecker, a value, and a split character (or null), return the value (or
   * values) as a set of IRIs.
   *
   * @param checker QuotedEntityChecker to get IRIs
   * @param value value (or values) to parse to IRI
   * @param split character to split value on
   * @return set of IRIs
   */
  public static Set<IRI> getValueIRIs(QuotedEntityChecker checker, String value, String split) {
    List<String> allValues = getAllValues(value, split);

    Set<IRI> iris = new HashSet<>();
    for (String v : allValues) {
      IRI iri = checker.getIRI(v.trim(), true);
      if (iri != null) {
        iris.add(iri);
      }
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
    return readXSV(new FileReader(path), ',');
  }

  /**
   * Read comma-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(InputStream stream) throws IOException {
    return readXSV(new InputStreamReader(stream), ',');
  }

  /**
   * Read comma-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(Reader reader) throws IOException {
    return readXSV(reader, ',');
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
    switch (extension) {
      case "csv":
        return readCSV(new FileReader(path));
      case "tsv":
      case "tab":
        return readTSV(new FileReader(path));
      default:
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
    return readXSV(new FileReader(path), '\t');
  }

  /**
   * Read tab-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(InputStream stream) throws IOException {
    return readXSV(new InputStreamReader(stream), '\t');
  }

  /**
   * Read tab-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(Reader reader) throws IOException {
    return readXSV(reader, '\t');
  }

  /**
   * Return true if the template string is valid, false otherwise.
   *
   * @param template the template string to check
   * @return true if valid, false otherwise
   */
  public static boolean validateTemplateString(String template) {
    template = template.trim();
    if (template.equals("ID")) {
      return true;
    }
    if (template.equals("LABEL")) {
      return true;
    }
    if (template.equals("TYPE")) {
      return true;
    }
    if (template.equals("CLASS_TYPE")) {
      return true;
    }
    if (template.matches("^(>?C|>{0,2}A[LTI]?|>?P|>?D|>?I|>?R|>?DT) .*")) {
      return true;
    }
    if (template.startsWith("PROPERTY_TYPE")) {
      return true;
    }
    if (template.startsWith("INDIVIDUAL_TYPE")) {
      return true;
    }
    return template.matches("^(CI|PI|DI|RI|II)");
  }

  /**
   * Given a Manchester class expression parser and a content string, try to parse the content
   * string. Throw a detailed exception message if parsing fails.
   *
   * @param parser ManchesterOWLSyntaxClassExpressionParser to parse string
   * @param content class expression string to parse
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return OWLClassExpression representation of the string
   * @throws Exception if string cannot be parsed for any reason
   */
  protected static OWLClassExpression tryParse(
      ManchesterOWLSyntaxClassExpressionParser parser, String content, int rowNum, int column)
      throws Exception {
    OWLClassExpression expr;
    logger.info("Parsing expression '%s'", content);
    try {
      expr = parser.parse(content);
    } catch (OWLParserException e) {
      String cause = getManchesterErrorCause(e);
      throw new Exception(String.format(manchesterParseError, content, rowNum, column, cause));
    }
    return expr;
  }

  /**
   * Given an OWLParserException, determine if we can identify the offending term. Return that as
   * the cause.
   *
   * @param e exception to get cause of
   * @return String cause of exception
   */
  private static String getManchesterErrorCause(OWLParserException e) {
    String cause = e.getMessage();
    String pattern = ".*Encountered ([^ ]*|'.*') at line.*";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(e.getMessage());
    if (m.find()) {
      if (m.group(1).startsWith("'")) {
        return "encountered unknown " + m.group(1);
      } else {
        return String.format("encountered unknown '%s'", m.group(1));
      }
    }
    return cause;
  }

  /**
   * Given a value (maybe separated by a split character) and a split character (or null), return
   * the string as a list of values. If there is no split character, the value is the only element
   * of the list.
   *
   * @param value string to split
   * @param split character to split on, or null
   * @return values as list
   */
  private static List<String> getAllValues(String value, String split) {
    List<String> allValues = new ArrayList<>();
    if (split != null) {
      String[] values = value.split(Pattern.quote(split));
      for (String v : values) {
        allValues.add(v.trim());
      }
    } else {
      allValues.add(value.trim());
    }
    return allValues;
  }

  /**
   * Given a quoted entity checker, a template string, and a character that separates the template
   * from a tag, return the annotation property.
   *
   * @param checker QuotedEntityChecker to resolve properties
   * @param template template string
   * @param chr character to split template string
   * @return OWLAnnotationProperty
   * @throws Exception on issue resolving property
   */
  private static OWLAnnotationProperty getAnnotationProperty(
      QuotedEntityChecker checker, String template, String chr) throws Exception {
    template = template.substring(2).trim();
    String name = template.substring(0, template.indexOf(chr)).trim();
    return getAnnotationProperty(checker, name);
  }

  /**
   * Given a checker, a template string, and a value for the template, return an IRI annotation.
   *
   * @param checker QuotedEntityChecker to resolve entities
   * @param template template string
   * @param value value to use with the template string
   * @param rowNum the row number for logging
   * @param column the column number for logging
   * @return OWLAnnotation created from template and value
   * @throws Exception if entities cannot be resolved
   */
  private static OWLAnnotation maybeGetIRIAnnotation(
      QuotedEntityChecker checker, String template, String value, int rowNum, int column)
      throws Exception {
    IRI iri = checker.getIRI(value, true);
    if (iri != null) {
      return getIRIAnnotation(checker, template, iri);
    } else {
      throw new Exception(String.format(iriError, rowNum, column, value));
    }
  }

  /**
   * Given a Reader and a separator character, return the contents of the table as a list of rows.
   *
   * @param reader a reader to read data from
   * @param separator separator character
   * @return a list of lists of strings
   * @throws IOException on file reading problems
   */
  private static List<List<String>> readXSV(Reader reader, char separator) throws IOException {
    CSVReader csv =
        new CSVReaderBuilder(reader)
            .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
            .build();
    List<List<String>> rows = new ArrayList<>();
    for (String[] nextLine : csv) {
      rows.add(new ArrayList<>(Arrays.asList(nextLine)));
    }
    csv.close();
    return rows;
  }

  /**
   * Get a set of annotation axioms for an OWLEntity. Supports axiom annotations and axiom
   * annotation annotations.
   *
   * @deprecated TemplateOperation replaced with Template class
   * @param entity OWLEntity to annotation
   * @param annotations Set of OWLAnnotations
   * @param nested Map with top-level OWLAnnotation as key and another map (axiom OWLAnnotation, set
   *     of axiom annotation OWLAnnotations) as value
   * @return Set of OWLAnnotationAssertionAxioms
   */
  @Deprecated
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
   * Given a set of rows, the row number, and the column number, get the content in the column for
   * the row. If there are any issues, return an empty string. If the cell is empty, return null.
   *
   * @deprecated TemplateOperation replaced with Template class
   * @param rows list of rows (lists of strings)
   * @param row row number to get ID of
   * @param column column number
   * @return content, null, or empty string.
   */
  @Deprecated
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
   * Use type, id, and label information to get an entity from the data in a row. Requires either:
   * an id (default type is owl:Class); an id and type; or a label.
   *
   * @deprecated TemplateOperation replaced with Template class
   * @param checker for looking up labels
   * @param type the IRI of the type for this entity, or null
   * @param id the ID for this entity, or null
   * @param label the label for this entity, or null
   * @return the entity or null
   */
  @Deprecated
  public static OWLEntity getEntity(
      QuotedEntityChecker checker, IRI type, String id, String label) {

    IOHelper ioHelper = checker.getIOHelper();

    if (id != null && ioHelper != null) {
      IRI iri = ioHelper.createIRI(id);
      if (type == null) {
        type = IRI.create(OWL_CLASS);
      }
      String t = type.toString();
      switch (t) {
        case OWL_CLASS:
          return dataFactory.getOWLClass(iri);
        case OWL_ANNOTATION_PROPERTY:
          return dataFactory.getOWLAnnotationProperty(iri);
        case OWL_OBJECT_PROPERTY:
          return dataFactory.getOWLObjectProperty(iri);
        case OWL_DATA_PROPERTY:
          return dataFactory.getOWLDataProperty(iri);
        case OWL_DATATYPE:
          return dataFactory.getOWLDatatype(iri);
        default:
          return dataFactory.getOWLNamedIndividual(iri);
      }
    }

    if (label != null && type != null) {
      String t = type.toString();
      switch (t) {
        case OWL_CLASS:
          return checker.getOWLClass(label);
        case OWL_ANNOTATION_PROPERTY:
          return checker.getOWLAnnotationProperty(label);
        case OWL_OBJECT_PROPERTY:
          return checker.getOWLObjectProperty(label);
        case OWL_DATA_PROPERTY:
          return checker.getOWLDataProperty(label);
        case OWL_DATATYPE:
          return checker.getOWLDatatype(label);
        default:
          return checker.getOWLIndividual(label);
      }
    }

    if (label != null) {
      return checker.getOWLEntity(label);
    }

    return null;
  }

  /**
   * Get a set of OWLAxioms (subclass or equivalent) for an OWLClass. Supports axiom annotations.
   *
   * @deprecated TemplateOperation replaced with Template class
   * @param cls OWLClass to add axioms to
   * @param classType subclass or equivalent
   * @param classExpressions Set of OWLClassExpressions
   * @param annotatedExpressions Map of annotated OWLClassExpressions and the Set of OWLAnnotations
   * @return Set of OWLAxioms, or null if classType is not subclass or equivalent
   * @throws Exception if classType is not subclass or equivalent
   */
  @Deprecated
  public static Set<OWLAxiom> getLogicalAxioms(
      OWLClass cls,
      String classType,
      Set<OWLClassExpression> classExpressions,
      Map<OWLClassExpression, Set<OWLAnnotation>> annotatedExpressions)
      throws Exception {
    Set<OWLAxiom> axioms = new HashSet<>();
    switch (classType) {
      case "subclass":
        for (OWLClassExpression expression : classExpressions) {
          axioms.add(dataFactory.getOWLSubClassOfAxiom(cls, expression));
        }
        for (Entry<OWLClassExpression, Set<OWLAnnotation>> annotatedEx :
            annotatedExpressions.entrySet()) {
          axioms.add(
              dataFactory.getOWLSubClassOfAxiom(cls, annotatedEx.getKey(), annotatedEx.getValue()));
        }
        return axioms;
      case "equivalent":
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
      default:
        throw new Exception(String.format(classTypeError, classType));
    }
  }

  /**
   * Given a template string, a cell value, and an empty list, fill the list with any number of
   * values based on a SPLIT character, then return the template string without SPLIT. If there are
   * no SPLITs, only add the original cell to the values.
   *
   * @deprecated TemplateOperation replaced with Template class
   * @param template template string
   * @param cell cell contents
   * @param values empty list to fill
   * @return template string without SPLIT
   */
  @Deprecated
  public static String processSplit(String template, String cell, List<String> values) {
    // If the template contains SPLIT=X,
    // then split the cell value
    // and remove that string from the template.
    Pattern splitter = Pattern.compile("SPLIT=(\\S+)");
    Matcher matcher = splitter.matcher(template);
    if (matcher.find()) {
      Pattern split = Pattern.compile(matcher.group(1));
      values.addAll(Arrays.asList(split.split(Pattern.quote(cell))));
      template = matcher.replaceAll("").trim();
    } else {
      values.add(cell);
    }
    return template;
  }
}
