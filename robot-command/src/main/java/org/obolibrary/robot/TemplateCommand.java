package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link TemplateOperation}.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class TemplateCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(TemplateCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "template#";

  /** Error message when user provides --merge-before and --merge-after as true. */
  private static final String mergeError =
      NS + "MERGE ERROR merge-before and merge-after cannot be combined";

  /** Error message when a template is not provided. */
  private static final String missingTemplateError =
      NS + "MISSING TEMPLATE ERROR at least one template is required";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public TemplateCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology to merge from a file");
    o.addOption("I", "input-iri", true, "load ontology to merge from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "ontology-iri", true, "set the output ontology IRI");
    o.addOption("V", "version-iri", true, "set the output version IRI");
    o.addOption("t", "template", true, "read template from a file");
    o.addOption("a", "ancestors", false, "MIREOT ancestors into results");
    o.addOption("m", "merge-before", false, "merge into input ontology before any output");
    o.addOption("M", "merge-after", false, "merge into input ontology after any output");
    o.addOption(
        "c", "collapse-import-closure", true, "if true, collapse the import closure when merging");
    o.addOption(
        "A", "include-annotations", true, "if true, include ontology annotations from merge input");

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "template";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "build an ontology from a template";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot template --input <file> " + "--template <file> " + "--output <file>";
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
   * Handle the command-line and file operations for the TemplateOperation.
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
   * Given an input state and command line arguments, build a new ontology from the given template,
   * and return a state with the new ontology.
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
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line, false);
    OWLOntology inputOntology = state.getOntology();

    // Read the whole CSV into a nested list of strings.
    List<String> templatePaths = CommandLineHelper.getOptionalValues(line, "template");
    if (templatePaths.size() == 0) {
      throw new IllegalArgumentException(missingTemplateError);
    }
    Map<String, List<List<String>>> tables = new LinkedHashMap<>();
    for (String templatePath : templatePaths) {
      tables.put(templatePath, TemplateHelper.readTable(templatePath));
    }

    OWLOntology outputOntology = TemplateOperation.template(tables, inputOntology, null, ioHelper);

    boolean collapseImports =
        CommandLineHelper.getBooleanValue(line, "collapse-import-closure", false);
    boolean includeAnnotations =
        CommandLineHelper.getBooleanValue(line, "include-annotations", false);

    // if the --ancestors option is used,
    // MIREOT in all the ancestors of the outputOntology terms
    // from the inputOntology, with just their labels.
    // Do not MIREOT the terms defined in the template,
    // just their dependencies!
    List<OWLOntology> ontologies;
    boolean hasAncestors = CommandLineHelper.getBooleanValue(line, "ancestors", false, true);
    if (hasAncestors && inputOntology != null) {
      Set<IRI> iris = OntologyHelper.getIRIs(outputOntology);
      iris.removeAll(TemplateHelper.getIRIs(tables, ioHelper));
      OWLOntology ancestors =
          MireotOperation.getAncestors(
              inputOntology, null, iris, MireotOperation.getDefaultAnnotationProperties());
      ontologies = new ArrayList<>();
      ontologies.add(ancestors);
      MergeOperation.mergeInto(ontologies, outputOntology, includeAnnotations, collapseImports);
    }

    // Either merge-then-save, save-then-merge, or don't merge
    ontologies = new ArrayList<>();
    ontologies.add(outputOntology);
    boolean mergeBefore = CommandLineHelper.getBooleanValue(line, "merge-before", false, true);
    boolean mergeAfter = CommandLineHelper.getBooleanValue(line, "merge-after", false, true);
    if (mergeBefore && mergeAfter) {
      throw new IllegalArgumentException(mergeError);
    }
    if (mergeBefore) {
      MergeOperation.mergeInto(ontologies, inputOntology, includeAnnotations, collapseImports);
      CommandLineHelper.maybeSaveOutput(line, inputOntology);
    } else if (mergeAfter) {
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      MergeOperation.mergeInto(ontologies, inputOntology, includeAnnotations, collapseImports);
    } else {
      // Set ontology and version IRI
      String ontologyIRI = CommandLineHelper.getOptionalValue(line, "ontology-iri");
      String versionIRI = CommandLineHelper.getOptionalValue(line, "version-iri");
      if (ontologyIRI != null || versionIRI != null) {
        OntologyHelper.setOntologyIRI(outputOntology, ontologyIRI, versionIRI);
      }
      CommandLineHelper.maybeSaveOutput(line, outputOntology);
      state.setOntology(outputOntology);
    }

    return state;
  }
}
