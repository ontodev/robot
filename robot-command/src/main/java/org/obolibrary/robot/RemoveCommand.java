package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.*;

public class RemoveCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RemoveCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "remove#";

  /** Error message when an invalid datatype is provided. */
  private static final String dataTypeError = NS + "DATATYPE ERROR %s is not a valid datatype";

  /**
   * Error message when a datatype is given for an annotation, but the annotation value does not
   * match the datatype.
   */
  private static final String literalValueError = NS + "LITERAL VALUE ERROR %s is not a %s value";

  /** Error message when --select is not a valid input. Expects: input string. */
  private static final String selectError = NS + "SELECT ERROR %s is not a valid selection";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public RemoveCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "output-iri", true, "set OntologyIRI for output");
    o.addOption("e", "entity", true, "remove an entity");
    o.addOption("E", "entities", true, "remove a set of entities");
    o.addOption("s", "select", true, "remove a set of entities using one or more relation options");
    o.addOption("a", "axioms", true, "remove axioms from a set of entities (default: all)");
    o.addOption("t", "trim", true, "if true, trim dangling entities");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "remove";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "remove axioms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot remove --input <file> " + "--output <file> " + "--output-iri <iri>";
  }

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations for the RemoveOperation.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(getUsage(), getOptions(), e);
    }
  }

  /**
   * Given an input state and command line arguments, create a new ontology with removed axioms and
   * return a new state. The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state with the new ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();

    // Get a set of entities to start with
    Set<OWLEntity> entities = new HashSet<>();
    if (line.hasOption("entity") || line.hasOption("entities")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "entity", "entities");
      entities = OntologyHelper.getEntities(ontology, entityIRIs);
    }

    // Get a set of axiom types
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);

    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");

    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }
    boolean noEntities = false;

    // Selects should be processed in order, allowing unions in one --select
    // Produces a set of Relation Types and a set of annotations, as well as booleans for miscs
    while (selects.size() > 0) {
      String select = selects.remove(0);
      Set<RelationType> relationTypes = new HashSet<>();
      Set<OWLAnnotation> annotations = new HashSet<>();
      boolean complement = false;
      boolean named = false;
      boolean anonymous = false;

      // Split on space, create a union of these relations
      for (String s : splitSelects(select)) {
        if (RelationType.isRelationType(s.toLowerCase())) {
          relationTypes.add(RelationType.getRelationType(s.toLowerCase()));
        } else if (s.equalsIgnoreCase("complement")) {
          complement = true;
        } else if (s.equalsIgnoreCase("named")) {
          named = true;
        } else if (s.equalsIgnoreCase("anonymous")) {
          anonymous = true;
        } else if (s.equalsIgnoreCase("imports")) {
          // Remove import statements
          RemoveOperation.removeImports(ontology);
          noEntities = true;
        } else if (s.contains("=")) {
          // This designates an annotation to find
          annotations.addAll(getAnnotations(ontology, ioHelper, s));
        } else {
          throw new IllegalArgumentException(String.format(selectError, s));
        }
      }

      // Add annotated entities to the set of entities
      entities.addAll(RelatedEntitiesHelper.getAnnotated(ontology, annotations));
      // If no entities were provided, add them all
      if (noEntities && entities.isEmpty()) {
        return state;
      } else if (entities.isEmpty()) {
        entities.addAll(OntologyHelper.getEntities(ontology));
      }

      // If no relation type selections were provided, add in "self"
      if (relationTypes.isEmpty()) {
        relationTypes.add(RelationType.SELF);
      }

      // (Maybe) get a complement set of the entity/entities provided
      if (complement) {
        Set<OWLEntity> complementSet = RelatedEntitiesHelper.getComplements(ontology, entities);
        entities = complementSet;
      }

      // Remove entities from ontology
      if (anonymous && !named) {
        // Remove only anonymous entities based on relations to given entities
        RemoveOperation.removeAnonymous(ontology, entities, relationTypes, axiomTypes);
      } else if (named && !anonymous) {
        // Otherwise get the related entities and proceed
        Set<OWLEntity> relatedEntities =
            RelatedEntitiesHelper.getRelated(ontology, entities, relationTypes);
        RemoveOperation.remove(ontology, relatedEntities, axiomTypes);
      } else {
        // If both named and anonymous = true OR neither was provided, remove all
        RemoveOperation.removeAnonymous(ontology, entities, relationTypes, axiomTypes);
        Set<OWLEntity> relatedEntities =
            RelatedEntitiesHelper.getRelated(ontology, entities, relationTypes);
        RemoveOperation.remove(ontology, relatedEntities, axiomTypes);
      }
    }

    // Maybe trim dangling (by default, false)
    if (CommandLineHelper.getBooleanValue(line, "trim", false)) {
      OntologyHelper.trimDangling(ontology);
    }

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }

  /**
   * Given an IOHelper and an annotation as CURIE=..., return the OWLAnnotation object(s).
   *
   * @param ontology OWLOntology to get annotations from
   * @param ioHelper IOHelper to get IRI
   * @param annotation String input
   * @return set of OWLAnnotations
   */
  protected static Set<OWLAnnotation> getAnnotations(
      OWLOntology ontology, IOHelper ioHelper, String annotation) throws Exception {
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    // Create an IRI from the CURIE
    IRI propertyIRI =
        CommandLineHelper.maybeCreateIRI(ioHelper, annotation.split("=")[0], "select");
    // Get the annotation property and string representation of value
    OWLAnnotationProperty annotationProperty = dataFactory.getOWLAnnotationProperty(propertyIRI);
    String value = annotation.split("=")[1];
    // Based on the value, determine the type of annotation
    if (value.contains("<") && value.contains(">")) {
      // Return an IRI annotation
      IRI valueIRI =
          CommandLineHelper.maybeCreateIRI(
              ioHelper, value.substring(1, value.length() - 1), "select");
      return Sets.newHashSet(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
    } else if (value.contains("~'")) {
      // Return a set of annotations in the ontology that match a regex pattern
      return getPatternAnnotations(ontology, annotationProperty, value);
    } else if (value.contains("'")) {
      // Return a literal (string, boolean, double, integer, float) annotation
      return Sets.newHashSet(
          getLiteralAnnotation(ioHelper, dataFactory, annotationProperty, value));
    } else {
      // Return an IRI annotation based on a CURIE
      IRI valueIRI = CommandLineHelper.maybeCreateIRI(ioHelper, value, "select");
      return Sets.newHashSet(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
    }
  }

  /**
   * Given an input string, return a list of the string split on whitespace, while ignoring any
   * whitespace in single string quotes.
   *
   * @param selects String of select options to split
   * @return List of split strings
   */
  protected static List<String> splitSelects(String selects) {
    List<String> split = new ArrayList<>();
    Matcher m = Pattern.compile("([^\']\\S*|\'.+?\')\\s*").matcher(selects);
    while (m.find()) {
      String s = m.group(1);
      split.add(s);
    }
    return split;
  }

  /**
   * Given an OWL ontology, an annotation property, and an annotation value (in regex pattern form),
   * return a set of OWLAnnotations that have values matching the regex value.
   *
   * @param ontology OWLOntology to retrieve annotations from
   * @param annotationProperty OWLAnnotationProperty
   * @param value regex pattern to match values to
   * @return set of matching OWLAnnotations
   */
  private static Set<OWLAnnotation> getPatternAnnotations(
      OWLOntology ontology, OWLAnnotationProperty annotationProperty, String value)
      throws Exception {
    Set<OWLAnnotation> annotations = new HashSet<>();
    String patternString = value.split("\'")[1];
    Pattern pattern = Pattern.compile(patternString);
    for (OWLEntity e : OntologyHelper.getEntities(ontology)) {
      for (OWLAnnotation a : EntitySearcher.getAnnotations(e, ontology)) {
        if (a.getProperty().equals(annotationProperty)) {
          OWLAnnotationValue av = a.getValue();
          String annotationValue;
          // The annotation value ONLY expects a plain or string
          try {
            OWLLiteralImplPlain plain = (OWLLiteralImplPlain) av;
            annotationValue = plain.getLiteral();
          } catch (Exception e1) {
            try {
              OWLLiteralImplString str = (OWLLiteralImplString) av;
              annotationValue = str.getLiteral();
            } catch (Exception e2) {
              throw e2;
              // TODO: does this block actually get hit?
              // The pattern should only match a string anyway
            }
          }
          Matcher matcher = pattern.matcher(annotationValue);
          if (matcher.matches()) {
            annotations.add(a);
          }
        }
      }
    }
    return annotations;
  }

  /**
   * Given an OWL data factory, an annotation property, and a literal value, return the
   * OWLAnnotation object.
   *
   * @param ioHelper IOHelper to retrieve prefix manager
   * @param dataFactory OWLDataFactory to create entities
   * @param annotationProperty OWLAnnotationProperty
   * @param value annotation value as string
   * @return OWLAnnotation object
   * @throws Exception on issue parsing to datatype
   */
  private static OWLAnnotation getLiteralAnnotation(
      IOHelper ioHelper,
      OWLDataFactory dataFactory,
      OWLAnnotationProperty annotationProperty,
      String value)
      throws Exception {
    // ioHelper.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    OWLAnnotationValue annotationValue;
    if (value.contains("^^")) {
      // A datatype is given
      String content = value.split("\\^\\^")[0].replace("'", "");
      String dataTypeID = value.split("\\^\\^")[1];
      IRI dataTypeIRI = CommandLineHelper.maybeCreateIRI(ioHelper, dataTypeID, "datatype");
      System.out.println(dataTypeIRI.toString());
      OWLDatatype dt = dataFactory.getOWLDatatype(dataTypeIRI);
      if (dt.isBoolean()) {
        if (content.equalsIgnoreCase("true")) {
          annotationValue = dataFactory.getOWLLiteral(true);
        } else if (content.equalsIgnoreCase("false")) {
          annotationValue = dataFactory.getOWLLiteral(false);
        } else {
          throw new Exception(String.format(literalValueError, dataTypeID, "boolean"));
        }
      } else if (dt.isDouble()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Double.parseDouble(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "double"));
        }
      } else if (dt.isFloat()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Float.parseFloat(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "float"));
        }
      } else if (dt.isInteger()) {
        try {
          annotationValue = dataFactory.getOWLLiteral(Integer.parseInt(content));
        } catch (Exception e) {
          throw new Exception(String.format(literalValueError, dataTypeID, "integer"));
        }
      } else if (dt.isString()) {
        annotationValue = dataFactory.getOWLLiteral(content);
      } else {
        annotationValue = dataFactory.getOWLLiteral(content, dt);
      }
    } else {
      // If a datatype isn't provided, default to string literal
      annotationValue = dataFactory.getOWLLiteral(value.replace("'", ""));
    }
    return dataFactory.getOWLAnnotation(annotationProperty, annotationValue);
  }
}
