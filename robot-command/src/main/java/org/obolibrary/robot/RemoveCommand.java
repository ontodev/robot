package org.obolibrary.robot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RemoveCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "remove#";

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

    IRI outputIRI = CommandLineHelper.getOutputIRI(line);
    if (outputIRI == null) {
      outputIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    }
    // Get a set of entities to start with
    Set<OWLEntity> entities = new HashSet<>();
    for (IRI iri : CommandLineHelper.getTerms(ioHelper, line, "entity", "entities")) {
      entities.add(OntologyHelper.getEntity(ontology, iri));
    }
    // If no entities were provided, add them all
    if (entities.isEmpty()) {
      entities.addAll(OntologyHelper.getEntities(ontology));
    }
    // Get a set of axiom types
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);
    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");
    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }
    // Selects should be processed in order, allowing unions in one --select
    // Produces a set of Relation Types and a set of annotations, as well as booleans for miscs
    while (selects.size() > 0) {
      String select = selects.remove(0);
      Set<RelationType> relationTypes = new HashSet<>();
      Set<OWLAnnotation> annotations = new HashSet<>();
      boolean complement = false;
      boolean named = false;
      boolean anonymous = false;
      // Process annotation selections
      if (select.contains("=")) {
        annotations.add(getAnnotations(ioHelper, select));
      } else {
        // Split on space, create a union of these relations
        for (String s : select.split(" ")) {
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
          } else {
            throw new IllegalArgumentException(String.format(selectError, s));
          }
        }
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
        relatedEntities.forEach(System.out::println);
        RemoveOperation.remove(ontology, relatedEntities, axiomTypes);
      }
    }
    // Maybe trim dangling (by default, false)
    if (CommandLineHelper.getBooleanValue(line, "trim", false)) {
      OntologyHelper.trimDangling(ontology);
    }
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }

  /**
   * Given an IOHelper and an annotation as CURIE=..., return the OWLAnnotation object.
   *
   * @param ioHelper IOHelper to get IRI
   * @param annotation String input
   * @return OWLAnnotation
   */
  private static OWLAnnotation getAnnotations(IOHelper ioHelper, String annotation) {
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    IRI propertyIRI =
        CommandLineHelper.maybeCreateIRI(ioHelper, annotation.split("=")[0], "select");
    OWLAnnotationProperty annotationProperty = dataFactory.getOWLAnnotationProperty(propertyIRI);
    String value = annotation.split("=")[1];
    if (value.contains("<") && value.contains(">")) {
      IRI valueIRI =
          CommandLineHelper.maybeCreateIRI(
              ioHelper, value.substring(1, value.length() - 1), "select");
      return dataFactory.getOWLAnnotation(annotationProperty, valueIRI);
    } else if (value.contains("~'")) {
      // TODO: Pattern
      throw new IllegalArgumentException("CURIE pattern is not yet implemented");
    } else if (value.contains("'")) {
      OWLLiteral literalValue = dataFactory.getOWLLiteral(value.substring(1, value.length() - 1));
      return dataFactory.getOWLAnnotation(annotationProperty, literalValue);
    } else {
      // Assume it's a CURIE
      IRI valueIRI = CommandLineHelper.maybeCreateIRI(ioHelper, value, "select");
      return dataFactory.getOWLAnnotation(annotationProperty, valueIRI);
    }
  }
}
