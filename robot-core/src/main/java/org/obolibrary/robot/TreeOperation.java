package org.obolibrary.robot;

import com.google.common.collect.Lists;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geneontology.owl.differ.ManchesterSyntaxOWLObjectRenderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a> */
public class TreeOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(QuotedEntityChecker.class);

  // Error namespace
  private static final String NS = "tree#";

  // Error messages
  private static final String markdownPatterError =
      NS + "MARKDOWN PATTERN ERROR --markdown-pattern must include %s";
  private static final String missingTreePath =
      NS + "MISSING TREE PATH a --tree output path is required";
  private static final String unknownFormatError =
      NS + "UNKNOWN FORMAT ERROR output format '%s' is not one of: HTML, MD, or JSON";

  // Shared data factory
  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  // Default text pattern for markdown nodes
  // TODO - rename TEXT to LABEL
  private static final String MD_PATTERN = "<{ID}> {LABEL}";

  // Class variables
  private IOHelper ioHelper;
  private OWLOntology ontology;
  private QuotedEntityChecker checker;
  private ManchesterSyntaxOWLObjectRenderer r;

  // Tree browser
  private JsonArray tree = new JsonArray();
  // Class attributes
  private JsonObject attributes = new JsonObject();

  // Label for owl:deprecated
  private String owlDeprecated;

  // Important annotation properties
  private final OWLAnnotationProperty OWL_DEPRECATED = dataFactory.getOWLDeprecated();

  private List<OWLAnnotationProperty> orderedAnnotations;

  /**
   * Get the default template options.
   *
   * @return map of template options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("format", null);
    options.put("tree", null);
    options.put("markdown-pattern", MD_PATTERN);
    return options;
  }

  public TreeOperation(IOHelper ioHelper, OWLOntology ontology) {
    this.ioHelper = ioHelper;
    this.ontology = ontology;

    // Checker to get labels/objects
    checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ontology);

    // Set the label for owl:deprecated
    owlDeprecated =
        checker.getLabel(OWL_DEPRECATED.getIRI(), OWL_DEPRECATED.getIRI().getShortForm());

    // Expression renderer
    r = new ManchesterSyntaxOWLObjectRenderer();
    r.setShortFormProvider(ioHelper.getPrefixManager());

    // TODO - allow custom ordering
    setOrderedAnnotations();
  }

  private void setOrderedAnnotations() {
    this.orderedAnnotations =
        Lists.newArrayList(
            dataFactory.getRDFSLabel(),
            dataFactory.getOWLAnnotationProperty(IRI.create("http://iedb.org/epitope-link")),
            dataFactory.getOWLAnnotationProperty(IRI.create("http://iedb.org/has-descendants")),
            dataFactory.getOWLAnnotationProperty(
                IRI.create("http://purl.obolibrary.org/obo/IAO_0000115")));
  }

  public void buildTree(
      Set<IRI> upperTerms, Set<IRI> annotationProperties, Map<String, String> options)
      throws Exception {
    if (options == null) {
      options = getDefaultOptions();
    }

    // An output is required
    String outputPath = OptionsHelper.getOption(options, "tree");
    if (outputPath == null) {
      throw new Exception(missingTreePath);
    }

    // If AP is null or empty, add all annotation properties
    if (annotationProperties == null || annotationProperties.isEmpty()) {
      annotationProperties = new HashSet<>();
      for (OWLAnnotationProperty ap : ontology.getAnnotationPropertiesInSignature()) {
        annotationProperties.add(ap.getIRI());
      }
    }

    // Get the JSON array for the data
    parseOntology(annotationProperties, upperTerms);

    // Get or guess the format
    String format = OptionsHelper.getOption(options, "format");
    if (format == null) {
      format = FilenameUtils.getExtension(outputPath);
    }
    switch (format.toLowerCase()) {
      case "json":
        writeJSON(outputPath);
        break;
      case "html":
        writeHTML(outputPath);
        break;
      case "md":
        String markdownPattern = OptionsHelper.getOption(options, "markdown-pattern", MD_PATTERN);
        writeMarkdown(markdownPattern, outputPath);
        break;
      default:
        throw new Exception(String.format(unknownFormatError, format));
    }
  }

  private void parseOntology(Set<IRI> annotationProperties, Set<IRI> upperTerms) {
    // ----------- Handle ontology annotations -----------
    tree.add(parseOntologyHeader());

    // Create annotation property map
    // These are OWLAnnotationProperties to their string labels (or short form IRIs)
    // The OWL AP is used to get the values
    // The string is used for the attributes JSON Object
    Map<OWLAnnotationProperty, String> annPropMap = new HashMap<>();
    for (IRI iri : annotationProperties) {
      OWLAnnotationProperty ap = dataFactory.getOWLAnnotationProperty(iri);
      String label = checker.getLabel(iri, iri.getShortForm());
      annPropMap.put(ap, label);
    }

    // Check upper terms
    if (upperTerms == null) {
      // If not provided, give an empty set
      upperTerms = new HashSet<>();
    }

    // ----------- Handle classes -----------

    Set<OWLClass> upperClasses = new HashSet<>();
    for (IRI iri : upperTerms) {
      if (ontology.containsClassInSignature(iri)) {
        upperClasses.add(dataFactory.getOWLClass(iri));
      }
    }

    // Get all top classes if upper classes is empty
    if (upperClasses.isEmpty()) {
      upperClasses = getTopClasses();
    }

    if (!upperClasses.isEmpty()) {
      // Sort by label
      List<OWLClass> orderedClasses = new ArrayList<>(upperClasses);
      orderedClasses.sort(new SortByLabel());
      orderedClasses.sort(new SortByOther());
      // Then sort by 'obsolete'
      orderedClasses.sort(new SortByStatus());

      JsonArray classes = new JsonArray();
      for (OWLClass cls : orderedClasses) {
        // Add class to tree details and attributes
        classes.add(parseClass(annPropMap, cls));
      }

      // Put all data properties under one node
      JsonObject topClass = new JsonObject();
      topClass.addProperty("id", dataFactory.getOWLThing().getIRI().toString());
      topClass.addProperty("text", "Classes");
      topClass.add("children", classes);
      tree.add(topClass);
    }

    // ----------- Handle individuals by type -----------

    Set<OWLNamedIndividual> namedIndividuals = ontology.getIndividualsInSignature();
    if (!namedIndividuals.isEmpty()) {
      List<String> typeLabels = new ArrayList<>();
      Map<String, OWLClass> labelToClass = new HashMap<>();
      for (OWLNamedIndividual ni : namedIndividuals) {
        // Only get named class types and add to type set
        for (OWLClassExpression expr : EntitySearcher.getTypes(ni, ontology)) {
          if (expr.isAnonymous()) {
            continue;
          }
          OWLClass cls = expr.asOWLClass();
          IRI clsIRI = cls.getIRI();
          String label = checker.getLabel(clsIRI, clsIRI.getShortForm());
          typeLabels.add(label);
          labelToClass.put(label, cls);
        }
      }
      // Make sure labels are distinct and ordered
      // These are either labels or full IRIs so that they can be converted back to
      List<String> sortedTypeLabels =
          typeLabels
              .stream()
              .distinct()
              .sorted(new SortByLabel())
              .sorted(new SortByOther())
              .sorted(new SortByStatus())
              .collect(Collectors.toList());

      JsonArray individualsByType = new JsonArray();
      for (String typeLabel : sortedTypeLabels) {
        OWLClass t = labelToClass.getOrDefault(typeLabel, null);
        if (t == null) {
          logger.error(String.format("Cannot find class: %s", typeLabel));
          continue;
        }

        IRI typeIRI = t.getIRI();
        JsonObject typeClass = new JsonObject();

        typeClass.addProperty("id", typeIRI.toString());
        typeClass.addProperty("text", typeLabel);
        typeClass.add("children", getIndiviudalArray(t, annPropMap));
        individualsByType.add(typeClass);
      }

      // Top-level entity
      JsonObject topIndividual = new JsonObject();
      topIndividual.addProperty("id", "http://www.w3.org/2002/07/owl#NamedIndividual");
      topIndividual.addProperty("text", "Individuals by Type");
      topIndividual.add("children", individualsByType);
      tree.add(topIndividual);
    }

    // ----------- Handle annotation properties -----------

    Set<OWLAnnotationProperty> upperAnnotationProperties = new HashSet<>();
    for (IRI iri : upperTerms) {
      if (ontology.containsAnnotationPropertyInSignature(iri)) {
        upperAnnotationProperties.add(dataFactory.getOWLAnnotationProperty(iri));
      }
    }

    if (upperAnnotationProperties.isEmpty()) {
      upperAnnotationProperties = getTopAnnotationProperties();
    }

    if (!upperAnnotationProperties.isEmpty()) {
      // Sort by label
      Map<String, OWLAnnotationProperty> labelToAnnProperty = new HashMap<>();
      List<String> annPropertyLabels = new ArrayList<>();
      for (OWLAnnotationProperty upper : upperAnnotationProperties) {
        String label = checker.getLabel(upper.getIRI(), upper.getIRI().getShortForm());
        annPropertyLabels.add(label);
        labelToAnnProperty.put(label, upper);
      }
      annPropertyLabels.sort(new SortByLabel());
      annPropertyLabels.sort(new SortByOther());
      annPropertyLabels.sort(new SortByStatus());

      JsonArray annProps = new JsonArray();
      for (String label : annPropertyLabels) {
        OWLAnnotationProperty property = labelToAnnProperty.get(label);
        // Add class to tree details and attributes
        annProps.add(parseAnnotationProperty(annPropMap, property));
      }

      // Put all data properties under one node
      JsonObject topAnnProperty = new JsonObject();
      topAnnProperty.addProperty("id", "http://www.w3.org/2002/07/owl#AnnotationProperty");
      topAnnProperty.addProperty("text", "Annotation Properties");
      topAnnProperty.add("children", annProps);
      tree.add(topAnnProperty);
    }

    // ----------- Handle data properties -----------

    Set<OWLDataProperty> upperDataProperties = new HashSet<>();
    for (IRI iri : upperTerms) {
      if (ontology.containsDataPropertyInSignature(iri)) {
        upperDataProperties.add(dataFactory.getOWLDataProperty(iri));
      }
    }

    if (upperDataProperties.isEmpty()) {
      upperDataProperties = getTopDataProperties();
    }

    if (!upperDataProperties.isEmpty()) {
      // Sort by label
      Map<String, OWLDataProperty> labelToDataProperty = new HashMap<>();
      List<String> dataPropertyLabels = new ArrayList<>();
      for (OWLDataProperty upper : upperDataProperties) {
        String label = checker.getLabel(upper.getIRI(), upper.getIRI().getShortForm());
        dataPropertyLabels.add(label);
        labelToDataProperty.put(label, upper);
      }
      dataPropertyLabels.sort(new SortByLabel());
      dataPropertyLabels.sort(new SortByOther());
      dataPropertyLabels.sort(new SortByStatus());

      JsonArray dataProps = new JsonArray();
      for (String label : dataPropertyLabels) {
        OWLDataProperty property = labelToDataProperty.get(label);
        // Add data property to tree details and attributes
        dataProps.add(parseDataProperty(annPropMap, property));
      }

      // Put all data properties under one node
      JsonObject topDataProperty = new JsonObject();
      topDataProperty.addProperty("id", dataFactory.getOWLTopDataProperty().getIRI().toString());
      topDataProperty.addProperty("text", "Data Properties");
      topDataProperty.add("children", dataProps);
      tree.add(topDataProperty);
    }

    // ----------- Handle object properties -----------

    Set<OWLObjectProperty> upperObjectProperties = new HashSet<>();
    for (IRI iri : upperTerms) {
      if (ontology.containsObjectPropertyInSignature(iri)) {
        upperObjectProperties.add(dataFactory.getOWLObjectProperty(iri));
      }
    }

    if (upperObjectProperties.isEmpty()) {
      upperObjectProperties = getTopObjectProperties();
    }

    if (!upperObjectProperties.isEmpty()) {
      // Sort by label
      Map<String, OWLObjectProperty> labelToObjProperty = new HashMap<>();
      List<String> objPropertyLabels = new ArrayList<>();
      for (OWLObjectProperty upper : upperObjectProperties) {
        String label = checker.getLabel(upper.getIRI(), upper.getIRI().getShortForm());
        objPropertyLabels.add(label);
        labelToObjProperty.put(label, upper);
      }
      objPropertyLabels.sort(new SortByLabel());
      objPropertyLabels.sort(new SortByOther());
      objPropertyLabels.sort(new SortByStatus());

      JsonArray objProps = new JsonArray();
      for (String label : objPropertyLabels) {
        OWLObjectProperty property = labelToObjProperty.get(label);
        // Add data property to tree details and attributes
        objProps.add(parseObjectProperty(annPropMap, property));
      }

      // Put all object properties under one node
      JsonObject topObjectProperty = new JsonObject();
      topObjectProperty.addProperty(
          "id", dataFactory.getOWLTopObjectProperty().getIRI().toString());
      topObjectProperty.addProperty("text", "Object Properties");
      topObjectProperty.add("children", objProps);
      tree.add(topObjectProperty);
    }

    // ----------- Handle datatypes -----------

    // Sort by label
    Set<OWLDatatype> allDatatypes = ontology.getDatatypesInSignature();

    if (!allDatatypes.isEmpty()) {
      // Sort by label
      Map<String, OWLDatatype> labelToDatatype = new HashMap<>();
      List<String> datatypeLabels = new ArrayList<>();
      for (OWLDatatype dt : allDatatypes) {
        String label = checker.getLabel(dt.getIRI(), dt.getIRI().getShortForm());
        datatypeLabels.add(label);
        labelToDatatype.put(label, dt);
      }
      datatypeLabels.sort(new SortByLabel());
      datatypeLabels.sort(new SortByOther());
      datatypeLabels.sort(new SortByStatus());

      JsonArray datatypes = new JsonArray();
      for (String label : datatypeLabels) {
        OWLDatatype dt = labelToDatatype.get(label);
        // Add data property to tree details and attributes
        datatypes.add(parseDatatype(annPropMap, dt));
      }

      // Put all datatypes under one node
      JsonObject topDatatype = new JsonObject();
      topDatatype.addProperty("id", dataFactory.getTopDatatype().getIRI().toString());
      topDatatype.addProperty("text", "Datatypes");
      topDatatype.add("children", datatypes);
      tree.add(topDatatype);
    }
  }

  private JsonObject parseOntologyHeader() {
    // Basic details
    JsonObject ontologyObj = new JsonObject();
    IRI ontologyIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    String strIRI;
    if (ontologyIRI != null) {
      strIRI = ontologyIRI.toString();
    } else {
      strIRI = "";
    }

    // Used to store imports and annotations (other details)
    JsonObject ontologyAttrs = new JsonObject();

    // Get the imports
    Collection<IRI> importIRIs = ontology.getDirectImportsDocuments();
    if (importIRIs.size() > 1) {
      JsonArray imports = new JsonArray();
      for (IRI importIRI : importIRIs) {
        imports.add(
            String.format(
                "<a href=\"%s\" target=\"_blank\">%s</a>",
                importIRI.toString(), importIRI.toString()));
      }
      ontologyAttrs.add("Imports", imports);
    } else if (importIRIs.size() == 1) {
      String importIRI = importIRIs.iterator().next().toString();
      ontologyAttrs.addProperty("Imports", importIRI);
    }

    // Get the ontology annotations
    Map<String, List<String>> annotations = new HashMap<>();
    for (OWLAnnotation ann : ontology.getAnnotations()) {
      // Get the property and its label
      OWLAnnotationProperty ap = ann.getProperty();
      String apLabel = checker.getLabel(ap.getIRI(), ap.getIRI().getShortForm());

      // Get the value of the annotation
      OWLAnnotationValue value = ann.getValue();
      String strValue = null;
      if (value.isLiteral()) {
        OWLLiteral lit = value.asLiteral().orNull();
        if (lit != null) {
          strValue = lit.getLiteral();
        }
      } else if (value.isIRI()) {
        IRI iri = (IRI) value;
        String quotedLabel =
            checker.getLabel(iri, iri.toString().replace("<", "").replace(">", ""));
        String label = quotedLabel.replace("'", "");
        strValue =
            String.format("<a href=\"javascript:clickSearch('%s')\">%s</a>", label, quotedLabel);
      }

      // Only add if the value is not null
      // We iterate through here to group together mulitple annotations with the same property
      if (strValue != null) {
        List<String> values = annotations.getOrDefault(apLabel, new ArrayList<>());
        values.add(strValue);
        annotations.put(apLabel, values);
      }
    }

    // Add the annotation properties and their values to the attribute JSON object
    for (Map.Entry<String, List<String>> annotation : annotations.entrySet()) {
      String property = annotation.getKey();
      List<String> values = annotation.getValue();

      if (values.size() > 0) {
        JsonArray valueArray = new JsonArray();
        for (String v : values) {
          valueArray.add(v);
        }
        ontologyAttrs.add(property, valueArray);
      }
    }

    // Add the ontology attribute object to all attributes
    attributes.add(strIRI, ontologyAttrs);

    // Add basic details to this JSON object
    ontologyObj.addProperty("id", strIRI);
    ontologyObj.addProperty("text", strIRI);
    return ontologyObj;
  }

  private Set<OWLClass> getTopClasses() {
    Set<OWLClass> topClasses = new HashSet<>();
    for (OWLClass c : ontology.getClassesInSignature()) {
      if (c.isBuiltIn()) {
        continue;
      }
      boolean hasNamedSuperClass = false;
      for (OWLClassExpression expr : EntitySearcher.getSuperClasses(c, ontology)) {
        if (!expr.isAnonymous() && !expr.asOWLClass().isOWLThing()) {
          hasNamedSuperClass = true;
        }
      }
      if (!hasNamedSuperClass) {
        topClasses.add(c);
      }
    }
    return topClasses;
  }

  private Set<OWLAnnotationProperty> getTopAnnotationProperties() {
    Set<OWLAnnotationProperty> topProperties = new HashSet<>();
    for (OWLAnnotationProperty p : ontology.getAnnotationPropertiesInSignature()) {
      if (p.isBuiltIn()) {
        continue;
      }
      boolean hasNamedSuperProperty = false;
      for (OWLAnnotationProperty ap : EntitySearcher.getSuperProperties(p, ontology)) {
        if (!ap.isBuiltIn()) {
          hasNamedSuperProperty = true;
        }
      }
      if (!hasNamedSuperProperty) {
        topProperties.add(p);
      }
    }
    return topProperties;
  }

  private Set<OWLDataProperty> getTopDataProperties() {
    Set<OWLDataProperty> topProperties = new HashSet<>();
    for (OWLDataProperty p : ontology.getDataPropertiesInSignature()) {
      if (p.isBuiltIn()) {
        continue;
      }
      boolean hasNamedSuperProperty = false;
      for (OWLDataPropertyExpression expr : EntitySearcher.getSuperProperties(p, ontology)) {
        if (!expr.isAnonymous() && !expr.asOWLDataProperty().isOWLTopDataProperty()) {
          hasNamedSuperProperty = true;
        }
      }
      if (!hasNamedSuperProperty) {
        topProperties.add(p);
      }
    }
    return topProperties;
  }

  private Set<OWLObjectProperty> getTopObjectProperties() {
    Set<OWLObjectProperty> topProperties = new HashSet<>();
    for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
      if (p.isBuiltIn()) {
        continue;
      }
      boolean hasNamedSuperProperty = false;
      for (OWLObjectPropertyExpression expr : EntitySearcher.getSuperProperties(p, ontology)) {
        if (!expr.isAnonymous() && !expr.asOWLObjectProperty().isOWLTopObjectProperty()) {
          hasNamedSuperProperty = true;
        }
      }
      if (!hasNamedSuperProperty) {
        topProperties.add(p);
      }
    }
    return topProperties;
  }

  private JsonObject parseClass(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLClass cls) {
    // Tree details are text (label) and children
    JsonObject treeDetails = getBasicDetails(cls);

    // Attributes are everything else (IRI, annotations, etc...)
    JsonObject attrs = getAttributes(annotationProperties, cls);

    // Get superclasses (named and anonymous)
    Set<OWLObject> renderValues = new HashSet<>(EntitySearcher.getSuperClasses(cls, ontology));
    renderValues.remove(dataFactory.getOWLThing());
    if (!renderValues.isEmpty()) {
      Set<String> renderedValues = renderExpressions(renderValues);
      // Add to JSON array
      JsonArray superClasses = new JsonArray();
      // Order alphabetically and add to array
      renderedValues
          .stream()
          .sorted(new SortByLabel())
          .sorted(new SortByOther())
          .sorted(new SortByStatus())
          .iterator()
          .forEachRemaining(superClasses::add);
      attrs.add("SubClass Of", superClasses);
    }

    // Get equivalent classes
    renderValues = new HashSet<>(EntitySearcher.getEquivalentClasses(cls, ontology));
    // Make sure to remove this class
    renderValues.remove(cls);
    if (!renderValues.isEmpty()) {
      Set<String> renderedValues = renderExpressions(renderValues);
      // Add to JSON array
      JsonArray equivalentClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(equivalentClasses::add);
      attrs.add("Equivalent Classes", equivalentClasses);
    }

    // Get disjoint classes
    renderValues = new HashSet<>(EntitySearcher.getDisjointClasses(cls, ontology));
    // Make sure to remove this class
    renderValues.remove(cls);
    if (!renderValues.isEmpty()) {
      Set<String> renderedValues = renderExpressions(renderValues);
      // Add to JSON array
      JsonArray disjointClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(disjointClasses::add);
      attrs.add("Disjoint Classes", disjointClasses);
    }

    // Maybe get children nodes (recursively)
    JsonArray children = getSubClassArray(cls, annotationProperties);
    if (children != null) {
      treeDetails.add("children", children);
    }

    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject parseIndividual(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLNamedIndividual individual) {
    JsonObject treeDetails = getBasicDetails(individual);

    // Attributes are everything else (IRI, annotations, etc...)
    JsonObject attrs = getAttributes(annotationProperties, individual);

    // TODO - other logic

    // Get types
    Set<OWLObject> renderValues = new HashSet<>(EntitySearcher.getTypes(individual, ontology));
    Set<String> renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray superProperties = new JsonArray();
      renderedValues
          .stream()
          .sorted(new SortByLabel())
          .sorted(new SortByOther())
          .sorted(new SortByStatus())
          .iterator()
          .forEachRemaining(superProperties::add);
      attrs.add("Type", superProperties);
    }

    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject parseAnnotationProperty(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLAnnotationProperty property) {
    // Tree details are ID, text (label), and children
    JsonObject treeDetails = getBasicDetails(property);

    // Attributes are everything else (IRI, annotations, etc...)
    JsonObject attrs = getAttributes(annotationProperties, property);

    // Get superproperties
    Set<OWLObject> renderValues =
        new HashSet<>(EntitySearcher.getSuperProperties(property, ontology));
    Set<String> renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray superProperties = new JsonArray();
      renderedValues
          .stream()
          .sorted(new SortByLabel())
          .sorted(new SortByOther())
          .sorted(new SortByStatus())
          .iterator()
          .forEachRemaining(superProperties::add);
      attrs.add("SubProperty Of", superProperties);
    }

    // Maybe get children nodes (recursively)
    JsonArray children = getSubPropertyArray(property, annotationProperties);
    if (children != null) {
      treeDetails.add("children", children);
    }

    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject parseDataProperty(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLDataProperty property) {
    // Tree details are ID, text (label), and children
    JsonObject treeDetails = getBasicDetails(property);

    // Attributes are everything else (IRI, annotations, etc...)
    JsonObject attrs = getAttributes(annotationProperties, property);

    // Get superproperties
    Set<OWLObject> renderValues =
        new HashSet<>(EntitySearcher.getSuperProperties(property, ontology));
    Set<String> renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray superProperties = new JsonArray();
      renderedValues
          .stream()
          .sorted(new SortByLabel())
          .sorted(new SortByOther())
          .sorted(new SortByStatus())
          .iterator()
          .forEachRemaining(superProperties::add);
      attrs.add("SubProperty Of", superProperties);
    }

    // Get equivalent properties
    renderValues = new HashSet<>(EntitySearcher.getEquivalentProperties(property, ontology));
    renderValues.remove(property);
    renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray equivalentClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(equivalentClasses::add);
      attrs.add("Equivalent Properties", equivalentClasses);
    }

    // Get disjoint properties
    renderValues = new HashSet<>(EntitySearcher.getDisjointProperties(property, ontology));
    renderValues.remove(property);
    renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray disjointClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(disjointClasses::add);
      attrs.add("Disjoint Properties", disjointClasses);
    }

    // Maybe get children nodes (recursively)
    JsonArray children = getSubPropertyArray(property, annotationProperties);
    if (children != null) {
      treeDetails.add("children", children);
    }

    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject parseObjectProperty(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLObjectProperty property) {
    // Tree details are ID, text (label), and children
    JsonObject treeDetails = getBasicDetails(property);

    // Attributes are everything else (IRI, annotations, etc...)
    JsonObject attrs = getAttributes(annotationProperties, property);

    // Get superproperties
    Set<OWLObject> renderValues =
        new HashSet<>(EntitySearcher.getSuperProperties(property, ontology));
    Set<String> renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray superProperties = new JsonArray();
      renderedValues
          .stream()
          .sorted(new SortByLabel())
          .sorted(new SortByOther())
          .sorted(new SortByStatus())
          .iterator()
          .forEachRemaining(superProperties::add);
      attrs.add("SubProperty Of", superProperties);
    }

    // Get equivalent properties
    renderValues = new HashSet<>(EntitySearcher.getEquivalentProperties(property, ontology));
    renderValues.remove(property);
    renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray equivalentClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(equivalentClasses::add);
      attrs.add("Equivalent Properties", equivalentClasses);
    }

    // Get disjoint properties
    renderValues = new HashSet<>(EntitySearcher.getDisjointProperties(property, ontology));
    renderValues.remove(property);
    renderedValues = renderExpressions(renderValues);
    if (!renderedValues.isEmpty()) {
      // Add to JSON array
      JsonArray disjointClasses = new JsonArray();
      renderedValues.iterator().forEachRemaining(disjointClasses::add);
      attrs.add("Disjoint Properties", disjointClasses);
    }

    // Maybe get children nodes (recursively)
    JsonArray children = getSubPropertyArray(property, annotationProperties);
    if (children != null) {
      treeDetails.add("children", children);
    }

    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject parseDatatype(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLDatatype datatype) {
    JsonObject treeDetails = getBasicDetails(datatype);
    JsonObject attrs = getAttributes(annotationProperties, datatype);
    // Only add to attributes once
    String label = treeDetails.get("text").getAsString();
    if (attributes.get(label) == null) {
      attributes.add(label, attrs);
    }

    return treeDetails;
  }

  private JsonObject getBasicDetails(OWLEntity e) {
    // Tree details are ID, text (label), and children
    JsonObject treeDetails = new JsonObject();

    // Get basic details (ID, display label)
    IRI iri = e.getIRI();
    String label = checker.getLabel(iri, iri.getShortForm().replace("<", "").replace(">", ""));

    // Add details for tree node
    treeDetails.addProperty("text", label);
    treeDetails.addProperty("id", label);

    return treeDetails;
  }

  private JsonObject getAttributes(
      Map<OWLAnnotationProperty, String> annotationProperties, OWLEntity e) {
    // Attributes are everything else (annotations, etc...)
    JsonObject attrs = new JsonObject();

    // Add the IRI to attributes
    // Always replace brackets in IRI
    String strIRI = e.getIRI().toString().replace("<", "").replace(">", "");
    attrs.addProperty("iri", strIRI);

    // Get ordered annotations
    for (OWLAnnotationProperty ap : orderedAnnotations) {
      if (ap.getIRI().toString().equals(dataFactory.getRDFSLabel().getIRI().toString())) {
        continue;
      }
      attrs.add(
          checker.getLabel(ap.getIRI(), ap.getIRI().getShortForm()), getAnnotationArray(e, ap));
    }

    // Get the unordered annotations, removing the ones we already got
    Collection<OWLAnnotationProperty> unorderedAnnotations = annotationProperties.keySet();
    unorderedAnnotations.removeAll(orderedAnnotations);
    for (OWLAnnotationProperty ap : unorderedAnnotations) {
      if (ap.getIRI().toString().equals(dataFactory.getRDFSLabel().getIRI().toString())) {
        continue;
      }
      attrs.add(annotationProperties.get(ap), getAnnotationArray(e, ap));
    }

    return attrs;
  }

  private JsonArray getAnnotationArray(OWLEntity entity, OWLAnnotationProperty ap) {
    JsonArray annArray = new JsonArray();
    for (OWLAnnotation a : EntitySearcher.getAnnotationObjects(entity, ontology, ap)) {
      OWLAnnotationValue val = a.getValue();
      if (val.isLiteral()) {
        OWLLiteral lit = val.asLiteral().orNull();
        if (lit != null) {
          annArray.add(lit.getLiteral());
        }
      } else if (val.isIRI()) {
        IRI iri = (IRI) val;
        String quotedLabel =
            checker.getLabel(iri, iri.toString().replace("<", "").replace(">", ""));
        String label = quotedLabel.replace("'", "");
        label =
            String.format("<a href=\"javascript:clickSearch('%s')\">%s</a>", label, quotedLabel);
        annArray.add(label);
      }
    }
    return annArray;
  }

  private JsonArray getSubClassArray(
      OWLClass cls, Map<OWLAnnotationProperty, String> annotationProperties) {
    JsonArray children = new JsonArray();

    // Order alphabetically (named only)
    List<OWLClassExpression> subClasses =
        EntitySearcher.getSubClasses(cls, ontology)
            .stream()
            .filter(expr -> !expr.isAnonymous())
            .distinct()
            .sorted(new SortByLabel())
            .sorted(new SortByOther())
            .sorted(new SortByStatus())
            .collect(Collectors.toList());

    // Parse and add to array
    int childCount = 0;
    for (OWLClassExpression child : subClasses) {
      children.add(parseClass(annotationProperties, child.asOWLClass()));
      childCount++;
    }

    if (childCount > 0) {
      return children;
    } else {
      return null;
    }
  }

  private JsonArray getIndiviudalArray(
      OWLClass cls, Map<OWLAnnotationProperty, String> annotationProperties) {
    JsonArray individuals = new JsonArray();

    List<OWLNamedIndividual> namedIndividuals = new ArrayList<>();
    for (OWLIndividual indiv : EntitySearcher.getIndividuals(cls, ontology)) {
      if (indiv.isAnonymous()) {
        // TODO - handle anon individuals
        continue;
      }
      namedIndividuals.add(indiv.asOWLNamedIndividual());
    }

    // Sort by label
    namedIndividuals.sort(new SortByLabel());
    namedIndividuals.sort(new SortByOther());
    namedIndividuals.sort(new SortByStatus());

    int indivCount = 0;
    for (OWLNamedIndividual ni : namedIndividuals) {
      if (cls.isOWLThing()) {
        // Special handling for OWL Thing to not duplicate unnecessarily
        // Get named types that are NOT OWL Thing
        List<OWLClassExpression> otherTypes =
            EntitySearcher.getTypes(ni, ontology)
                .stream()
                .filter(expr -> !expr.isAnonymous() && !expr.asOWLClass().isOWLThing())
                .collect(Collectors.toList());
        if (!otherTypes.isEmpty()) {
          // If there are other named classes than OWL Thing
          // Don't put this under Thing as well
          continue;
        }
      }
      indivCount++;
      individuals.add(parseIndividual(annotationProperties, ni));
    }
    if (indivCount > 0) {
      return individuals;
    } else {
      return null;
    }
  }

  private JsonArray getSubPropertyArray(
      OWLAnnotationProperty property, Map<OWLAnnotationProperty, String> annotationProperties) {
    JsonArray children = new JsonArray();

    // Order alphabetically
    List<OWLAnnotationProperty> subProperties =
        EntitySearcher.getSubProperties(property, ontology)
            .stream()
            .distinct()
            .sorted(new SortByLabel())
            .sorted(new SortByOther())
            .sorted(new SortByStatus())
            .collect(Collectors.toList());

    // Parse and add to the array
    int childCount = 0;
    for (OWLAnnotationProperty child : subProperties) {
      children.add(parseAnnotationProperty(annotationProperties, child));
      childCount++;
    }

    if (childCount > 0) {
      return children;
    } else {
      return null;
    }
  }

  private JsonArray getSubPropertyArray(
      OWLDataProperty property, Map<OWLAnnotationProperty, String> annotationProperties) {
    JsonArray children = new JsonArray();

    // Order alphabetically (named only)
    List<OWLDataPropertyExpression> subDataProperties =
        EntitySearcher.getSubProperties(property, ontology)
            .stream()
            .filter(expr -> !expr.isAnonymous())
            .distinct()
            .sorted(new SortByLabel())
            .sorted(new SortByOther())
            .sorted(new SortByStatus())
            .collect(Collectors.toList());

    // Parse and add to the array
    int childCount = 0;
    for (OWLDataPropertyExpression child : subDataProperties) {
      children.add(parseDataProperty(annotationProperties, child.asOWLDataProperty()));
      childCount++;
    }

    if (childCount > 0) {
      return children;
    } else {
      return null;
    }
  }

  private JsonArray getSubPropertyArray(
      OWLObjectProperty property, Map<OWLAnnotationProperty, String> annotationProperties) {
    JsonArray children = new JsonArray();

    // Order alphabetically (named only)
    List<OWLObjectPropertyExpression> subObjectProperties =
        EntitySearcher.getSubProperties(property, ontology)
            .stream()
            .filter(expr -> !expr.isAnonymous())
            .distinct()
            .sorted(new SortByLabel())
            .sorted(new SortByOther())
            .sorted(new SortByStatus())
            .collect(Collectors.toList());

    // Parse and add to the array
    int childCount = 0;
    for (OWLObjectPropertyExpression child : subObjectProperties) {
      children.add(parseObjectProperty(annotationProperties, child.asOWLObjectProperty()));
      childCount++;
    }

    if (childCount > 0) {
      return children;
    } else {
      return null;
    }
  }

  private boolean isObsolete(OWLEntity e) {
    for (OWLAnnotation ann :
        EntitySearcher.getAnnotationObjects(e, ontology, dataFactory.getOWLDeprecated())) {
      OWLAnnotationValue v = ann.getValue();
      if (!v.isLiteral()) {
        continue;
      }
      OWLLiteral lit = v.asLiteral().orNull();
      if (lit == null) {
        continue;
      }
      if (lit.isBoolean()) {
        return lit.parseBoolean();
      } else {
        String str = lit.getLiteral().trim().toLowerCase();
        return str.equals("true");
      }
    }
    return false;
  }

  private Set<String> renderExpressions(Set<OWLObject> values) {
    Set<String> renderedValues = new HashSet<>();
    for (OWLObject v : values) {
      // Render the value in Manchester Syntax
      String render = r.render(v);
      // Match term IDs
      Matcher m = Pattern.compile("([^\\s()]+[_:][^\\s()]+)").matcher(render);
      while (m.find()) {
        // Maybe replace term IDs with their labels
        String shortID = m.group(1);
        IRI iri = ioHelper.createIRI(shortID);
        if (iri != null) {
          // Short form used if label does not exist in ontology
          // Sometimes ends up with extra brackets
          String shortForm = iri.getShortForm().replace(">", "").replace("<", "");
          String label = checker.getLabel(iri, shortForm);
          String quotedLabel = label;

          if (label.contains(" ")) {
            // add quotes to anything with more than one word
            quotedLabel = "'" + label + "'";
          }
          // Escape single quotes
          label = label.replace("'", "\\'");

          // Click search function on click
          label =
              String.format("<a href=\"javascript:clickSearch('%s')\">%s</a>", label, quotedLabel);
          // Replace the IDs with the click to search links
          render = render.replace(shortID, label);
        }
      }
      renderedValues.add(render);
    }
    return renderedValues;
  }

  private void writeHTML(String outputPath) throws IOException {
    // JSON data
    Gson g = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    String data = g.toJson(tree);

    String attrs = g.toJson(attributes);

    // HTML with Inspire Tree JS
    String htmlTemplate = IOHelper.readResource("/inspire-tree.html");
    // Replace with the correct data
    String htmlOutput =
        htmlTemplate
            .replace("{DATA}", data)
            .replace("{ATTRIBUTES}", attrs)
            .replace("{OWL_DEPRECATED}", owlDeprecated);

    // Write the main HTML
    FileUtils.writeStringToFile(new File(outputPath), htmlOutput);
  }

  private void writeJSON(String outputPath) throws IOException {
    Gson g = new GsonBuilder().setPrettyPrinting().create();
    FileUtils.writeStringToFile(new File(outputPath), g.toJson(tree));
  }

  private void writeMarkdown(String markdownPattern, String outputPath) throws Exception {
    // Validate the MD pattern
    if (!markdownPattern.contains("{ID}") && !markdownPattern.contains("{LABEL}")) {
      throw new Exception(String.format(markdownPatterError, "{ID} and {LABEL}"));
    } else if (!markdownPattern.contains("{ID}")) {
      throw new Exception(String.format(markdownPatterError, "{ID}"));
    } else if (!markdownPattern.contains("{LABEL}")) {
      throw new Exception(String.format(markdownPatterError, "{LABEL}"));
    }
    List<String> lines = getMarkdown(tree, markdownPattern, 0);
    String md = String.join("\n", lines);
    FileUtils.writeStringToFile(new File(outputPath), md);
  }

  private static List<String> getMarkdown(JsonArray arr, String markdownPattern, int level) {
    List<String> lines = new ArrayList<>();
    for (JsonElement element : arr) {
      if (element.isJsonObject()) {
        JsonObject obj = element.getAsJsonObject();
        String id = obj.get("id").getAsString();
        String text = obj.get("text").getAsString();

        // Number of spaces is 2*level
        String spaces = String.join("", Collections.nCopies(level, "  "));
        // Substitute the markdown pattern
        String line = markdownPattern.replace("{ID}", id).replace("{LABEL}", text);
        lines.add(String.format("%s* %s", spaces, line));

        // Maybe add children
        JsonElement childrenElement = obj.get("children");
        if (childrenElement != null) {
          level++;
          JsonArray children = childrenElement.getAsJsonArray();
          lines.addAll(getMarkdown(children, markdownPattern, level));
        }
      }
    }
    return lines;
  }

  class SortByLabel implements Comparator<Object> {
    public int compareEntities(OWLEntity e1, OWLEntity e2) {
      String label1 = checker.getLabel(e1.getIRI(), e1.getIRI().getShortForm());
      String label2 = checker.getLabel(e2.getIRI(), e2.getIRI().getShortForm());
      return compareStrings(label1, label2);
    }

    public int compareStrings(String s1, String s2) {
      return s1.toLowerCase().compareTo(s2.toLowerCase());
    }

    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof OWLEntity && o2 instanceof OWLEntity) {
        return compareEntities((OWLEntity) o1, (OWLEntity) o2);
      } else if (o1 instanceof String && o2 instanceof String) {
        return compareStrings((String) o1, (String) o2);
      }
      return 0;
    }
  }

  class SortByOther implements Comparator<Object> {
    public int compareEntities(OWLEntity e1, OWLEntity e2) {
      boolean b1 = checker.getLabel(e1.getIRI(), e1.getIRI().getShortForm()).startsWith("other");
      boolean b2 = checker.getLabel(e2.getIRI(), e2.getIRI().getShortForm()).startsWith("other");
      return Boolean.compare(b1, b2);
    }

    public int compareStrings(String s1, String s2) {
      return s1.toLowerCase().compareTo(s2.toLowerCase());
    }

    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof OWLEntity && o2 instanceof OWLEntity) {
        return compareEntities((OWLEntity) o1, (OWLEntity) o2);
      } else if (o1 instanceof String && o2 instanceof String) {
        return compareStrings((String) o1, (String) o2);
      }
      return 0;
    }
  }

  class SortByStatus implements Comparator<Object> {
    public int compareEntities(OWLEntity e1, OWLEntity e2) {
      boolean b1 = isObsolete(e1);
      boolean b2 = isObsolete(e2);
      return Boolean.compare(b1, b2);
    }

    public int compareStrings(String s1, String s2) {
      return s1.toLowerCase().compareTo(s2.toLowerCase());
    }

    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof OWLEntity && o2 instanceof OWLEntity) {
        return compareEntities((OWLEntity) o1, (OWLEntity) o2);
      } else if (o1 instanceof String && o2 instanceof String) {
        return compareStrings((String) o1, (String) o2);
      }
      return 0;
    }
  }
}
