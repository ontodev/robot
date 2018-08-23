package org.obolibrary.robot;

import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.*;
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
    o.addOption("r", "trim", true, "if true, trim dangling entities");
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
    OWLOntology ontology = state.getOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();

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
    boolean removeImports = false;
    boolean trim = CommandLineHelper.getBooleanValue(line, "trim", false);

    // Selects should be processed in order, allowing unions in one --select
    List<List<String>> selectGroups = new ArrayList<>();
    for (String select : selects) {
      // The single group is a split of the one --select
      List<String> selectGroup = CommandLineHelper.splitSelects(select);
      // Imports should be handled separately
      if (selectGroup.contains("imports")) {
        OntologyHelper.removeImports(ontology);
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
        OntologyHelper.trimOntology(ontology);
      }
      CommandLineHelper.maybeSaveOutput(line, ontology);
      state.setOntology(ontology);
      return state;
    } else if (objects.isEmpty()) {
      // Otherwise, proceed, and if objects is empty, add all objects
      for (OWLAxiom axiom : ontology.getAxioms()) {
        objects.addAll(OntologyHelper.getObjects(axiom));
      }
    }

    Set<OWLObject> relatedObjects =
        RelatedObjectsHelper.selectGroups(ontology, objects, selectGroups);
    Set<OWLAxiom> axiomsToRemove;
    if (trim) {
      axiomsToRemove = RelatedObjectsHelper.getPartialAxioms(ontology, relatedObjects, axiomTypes);
    } else {
      axiomsToRemove = RelatedObjectsHelper.getCompleteAxioms(ontology, relatedObjects, axiomTypes);
    }
    manager.removeAxioms(ontology, axiomsToRemove);

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}
