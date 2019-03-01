package org.obolibrary.robot.template;

import com.google.common.collect.Lists;
import java.util.*;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.QuotedEntityChecker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/** @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a> */
public class Template {

  /** Template IOHelper to resolve prefixes. */
  private IOHelper ioHelper;

  /** Template QuotedEntityChecker to get entities and IRIs by label. */
  private QuotedEntityChecker checker;

  /** Manchester Syntax parser to parse class expressions. */
  private ManchesterOWLSyntaxClassExpressionParser parser;

  /** Set of axioms generated from template. */
  private Set<OWLAxiom> axioms;

  /** Location of IDs (ID). */
  private int idColumn = -1;

  /** Location of labels (LABEL, A rdfs:label, A label). */
  private int labelColumn = -1;

  /** Location of entity types (TYPE). */
  private int typeColumn = -1;

  /** Location of class types (CLASS_TYPE). */
  private int classTypeColumn = -1;

  /** Location of property types (PROPERTY_TYPE). */
  private int propertyTypeColumn = -1;

  /** Location of individual types (INDIVIDUAL_TYPE). */
  private int individualTypeColumn = -1;

  /** Character to split property types on. */
  private String propertyTypeSplit = null;

  /** Character to split generic types on. */
  private String typeSplit = null;

  /** List of human-readable template headers. */
  private List<String> headers;

  /** List of ROBOT template strings. */
  private List<String> templates;

  /** All other rows of the table (does not include headers and template strings). */
  private List<List<String>> tableRows;

  /** Row number tracker. */
  private int rowNum = 0;

  /** Shared data factory. */
  private final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  /** Namespace for error messages. */
  private static final String NS = "template#";

  /** Error message when an annotation property gets a property type other than subproperty. */
  private static final String annotationPropertyTypeError = NS + "ANNOTATION PROPERTY TYPE ERROR annotation property %s type '%s' must be 'subproperty' at row %d, column %d";

  /** Error message when an invalid class type is provided. */
  private static final String classTypeError = NS + "CLASS TYPE ERROR class %s has unknown type '%s' at row %d, column %d";

  /**
   * Error message when the number of header columns does not match the number of template columns.
   * Expects: table name, header count, template count.
   */
  private static final String columnMismatchError =
      NS  + "COLUMN MISMATCH ERROR the number of header columns (%2$d) must match the number of template columns (%3$d) in table \"%1$s\".";

  /** Error message when an invalid individual type is provided. */
  private static final String individualTypeError =
      NS + "INDIVIDUAL TYPE ERROR individual %s has unknown type '%s' at row %d, column %d";

  /** Error message when an invalid property type is provided. */
  private static final String propertyTypeError = NS + "PROPERTY TYPE ERROR property %s has unknown type '%s' at row %d, column %d";

  private static final String multiplePropertyTypeError = NS + "MULTIPLE PROPERTY TYPE ERROR property type list may only include one of: subproperty, equivalent, disjoint, or inverse.";

  /**
   * Error message when a template cannot be understood. Expects: table name, column number, column
   * name, template.
   */
  private static final String unknownTemplateError =
      NS + "UNKNOWN TEMPLATE ERROR could not interpret template string \"%4$s\" for column %2$d (\"%3$s\") in table \"%1$s\".";

  /**
   * Given a template name and a list of rows, create a template object with a new IOHelper and
   * QuotedEntityChecker. The rows are added to the object, new labels from the rows are added to
   * the checker, and a Manchester Syntax parser is created.
   *
   * @param name template name
   * @param rows list of rows (lists)
   * @throws Exception on issue creating IOHelper or adding table to template object
   */
  public Template(String name, List<List<String>> rows) throws Exception {
    this.ioHelper = new IOHelper();

    checker = new QuotedEntityChecker();
    checker.setIOHelper(this.ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());

    tableRows = new ArrayList<>();
    templates = new ArrayList<>();
    headers = new ArrayList<>();
    axioms = new HashSet<>();

    // Add the contents of the tableRows
    addTable(name, rows);

    // Set all the new labels used in the template
    // Other labels can be found from the input ontology with the checker
    addLabels();
    createParser();
  }

  /**
   * Given a template name, a list of rows, and an IOHelper, create a template object with a new
   * QuotedEntityChecker. The rows are added to the object, new labels from the rows are added to
   * the checker, and a Manchester Syntax parser is created.
   *
   * @param name template name
   * @param rows list of rows (lists)
   * @param ioHelper IOHelper to resolve prefixes
   * @throws Exception on issue adding table to template object
   */
  public Template(String name, List<List<String>> rows, IOHelper ioHelper) throws Exception {
    this.ioHelper = ioHelper;

    checker = new QuotedEntityChecker();
    checker.setIOHelper(this.ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());

    tableRows = new ArrayList<>();
    templates = new ArrayList<>();
    headers = new ArrayList<>();
    axioms = new HashSet<>();

    // Add the contents of the tableRows
    addTable(name, rows);

    // Set all the new labels used in the template
    // Other labels can be found from the input ontology with the checker
    addLabels();
    createParser();
  }

  /**
   * Given a template name, a list of rows, and an input ontology, create a template object with a
   * new IOHelper and QuotedEntityChecker populated by the input ontology. The rows are added to the
   * object, new labels from the rows are added to the checker, and a Manchester Syntax parser is
   * created.
   *
   * @param name template name
   * @param rows list of rows (lists)
   * @param inputOntology OWLOntology to get labels of entities for QuotedEntityChecker
   * @throws Exception on issue creating IOHelper or adding table to template object
   */
  public Template(String name, List<List<String>> rows, OWLOntology inputOntology)
      throws Exception {
    ioHelper = new IOHelper();

    checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    if (inputOntology != null) {
      checker.addAll(inputOntology);
    }

    tableRows = new ArrayList<>();
    templates = new ArrayList<>();
    headers = new ArrayList<>();
    axioms = new HashSet<>();

    // Add the contents of the tableRows
    addTable(name, rows);

    // Set all the new labels used in the template
    // Other labels can be found from the input ontology with the checker
    addLabels();
    createParser();
  }

  /**
   * Given a template name, a list of rows, an input ontology, and an IOHelper, create a template
   * object with a new QuotedEntityChecker with the IOHelper populated by the input ontology. The
   * rows are added to the object, new labels from the rows are added to the checker, and a
   * Manchester Syntax parser is created.
   *
   * @param name template name
   * @param rows list of rows (lists)
   * @param inputOntology OWLOntology to get labels of entities for QuotedEntityChecker
   * @param ioHelper IOHelper to resolve prefixes
   * @throws Exception on issue adding table to template object
   */
  public Template(
      String name, List<List<String>> rows, OWLOntology inputOntology, IOHelper ioHelper)
      throws Exception {
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    if (inputOntology != null) {
      checker.addAll(inputOntology);
    }

    this.ioHelper = ioHelper;
    this.checker = checker;

    tableRows = new ArrayList<>();
    templates = new ArrayList<>();
    headers = new ArrayList<>();
    axioms = new HashSet<>();

    // Add the contents of the tableRows
    addTable(name, rows);

    // Set all the new labels used in the template
    // Other labels can be found from the input ontology with the checker
    addLabels();
    createParser();
  }

  /**
   * Given a template name, a list of rows, an IOHelper, and a QuotedEntityChecker, create a
   * template object. The rows are added to the object, new labels from the rows are added to the
   * checker, and a Manchester Syntax parser is created.
   *
   * @param name template name
   * @param rows list of rows (lists)
   * @param ioHelper IOHelper to resolve prefixes
   * @param checker QuotedEntityChecker to get entities by label
   * @throws Exception on issue adding table to template object
   */
  public Template(
      String name, List<List<String>> rows, IOHelper ioHelper, QuotedEntityChecker checker)
      throws Exception {
    this.ioHelper = ioHelper;
    this.checker = checker;

    tableRows = new ArrayList<>();
    templates = new ArrayList<>();
    headers = new ArrayList<>();
    axioms = new HashSet<>();

    // Add the contents of the tableRows
    addTable(name, rows);

    // Set all the new labels used in the template
    // Other labels can be found from the input ontology with the checker
    addLabels();
    createParser();
  }

  /**
   * Generate an OWLOntology based on the rows of the template.
   *
   * @return new OWLOntology
   * @throws Exception on issue parsing rows to axioms or creating new ontology
   */
  public OWLOntology generateOutputOntology() throws Exception {
    return generateOutputOntology(null);
  }

  /**
   * Generate an OWLOntology with given IRI based on the rows of the template.
   *
   * @return new OWLOntology
   * @throws Exception on issue parsing rows to axioms or creating new ontology
   */
  public OWLOntology generateOutputOntology(String outputIRI) throws Exception {
    for (List<String> row : tableRows) {
      processRow(row);
    }

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology;
    if (outputIRI != null) {
      IRI iri = IRI.create(outputIRI);
      outputOntology = manager.createOntology(iri);
    } else {
      outputOntology = manager.createOntology();
    }

    manager.addAxioms(outputOntology, axioms);

    return outputOntology;
  }

  /**
   * Given the name of a table and a list of rows, first validate the headers and template strings.
   * Then, get the location of important columns (e.g. IDs and labels). Finally, add all template
   * rows to the object.
   *
   * @param name name of table
   * @param rows list of rows (lists)
   * @throws Exception on malformed template
   */
  private void addTable(String name, List<List<String>> rows) throws Exception {
    // Get and validate headers
    headers = rows.get(0);
    templates = rows.get(1);
    if (headers.size() != templates.size()) {
      throw new Exception(
          String.format(columnMismatchError, name, headers.size(), templates.size()));
    }

    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);

      // If the template is null or the column is empty, skip this column
      if (template == null) {
        continue;
      }
      template = template.trim();
      if (template.isEmpty()) {
        continue;
      }

      // Validate the template string
      if (!TemplateHelper.validateTemplateString(template)) {
        throw new Exception(
          String.format(unknownTemplateError, name, column + 1, headers.get(column), template));
      }

      // Get the location of important columns
      // TODO: support other label properties
      switch (template) {
        case "ID":
          idColumn = column;
          break;
        case "LABEL":
        case "A rdfs:label":
        case "A label":
          labelColumn = column;
          break;
        default:
          break;
      }
      if (template.startsWith("TYPE")) {
        typeColumn = column;
        if (template.contains("SPLIT=")) {
          typeSplit = template.substring(template.indexOf("SPLIT=") + 6);
        }
      } else if (template.startsWith("CLASS_TYPE")) {
        classTypeColumn = column;
        if (template.contains("SPLIT=")) {
          // TODO - add message
          throw new Exception();
        }
      } else if (template.startsWith("PROPERTY_TYPE")) {
        propertyTypeColumn = column;
        if (template.contains("SPLIT=")) {
          propertyTypeSplit = template.substring(template.indexOf("SPLIT=") + 6);
        }
      } else if (template.startsWith("INDIVIDUAL_TYPE")) {
        individualTypeColumn = column;
        if (template.contains("SPLIT=")) {
          // TODO - add message
          throw new Exception();
        }
      }
    }

    if (idColumn == -1 && labelColumn == -1) {
      throw new Exception(
          "Template row must include an \"ID\" or \"LABEL\" column in table: " + name);
    }

    // Add the rest of the tableRows to Template
    for (int row = 2; row < rows.size(); row++) {
      tableRows.add(rows.get(row));
    }
  }

  /** Add the labels from the rows of the template to the QuotedEntityChecker. */
  private void addLabels() {
    for (List<String> row : tableRows) {
      String id = null;
      if (idColumn != -1) {
        try {
          id = row.get(idColumn);
        } catch (IndexOutOfBoundsException e) {
          // ignore
        }
      }

      String label = null;
      if (labelColumn != -1) {
        try {
          label = row.get(labelColumn);
        } catch (IndexOutOfBoundsException e) {
          // ignore
        }
      }

      if (idColumn != -1 && id == null) {
        continue;
      }

      if (id == null || label == null) {
        continue;
      }

      String type = null;
      if (typeColumn != -1) {
        try {
          type = row.get(typeColumn);
        } catch (IndexOutOfBoundsException e) {
          // ignore
        }
      }
      if (type == null) {
        type = "class";
      }

      IRI iri = ioHelper.createIRI(id);
      if (iri == null) {
        iri = IRI.create(id);
      }

      OWLEntity entity;
      switch (type) {
        case "":
        case "owl:Class":
        case "class":
          entity = dataFactory.getOWLEntity(EntityType.CLASS, iri);
          break;

        case "owl:ObjectProperty":
        case "object property":
          entity = dataFactory.getOWLEntity(EntityType.OBJECT_PROPERTY, iri);
          break;

        case "owl:DataProperty":
        case "data property":
          entity = dataFactory.getOWLEntity(EntityType.DATA_PROPERTY, iri);
          break;

        case "owl:AnnotationProperty":
        case "annotation property":
          entity = dataFactory.getOWLEntity(EntityType.ANNOTATION_PROPERTY, iri);
          break;

        case "owl:Individual":
        case "individual":
        case "owl:NamedIndividual":
        case "named individual":
          entity = dataFactory.getOWLEntity(EntityType.NAMED_INDIVIDUAL, iri);
          break;

        case "owl:Datatype":
        case "datatype":
          entity = dataFactory.getOWLEntity(EntityType.DATATYPE, iri);
          break;

        default:
          // Assume type is an individual (checked later)
          entity = dataFactory.getOWLEntity(EntityType.NAMED_INDIVIDUAL, iri);
          break;
      }
      checker.add(entity, label);
    }
  }

  /** Create a Manchester Syntax parser from the OWLDataFactory and QuotedEntityChecker. */
  private void createParser() {
    this.parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
  }

  /**
   * Process each of the table rows. First, get an entity based on ID or label. If the template
   * contains an ID column, but it is empty, skip that row. If it does not contain an ID column,
   * skip if the label is empty. Add axioms based on the entity type (class, object property, data
   * property, annotation property, datatype, or individual).
   *
   * @throws Exception on issue creating axioms from template
   */
  private void processRow(List<String> row) throws Exception {
      rowNum++;
      String id = null;
      try {
        id = row.get(idColumn);
      } catch (IndexOutOfBoundsException e) {
        // ignore
      }
      String label = null;
      try {
        label = row.get(labelColumn);
      } catch (IndexOutOfBoundsException e) {
        // ignore
      }
      String type = null;
      try {
        type = row.get(typeColumn);
      } catch (IndexOutOfBoundsException e) {
        // ignore
      }

      // Skip if no ID and no label
      if (id == null && label == null) {
        return;
      }

      if (type == null || type.trim().isEmpty()) {
        type = "class";
      }

      IRI iri = getIRI(id, label);
      switch (type) {
        case "owl:Class":
        case "class":
          addClassAxioms(iri, row);
          break;

        case "owl:ObjectProperty":
        case "object property":
          addObjectPropertyAxioms(iri, row);
          break;

        case "owl:DataProperty":
        case "data property":
          addDataPropertyAxioms(iri, row);
          break;

        case "owl:AnnotationProperty":
        case "annotation property":
          addAnnotationPropertyAxioms(iri, row);
          break;

        case "owl:Datatype":
        case "datatype":
          addDatatypeAxioms(iri, row);
          break;

        case "owl:Individual":
        case "individual":
        case "owl:NamedIndividual":
        case "named individual":
        default:
          addIndividualAxioms(iri, row);
          break;
      }
  }

  /* CLASS AXIOMS */

  /**
   * Given a class IRI and the row containing the class details, generate class axioms.
   *
   * @param iri class IRI
   * @param row list of template values for given class
   * @throws Exception on issue creating class axioms from template
   */
  private void addClassAxioms(IRI iri, List<String> row) throws Exception {
    if (iri == null) {
      return;
    }
    // Add the declaration
    OWLClass cls = dataFactory.getOWLClass(iri);
    OWLDeclarationAxiom ax = dataFactory.getOWLDeclarationAxiom(cls);
    axioms.add(ax);

    String classType = null;
    if (classTypeColumn != -1) {
      try {
        classType = row.get(classTypeColumn);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }
    }
    if (classType == null || classType.trim().isEmpty()) {
      classType = "subclass";
    } else {
      classType = classType.trim().toLowerCase();
    }

    Set<OWLClassExpression> expressions = new HashSet<>();
    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      if (template.startsWith("A")) {
        // Handle class annotations
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      } else if (template.startsWith("C") && !template.startsWith("CLASS_TYPE")) {
        // Handle class logic
        expressions.addAll(TemplateHelper.getClassExpressions(parser, template, value));
        switch (classType) {
          case "subclass":
            addSubClassAxioms(cls, expressions, row, column);
            break;
          case "equivalent":
            addEquivalentClassesAxioms(cls, expressions, row, column);
            break;
          case "disjoint":
            addDisjointClassAxioms(cls, expressions, row, column);
            break;
          default:
            // TODO - unknown class type
            throw new Exception(String.format(classTypeError, cls.getIRI().getShortForm(), classType, rowNum, column));
        }
      }
    }
  }

  /**
   * Given an OWLClass, a set of OWLClassExpressions, and the row containing the class details,
   * generate subClassOf axioms for the class where the parents are the class expressions. Maybe
   * annotate the axioms.
   *
   * @param cls OWLClass to create subClassOf axioms for
   * @param expressions set of parent OWLClassExpressions
   * @param row list of template values for given class
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addSubClassAxioms(
      OWLClass cls, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLClassExpression expr : expressions) {
      axioms.add(dataFactory.getOWLSubClassOfAxiom(cls, expr, axiomAnnotations));
    }
  }
  
  /**
   * Given an OWLClass, a set of OWLClassExpressions, and the row containing the class details,
   * generate equivalentClasses axioms for the class where the equivalents are the class
   * expressions. Maybe annotate the axioms.
   *
   * @param cls OWLClass to create equivalentClasses axioms for
   * @param expressions set of equivalent OWLClassExpressions
   * @param row list of template values for given class
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addEquivalentClassesAxioms(
      OWLClass cls, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLClassExpression expr : expressions) {
      axioms.add(dataFactory.getOWLEquivalentClassesAxiom(cls, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLClass, a set of OWLClassExpressions, and the row containing the class details,
   * generate disjointClasses axioms for the class where the disjoints are the class expressions.
   * Maybe annotate the axioms.
   *
   * @param cls OWLClass to create disjointClasses axioms for
   * @param expressions set of disjoint OWLClassExpressions
   * @param row list of template values for given class
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addDisjointClassAxioms(
      OWLClass cls, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    expressions.add(cls);
    axioms.add(dataFactory.getOWLDisjointClassesAxiom(expressions, axiomAnnotations));
  }

  /* OBJECT PROPERTY AXIOMS */

  /**
   * Given an object property IRI and the row containing the property details, generate
   * property axioms.
   *
   * @param iri object property IRI
   * @param row list of template values for given object property
   * @throws Exception on issue creating object property axioms from template
   */
  private void addObjectPropertyAxioms(IRI iri, List<String> row) throws Exception {
    // Add the declaration
    axioms.add(
        dataFactory.getOWLDeclarationAxiom(
            dataFactory.getOWLEntity(EntityType.OBJECT_PROPERTY, iri)));

    String propertyType = null;
    if (propertyTypeColumn != 1) {
      try {
        propertyType = row.get(propertyTypeColumn);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }
    }
    List<String> propertyTypes =
        TemplateHelper.getTypes(propertyType, propertyTypeSplit, "subproperty");

    OWLObjectProperty property = dataFactory.getOWLObjectProperty(iri);

    // Handle special property types
    for (String pt : propertyTypes) {
      switch (pt.trim().toLowerCase()) {
        case "asymmetric":
          axioms.add(dataFactory.getOWLAsymmetricObjectPropertyAxiom(property));
          break;
        case "functional":
          axioms.add(dataFactory.getOWLFunctionalObjectPropertyAxiom(property));
          break;
        case "inversefunctional":
        case "inverse functional":
          axioms.add(dataFactory.getOWLInverseFunctionalObjectPropertyAxiom(property));
          break;
        case "irreflexive":
          axioms.add(dataFactory.getOWLIrreflexiveObjectPropertyAxiom(property));
          break;
        case "reflexive":
          axioms.add(dataFactory.getOWLReflexiveObjectPropertyAxiom(property));
          break;
        case "symmetric":
          axioms.add(dataFactory.getOWLSymmetricObjectPropertyAxiom(property));
          break;
        case "transitive":
          axioms.add(dataFactory.getOWLTransitiveObjectPropertyAxiom(property));
          break;
      }
    }
    // Remove the special types
    propertyTypes.removeAll(
        Lists.newArrayList(
            "asymmetric",
            "functional",
            "inversefunctional",
            "inverse functional",
            "irreflexive",
            "reflexive",
            "symmetric",
            "transitive"));
    if (propertyTypes.size() > 1) {
      // There may only be one of: subproperty, equivalent, disjoint, or inverse
      throw new Exception(multiplePropertyTypeError);
    } else if (propertyTypes.size() == 0) {
      propertyTypes.add("subproperty");
    }
    propertyType = propertyTypes.get(0);

    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      if (template.startsWith("A")) {
        // Handle annotations
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      } else if (template.startsWith("P") && !template.startsWith("PROPERTY_TYPE")) {
        // Handle property logic
        Set<OWLObjectPropertyExpression> expressions =
            TemplateHelper.getObjectPropertyExpressions(checker, template, value);
        switch (propertyType) {
          case "subproperty":
            addSubObjectPropertyAxioms(property, expressions, row, column);
            break;
          case "equivalent":
            addEquivalentObjectPropertiesAxioms(property, expressions, row, column);
            break;
          case "disjoint":
            addDisjointObjectPropertiesAxioms(property, expressions, row, column);
            break;
          case "inverse":
            addInverseObjectPropertiesAxioms(property, expressions, row, column);
            break;
          default:
            // TODO - invalid property type
            throw new Exception(String.format(propertyTypeError, iri.getShortForm(), propertyType, rowNum, column));
        }
      } else if (template.startsWith("DOMAIN")) {
        // Handle domains
        Set<OWLClassExpression> expressions =
            TemplateHelper.getClassExpressions(parser, template, value);
        addObjectPropertyDomains(property, expressions, row, column);
      } else if (template.startsWith("RANGE")) {
        // Handle ranges
        Set<OWLClassExpression> expressions =
            TemplateHelper.getClassExpressions(parser, template, value);
        addObjectPropertyRanges(property, expressions, row, column);
      }
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLObjectPropertyExpressions, and the row containing the
   * property details, generate subPropertyOf axioms for the property where the parents are the
   * property expressions. Maybe annotate the axioms.
   *
   * @param property OWLObjectProperty to create subPropertyOf axioms for
   * @param expressions set of parent OWLObjectPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addSubObjectPropertyAxioms(
      OWLObjectProperty property,
      Set<OWLObjectPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLObjectPropertyExpression expr : expressions) {
      axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLObjectPropertyExpressions, and the row containing the
   * property details, generate equivalentProperties axioms for the property where the equivalents
   * are the property expressions. Maybe annotate the axioms.
   *
   * @param property OWLObjectProperty to create equivalentProperties axioms for
   * @param expressions set of equivalent OWLObjectPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addEquivalentObjectPropertiesAxioms(
      OWLObjectProperty property,
      Set<OWLObjectPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLObjectPropertyExpression expr : expressions) {
      axioms.add(
          dataFactory.getOWLEquivalentObjectPropertiesAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLObjectPropertyExpressions, and the row containing the
   * property details, generate disjointProperties axioms for the property where the disjoints are
   * the property expressions. Maybe annotate the axioms.
   *
   * @param property OWLObjectProperty to create disjointProperties axioms for
   * @param expressions set of disjoint OWLObjectPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addDisjointObjectPropertiesAxioms(
      OWLObjectProperty property,
      Set<OWLObjectPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    expressions.add(property);
    axioms.add(dataFactory.getOWLDisjointObjectPropertiesAxiom(expressions, axiomAnnotations));
  }

  /**
   * Given an OWLObjectProperty, a set of OWLObjectPropertyExpressions, and the row containing the
   * property details, generate inverseProperties axioms for the property where the inverses are the
   * property expressions. Maybe annotate the axioms.
   *
   * @param property OWLObjectProperty to create inverseProperties axioms for
   * @param expressions set of inverse OWLObjectPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addInverseObjectPropertiesAxioms(
      OWLObjectProperty property,
      Set<OWLObjectPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLObjectPropertyExpression expr : expressions) {
      axioms.add(dataFactory.getOWLInverseObjectPropertiesAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLClassExpressions, the row containing the property
   * details, and a column location, generate domain axioms where the domains are the class
   * expressions. Maybe annotation the axioms.
   *
   * @param property OWLObjectProperty to create domain axioms for
   * @param expressions set of domain OWLClassExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addObjectPropertyDomains(
      OWLObjectProperty property, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLClassExpression expr : expressions) {
      axioms.add(dataFactory.getOWLObjectPropertyDomainAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLClassExpressions, the row containing the property
   * details, and a column location, generate range axioms where the ranges are the class
   * expressions. Maybe annotation the axioms.
   *
   * @param property OWLObjectProperty to create range axioms for
   * @param expressions set of range OWLClassExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addObjectPropertyRanges(
      OWLObjectProperty property, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLClassExpression expr : expressions) {
      axioms.add(dataFactory.getOWLObjectPropertyRangeAxiom(property, expr, axiomAnnotations));
    }
  }

  /* DATA PROPERTY AXIOMS */

  /**
   * Given an data property IRI and the row containing the property details, generate
   * property axioms.
   *
   * @param iri data property IRI
   * @param row list of template values for given data property
   * @throws Exception on issue creating data property axioms from template
   */
  private void addDataPropertyAxioms(IRI iri, List<String> row) throws Exception {
    // Add the declaration
    axioms.add(
        dataFactory.getOWLDeclarationAxiom(
            dataFactory.getOWLEntity(EntityType.DATA_PROPERTY, iri)));

    String propertyType = null;
    if (propertyTypeColumn != 1) {
      try {
        propertyType = row.get(propertyTypeColumn);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }
    }
    List<String> propertyTypes =
        TemplateHelper.getTypes(propertyType, propertyTypeSplit, "subproperty");

    OWLDataProperty property = dataFactory.getOWLDataProperty(iri);

    // Handle special property types
    for (String pt : propertyTypes) {
      if (pt.trim().toLowerCase().equals("functional")) {
        axioms.add(dataFactory.getOWLFunctionalDataPropertyAxiom(property));
      }
    }
    propertyTypes.remove("functional");
    if (propertyTypes.size() > 1) {
      // TODO - can only have one of: subproperty, equivalent, or disjoint
      throw new Exception();
    } else if (propertyTypes.size() == 0) {
      propertyTypes.add("subproperty");
    }
    propertyType = propertyTypes.get(0);

    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      String split = null;
      if (template.contains("SPLIT=")) {
        split = template.substring(template.indexOf("SPLIT=") + 6).trim();
      }

      if (template.startsWith("A")) {
        // Handle annotations
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      } else if (template.startsWith("P ") && !template.startsWith("PROPERTY_TYPE")) {
        // Handle property logic
        Set<OWLDataPropertyExpression> expressions =
            TemplateHelper.getDataPropertyExpressions(checker, template, value);
        switch (propertyType) {
          case "subproperty":
            addSubDataPropertyAxioms(property, expressions, row, column);
            break;
          case "equivalent":
            addEquivalentDataPropertiesAxioms(property, expressions, row, column);
            break;
          case "disjoint":
            addDisjointDataPropertiesAxioms(property, expressions, row, column);
            break;
          default:
            // TODO - invalid property type
            throw new Exception();
        }
      } else if (template.startsWith("DOMAIN")) {
        // Handle domains
        Set<OWLClassExpression> expressions =
            TemplateHelper.getClassExpressions(parser, template, value);
        addDataPropertyDomains(property, expressions, row, column);
      } else if (template.startsWith("RANGE")) {
        // Handle ranges
        Set<OWLDatatype> datatypes = TemplateHelper.getDatatypes(checker, value, split);
        addDataPropertyRanges(property, datatypes, row, column);
      }
    }
  }

  /**
   * Given an OWLDataProperty, a set of OWLDataPropertyExpressions, and the row containing the
   * property details, generate subPropertyOf axioms for the property where the parents are the
   * property expressions. Maybe annotate the axioms.
   *
   * @param property OWLDataProperty to create subPropertyOf axioms for
   * @param expressions set of parent OWLDataPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addSubDataPropertyAxioms(
      OWLDataProperty property,
      Set<OWLDataPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLDataPropertyExpression expr : expressions) {
      if (expr != null) {
        axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(property, expr, axiomAnnotations));
      }
    }
  }

  /**
   * Given an OWLDataProperty, a set of OWLDataPropertyExpressions, and the row containing the
   * property details, generate equivalentProperties axioms for the property where the equivalents
   * are the property expressions. Maybe annotate the axioms.
   *
   * @param property OWLDataProperty to create equivalentProperties axioms for
   * @param expressions set of equivalent OWLDataPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addEquivalentDataPropertiesAxioms(
      OWLDataProperty property,
      Set<OWLDataPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLDataPropertyExpression expr : expressions) {
      axioms.add(dataFactory.getOWLEquivalentDataPropertiesAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLDataProperty, a set of OWLObjectPropertyExpressions, and the row containing the
   * property details, generate disjointProperties axioms for the property where the disjoints are
   * the property expressions. Maybe annotate the axioms.
   *
   * @param property OWLDataProperty to create disjointProperties axioms for
   * @param expressions set of disjoint OWLDataPropertyExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addDisjointDataPropertiesAxioms(
      OWLDataProperty property,
      Set<OWLDataPropertyExpression> expressions,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    expressions.add(property);
    axioms.add(dataFactory.getOWLDisjointDataPropertiesAxiom(expressions, axiomAnnotations));
  }

  /**
   * Given an OWLDataProperty, a set of OWLClassExpressions, the row containing the property
   * details, and a column location, generate domain axioms where the domains are the class
   * expressions. Maybe annotation the axioms.
   *
   * @param property OWLDataProperty to create domain axioms for
   * @param expressions set of domain OWLClassExpressions
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addDataPropertyDomains(
      OWLDataProperty property, Set<OWLClassExpression> expressions, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLClassExpression expr : expressions) {
      axioms.add(dataFactory.getOWLDataPropertyDomainAxiom(property, expr, axiomAnnotations));
    }
  }

  /**
   * Given an OWLObjectProperty, a set of OWLDatatypes, the row containing the property details, and
   * a column location, generate range axioms where the ranges are the datatypes. Maybe annotation
   * the axioms.
   *
   * @param property OWLObjectProperty to create range axioms for
   * @param datatypes set of range OWLDatatypes
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addDataPropertyRanges(
      OWLDataProperty property, Set<OWLDatatype> datatypes, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLDatatype datatype : datatypes) {
      axioms.add(dataFactory.getOWLDataPropertyRangeAxiom(property, datatype, axiomAnnotations));
    }
  }

  /* ANNOTATION PROPERTY AXIOMS */

  /**
   * Given an annotation property IRI and the row containing the property details,
   * generate property axioms.
   *
   * @param iri annotation property IRI
   * @param row list of template values for given annotation property
   * @throws Exception on issue creating annotation property axioms from template
   */
  private void addAnnotationPropertyAxioms(IRI iri, List<String> row)
      throws Exception {
    // Add the declaration
    axioms.add(
        dataFactory.getOWLDeclarationAxiom(
            dataFactory.getOWLEntity(EntityType.ANNOTATION_PROPERTY, iri)));

    String propertyType = null;
    if (propertyTypeColumn != 1) {
      try {
        propertyType = row.get(propertyTypeColumn);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }
    }
    if (propertyType == null) {
      propertyType = "subproperty";
    } else {
      propertyType = propertyType.trim().toLowerCase();
    }
    if (!propertyType.equals("subproperty")) {
      // Annotation properties can only have type "subproperty"
      throw new Exception(String.format(annotationPropertyTypeError, iri, propertyType, rowNum, propertyTypeColumn));
    }

    OWLAnnotationProperty property = dataFactory.getOWLAnnotationProperty(iri);

    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      // Maybe get the split character
      String split = null;
      if (template.contains("SPLIT=")) {
        split = template.substring(template.indexOf("SPLIT=") + 6).trim();
      }

      if (template.startsWith("A")) {
        // Handle annotations
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      } else if (template.startsWith("P") && !template.startsWith("PROPERTY_TYPE")) {
        // Handle property logic
        Set<OWLAnnotationProperty> parents =
            TemplateHelper.getAnnotationProperties(checker, value, split);
        addSubAnnotationPropertyAxioms(property, parents, row, column);
      } else if (template.startsWith("DOMAIN")) {
        // Handle domains
        Set<IRI> iris = TemplateHelper.getValueIRIs(checker, value, split);
        addAnnotationPropertyDomains(property, iris, row, column);
      } else if (template.startsWith("RANGE")) {
        // Handle ranges
        Set<IRI> iris = TemplateHelper.getValueIRIs(checker, value, split);
        addAnnotationPropertyRanges(property, iris, row, column);
      }
    }
  }

  /**
   * Given an OWLAnnotationProperty, a set of OWLAnnotationProperties, and the row containing the
   * property details, generate subPropertyOf axioms for the property where the parents are the
   * other properties. Maybe annotate the axioms.
   *
   * @param property OWLObjectProperty to create subPropertyOf axioms for
   * @param parents set of parent OWLAnnotationProperties
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addSubAnnotationPropertyAxioms(
      OWLAnnotationProperty property,
      Set<OWLAnnotationProperty> parents,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the subproperty axiom
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (OWLAnnotationProperty parent : parents) {
      axioms.add(
          dataFactory.getOWLSubAnnotationPropertyOfAxiom(property, parent, axiomAnnotations));
    }
  }

  /**
   * Given an OWLAnnotationProperty, a set of IRIs, the row containing the property details, and a
   * column location, generate domain axioms where the domains are the IRIs. Maybe annotation the
   * axioms.
   *
   * @param property OWLObjectProperty to create domain axioms for
   * @param iris set of domain IRIs
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addAnnotationPropertyDomains(
      OWLAnnotationProperty property, Set<IRI> iris, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (IRI iri : iris) {
      axioms.add(dataFactory.getOWLAnnotationPropertyDomainAxiom(property, iri, axiomAnnotations));
    }
  }

  /**
   * Given an OWLAnnotationProperty, a set of IRIs, the row containing the property details, and a
   * column location, generate range axioms where the ranges are the IRIs. Maybe annotation the
   * axioms.
   *
   * @param property OWLAnnotationProperty to create range axioms for
   * @param iris set of range IRIs
   * @param row list of template values for given property
   * @param column column number of logical template string
   * @throws Exception on issue getting axiom annotations
   */
  private void addAnnotationPropertyRanges(
      OWLAnnotationProperty property, Set<IRI> iris, List<String> row, int column)
      throws Exception {
    // Maybe get an annotation on the expression
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    for (IRI iri : iris) {
      axioms.add(dataFactory.getOWLAnnotationPropertyRangeAxiom(property, iri, axiomAnnotations));
    }
  }

  /* DATATYPE AXIOMS */

  /**
   * Given a datatype IRI and the row containing the datatype details, generate datatype
   * axioms.
   *
   * @param iri datatype IRI
   * @param row list of template values for given datatype
   * @throws Exception on issue creating datatype annotations
   */
  private void addDatatypeAxioms(IRI iri, List<String> row) throws Exception {
    // Add the declaration
    axioms.add(
        dataFactory.getOWLDeclarationAxiom(dataFactory.getOWLEntity(EntityType.DATATYPE, iri)));

    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      // Handle annotations
      if (template.startsWith("A")) {
        // Add the annotations to the datatype
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      }
      // TODO support data definitions
    }
  }

  /* INDIVIDUAL AXIOMS */

  /**
   * Given an individual IRI and the row containing the individual details, generate
   * individual axioms.
   *
   * @param iri individual IRI
   * @param row list of template values for given individual
   * @throws Exception on issue creating individual axioms from template
   */
  private void addIndividualAxioms(IRI iri, List<String> row) throws Exception {
    // Should not return null, as empty defaults to a class
    String typeCol = row.get(typeColumn).trim();
    // Use the 'type' to get the class assertion for the individual
    // If it is owl:Individual or owl:NamedIndividual, it will not have a class assertion
    // There may be more than one class assertion - right now only named classes are supported
    List<String> types = new ArrayList<>();
    if (typeSplit != null) {
      for (String t : typeCol.split(typeSplit)) {
        types.add(t.trim());
      }
    } else {
      types.add(typeCol.trim());
    }

    // The individualType is used to determine what kind of axioms are associated
    // e.g. different individuals, same individuals...
    String individualType = null;
    if (individualTypeColumn != -1) {
      try {
        individualType = row.get(individualTypeColumn);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }
    }

    OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(iri);
    // Add declaration
    axioms.add(dataFactory.getOWLDeclarationAxiom(dataFactory.getOWLNamedIndividual(iri)));

    for (String type : types) {
      // Add a type if the type is not owl:Individual or owl:NamedIndividual
      if (!type.equalsIgnoreCase("individual")
        && !type.equalsIgnoreCase("named individual")
        && !type.equalsIgnoreCase("owl:NamedIndividual")
        && !type.equalsIgnoreCase("owl:Individual")) {
        OWLClass typeCls = checker.getOWLClass(type);
        if (typeCls != null) {
          axioms.add(dataFactory.getOWLClassAssertionAxiom(typeCls, individual));
        }
        // TODO: Manchester syntax parsing for anonymous classes
      }
    }


    for (int column = 0; column < templates.size(); column++) {
      String template = templates.get(column);
      String split = null;
      if (template.contains("SPLIT=")) {
        split = template.substring(template.indexOf("SPLIT=") + 6).trim();
      }

      String value = null;
      try {
        value = row.get(column);
      } catch (IndexOutOfBoundsException e) {
        // do nothing
      }

      if (value == null || value.trim().isEmpty()) {
        continue;
      }

      // Handle annotations
      if (template.startsWith("A")) {
        // Add the annotations to the individual
        Set<OWLAnnotation> annotations = getAnnotations(template, value, row, column);
        for (OWLAnnotation annotation : annotations) {
          axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(iri, annotation));
        }
      } else if (template.startsWith("I") && !template.startsWith("INDIVIDUAL_TYPE")) {
        switch (individualType) {
          case "named":
            if (template.startsWith("I ")) {
              String propStr = template.split(" ")[1];
              OWLObjectProperty objectProperty = checker.getOWLObjectProperty(propStr);
              if (objectProperty != null) {
                Set<OWLIndividual> otherIndividuals =
                    TemplateHelper.getIndividuals(checker, value, split);
                addObjectPropertyAssertionAxioms(
                    individual, otherIndividuals, objectProperty, row, column);
                break;
              }
              OWLDataProperty dataProperty = checker.getOWLDataProperty(propStr);
              if (dataProperty != null) {
                Set<OWLLiteral> literals = TemplateHelper.getLiterals(checker, value, split);
                addDataPropertyAssertionAxioms(individual, literals, dataProperty, row, column);
                break;
              }
            }
            break;
          case "same":
            Set<OWLIndividual> sameIndividuals =
                TemplateHelper.getIndividuals(checker, value, split);
            if (!sameIndividuals.isEmpty()) {
              addSameIndividualsAxioms(individual, sameIndividuals, row, column);
            }
            break;
          case "different":
            Set<OWLIndividual> differentIndividuals =
                TemplateHelper.getIndividuals(checker, value, split);
            if (!differentIndividuals.isEmpty()) {
              addDifferentIndividualsAxioms(individual, differentIndividuals, row, column);
            }
            break;
          default:
            // TODO - unknown individual type
            throw new Exception(
                String.format(
                    individualTypeError, iri.getShortForm(), individualType, rowNum, column));
        }
      }
    }
  }

  /**
   * Given an OWLIndividual, a set of OWLIndividuals, an object property expression, the row as list of strings, and the column number, add each individual as the object of the object property expression for that individual.
   *
   * @param individual OWLIndividual to add object property assertion axioms to
   * @param otherIndividuals set of other OWLIndividuals representing the objects of the axioms
   * @param expression OWLObjectPropertyExpression to use as property of the axioms
   * @param row list of strings
   * @param column column number
   * @throws Exception on problem handling axiom annotations
   */
  private void addObjectPropertyAssertionAxioms(
      OWLNamedIndividual individual,
      Set<OWLIndividual> otherIndividuals,
      OWLObjectPropertyExpression expression,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the subproperty axiom
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    for (OWLIndividual other : otherIndividuals) {
      axioms.add(
          dataFactory.getOWLObjectPropertyAssertionAxiom(
              expression, individual, other, axiomAnnotations));
    }
  }

  /**
   * Given an OWLIndividual, a set of OWLLiterals, a data property expression, the row as list of strings, and the column number, add each literal as the object of the data property expression for that individual.
   *
   * @param individual OWLIndividual to add data property assertion axioms to
   * @param literals set of OWLLiterals representing the objects of the axioms
   * @param expression OWLDataPropertyExpression to use as property of the axioms
   * @param row list of strings
   * @param column column number
   * @throws Exception on problem handling axiom annotations
   */
  private void addDataPropertyAssertionAxioms(
      OWLNamedIndividual individual,
      Set<OWLLiteral> literals,
      OWLDataPropertyExpression expression,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the subproperty axiom
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    for (OWLLiteral lit : literals) {
      axioms.add(
          dataFactory.getOWLDataPropertyAssertionAxiom(
              expression, individual, lit, axiomAnnotations));
    }
  }

  /**
   * Given an OWLIndividual, a set of same individuals, a row as list of strings, and a column number, add the same individual axioms.
   *
   * @param individual OWLIndiviudal to add axioms to
   * @param sameIndividuals set of same individuals
   * @param row list of strings
   * @param column column number
   * @throws Exception on problem handling axiom annotations
   */
  private void addSameIndividualsAxioms(
      OWLNamedIndividual individual,
      Set<OWLIndividual> sameIndividuals,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the subproperty axiom
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    sameIndividuals.add(individual);
    axioms.add(dataFactory.getOWLSameIndividualAxiom(sameIndividuals, axiomAnnotations));
  }

  /**
   * Given an OWLIndividual, a set of different individuals, a row as list of strings, and a column number, add the different individual axioms.
   *
   * @param individual OWLIndiviudal to add axioms to
   * @param differentIndividuals set of different individuals
   * @param row list of strings
   * @param column column number
   * @throws Exception on problem handling axiom annotations
   */
  private void addDifferentIndividualsAxioms(
      OWLNamedIndividual individual,
      Set<OWLIndividual> differentIndividuals,
      List<String> row,
      int column)
      throws Exception {
    // Maybe get an annotation on the subproperty axiom
    Set<OWLAnnotation> axiomAnnotations = maybeGetAxiomAnnotations(row, column);

    // Generate axioms
    differentIndividuals.add(individual);
    axioms.add(dataFactory.getOWLDifferentIndividualsAxiom(differentIndividuals, axiomAnnotations));
  }

  /* ANNOTATION HELPERS */

  /**
   * Given a template string, a value string, a row as a list of strings, and the column number, return a set of one or more OWLAnnotations.
   *
   * @param template template string
   * @param value value of annotation(s)
   * @param row list of strings
   * @param column column number
   * @return Set of OWLAnnotations
   * @throws Exception on issue getting OWLAnnotations
   */
  private Set<OWLAnnotation> getAnnotations(
      String template, String value, List<String> row, int column) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return new HashSet<>();
    }

    Set<OWLAnnotation> annotations =
        TemplateHelper.getAnnotations(checker, ioHelper, template, value);

    // Maybe get an annotation on the annotation
    String nextTemplate;
    try {
      nextTemplate = templates.get(column + 1);
    } catch (IndexOutOfBoundsException e) {
      nextTemplate = null;
    }

    if (nextTemplate != null && !nextTemplate.trim().isEmpty() && nextTemplate.startsWith(">A")) {
      nextTemplate = nextTemplate.substring(1);
      String nextValue;
      try {
        nextValue = row.get(column + 1);
      } catch (IndexOutOfBoundsException e) {
        nextValue = null;
      }
      if (nextValue != null) {
        Set<OWLAnnotation> nextAnnotations =
            TemplateHelper.getAnnotations(checker, ioHelper, nextTemplate, nextValue);
        Set<OWLAnnotation> fixedAnnotations = new HashSet<>();
        for (OWLAnnotation annotation : annotations) {
          fixedAnnotations.add(annotation.getAnnotatedAnnotation(nextAnnotations));
        }
        // Replace the annotation set with the annotated annotations
        annotations = fixedAnnotations;
      }
    }

    return annotations;
  }

  /**
   * Given a row as a list of strings, the template string, and the number of the next column, maybe get axiom annotations on existing axiom annotations.
   *
   * @param row list of strings
   * @param template template string for the column
   * @param nextColumn next column number
   * @return set of OWLAnnotations, or an empty set
   * @throws Exception on issue getting the OWLAnnotations
   */
  private Set<OWLAnnotation> getAxiomAnnotations(List<String> row, String template, int nextColumn)
      throws Exception {
    Set<OWLAnnotation> annotations = new HashSet<>();
    template = template.substring(1);
    String nextValue;
    try {
      nextValue = row.get(nextColumn);
    } catch (IndexOutOfBoundsException e) {
      nextValue = null;
    }
    if (nextValue == null || nextValue.trim().isEmpty()) {
      return annotations;
    }
    annotations.addAll(TemplateHelper.getAnnotations(checker, ioHelper, template, nextValue));
    return annotations;
  }

  /**
   * Given a string ID and a string label, with at least one of those being non-null, return an IRI for the entity.
   *
   * @param id String ID of entity, maybe null
   * @param label String label of entity, maybe null
   * @return IRI of entity
   * @throws Exception if both id and label are null
   */
  private IRI getIRI(String id, String label) throws Exception {
    if (id == null && label == null) {
      // This cannot be hit by CLI users
      throw new Exception("You must specify either an ID or a label");
    }
    if (id != null) {
      return ioHelper.createIRI(id);
    }
    return checker.getIRI(label, true);
  }

  /**
   * Given a row as a list of strings and a column number, determine if the next column contains a one or more axiom annotations. If so, return the axiom annotation or annotations as a set of OWLAnnotations.
   *
   * @param row list of strings
   * @param column column number
   * @return set of OWLAnnotations, maybe empty
   * @throws Exception on issue getting the OWLAnnotations
   */
  private Set<OWLAnnotation> maybeGetAxiomAnnotations(List<String> row, int column) throws Exception {
    String nextTemplate;
    try {
      nextTemplate = templates.get(column + 1);
    } catch (IndexOutOfBoundsException e) {
      nextTemplate = null;
    }

    Set<OWLAnnotation> axiomAnnotations = new HashSet<>();
    if (nextTemplate != null && !nextTemplate.trim().isEmpty() && (nextTemplate.startsWith(">"))) {
      axiomAnnotations = getAxiomAnnotations(row, nextTemplate, column + 1);
    }
    return axiomAnnotations;
  }
}
