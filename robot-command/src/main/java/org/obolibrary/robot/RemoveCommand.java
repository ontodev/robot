package org.obolibrary.robot;

import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove axioms from an ontology based on a series of inputs.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
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
    o.addOption("t", "term", true, "term to remove");
    o.addOption("T", "term-file", true, "load terms from a file");
    o.addOption("s", "select", true, "select a set of terms based on relations");
    o.addOption("a", "axioms", true, "filter only for given axiom types");
    o.addOption("r", "trim", true, "if false, do not trim dangling entities");
    o.addOption(
        "p", "preserve-structure", true, "if false, do not preserve hierarchical relationships");
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
    return "robot remove --input <file> " + "--output <file>";
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
    OWLOntology inputOntology = state.getOntology();
    OWLOntologyManager manager = inputOntology.getOWLOntologyManager();

    // Get a set of entities to start with
    Set<OWLObject> objects = new HashSet<>();
    if (line.hasOption("term") || line.hasOption("term-file")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "term", "term-file");
      objects.addAll(OntologyHelper.getEntities(inputOntology, entityIRIs));
    }

    // Get a set of axiom types
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);

    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");

    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }
    boolean removeImports = false;
    boolean trim = CommandLineHelper.getBooleanValue(line, "trim", true);

    // Copy the input ontology to create the output ontology
    OWLOntology outputOntology =
        OWLManager.createOWLOntologyManager().copyOntology(inputOntology, OntologyCopy.DEEP);

    // Selects should be processed in order, allowing unions in one --select
    List<List<String>> selectGroups = new ArrayList<>();
    for (String select : selects) {
      // The single group is a split of the one --select
      List<String> selectGroup = CommandLineHelper.splitSelects(select);
      // Imports should be handled separately
      if (selectGroup.contains("imports")) {
        OntologyHelper.removeImports(outputOntology);
        removeImports = true;
        selectGroup.remove("imports");
      }
      if (!selectGroup.isEmpty()) {
        selectGroups.add(selectGroup);
      }
    }

    // If removing imports, and there are no other selects, save and return
    if (removeImports && selectGroups.isEmpty()) {
      if (trim) {
        OntologyHelper.trimOntology(outputOntology);
      }
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
      return state;
    } else if (objects.isEmpty()) {
      // Otherwise, proceed, and if objects is empty, add all objects
      objects.addAll(OntologyHelper.getObjects(outputOntology));
    }

    // Use the select statements to get a set of objects to remove
    Set<OWLObject> relatedObjects =
        RelatedObjectsHelper.selectGroups(outputOntology, ioHelper, objects, selectGroups);
    Set<OWLAxiom> axiomsToRemove;
    if (trim) {
      // Get axioms that include at least one object
      axiomsToRemove =
          RelatedObjectsHelper.getPartialAxioms(outputOntology, relatedObjects, axiomTypes);
    } else {
      // Get axioms that ONLY include the objects
      axiomsToRemove =
          RelatedObjectsHelper.getCompleteAxioms(outputOntology, relatedObjects, axiomTypes);
    }
    manager.removeAxioms(outputOntology, axiomsToRemove);

    // Handle gaps
    boolean preserveStructure = CommandLineHelper.getBooleanValue(line, "preserve-structure", true);
    if (preserveStructure) {
      // Since we are preserving the structure between the objects that were NOT removed, we need to
      // get the complement of the removed object set and build relationships between those objects.
      relatedObjects = RelatedObjectsHelper.select(outputOntology, ioHelper, objects, "complement");
      manager.addAxioms(
          outputOntology, RelatedObjectsHelper.spanGaps(inputOntology, relatedObjects));
    }

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    state.setOntology(outputOntology);
    return state;
  }
}
