package org.obolibrary.robot;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportOperation {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ExportOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "export#";

  private static final String invalidColumnError =
      NS + "INVALID COLUMN ERROR unable to find property for column header '%s'";

  private static final String excludeAllError =
      NS + "EXCLUDE ALL ERROR you cannot exclude all types of ontology terms";

  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  /**
   * Return a map from option name to default option value, for all the available export options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("exclude-classes", "false");
    options.put("exclude-properties", "true");
    options.put("exclude-individuals", "false");
    options.put("exclude-anonymous", "false");
    options.put("sort", null);
    return options;
  }

  /**
   * Given an ontology, an ioHelper, a list of columns, an output export file, and a map of options,
   * export details about the entities in the ontology to the export file. Use the columns to
   * determine which details are included in the output.
   *
   * @param ontology OWLOntology to export to table
   * @param ioHelper IOHelper to handle labels
   * @param columns List of columns, in order
   * @param exportFile File to export to
   * @param options Map of Export options
   * @throws Exception if file does not exist to write to or a column is not a valid property
   */
  public static void export(
      OWLOntology ontology,
      IOHelper ioHelper,
      List<String> columns,
      File exportFile,
      Map<String, String> options)
      throws Exception {
    boolean excludeAnonymous = OptionsHelper.optionIsTrue(options, "exclude-anonymous");
    String sortColumn = OptionsHelper.getOption(options, "sort");
    if (sortColumn == null) {
      sortColumn = columns.get(0);
    }

    String delimiter;
    if (exportFile.getPath().endsWith(".csv")) {
      delimiter = ",";
    } else {
      delimiter = "\t";
    }

    // Create a QuotedEntityChecker to handle column names
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ontology);

    // Get the entities to include in the spreadsheet
    Set<OWLEntity> entities = getEntities(ontology, options);

    // Get the label provider for Manchester rendering and labels
    DefaultPrefixManager pm = ioHelper.getPrefixManager();
    AnnotationValueShortFormProvider labelProvider =
        new AnnotationValueShortFormProvider(
            ontology.getOWLOntologyManager(),
            pm,
            pm,
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());

    // Get the cell values based on columns
    List<Map<String, String>> cellMaps = new ArrayList<>();
    for (OWLEntity e : entities) {
      cellMaps.add(getCellValues(ontology, checker, e, columns, labelProvider, excludeAnonymous));
    }

    // Sort the lines by sort column (default is first column)
    final String comparator = sortColumn;
    Comparator<Map<String, String>> mapComparator =
        Comparator.comparing(m -> m.getOrDefault(comparator, ""));
    cellMaps.sort(mapComparator);

    // Format the lines for the file
    List<String> lines = new ArrayList<>();
    lines.add(buildHeaders(columns, delimiter));
    for (Map<String, String> cellMap : cellMaps) {
      List<String> line = new ArrayList<>();
      for (String col : columns) {
        String value = cellMap.get(col);
        // Value may be "null" as string
        if (value == null || value.equalsIgnoreCase("null")) {
          value = "";
        }
        // Quote the value if it contains delim character
        if (value.contains(delimiter)) {
          value = String.format("\"%s\"", value);
        }
        line.add(value);
      }
      String lineStr = String.join(delimiter, line);
      lines.add(lineStr);
    }

    // Write lines to file
    try (PrintWriter pw = new PrintWriter(exportFile)) {
      lines.forEach(pw::println);
    }
  }

  /**
   * Given a list of columns and a delimiter, return the table headers as a string.
   *
   * @param columns List of columns
   * @param delimiter character to separate cells
   * @return headers as one string
   */
  private static String buildHeaders(List<String> columns, String delimiter) {
    List<String> headers = new ArrayList<>();
    for (String col : columns) {
      switch (col) {
        case "IRI":
          headers.add("IRI");
          break;
        case "CURIE":
          headers.add("CURIE");
          break;
        case "LABEL":
          headers.add("Label");
          break;
        case "subclass-of":
        case "rdfs:subClassOf":
          headers.add("SubClass Of");
          break;
        case "subproperty-of":
        case "rdfs:subPropertyOf":
          headers.add("SubProperty Of");
          break;
        case "equivalent-classes":
        case "owl:equivalentClass":
          headers.add("Equivalent Classes");
          break;
        case "equivalent-properties":
        case "owl:equivalentProperty":
          headers.add("Equivalent Properties");
          break;
        case "disjoints":
        case "owl:disjointWith":
          headers.add("Disjoint With");
          break;
        case "types":
        case "rdf:type":
          headers.add("Instance Of");
          break;
        default:
          headers.add(col);
      }
    }
    return String.join(delimiter, headers);
  }

  /**
   * Given a label function, a collection of class expressions, and a boolean indicating to include
   * anonymous classes, return the classes as a string. Multiple values are separated by the pipe
   * character. Use labels when possible. Return null if no values exist.
   *
   * @param classes Set of class expressions to convert to string
   * @param provider ShortFormProvider for Manchester expressions
   * @param excludeAnonymous if true, exclude anonymous class expressions
   * @return String of class expressions or null
   */
  private static String classExpressionsToString(
      Collection<OWLClassExpression> classes,
      ShortFormProvider provider,
      boolean excludeAnonymous) {
    List<String> strings = new ArrayList<>();
    for (OWLClassExpression expr : classes) {
      if (expr.isAnonymous() && !excludeAnonymous) {
        // Get a Manchester string using labels
        String manString = renderManchester(expr, provider);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLClass sc = expr.asOWLClass();
        strings.add(provider.getShortForm(sc));
      }
    }
    if (strings.isEmpty()) {
      return null;
    }
    List<String> sorted =
        strings.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());
    return String.join("|", sorted);
  }

  /**
   * Given an ontology, a quoted entity checker, an entity, a list of columns, a label function, and
   * a boolean indicating to exclude anonymous classes, return a map of cell values for this entity.
   *
   * @param ontology OWLOntology to get details from
   * @param checker QuotedEntityChecker to resolve entities
   * @param e OWLEntity to get details of
   * @param columns List of columns indicating the details to get
   * @param provider ShortFormProvider for Manchester expressions
   * @param excludeAnonymous if true, exclude anonymous class expressions
   * @return Map of column names to cell values
   * @throws Exception if a property (from columns) cannot be resolved
   */
  private static Map<String, String> getCellValues(
      OWLOntology ontology,
      QuotedEntityChecker checker,
      OWLEntity e,
      List<String> columns,
      ShortFormProvider provider,
      boolean excludeAnonymous)
      throws Exception {
    Map<String, String> cellMap = new HashMap<>();
    for (String col : columns) {
      switch (col) {
        case "IRI":
          cellMap.put("IRI", e.getIRI().toString());
          break;
        case "CURIE":
          cellMap.put("CURIE", e.getIRI().getShortForm().replace("_", ":"));
          break;
        case "LABEL":
          cellMap.put("LABEL", provider.getShortForm(e));
          break;
        case "subclass-of":
        case "rdfs:subClassOf":
          if (e.isOWLClass()) {
            cellMap.put(
                col,
                classExpressionsToString(
                    EntitySearcher.getSuperClasses(e.asOWLClass(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        case "subproperty-of":
        case "rdfs:subPropertyOf":
          if (e.isOWLAnnotationProperty()) {
            List<String> supers =
                EntitySearcher.getSuperProperties(e.asOWLAnnotationProperty(), ontology)
                    .stream()
                    .map(provider::getShortForm)
                    .collect(Collectors.toList());
            cellMap.put(col, String.join("|", supers));
          } else if (e.isOWLDataProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getSuperProperties(e.asOWLDataProperty(), ontology),
                    provider,
                    excludeAnonymous));
          } else if (e.isOWLObjectProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getSuperProperties(e.asOWLObjectProperty(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        case "equivalent-classes":
        case "owl:equivalentClass":
          if (e.isOWLClass()) {
            cellMap.put(
                col,
                classExpressionsToString(
                    EntitySearcher.getEquivalentClasses(e.asOWLClass(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        case "equivalent-properties":
        case "owl:equivalentProperty":
          if (e.isOWLAnnotationProperty()) {
            List<String> eqs =
                EntitySearcher.getEquivalentProperties(e.asOWLAnnotationProperty(), ontology)
                    .stream()
                    .map(provider::getShortForm)
                    .collect(Collectors.toList());
            cellMap.put(col, String.join("|", eqs));
          } else if (e.isOWLDataProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getEquivalentProperties(e.asOWLDataProperty(), ontology),
                    provider,
                    excludeAnonymous));
          } else if (e.isOWLObjectProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getEquivalentProperties(e.asOWLObjectProperty(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        case "disjoints":
        case "owl:disjointWith":
          if (e.isOWLClass()) {
            cellMap.put(
                col,
                classExpressionsToString(
                    EntitySearcher.getDisjointClasses(e.asOWLClass(), ontology),
                    provider,
                    excludeAnonymous));
          } else if (e.isOWLAnnotationProperty()) {
            List<String> eqs =
                EntitySearcher.getDisjointProperties(e.asOWLAnnotationProperty(), ontology)
                    .stream()
                    .map(provider::getShortForm)
                    .collect(Collectors.toList());
            cellMap.put(col, String.join("|", eqs));
          } else if (e.isOWLDataProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getDisjointProperties(e.asOWLDataProperty(), ontology),
                    provider,
                    excludeAnonymous));
          } else if (e.isOWLObjectProperty()) {
            cellMap.put(
                col,
                propertyExpressionsToString(
                    EntitySearcher.getDisjointProperties(e.asOWLObjectProperty(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        case "types":
        case "rdf:type":
          if (e.isOWLNamedIndividual()) {
            cellMap.put(
                col,
                classExpressionsToString(
                    EntitySearcher.getTypes(e.asOWLNamedIndividual(), ontology),
                    provider,
                    excludeAnonymous));
          }
          break;
        default:
          OWLAnnotationProperty ap = checker.getOWLAnnotationProperty(col, false);
          if (ap != null) {
            cellMap.put(col, getPropertyValue(ontology, e, ap));
            continue;
          }
          OWLDataProperty dp = checker.getOWLDataProperty(col);
          if (dp != null) {
            cellMap.put(col, getPropertyValue(ontology, provider, e, dp, excludeAnonymous));
            continue;
          }
          OWLObjectProperty op = checker.getOWLObjectProperty(col);
          if (op != null) {
            cellMap.put(col, getPropertyValue(ontology, provider, e, op, excludeAnonymous));
            continue;
          }
          throw new Exception(String.format(invalidColumnError, col));
      }
    }
    return cellMap;
  }

  /**
   * Given an OWLOntology and a boolean indicating if ONLY classes should be included, return a set
   * of all entities in the ontology.
   *
   * @param ontology OWLOntology to get entities from
   * @param options map of export options
   * @return set of OWLEntities in the ontology
   */
  private static Set<OWLEntity> getEntities(OWLOntology ontology, Map<String, String> options) {
    // Determine what types of entities to include
    boolean excludeClasses = OptionsHelper.optionIsTrue(options, "exclude-classes");
    boolean excludeProperties = OptionsHelper.optionIsTrue(options, "exclude-properties");
    boolean excludeIndividuals = OptionsHelper.optionIsTrue(options, "exclude-individuals");
    if (excludeClasses && excludeProperties && excludeIndividuals) {
      // If all three are true, nothing to include
      throw new IllegalArgumentException(excludeAllError);
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
      if (e.isOWLClass() && excludeClasses) {
        continue;
      } else if ((e.isOWLObjectProperty() || e.isOWLDataProperty() || e.isOWLAnnotationProperty())
          && excludeProperties) {
        continue;
      } else if (e.isOWLNamedIndividual() && excludeIndividuals) {
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
  private static String getPropertyValue(
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
    if (values.isEmpty()) {
      return null;
    }
    return String.join("|", values);
  }

  /**
   * Given an OWL Ontology, an OWL Entity, and an OWL Data Property, get all property values as one
   * string separated by the pipe character. If there are no property values, return null.
   *
   * @param ontology OWLOntology to get values from
   * @param provider ShortFormProvider for Manchester expressions
   * @param entity OWLEntity to get relations of
   * @param dp OWLDataProperty to get the value(s) of
   * @param excludeAnonymous if true, do not include anonymous class expressions
   * @return String of values or null
   */
  private static String getPropertyValue(
      OWLOntology ontology,
      ShortFormProvider provider,
      OWLEntity entity,
      OWLDataProperty dp,
      boolean excludeAnonymous) {
    if (entity.isOWLNamedIndividual()) {
      OWLNamedIndividual i = entity.asOWLNamedIndividual();
      Collection<OWLLiteral> propVals = EntitySearcher.getDataPropertyValues(i, dp, ontology);
      List<String> pvStrings =
          propVals
              .stream()
              .map(OWLLiteral::toString)
              .sorted(String::compareToIgnoreCase)
              .collect(Collectors.toList());
      return String.join("|", pvStrings);
    } else if (entity.isOWLClass()) {
      // Find super class expressions that use this property
      Set<OWLDatatype> vals = new HashSet<>();
      for (OWLClassExpression expr :
          EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        vals.addAll(getRestrictionFillers(expr.asConjunctSet(), dp, excludeAnonymous));
      }
      // Find equivalent class expressions that use this property
      for (OWLClassExpression expr :
          EntitySearcher.getEquivalentClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        vals.addAll(getRestrictionFillers(expr.asConjunctSet(), dp, excludeAnonymous));
      }
      if (vals.isEmpty()) {
        return null;
      }
      // Return values separated by pipes
      return vals.stream().map(provider::getShortForm).collect(Collectors.joining("|"));
    } else {
      return null;
    }
  }

  /**
   * Given an OWL Ontology, an OWL Entity, and an OWL Object Property, get all property values as
   * one string separated by the pipe character. Use labels when possible. If there are no property
   * values, return null.
   *
   * @param ontology OWLOntology to get values from
   * @param provider ShortFormProvider for Manchester expressions
   * @param entity OWLEntity to get annotations on
   * @param op OWLObjectProperty to get the value(s) of
   * @param excludeAnonymous if true, do not include anonymous class expressions
   * @return String of values or null
   */
  private static String getPropertyValue(
      OWLOntology ontology,
      ShortFormProvider provider,
      OWLEntity entity,
      OWLObjectProperty op,
      boolean excludeAnonymous) {
    if (entity.isOWLNamedIndividual()) {
      OWLNamedIndividual i = entity.asOWLNamedIndividual();
      Collection<OWLIndividual> propVals = EntitySearcher.getObjectPropertyValues(i, op, ontology);
      List<String> pvStrings =
          propVals
              .stream()
              .filter(OWLIndividual::isNamed)
              .map(pv -> provider.getShortForm(pv.asOWLNamedIndividual()))
              .sorted(String::compareToIgnoreCase)
              .collect(Collectors.toList());
      return String.join("|", pvStrings);
    } else if (entity.isOWLClass()) {
      // Find super class expressions that use this property
      Set<OWLClassExpression> exprs = new HashSet<>();
      for (OWLClassExpression expr :
          EntitySearcher.getSuperClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        exprs.addAll(getRestrictionFillers(expr.asConjunctSet(), op, excludeAnonymous));
      }
      // Find equivalent class expressions that use this property
      for (OWLClassExpression expr :
          EntitySearcher.getEquivalentClasses(entity.asOWLClass(), ontology)) {
        if (!expr.isAnonymous()) {
          continue;
        }
        // break down into conjuncts
        exprs.addAll(getRestrictionFillers(expr.asConjunctSet(), op, excludeAnonymous));
      }
      if (exprs.isEmpty()) {
        return null;
      }
      return classExpressionsToString(exprs, provider, false);
    } else {
      return null;
    }
  }

  /**
   * Given a set of OWL class expressions and a data property, get the fillers of the restrictions
   * (SOME or ALL) as datatypes and determine if the data property used in the original expression
   * matches the provided data property. If so, add the filler to the set of datatypes to return.
   *
   * @param exprs set of OWLClassExpressions to check
   * @param dp OWLDataProperty to look for
   * @param excludeAnonymous if true, exclude anonymous data ranges
   * @return set of OWLDatatype fillers that are 'values' of the data property
   */
  private static Set<OWLDatatype> getRestrictionFillers(
      Set<OWLClassExpression> exprs, OWLDataProperty dp, boolean excludeAnonymous) {
    Set<OWLDatatype> fillers = new HashSet<>();
    for (OWLClassExpression ce : exprs) {
      ClassExpressionType t = ce.getClassExpressionType();
      if (t == ClassExpressionType.DATA_ALL_VALUES_FROM
          || t == ClassExpressionType.DATA_SOME_VALUES_FROM) {
        // Convert to quantified restriction
        OWLQuantifiedDataRestriction qr = (OWLQuantifiedDataRestriction) ce;
        // Get the property used
        OWLDataPropertyExpression pe = qr.getProperty();
        // Get the data range
        OWLDataRange f = qr.getFiller();
        if (excludeAnonymous && f.isAnonymous()) {
          // Maybe skip anonymous
          continue;
        }
        if (!pe.isAnonymous()) {
          OWLDataProperty p = pe.asOWLDataProperty();
          if (p.getIRI() == dp.getIRI()) {
            if (f.isDatatype()) {
              fillers.add(f.asOWLDatatype());
            }
          }
        }
      }
    }
    return fillers;
  }

  /**
   * Given a set of OWL class expressions and an object property, get the fillers of the
   * restrictions (SOME or ALL) as class expressions and determine if the object property used in
   * the original expression matches the provided object property. If so, add the filler to the set
   * of class expressions to return.
   *
   * @param exprs set of OWLClassExpressions to check
   * @param op OWLObjectProperty to look for
   * @param excludeAnonymous if true, exclude anonymous class expressions
   * @return set of OWLClassExpression fillers that are 'values' of the object property
   */
  private static Set<OWLClassExpression> getRestrictionFillers(
      Set<OWLClassExpression> exprs, OWLObjectProperty op, boolean excludeAnonymous) {
    Set<OWLClassExpression> fillers = new HashSet<>();
    for (OWLClassExpression ce : exprs) {
      ClassExpressionType t = ce.getClassExpressionType();
      if (t == ClassExpressionType.OBJECT_ALL_VALUES_FROM
          || t == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
        // Convert to quantified restriction
        OWLQuantifiedObjectRestriction qr = (OWLQuantifiedObjectRestriction) ce;
        // Get the property used
        OWLObjectPropertyExpression pe = qr.getProperty();
        // Get the class expression
        OWLClassExpression f = qr.getFiller();
        if (excludeAnonymous && f.isAnonymous()) {
          continue;
        }
        if (!pe.isAnonymous()) {
          OWLObjectProperty p = pe.asOWLObjectProperty();
          if (p.getIRI() == op.getIRI()) {
            fillers.add(f);
          }
        }
      }
    }
    return fillers;
  }

  /**
   * Given a label function, a collection of property expressions, and a boolean indicating to
   * include anonymous expressions, return the parent properties as a string. Multiple values are
   * separated by the pipe character. Use labels when possible. Return null if no values exist.
   *
   * @param props Set of property expressions to convert to string
   * @param provider ShortFormProvider for Manchester expressions and short forms
   * @param excludeAnonymous if true, exclude anonymous expressions
   * @return String of property expressions or null
   */
  private static String propertyExpressionsToString(
      Collection<?> props, ShortFormProvider provider, boolean excludeAnonymous) {
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
        String manString = renderManchester(expr, provider);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLObjectProperty op = expr.asOWLObjectProperty();
        strings.add(provider.getShortForm(op));
      }
    }
    // Maybe process data property expressions
    for (OWLDataPropertyExpression expr : dpes) {
      if (expr.isAnonymous() && !excludeAnonymous) {
        String manString = renderManchester(expr, provider);
        strings.add(manString);
      } else if (!expr.isAnonymous()) {
        OWLDataProperty dp = expr.asOWLDataProperty();
        strings.add(provider.getShortForm(dp));
      }
    }
    // Sort alphabetically
    List<String> sorted =
        strings.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());

    // Return split by pipe character
    return String.join("|", sorted);
  }

  /**
   * Given an OWL Object and a short form provider for labels, return the Manchester representation
   * of that object.
   *
   * @param o OWLObject to render in Manchester
   * @param p ShortFormProvider to render short forms
   * @return Manchester string
   */
  private static String renderManchester(OWLObject o, ShortFormProvider p) {
    StringWriter sw = new StringWriter();
    ManchesterOWLSyntaxObjectRenderer r = new ManchesterOWLSyntaxObjectRenderer(sw, p);
    o.accept(r);
    return sw.toString();
  }
}
