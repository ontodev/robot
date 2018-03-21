package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveCommand implements Command {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RemoveCommand.class);

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
    options = o;

    // TODO: imports, anonymous, annotations
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

    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();

    IRI outputIRI = CommandLineHelper.getOutputIRI(line);
    if (outputIRI == null) {
      outputIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    }
    // Get a set of entities
    Set<OWLEntity> entities = new HashSet<>();
    for (IRI iri : CommandLineHelper.getTerms(ioHelper, line, "entity", "entities")) {
      entities.add(OntologyHelper.getEntity(ontology, iri));
    }
    // Get a set of relation types, or annotations to select
    Set<RelationType> relationTypes = new HashSet<>();
    Set<OWLAnnotation> annotations = new HashSet<>();
    List<String> selectStrings = new ArrayList<>();
    for (String select : CommandLineHelper.getOptionalValues(line, "select")) {
      if (select.contains("+")) {
        for (String s : select.split("+")) {
          selectStrings.add(s);
        }
      } else {
        selectStrings.add(select);
      }
    }
    // If the select option wasn't provided, default to self
    if (selectStrings.isEmpty()) {
      selectStrings.add("self");
    }

    for (String select : selectStrings) {
      if (RelationType.isRelationType(select.toUpperCase())) {
        relationTypes.add(RelationType.getRelationType(select.toUpperCase()));
      } else if (select.contains("=")) {
        IRI propertyIRI =
            CommandLineHelper.maybeCreateIRI(ioHelper, select.split("=")[0], "select");
        OWLAnnotationProperty annotationProperty =
            dataFactory.getOWLAnnotationProperty(propertyIRI);
        String value = select.split("=")[1];
        if (value.contains("<") && value.contains(">")) {
          // IRI provided
          IRI valueIRI =
              CommandLineHelper.maybeCreateIRI(
                  ioHelper, value.substring(1, value.length() - 1), "select");
          annotations.add(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
        } else if (value.contains("~'")) {
          // TODO: Pattern
        } else if (value.contains("'")) {
          // Literal provided
          OWLLiteral literalValue =
              dataFactory.getOWLLiteral(value.substring(1, value.length() - 1));
          annotations.add(dataFactory.getOWLAnnotation(annotationProperty, literalValue));
        } else {
          // Assume it's a CURIE
          IRI valueIRI = CommandLineHelper.maybeCreateIRI(ioHelper, value, "select");
          annotations.add(dataFactory.getOWLAnnotation(annotationProperty, valueIRI));
        }
      }
    }
    annotations.forEach(a -> System.out.println(a.toString()));
    // Get a set of axiom types
    Set<AxiomType<?>> axiomTypes = new HashSet<>();
    List<String> axiomStrings = new ArrayList<>();
    for (String axiom : CommandLineHelper.getOptionalValues(line, "axioms")) {
      if (axiom.contains("+")) {
        for (String a : axiom.split("+")) {
          axiomStrings.add(a.replace("-", "_"));
        }
      } else {
        axiomStrings.add(axiom.replace("-", "_"));
      }
    }
    // If the axiom option wasn't provided, default to all
    if (axiomStrings.isEmpty()) {
      axiomStrings.add("all");
    }
    for (String axiom : axiomStrings) {
      if (AxiomType.isAxiomType(axiom.toUpperCase())) {
        axiomTypes.add(AxiomType.getAxiomType(axiom.toUpperCase()));
      } else if (axiom.equalsIgnoreCase("all")) {
        axiomTypes.addAll(AxiomType.AXIOM_TYPES);
      } else {
        // TODO
        throw new IllegalArgumentException("");
      }
    }

    logger.debug("SELECTING ENTITIES BY RELATIONS:");
    relationTypes.forEach(rt -> logger.debug(rt.toString()));
    // Find entities based on relation to specified entity
    Set<OWLEntity> removeEntities =
        RelatedEntitiesHelper.getRelated(ontology, entities, relationTypes);
    // Find entities based on annotations
    removeEntities.addAll(RelatedEntitiesHelper.getAnnotated(ontology, annotations));
    // Remove axioms associated with entity
    RemoveOperation.remove(ontology, removeEntities, axiomTypes);

    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}
