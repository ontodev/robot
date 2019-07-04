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
    o.addOption(
        "p", "preserve-structure", true, "if false, do not preserve hierarchical relationships");
    o.addOption("a", "axioms", true, "filter only for given axiom types");
    o.addOption("r", "trim", true, "if true, keep axioms containing only selected objects");
    o.addOption(
        "S", "signature", true, "if true, keep axioms with any selected entity in their signature");
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
    OWLOntology inputOntology = state.getOntology();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    // Get a set of entities to start with
    Set<OWLObject> objects = new HashSet<>();
    // track if a set of input IRIs were provided
    boolean hasInputIRIs = false;
    if (line.hasOption("term") || line.hasOption("term-file")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "term", "term-file");
      if (!entityIRIs.isEmpty()) {
        objects.addAll(OntologyHelper.getEntities(inputOntology, entityIRIs));
        hasInputIRIs = true;
      }
    }

    // Get a set of axiom types
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);

    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");

    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }

    // Get the output IRI for the new ontology
    IRI outputIRI;
    String outputIRIString = CommandLineHelper.getOptionalValue(line, "ontology-iri");
    if (outputIRIString != null) {
      outputIRI = IRI.create(outputIRIString);
    } else {
      // If it is not provided, copy the input IRI
      outputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();
    }

    // Create the output ontology
    OWLOntology outputOntology;
    if (outputIRI != null) {
      outputOntology = manager.createOntology(outputIRI);
    } else {
      outputOntology = manager.createOntology();
    }

    // Selects should be processed in order, allowing unions in one --select
    List<List<String>> selectGroups = new ArrayList<>();

    // Track if 'annotations' selection is included
    boolean includeAnnotations = false;

    // Track if selection was 'imports' or 'ontology'
    // These get processed separately and then removed
    // If no other select options are provided, we save and quit after that
    boolean hadSelection = false;

    for (String select : selects) {
      // The single group is a split of the one --select
      List<String> selectGroup = CommandLineHelper.splitSelects(select);
      // Annotations is a special keyword that applies to all selected objects
      if (selectGroup.contains("annotations")) {
        includeAnnotations = true;
        selectGroup.remove("annotations");
      }
      if (selectGroup.contains("imports")) {
        // The imports keyword copies the import delcarations to the output ontology
        for (OWLImportsDeclaration imp : inputOntology.getImportsDeclarations()) {
          manager.applyChange(new AddImport(outputOntology, imp));
        }
        hadSelection = true;
        selectGroup.remove("imports");
      } else if (selectGroup.contains("ontology")) {
        // The ontology keyword retrieves the ontology annotations
        for (OWLAnnotation annotation : inputOntology.getAnnotations()) {
          OntologyHelper.addOntologyAnnotation(outputOntology, annotation);
        }
        hadSelection = true;
        selectGroup.remove("ontology");
      }
      if (!selectGroup.isEmpty()) {
        selectGroups.add(selectGroup);
      }
    }

    // If filtering imports or ontology annotations,
    // and there are no other selects, save and return
    if (hadSelection && selectGroups.isEmpty() && objects.isEmpty()) {
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    } else if (objects.isEmpty() && hasInputIRIs) {
      // if objects is empty AND there WERE input IRIs
      // there is nothing to filter because the IRIs do not exist in the ontology
      // save and exit with empty ontology
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    } else if (objects.isEmpty()) {
      // if objects is empty AND there were NO input IRIs add all
      // this means that we are adding everything to the set to start
      objects.addAll(OntologyHelper.getObjects(inputOntology));
    }

    // Use the select statements to get a set of objects to remove
    Set<OWLObject> relatedObjects =
        RelatedObjectsHelper.selectGroups(inputOntology, ioHelper, objects, selectGroups);

    // Use these two options to determine which axioms to remove
    boolean trim = CommandLineHelper.getBooleanValue(line, "trim", true);
    boolean signature = CommandLineHelper.getBooleanValue(line, "signature", false);

    // Get the axioms
    // Use !trim for the 'partial' option in getAxioms
    Set<OWLAxiom> axiomsToAdd =
        RelatedObjectsHelper.getAxioms(inputOntology, relatedObjects, axiomTypes, !trim, signature);

    // Handle gaps
    boolean preserveStructure = CommandLineHelper.getBooleanValue(line, "preserve-structure", true);
    if (preserveStructure) {
      axiomsToAdd.addAll(RelatedObjectsHelper.spanGaps(inputOntology, relatedObjects));
    }

    // Handle annotations for any referenced object
    if (includeAnnotations) {
      Set<OWLObject> referencedObjects = new HashSet<>();
      for (OWLAxiom axiom : axiomsToAdd) {
        referencedObjects.addAll(OntologyHelper.getObjects(axiom));
      }
      axiomsToAdd.addAll(
          RelatedObjectsHelper.getAnnotationAxioms(inputOntology, referencedObjects));
    }

    // Add the additional axioms to the output ontology
    manager.addAxioms(outputOntology, axiomsToAdd);

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    state.setOntology(outputOntology);
    return state;
  }
}
