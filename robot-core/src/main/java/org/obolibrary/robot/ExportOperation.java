package org.obolibrary.robot;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.obolibrary.robot.export.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class ExportOperation {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ExportOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "export#";

  private static final String invalidColumnError =
      NS + "INVALID COLUMN ERROR unable to find property for column header '%s'";

  private static final String includeNothingError =
      NS + "INCLUDE NOTHING ERROR you must include some types of ontology terms";

  private static final String unknownFormatError =
      NS + "UNKNOWN FORMAT ERROR '%s' is an unknown export format";

  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  /**
   * Return a map from option name to default option value, for all the available export options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("include", "classes individuals");
    options.put("exclude-anonymous", "false");
    options.put("format", "tsv");
    options.put("sort", null);
    options.put("split", "|");
    return options;
  }

  /**
   * Given an ontology, an ioHelper, a list of columns, an output export file, and a map of options,
   * export details about the entities in the ontology to the export file. Use the columns to
   * determine which details are included in the output.
   *
   * @param ontology OWLOntology to export to table
   * @param ioHelper IOHelper to handle labels
   * @param columnNames List of column names, in order
   * @param exportFile File to export to
   * @param options Map of Export options
   * @throws Exception if file does not exist to write to or a column is not a valid property
   */
  public static void export(
      OWLOntology ontology,
      IOHelper ioHelper,
      List<String> columnNames,
      File exportFile,
      Map<String, String> options)
      throws Exception {
    boolean excludeAnonymous = OptionsHelper.optionIsTrue(options, "exclude-anonymous");

    // Get column or columns to sort on
    // If not provided, use the first column
    String sortColumn = OptionsHelper.getOption(options, "sort", columnNames.get(0));
    if (sortColumn == null) {
      sortColumn = columnNames.get(0);
    }

    // Get the label provider for Manchester rendering and labels
    DefaultPrefixManager pm = ioHelper.getPrefixManager();
    QuotedAnnotationValueShortFormProvider labelProvider =
        new QuotedAnnotationValueShortFormProvider(
            ontology.getOWLOntologyManager(),
            pm,
            pm,
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());

    // Different providers:
    // Provider for full IRIs
    // Provider for CURIES
    // TODO - Each column should have a ShortFormProvider

    // TODO - maybe set sort renderer to null so that we don't render stuff twice if it is the same

    // Create table object
    String format = OptionsHelper.getOption(options, "format", "tsv");
    Table table = new Table(format);

    // Get a label map of all labels -> IRIs
    Map<String, IRI> labelMap = OntologyHelper.getLabelIRIs(ontology);

    // Create a QuotedEntityChecker to handle names
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ontology);

    // Create the Column objects and add to table
    List<String> sorts = Arrays.asList(sortColumn.trim().split("\\|"));
    for (String c : columnNames) {
      // Try to resolve a CURIE or a label
      IRI iri = ioHelper.createIRI(c);

      if (iri == null) {
        iri = labelMap.getOrDefault(c, null);
      }

      // Maybe get a property
      OWLAnnotationProperty ap = checker.getOWLAnnotationProperty(c, false);
      OWLDataProperty dp = checker.getOWLDataProperty(c);
      OWLObjectProperty op = checker.getOWLObjectProperty(c);

      Column column = null;
      for (String s : sorts) {
        if (c.equalsIgnoreCase(s)) {
          // Regular sort column
          int sortOrder = sorts.indexOf(c);
          if (ap != null) {
            column = new Column(c, ap, sortOrder, false);
          } else if (dp != null) {
            column = new Column(c, dp, sortOrder, false);
          } else if (op != null) {
            column = new Column(c, op, sortOrder, false);
          } else {
            column = new Column(c, iri, sortOrder, false);
          }
          break;
        } else if (s.equalsIgnoreCase("*" + c)) {
          // Reverse sort column
          int sortOrder = sorts.indexOf("*" + c);
          if (ap != null) {
            column = new Column(c, ap, sortOrder, true);
          } else if (dp != null) {
            column = new Column(c, dp, sortOrder, true);
          } else if (op != null) {
            column = new Column(c, op, sortOrder, true);
          } else {
            column = new Column(c, iri, sortOrder, true);
          }
          break;
        }
      }
      if (column == null) {
        // Not a sort column
        if (ap != null) {
          column = new Column(c, ap);
        } else if (dp != null) {
          column = new Column(c, dp);
        } else if (op != null) {
          column = new Column(c, op);
        } else {
          column = new Column(c, iri);
        }
      }
      table.addColumn(column);
    }
    // Order the sort columns
    table.setSortColumns();

    // Get the entities to include in the spreadsheet
    Set<OWLEntity> entities = getEntities(ontology, options);

    // Get the cell values based on columns
    for (OWLEntity entity : entities) {
      table.addRow(getRow(ontology, table, labelProvider, entity, excludeAnonymous));
    }

    // Sort the rows by sort column or columns
    table.sortRows();

    String split = OptionsHelper.getOption(options, "split", "|");
        switch (format) {
          case "tsv":
            IOHelper.writeTable(table.toList(split), exportFile, '\t');
            break;
          case "csv":
            IOHelper.writeTable(table.toList(split), exportFile, ',');
        break;
      case "html":
        try (PrintWriter out = new PrintWriter(exportFile)) {
          out.print(table.toHTML(split));
        }
        break;
      default:
        throw new Exception(String.format(unknownFormatError, format));
    }
  }

  /**
   * Given a label function, a collection of class expressions, and a boolean indicating to include
   * anonymous classes, return the classes as a string. Multiple values are separated by the pipe
   * character. Use labels when possible. Return null if no values exist.
   *
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param classes Set of class expressions to convert to string
   * @param excludeAnonymous if true, exclude anonymous class expressions
   * @return String of class expressions or null
   */
  private static List<String> classExpressionsToString(
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      Collection<OWLClassExpression> classes,
      boolean excludeAnonymous) {
    List<String> strings = new ArrayList<>();
    for (OWLClassExpression expr : classes) {
      if (expr.isAnonymous() && !excludeAnonymous) {
        // Get a Manchester string using labels
        String manString = renderManchester(rt, provider, expr);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLClass sc = expr.asOWLClass();
        strings.add(renderManchester(rt, provider, sc));
      }
    }
    if (strings.isEmpty()) {
      return Collections.emptyList();
    }
    return strings.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());
  }

  /**
   * Return a Cell containing class expressions.
   *
   * @param exprs Collection of OWLClassExpressions to render in property cell
   * @param column Column for this Cell
   * @param displayRendererType RendererType for display value
   * @param sortRendererType RendererType for sort value
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param excludeAnonymous if true, exclude anonymous expressions
   * @return Cell for this Column containing class expressions
   */
  private static Cell getClassCell(
      Collection<OWLClassExpression> exprs,
      Column column,
      RendererType displayRendererType,
      RendererType sortRendererType,
      QuotedAnnotationValueShortFormProvider provider,
      boolean excludeAnonymous) {
    List<String> displays =
        classExpressionsToString(displayRendererType, provider, exprs, excludeAnonymous);
    List<String> sorts =
        classExpressionsToString(sortRendererType, provider, exprs, excludeAnonymous);
    return new Cell(column, displays, sorts);
  }

  /**
   * Return a Cell containing property expressions.
   *
   * @param exprs Collection of OWLPropertyExpressions to render in property cell
   * @param column Column for this Cell
   * @param displayRendererType RendererType for display value
   * @param sortRendererType RendererType for sort value
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param excludeAnonymous if true, exclude anonymous expressions
   * @return Cell for this Column containing property expressions
   */
  private static Cell getPropertyCell(
      Collection<? extends OWLPropertyExpression> exprs,
      Column column,
      RendererType displayRendererType,
      RendererType sortRendererType,
      QuotedAnnotationValueShortFormProvider provider,
      boolean excludeAnonymous) {
    List<String> displays =
        propertyExpressionsToString(displayRendererType, provider, exprs, excludeAnonymous);
    List<String> sorts =
        propertyExpressionsToString(sortRendererType, provider, exprs, excludeAnonymous);
    return new Cell(column, displays, sorts);
  }

  /**
   * Return a Row for an OWLEntity from an ontology.
   *
   * @param ontology OWLOntology to get details from
   * @param table Table to get rendering information and columns
   * @param entity OWLEntity to get details of
   * @param excludeAnonymous if true, exclude anonymous expressions
   * @return Row object for the OWLEntity
   * @throws Exception on invalid column
   */
  private static Row getRow(
      OWLOntology ontology,
      Table table,
      QuotedAnnotationValueShortFormProvider provider,
      OWLEntity entity,
      boolean excludeAnonymous)
      throws Exception {

    String format = table.getFormat();

    RendererType displayRendererType = table.getDisplayRendererType();
    RendererType sortRendererType = table.getSortRendererType();

    Row row = new Row();
    for (Column col : table.getColumns()) {

      String colName = col.getName();
      Cell cell;
      switch (colName) {
        case "IRI":
          String iriString = entity.getIRI().toString();
          if (format.equalsIgnoreCase("html")) {
            String display = String.format("<a href=\"%s'\">%s</a>", iriString, iriString);
            cell = new Cell(col, display, iriString);
          } else {
            cell = new Cell(col, iriString);
          }
          row.add(cell);
          continue;
        case "CURIE":
          String curie = entity.getIRI().getShortForm().replace("_", ":");
          if (format.equalsIgnoreCase("html")) {
            String display =
                String.format("<a href=\"%s'\">%s</a>", entity.getIRI().toString(), curie);
            cell = new Cell(col, display, curie);
          } else {
            cell = new Cell(col, curie);
          }
          row.add(cell);
          continue;
        case "LABEL":
          String display = renderManchester(displayRendererType, provider, entity);
          String sort = renderManchester(sortRendererType, provider, entity);
          row.add(new Cell(col, display, sort));
          continue;
        default:
          break;
      }

      // If a property exists, use this property to get values
      OWLProperty colProperty = col.getProperty();
      if (colProperty != null) {
        if (colProperty instanceof OWLAnnotationProperty) {
          OWLAnnotationProperty ap = (OWLAnnotationProperty) colProperty;
          List<String> displayAndSort = getPropertyValues(ontology, entity, ap);
          row.add(new Cell(col, displayAndSort));
          continue;

        } else if (colProperty instanceof OWLDataProperty) {
          OWLDataProperty dp = (OWLDataProperty) colProperty;
          List<String> display =
              getPropertyValues(
                  ontology, displayRendererType, provider, entity, dp, excludeAnonymous);
          List<String> sort =
              getPropertyValues(ontology, sortRendererType, provider, entity, dp, excludeAnonymous);
          row.add(new Cell(col, display, sort));
          continue;

        } else {
          // Object property
          OWLObjectProperty op = (OWLObjectProperty) colProperty;
          List<String> display =
              getPropertyValues(
                  ontology, displayRendererType, provider, entity, op, excludeAnonymous);
          List<String> sort =
              getPropertyValues(ontology, sortRendererType, provider, entity, op, excludeAnonymous);
          row.add(new Cell(col, display, sort));
          continue;
        }
      }

      // Otherwise, determine what to do based on the IRI
      IRI colIRI = col.getIRI();
      // Set to IRI string or empty string
      String iriStr;
      if (colIRI != null) {
        iriStr = colIRI.toString();
      } else {
        iriStr = "";
      }

      // Check for default column names or IRI matches
      if (colName.equalsIgnoreCase("subclasses")) {
        if (entity.isOWLClass()) {
          Collection<OWLClassExpression> subclasses = EntitySearcher.getSubClasses(entity.asOWLClass(), ontology);
          row.add(
            getClassCell(
              subclasses, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }
      } else if (colName.equalsIgnoreCase("subclass of")
          || iriStr.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
        if (entity.isOWLClass()) {
          Collection<OWLClassExpression> supers =
              EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology);
          row.add(
              getClassCell(
                  supers, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }

      } else if (colName.equalsIgnoreCase("subproperty of")
          || iriStr.equals("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")) {
        if (entity.isOWLAnnotationProperty()) {
          // Annotation properties always render as labels (no expressions)
          Collection<OWLAnnotationProperty> supers =
              EntitySearcher.getSuperProperties(entity.asOWLAnnotationProperty(), ontology);
          row.add(
              getPropertyCell(
                  supers, col, displayRendererType, sortRendererType, provider, excludeAnonymous));

        } else if (entity.isOWLDataProperty()) {
          Collection<OWLDataPropertyExpression> supers =
              EntitySearcher.getSuperProperties(entity.asOWLDataProperty(), ontology);
          row.add(
              getPropertyCell(
                  supers, col, displayRendererType, sortRendererType, provider, excludeAnonymous));

        } else if (entity.isOWLObjectProperty()) {
          Collection<OWLObjectPropertyExpression> supers =
              EntitySearcher.getSuperProperties(entity.asOWLObjectProperty(), ontology);
          row.add(
              getPropertyCell(
                  supers, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }
      } else if (colName.equalsIgnoreCase("equivalent class")
          || iriStr.equals("http://www.w3.org/2002/07/owl#equivalentClass")) {
        if (entity.isOWLClass()) {
          Collection<OWLClassExpression> eqs =
              EntitySearcher.getEquivalentClasses(entity.asOWLClass(), ontology);
          row.add(
              getClassCell(
                  eqs, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }
      } else if (colName.equalsIgnoreCase("equivalent property")
          || iriStr.equals("http://www.w3.org/2002/07/owl#equivalentProperty")) {
        if (entity.isOWLAnnotationProperty()) {
          Collection<OWLAnnotationProperty> eqs =
              EntitySearcher.getEquivalentProperties(entity.asOWLAnnotationProperty(), ontology);
          row.add(
              getPropertyCell(
                  eqs, col, displayRendererType, sortRendererType, provider, excludeAnonymous));

        } else if (entity.isOWLDataProperty()) {
          Collection<OWLDataPropertyExpression> eqs =
              EntitySearcher.getEquivalentProperties(entity.asOWLDataProperty(), ontology);
          row.add(
              getPropertyCell(
                  eqs, col, displayRendererType, sortRendererType, provider, excludeAnonymous));

        } else if (entity.isOWLObjectProperty()) {
          Collection<OWLObjectPropertyExpression> eqs =
              EntitySearcher.getEquivalentProperties(entity.asOWLObjectProperty(), ontology);
          row.add(
              getPropertyCell(
                  eqs, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }
      } else if (colName.equalsIgnoreCase("disjoint with")
          || iriStr.equals("http://www.w3.org/2002/07/owl#disjointWith")) {
        if (entity.isOWLClass()) {
          Collection<OWLClassExpression> disjoints =
              EntitySearcher.getDisjointClasses(entity.asOWLClass(), ontology);
          row.add(
              getClassCell(
                  disjoints,
                  col,
                  displayRendererType,
                  sortRendererType,
                  provider,
                  excludeAnonymous));

        } else if (entity.isOWLAnnotationProperty()) {
          Collection<OWLAnnotationProperty> disjoints =
              EntitySearcher.getDisjointProperties(entity.asOWLAnnotationProperty(), ontology);
          row.add(
              getPropertyCell(
                  disjoints,
                  col,
                  displayRendererType,
                  sortRendererType,
                  provider,
                  excludeAnonymous));

        } else if (entity.isOWLDataProperty()) {
          Collection<OWLDataPropertyExpression> disjoints =
              EntitySearcher.getDisjointProperties(entity.asOWLDataProperty(), ontology);
          row.add(
              getPropertyCell(
                  disjoints,
                  col,
                  displayRendererType,
                  sortRendererType,
                  provider,
                  excludeAnonymous));

        } else if (entity.isOWLObjectProperty()) {
          Collection<OWLObjectPropertyExpression> disjoints =
              EntitySearcher.getDisjointProperties(entity.asOWLObjectProperty(), ontology);
          row.add(
              getPropertyCell(
                  disjoints,
                  col,
                  displayRendererType,
                  sortRendererType,
                  provider,
                  excludeAnonymous));
        }
      } else if (colName.equalsIgnoreCase("type")
          || iriStr.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
        if (entity.isOWLNamedIndividual()) {
          Collection<OWLClassExpression> types =
              EntitySearcher.getTypes(entity.asOWLNamedIndividual(), ontology);
          row.add(
              getClassCell(
                  types, col, displayRendererType, sortRendererType, provider, excludeAnonymous));
        }
      } else {
        throw new Exception(String.format(invalidColumnError, colName));
      }
    }

    return row;
  }

  /**
   * Given an OWLOntology and a map of options for which types of entities to included return a set
   * of entities in the ontology.
   *
   * @param ontology OWLOntology to get entities from
   * @param options map of export options
   * @return set of OWLEntities in the ontology
   */
  private static Set<OWLEntity> getEntities(OWLOntology ontology, Map<String, String> options) {
    // Determine what types of entities to include
    boolean includeClasses = false;
    boolean includeProperties = false;
    boolean includeIndividuals = false;
    String include = OptionsHelper.getOption(options, "include");

    // Split include on space, comma, or tab
    // If 'include' doesn't have any of these characters, it is only one value
    String[] split = {include};
    if (include.contains(" ")) {
      split = include.split(" ");
    } else if (include.contains(",")) {
      split = include.split(",");
    } else if (include.contains("\t")) {
      split = include.split("\t");
    }

    for (String i : split) {
      switch (i.toLowerCase().trim()) {
        case "classes":
          includeClasses = true;
          break;
        case "properties":
          includeProperties = true;
          break;
        case "individuals":
          includeIndividuals = true;
          break;
      }
    }
    if (!includeClasses && !includeProperties && !includeIndividuals) {
      // If all three are false, nothing to include
      throw new IllegalArgumentException(includeNothingError);
    }

    Set<OWLEntity> entities = OntologyHelper.getEntities(ontology);
    // Remove defaults
    entities.remove(dataFactory.getOWLThing());
    entities.remove(dataFactory.getOWLNothing());
    entities.remove(dataFactory.getOWLTopObjectProperty());
    entities.remove(dataFactory.getOWLBottomObjectProperty());
    entities.remove(dataFactory.getOWLTopDataProperty());
    entities.remove(dataFactory.getOWLBottomDataProperty());

    // If we need to exclude anything, return a set of trimmed entities
    Set<OWLEntity> trimmedEntities = new HashSet<>();
    for (OWLEntity e : entities) {
      if (e.isOWLClass() && !includeClasses) {
        continue;
      } else if ((e.isOWLObjectProperty() || e.isOWLDataProperty() || e.isOWLAnnotationProperty())
          && !includeProperties) {
        continue;
      } else if (e.isOWLNamedIndividual() && !includeIndividuals) {
        continue;
      } else if (e.isOWLDatatype()) {
        // Always exclude datatypes
        continue;
      }
      trimmedEntities.add(e);
    }
    return trimmedEntities;
  }

  /**
   * Given an OWL Ontology, an OWL Entity, and an OWL Annotation Property, get all property values
   * as one string separated by the pipe character. If there are no property values, return null.
   *
   * @param ontology OWLOntology to get values from
   * @param entity OWLEntity to get annotations on
   * @param ap OWLAnnotationProperty to get the value(s) of
   * @return String of values or null
   */
  private static List<String> getPropertyValues(
      OWLOntology ontology, OWLEntity entity, OWLAnnotationProperty ap) {
    List<String> values = new ArrayList<>();
    for (OWLAnnotationAssertionAxiom a :
        EntitySearcher.getAnnotationAssertionAxioms(entity, ontology)) {
      if (a.getProperty().getIRI() == ap.getIRI()) {
        OWLLiteral literal = a.getValue().asLiteral().orNull();
        if (literal == null) {
          logger.error(
              String.format(
                  "Missing literal for '%s' %s",
                  entity.getIRI().getShortForm(), ap.getIRI().getShortForm()));
        } else {
          values.add(literal.getLiteral());
        }
      }
    }
    return values;
  }

  /**
   * Given an OWL Ontology, a quoted short form provider, an OWL Entity, and an OWL Data Property,
   * get all property values as one string separated by the pipe character. If there are no property
   * values, return null.
   *
   * @param ontology OWLOntology to get values from
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param entity OWLEntity to get relations of
   * @param dp OWLDataProperty to get the value(s) of
   * @param excludeAnonymous if true, do not include anonymous class expressions
   * @return String of values or null
   */
  private static List<String> getPropertyValues(
      OWLOntology ontology,
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      OWLEntity entity,
      OWLDataProperty dp,
      boolean excludeAnonymous) {
    if (entity.isOWLNamedIndividual()) {
      OWLNamedIndividual i = entity.asOWLNamedIndividual();
      Collection<OWLLiteral> propVals = EntitySearcher.getDataPropertyValues(i, dp, ontology);
      return propVals
          .stream()
          .map(OWLLiteral::toString)
          .sorted(String::compareToIgnoreCase)
          .collect(Collectors.toList());
    } else if (entity.isOWLClass()) {
      // Find super class expressions that use this property
      List<String> vals = new ArrayList<>();
      for (OWLClassExpression expr :
          EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        vals.addAll(
            getRestrictionFillers(expr.asConjunctSet(), dp, rt, provider, excludeAnonymous));
      }
      // Find equivalent class expressions that use this property
      for (OWLClassExpression expr :
          EntitySearcher.getEquivalentClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        vals.addAll(
            getRestrictionFillers(expr.asConjunctSet(), dp, rt, provider, excludeAnonymous));
      }
      return vals;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Given an OWL Ontology, a quoted short form provider, an OWL Entity, and an OWL Object Property,
   * get all property values as one string separated by the pipe character. Use labels when
   * possible. If there are no property values, return null.
   *
   * @param ontology OWLOntology to get values from
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param entity OWLEntity to get annotations on
   * @param op OWLObjectProperty to get the value(s) of
   * @param excludeAnonymous if true, do not include anonymous class expressions
   * @return String of values or null
   */
  private static List<String> getPropertyValues(
      OWLOntology ontology,
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      OWLEntity entity,
      OWLObjectProperty op,
      boolean excludeAnonymous) {
    if (entity.isOWLNamedIndividual()) {
      OWLNamedIndividual i = entity.asOWLNamedIndividual();
      Collection<OWLIndividual> propVals = EntitySearcher.getObjectPropertyValues(i, op, ontology);
      return propVals
          .stream()
          .filter(OWLIndividual::isNamed)
          .map(pv -> renderManchester(rt, provider, pv.asOWLNamedIndividual()))
          .sorted(String::compareToIgnoreCase)
          .collect(Collectors.toList());
    } else if (entity.isOWLClass()) {
      // Find super class expressions that use this property
      List<String> exprs = new ArrayList<>();
      for (OWLClassExpression expr :
          EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        exprs.addAll(
            getRestrictionFillers(expr.asConjunctSet(), op, rt, provider, excludeAnonymous));
      }
      // Find equivalent class expressions that use this property
      for (OWLClassExpression expr :
          EntitySearcher.getEquivalentClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        exprs.addAll(
            getRestrictionFillers(expr.asConjunctSet(), op, rt, provider, excludeAnonymous));
      }
      return exprs;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Given a set of OWL class expressions, a data property, a quoted short form provider, and a
   * boolean indicating to exclude anonymous expressions, get the fillers of the restrictions and
   * determine if the data property used in the original expression matches the provided data
   * property. If so, add the filler to the set.
   *
   * @param exprs set of OWLClassExpressions to check
   * @param dp OWLDataProperty to look for
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param excludeAnonymous if true, exclude anonymous data ranges
   * @return set of fillers that are 'values' of the data property
   */
  private static Set<String> getRestrictionFillers(
      Set<OWLClassExpression> exprs,
      OWLDataProperty dp,
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      boolean excludeAnonymous) {
    Set<String> fillers = new HashSet<>();
    for (OWLClassExpression ce : exprs) {
      // Determine the type of restriction
      ClassExpressionType t = ce.getClassExpressionType();
      OWLDataPropertyExpression pe;
      OWLDataRange f;
      int n;
      switch (t) {
        case DATA_ALL_VALUES_FROM:
          OWLDataAllValuesFrom avf = (OWLDataAllValuesFrom) ce;
          f = avf.getFiller();
          pe = avf.getProperty();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLDataProperty prop = pe.asOWLDataProperty();
            if (prop.getIRI() == dp.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, null));
            }
          }
          break;
        case DATA_SOME_VALUES_FROM:
          OWLDataSomeValuesFrom svf = (OWLDataSomeValuesFrom) ce;
          f = svf.getFiller();
          pe = svf.getProperty();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLDataProperty prop = pe.asOWLDataProperty();
            if (prop.getIRI() == dp.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, null));
            }
          }
          break;
        case DATA_EXACT_CARDINALITY:
          OWLDataExactCardinality ec = (OWLDataExactCardinality) ce;
          f = ec.getFiller();
          pe = ec.getProperty();
          n = ec.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLDataProperty prop = pe.asOWLDataProperty();
            if (prop.getIRI() == dp.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
        case DATA_MIN_CARDINALITY:
          OWLDataMinCardinality minc = (OWLDataMinCardinality) ce;
          f = minc.getFiller();
          pe = minc.getProperty();
          n = minc.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLDataProperty prop = pe.asOWLDataProperty();
            if (prop.getIRI() == dp.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
        case DATA_MAX_CARDINALITY:
          OWLDataMaxCardinality maxc = (OWLDataMaxCardinality) ce;
          f = maxc.getFiller();
          pe = maxc.getProperty();
          n = maxc.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLDataProperty prop = pe.asOWLDataProperty();
            if (prop.getIRI() == dp.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
      }
    }
    return fillers;
  }

  /**
   * Given a set of OWL class expressions, an object property, a quoted short form provider, and a
   * boolean indicating to exclude anonymous expressions, get the fillers of the restrictions and
   * determine if the object property used in the original expression matches the provided object
   * property. If so, add the filler to the set.
   *
   * @param exprs set of OWLClassExpressions to check
   * @param op OWLObjectProperty to look for
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param excludeAnonymous if true, exclude anonymous class expressions
   * @return set of fillers that are 'values' of the object property
   */
  private static Set<String> getRestrictionFillers(
      Set<OWLClassExpression> exprs,
      OWLObjectProperty op,
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      boolean excludeAnonymous) {
    Set<String> fillers = new HashSet<>();
    for (OWLClassExpression ce : exprs) {
      // Determine the type of restriction
      ClassExpressionType t = ce.getClassExpressionType();
      OWLObjectPropertyExpression pe;
      OWLClassExpression f;
      int n;
      switch (t) {
        case OBJECT_ALL_VALUES_FROM:
          // property only object
          OWLObjectAllValuesFrom avf = (OWLObjectAllValuesFrom) ce;
          pe = avf.getProperty();
          f = avf.getFiller();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLObjectProperty prop = pe.asOWLObjectProperty();
            if (prop.getIRI() == op.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, null));
            }
          }
          break;
        case OBJECT_SOME_VALUES_FROM:
          // property some object
          OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) ce;
          pe = svf.getProperty();
          f = svf.getFiller();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLObjectProperty prop = pe.asOWLObjectProperty();
            if (prop.getIRI() == op.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, null));
            }
          }
          break;
        case OBJECT_EXACT_CARDINALITY:
          // property exactly n object
          OWLObjectExactCardinality ec = (OWLObjectExactCardinality) ce;
          pe = ec.getProperty();
          f = ec.getFiller();
          n = ec.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLObjectProperty prop = pe.asOWLObjectProperty();
            if (prop.getIRI() == op.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
        case OBJECT_MIN_CARDINALITY:
          // property min n object
          OWLObjectMinCardinality minc = (OWLObjectMinCardinality) ce;
          pe = minc.getProperty();
          f = minc.getFiller();
          n = minc.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLObjectProperty prop = pe.asOWLObjectProperty();
            if (prop.getIRI() == op.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
        case OBJECT_MAX_CARDINALITY:
          // property max n object
          OWLObjectMaxCardinality maxc = (OWLObjectMaxCardinality) ce;
          pe = maxc.getProperty();
          f = maxc.getFiller();
          n = maxc.getCardinality();
          if (excludeAnonymous && f.isAnonymous()) {
            continue;
          }
          if (!pe.isAnonymous()) {
            OWLObjectProperty prop = pe.asOWLObjectProperty();
            if (prop.getIRI() == op.getIRI()) {
              fillers.add(renderRestrictionString(rt, provider, f, n));
            }
          }
          break;
        case OBJECT_ONE_OF:
          System.out.println("ONE OF " + ce);
          break;
        case OBJECT_UNION_OF:
          System.out.println("UNION OF " + ce);
          break;
        case OBJECT_INTERSECTION_OF:
          System.out.println("INTERSECTION OF " + ce);
          break;
      }
    }

    // Not handled:
    // ClassExpressionType.OBJECT_ONE_OF
    // ClassExpressionType.OBJECT_INTERSECTION_OF
    // ClassExpressionType.OBJECT_UNION_OF
    // ClassExpressionType.OBJECT_COMPLEMENT_OF
    // ClassExpressionType.OBJECT_HAS_SELF
    // ClassExpressionType.OBJECT_HAS_VALUE

    return fillers;
  }

  /**
   * Given a label function, a collection of property expressions, and a boolean indicating to
   * include anonymous expressions, return the parent properties as a string. Multiple values are
   * separated by the pipe character. Use labels when possible. Return null if no values exist.
   *
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param props Set of property expressions to convert to string
   * @param excludeAnonymous if true, exclude anonymous expressions
   * @return String of property expressions or null
   */
  private static List<String> propertyExpressionsToString(
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      Collection<?> props,
      boolean excludeAnonymous) {
    // Try to convert to object property expressions
    Collection<OWLObjectPropertyExpression> opes =
        props
            .stream()
            .map(p -> (OWLPropertyExpression) p)
            .filter(OWLPropertyExpression::isObjectPropertyExpression)
            .map(p -> (OWLObjectPropertyExpression) p)
            .collect(Collectors.toList());
    // Try to convert to data property expressions
    Collection<OWLDataPropertyExpression> dpes =
        props
            .stream()
            .map(p -> (OWLPropertyExpression) p)
            .filter(OWLPropertyExpression::isDataPropertyExpression)
            .map(p -> (OWLDataPropertyExpression) p)
            .collect(Collectors.toList());

    List<String> strings = new ArrayList<>();
    // Only one of the above collections will have entries
    // Maybe process object property expressions
    for (OWLObjectPropertyExpression expr : opes) {
      if (expr.isAnonymous() && !excludeAnonymous) {
        String manString = renderManchester(rt, provider, expr);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLObjectProperty op = expr.asOWLObjectProperty();
        strings.add(renderManchester(rt, provider, op));
      }
    }
    // Maybe process data property expressions
    for (OWLDataPropertyExpression expr : dpes) {
      if (expr.isAnonymous() && !excludeAnonymous) {
        String manString = renderManchester(rt, provider, expr);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLDataProperty dp = expr.asOWLDataProperty();
        strings.add(renderManchester(rt, provider, dp));
      }
    }
    // Sort alphabetically
    return strings.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());
  }

  /**
   * Render a Manchester String for a restriction. The RendererType will determine what Manchester OWL Syntax renderer will be created. If an Integer is provided, this will be included as the cardinality.
   *
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param filler OWLObject restriction filler
   * @param n Integer optional cardinality restriction
   * @return String rendering of OWLObject based on renderer type
   */
  private static String renderRestrictionString(
      RendererType rt,
      QuotedAnnotationValueShortFormProvider provider,
      OWLObject filler,
      Integer n) {
    String render = renderManchester(rt, provider, filler);
    if (filler instanceof OWLEntity && n != null) {
      // Not anonymous, has cardinality
      return String.format("%d %s", n, render);
    } else if (filler instanceof OWLEntity) {
      // Not anonymous, no cardinality
      return render;
    } else if (n != null) {
      // Expression, has cardinality
      return String.format("%d (%s)", n, render);
    } else {
      // Expression, no cardinality
      return String.format("(%s)", render);
    }
  }

  /**
   * Render a Manchester string for an OWLObject. The RendererType will determine what Manchester OWL Syntax renderer will be created.
   *
   * @param rt RendererType to use to render Manchester
   * @param provider QuotedAnnotationValueShortFormProvider to resolve labels
   * @param object OWLObject to render
   * @return String rendering of OWLObject based on renderer type
   */
  private static String renderManchester(
      RendererType rt, QuotedAnnotationValueShortFormProvider provider, OWLObject object) {
    ManchesterOWLSyntaxObjectRenderer renderer;
    StringWriter sw = new StringWriter();
    if (rt == RendererType.OBJECT_HTML_RENDERER) {
      renderer = new ManchesterOWLSyntaxObjectHTMLRenderer(sw, provider);
    } else {
      // Default renderer
      renderer = new ManchesterOWLSyntaxObjectRenderer(sw, provider);
    }
    object.accept(renderer);
    return sw.toString().replace("\n", "").replaceAll(" +", " ");
  }
}
