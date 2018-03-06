package org.obolibrary.robot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveCommand implements Command {

  private static final String CLASS = "class";
  private static final String INDIV = "individual";
  private static final String OBJ = "object-property";
  private static final String ANN = "annotation-property";
  private static final String DATA = "datatype-property";

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExtractCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public RemoveCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "output-iri", true, "set OntologyIRI for output");
    o.addOption("c", "class", true, "remove a Class");
    o.addOption("n", "individual", true, "remove a NamedIndividual");
    o.addOption("e", "entity", true, "remove an OWL entity");
    o.addOption("E", "entities", true, "remove a set of OWL entities");
    o.addOption("b", "object-property", true, "remove an ObjectProperty");
    o.addOption("a", "annotation-property", true, "remove an AnnotationProperty");
    o.addOption("d", "datatype-property", true, "remove a DatatypeProperty");
    o.addOption("N", "all-individuals", true, "remove all NamedIndividuals");
    o.addOption("D", "descendant-classes", true, "remove all classes descended from a class");
    o.addOption(
        "A", "anonymous-superclasses", true, "remove all anonymous superclasses from a class");
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

    // Remove all individuals if requested
    if (CommandLineHelper.hasFlagOrCommand(line, "all-individuals")) {
      RemoveOperation.removeIndividuals(ontology);
    }

    // Remove anonymous superclasses if requested (given CURIE)
    String subClassID = CommandLineHelper.getOptionalValue(line, "anonymous-superclasses");
    if (subClassID != null) {
      RemoveOperation.removeAnonymousSuperclasses(ontology, subClassID);
    }

    // Remove descendant classes if requested (given CURIE)
    String superClassID = CommandLineHelper.getOptionalValue(line, "descendant-classes");
    if (superClassID != null) {
      RemoveOperation.removeDescendantClasses(ontology, superClassID);
    }

    // Remove single entity if requesterd (given CURIE)
    String entityID = CommandLineHelper.getOptionalValue(line, "entity");
    if (entityID != null) {
      RemoveOperation.remove(ontology, entityID);
    }

    // Remove list of entities if requested
    // `--entities` expects a text file with entity IDs on separate lines
    String idsFile = CommandLineHelper.getOptionalValue(line, "enitities");
    if (idsFile != null) {
      try (BufferedReader br = new BufferedReader(new FileReader(idsFile))) {
        String id;
        // Line by line remove the entity
        while ((id = br.readLine()) != null) {
          RemoveOperation.remove(ontology, id);
        }
      }
    }

    // Create map of options & their args (null if not provided)
    // Expects CURIEs for the entities
    Map<String, String> entityIDs = new HashMap<>();
    entityIDs.put(CLASS, CommandLineHelper.getOptionalValue(line, CLASS));
    entityIDs.put(INDIV, CommandLineHelper.getOptionalValue(line, INDIV));
    entityIDs.put(OBJ, CommandLineHelper.getOptionalValue(line, OBJ));
    entityIDs.put(ANN, CommandLineHelper.getOptionalValue(line, ANN));
    entityIDs.put(DATA, CommandLineHelper.getOptionalValue(line, DATA));

    RemoveOperation.remove(ontology, entityIDs);
    CommandLineHelper.maybeSaveOutput(line, ontology);

    state.setOntology(ontology);
    return state;
  }
}
