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
    o.addOption(null, "base-iri", true, "specify a base namespace");
    o.addOption("t", "term", true, "term to remove");
    o.addOption("T", "term-file", true, "load terms from a file");
    o.addOption("e", "exclude-term", true, "term to exclude from removal");
    o.addOption("E", "exclude-terms", true, "set of terms in text file to exclude from removal");
    o.addOption("n", "include-term", true, "term to force include");
    o.addOption("N", "include-terms", true, "set of terms in file to force include");
    o.addOption("s", "select", true, "select a set of terms based on relations");
    o.addOption("a", "axioms", true, "filter only for given axiom types");
    o.addOption("r", "trim", true, "if true, remove axioms containing any selected object");
    o.addOption(
        "S",
        "signature",
        true,
        "if true, remove axioms with any selected entity in their signature");
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
    OWLOntology ontology = state.getOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();

    // Get a set of relation types, or annotations to select
    List<String> selects = CommandLineHelper.getOptionalValues(line, "select");

    // If the select option wasn't provided, default to self
    if (selects.isEmpty()) {
      selects.add("self");
    }

    // Selects should be processed in order, allowing unions in one --select
    List<List<String>> selectGroups = new ArrayList<>();
    boolean anonymous = false;
    for (String select : selects) {
      // The single group is a split of the one --select
      List<String> selectGroup = CommandLineHelper.splitSelects(select);
      // Imports should be handled separately
      if (selectGroup.contains("imports")) {
        OntologyHelper.removeImports(ontology);
        selectGroup.remove("imports");
      }
      if (selectGroup.contains("ontology")) {
        OntologyHelper.removeOntologyAnnotations(ontology);
        selectGroup.remove("ontology");
      }
      if (selectGroup.contains("anonymous")) {
        anonymous = true;
      }
      if (!selectGroup.isEmpty()) {
        selectGroups.add(selectGroup);
      }
    }

    // Get the objects to remove
    Set<OWLObject> relatedObjects = getObjects(line, ioHelper, ontology, selectGroups);
    if (relatedObjects.isEmpty()) {
      // nothing to remove - save and exit
      CommandLineHelper.maybeSaveOutput(line, ontology);
      state.setOntology(ontology);
      return state;
    }

    // Copy the unchanged ontology to reserve for filling gaps later
    OWLOntology copy =
        OWLManager.createOWLOntologyManager().copyOntology(ontology, OntologyCopy.DEEP);

    // Remove specific axioms
    manager.removeAxioms(ontology, getAxioms(line, ioHelper, ontology, relatedObjects));

    // Handle gaps
    boolean preserveStructure = CommandLineHelper.getBooleanValue(line, "preserve-structure", true);
    if (preserveStructure) {
      // Since we are preserving the structure between the objects that were NOT removed, we need to
      // get the complement of the removed object set and build relationships between those objects.
      Set<OWLObject> complementObjects =
          RelatedObjectsHelper.select(ontology, ioHelper, relatedObjects, "complement");
      manager.addAxioms(
          ontology, RelatedObjectsHelper.spanGaps(copy, complementObjects, anonymous));
    }

    // Save the changed ontology and return the state
    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }

  /**
   * Given a command line, an IOHelper, an ontology, and a list of select groups, return the objects
   * from the ontology based on the select groups.
   *
   * @param line
   * @param ioHelper
   * @param ontology
   * @param selectGroups
   * @return
   * @throws Exception
   */
  protected static Set<OWLObject> getObjects(
      CommandLine line, IOHelper ioHelper, OWLOntology ontology, List<List<String>> selectGroups)
      throws Exception {
    // Get a set of entities to start with
    Set<OWLObject> objects = new HashSet<>();
    // track if a set of input IRIs were provided
    boolean hasInputIRIs = false;
    if (line.hasOption("term") || line.hasOption("term-file")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "term", "term-file");
      if (!entityIRIs.isEmpty()) {
        objects.addAll(OntologyHelper.getEntities(ontology, entityIRIs));
        hasInputIRIs = true;
      }
    }

    boolean hadSelection = CommandLineHelper.hasFlagOrCommand(line, "select");
    boolean internal = false;
    boolean external = false;
    if (line.hasOption("axioms")) {
      for (String ats : CommandLineHelper.getOptionalValue(line, "axioms").split(" ")) {
        if (ats.equalsIgnoreCase("internal")) {
          internal = true;
        } else if (ats.equalsIgnoreCase("external")) {
          external = true;
        }
      }
    }

    if (hadSelection && selectGroups.isEmpty() && objects.isEmpty() && !internal && !external) {
      // If removing imports or ontology annotations
      // and there are no other selects, save and return
      return objects;
    } else if (objects.isEmpty() && hasInputIRIs && !internal && !external) {
      // if objects is empty AND there WERE input IRIs
      // there is nothing to remove because the IRIs do not exist in the ontology
      return objects;
    } else if (objects.isEmpty()) {
      // if objects is empty AND there were NO input IRIs add all
      // OR internal/external were selected
      // this means that we are adding everything to the set to start
      objects.addAll(OntologyHelper.getObjects(ontology));
    }

    // Use the select statements to get a set of objects to remove
    Set<OWLObject> relatedObjects =
        RelatedObjectsHelper.selectGroups(ontology, ioHelper, objects, selectGroups);

    // Remove all the excluded terms from that set
    if (line.hasOption("exclude-term") || line.hasOption("exclude-terms")) {
      Set<IRI> excludeIRIs =
          CommandLineHelper.getTerms(ioHelper, line, "exclude-term", "exclude-terms");
      Set<OWLObject> excludeObjects =
          new HashSet<>(OntologyHelper.getEntities(ontology, excludeIRIs));
      relatedObjects.removeAll(excludeObjects);
    }

    // Add all the include terms
    if (line.hasOption("include-term") || line.hasOption("include-terms")) {
      Set<IRI> includeIRIs =
          CommandLineHelper.getTerms(ioHelper, line, "include-term", "include-terms");
      Set<OWLObject> includeObjects =
          new HashSet<>(OntologyHelper.getEntities(ontology, includeIRIs));
      relatedObjects.addAll(includeObjects);
    }

    return relatedObjects;
  }

  /**
   * Given a command line, an IOHelper, an ontology, and a set of objects, return the related axioms
   * for those objects to remove from the ontology.
   *
   * @param line command line to use
   * @param ioHelper IOHelper to resolve prefixes for base namespaces
   * @param ontology ontology to select axioms from
   * @param relatedObjects objects to select axioms for
   * @return set of axioms to remove
   */
  private static Set<OWLAxiom> getAxioms(
      CommandLine line, IOHelper ioHelper, OWLOntology ontology, Set<OWLObject> relatedObjects) {
    // Get a set of axiom types
    boolean internal = false;
    boolean external = false;
    if (line.hasOption("axioms")) {
      for (String ats : CommandLineHelper.getOptionalValue(line, "axioms").split(" ")) {
        if (ats.equalsIgnoreCase("internal")) {
          internal = true;
        } else if (ats.equalsIgnoreCase("external")) {
          external = true;
        }
      }
    }
    Set<Class<? extends OWLAxiom>> axiomTypes = CommandLineHelper.getAxiomValues(line);

    // Use these two options to determine which axioms to remove
    boolean trim = CommandLineHelper.getBooleanValue(line, "trim", true);
    boolean signature = CommandLineHelper.getBooleanValue(line, "signature", false);

    // Get the axioms and remove them
    Set<OWLAxiom> axiomsToRemove =
        RelatedObjectsHelper.getAxioms(ontology, relatedObjects, axiomTypes, trim, signature);

    // Then select internal or external, if present
    List<String> baseNamespaces = CommandLineHelper.getBaseNamespaces(line, ioHelper);
    if (internal && external) {
      logger.warn(
          "Both 'internal' and 'external' axioms are selected - these axiom selectors will be ignored.");
    } else if (internal) {
      axiomsToRemove = RelatedObjectsHelper.getInternalAxioms(baseNamespaces, axiomsToRemove);
    } else if (external) {
      axiomsToRemove = RelatedObjectsHelper.getExternalAxioms(baseNamespaces, axiomsToRemove);
    }

    return axiomsToRemove;
  }
}
