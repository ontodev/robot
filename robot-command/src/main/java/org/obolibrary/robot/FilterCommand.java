package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link FilterOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "filter#";

  /** Error message when --select is not a valid input. Expects: input string. */
  private static final String selectError = NS + "SELECT ERROR %s is not a valid selection";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public FilterCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "output-iri", true, "set OntologyIRI for output");
    o.addOption("e", "entity", true, "filter for an entity");
    o.addOption("E", "entities", true, "filter for a of entities");
    o.addOption("s", "select", true, "filter for a of entities using one or more relation options");
    o.addOption("a", "axioms", true, "filter for axioms from a set of entities (default: all)");
    o.addOption("t", "trim", true, "if true, trim dangling entities (default: true)");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "filter";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "filter ontology axioms";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot filter --input <file> " + "--term-file <file> " + "--output <file>";
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
   * Handle the command-line and file operations for the FilterOperation.
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
   * Given an input state and command line arguments, filter axioms from its ontology, modifying it,
   * and return a state with the modified ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with the filtered ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    if (state == null) {
      state = new CommandState();
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology inputOntology = state.getOntology();
    OWLOntology outputOntology = null;

    // Get a set of entities
    Set<OWLEntity> entities = new HashSet<>();
    for (IRI iri : CommandLineHelper.getTerms(ioHelper, line, "entity", "entities")) {
      entities.add(OntologyHelper.getEntity(inputOntology, iri));
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
      boolean includeAnnotations = false;

      // Split on space, create a union of these relations
      for (String s : RemoveCommand.splitSelects(select)) {
        if (RelationType.isRelationType(s.toLowerCase())) {
          relationTypes.add(RelationType.getRelationType(s.toLowerCase()));
        } else if (s.equalsIgnoreCase("complement")) {
          complement = true;
        } else if (s.equalsIgnoreCase("named")) {
          named = true;
        } else if (s.equalsIgnoreCase("anonymous")) {
          anonymous = true;
        } else if (s.equalsIgnoreCase("annotations")) {
          // If "annotations" is included, all annotations on the entities will be included,
          // regardless of if the property is included in entities or not.
          includeAnnotations = true;
        } else if (s.contains("=")) {
          // This designates an annotation to find
          annotations.addAll(RemoveCommand.getAnnotations(inputOntology, ioHelper, s));
        } else {
          throw new IllegalArgumentException(String.format(selectError, s));
        }
      }

      // Add annotated entities to the set of entities
      entities.addAll(RelatedEntitiesHelper.getAnnotated(inputOntology, annotations));
      // If no entities were provided by entity or annotation, add them all
      if (entities.isEmpty()) {
        entities.addAll(OntologyHelper.getEntities(inputOntology));
      }

      // If no relation type selections were provided, add in "self"
      if (relationTypes.isEmpty()) {
        relationTypes.add(RelationType.SELF);
      }

      // Maybe get a complement set of the entity/entities provided
      if (complement) {
        Set<OWLEntity> complementSet =
            RelatedEntitiesHelper.getComplements(inputOntology, entities);
        entities = complementSet;
      }

      // Filter entities from ontology
      if (anonymous && !named) {
        // Filter only anonymous entities based on relations to given entities
        outputOntology =
            FilterOperation.filterAnonymous(inputOntology, entities, relationTypes, axiomTypes);
      } else if (named && !anonymous) {
        // Otherwise get the related entities and proceed
        Set<OWLEntity> relatedEntities =
            RelatedEntitiesHelper.getRelated(inputOntology, entities, relationTypes);
        outputOntology =
            FilterOperation.filter(inputOntology, relatedEntities, axiomTypes, includeAnnotations);
      } else {
        // If both named and anonymous = true OR neither was provided, filter both
        List<OWLOntology> outputOntologies = new ArrayList<>();
        outputOntologies.add(
            FilterOperation.filterAnonymous(inputOntology, entities, relationTypes, axiomTypes));
        Set<OWLEntity> relatedEntities =
            RelatedEntitiesHelper.getRelated(inputOntology, entities, relationTypes);
        outputOntologies.add(
            FilterOperation.filter(inputOntology, relatedEntities, axiomTypes, includeAnnotations));
        outputOntology = MergeOperation.merge(outputOntologies, false, false);
      }

      // Reset the input as the product of this set of selects
      inputOntology = outputOntology;
    }

    // Maybe trim dangling
    if (CommandLineHelper.getBooleanValue(line, "trim", true)) {
      OntologyHelper.trimDangling(outputOntology);
    }

    // Save output and return the new ontology as the state
    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    state.setOntology(outputOntology);
    return state;
  }
}
