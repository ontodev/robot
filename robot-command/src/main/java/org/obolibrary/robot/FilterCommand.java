package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a subset of an ontology based on a series of inputs.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class FilterCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(FilterCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public FilterCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "ontology-iri", true, "set OntologyIRI for output");
    o.addOption("t", "term", true, "term to filter");
    o.addOption("T", "term-file", true, "load terms from a file");
    o.addOption("s", "select", true, "select a set of terms based on relations");
    o.addOption("a", "axioms", true, "filter only for given axiom types");
    o.addOption("r", "trim", true, "if true, trim dangling entities");
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
   * Handle the command-line and file operations.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(e);
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

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    // Get a set of entities to start with
    Set<OWLObject> objects = new HashSet<>();
    if (line.hasOption("term") || line.hasOption("term-file")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "term", "term-file");
      objects.addAll(OntologyHelper.getEntities(ontology, entityIRIs));
    }

    // Get a set of axiom types
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);

    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");

    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }
    boolean trim = CommandLineHelper.getBooleanValue(line, "trim", true);

    // Selects should be processed in order, allowing unions in one --select
    List<List<String>> selectGroups = new ArrayList<>();
    boolean includeAllAnnotations = false;
    for (String select : selects) {
      // The single group is a split of the one --select
      List<String> selectGroup = CommandLineHelper.splitSelects(select);
      if (selectGroup.contains("annotations")) {
        includeAllAnnotations = true;
        selectGroup.remove("annotations");
      }
      selectGroups.add(selectGroup);
    }

    // If there are no objects, add all the objects from the ontology
    if (objects.isEmpty()) {
      for (OWLAxiom axiom : ontology.getAxioms()) {
        objects.addAll(OntologyHelper.getObjects(axiom));
      }
    }

    IRI outputIRI;
    String outputIRIString = CommandLineHelper.getOptionalValue(line, "ontology-iri");
    if (outputIRIString != null) {
      outputIRI = IRI.create(outputIRIString);
    } else {
      outputIRI = ontology.getOntologyID().getOntologyIRI().orNull();
    }

    // Get a set of axioms to copy over
    Set<OWLObject> relatedObjects =
        RelatedObjectsHelper.selectGroups(ontology, objects, selectGroups);
    // Add the annotation properties if included
    if (includeAllAnnotations) {
      for (OWLEntity entity : OntologyHelper.getEntities(ontology)) {
        if (entity.isOWLAnnotationProperty()) {
          relatedObjects.add(entity);
        }
      }
    }

    Set<OWLAxiom> axiomsToCopy;
    if (trim) {
      axiomsToCopy = RelatedObjectsHelper.getCompleteAxioms(ontology, relatedObjects, axiomTypes);
    } else {
      axiomsToCopy = RelatedObjectsHelper.getPartialAxioms(ontology, relatedObjects, axiomTypes);
    }

    // Create a new ontology from that set of axioms
    OWLOntology outputOntology;
    if (outputIRI != null) {
      outputOntology = manager.createOntology(axiomsToCopy, outputIRI);
    } else {
      outputOntology = manager.createOntology(axiomsToCopy);
    }

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    state.setOntology(outputOntology);
    return state;
  }
}
