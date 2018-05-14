package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add and remove annotations from an ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class AnnotateCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(AnnotateCommand.class);

  /** Namespace for error messages. */
  private static final String NS = "annotate#";

  /** Error message when --annotation is not a valid PROP VALUE. */
  private static final String annotationFormatError =
      NS + "ANNOTATION FORMAT ERROR each annotation must include PROP VALUE";

  /** Error message when --link-annotation is not a valid PROP LINK. */
  private static final String linkAnnotationFormatError =
      NS + "ANNOTATION FORMAT ERROR each link annotation must include PROP LINK";

  /** Error message when --language-annotation is not a valid PROP VALUE LANG. */
  private static final String langAnnotationFormatError =
      NS + "ANNOTATION FORMAT ERROR " + "each language annotation must include PROP VALUE LANG";

  /** Error message when --typed-annotation is not a valid PROP VALUE TYPE. */
  private static final String typedAnnotationFormatError =
      NS + "ANNOTATION FORMAT ERROR " + "each typed annotation must include PROP VALUE TYPE";

  /** Error message when --axiom-annotation is not a valid PROP VALUE. */
  private static final String axiomAnnotationFormatError =
      NS + "ANNOTATION FORMAT ERROR each axiom annotation must include PROP VALUE";

  /** Error message when there are no annotation inputs. */
  private static final String missingAnnotationError =
      NS
          + "MISSING ANNOTATION ERROR "
          + "at least one annotation option or annotation file is required";

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public AnnotateCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("A", "annotation-file", true, "add annotation from a file");
    o.addOption("O", "ontology-iri", true, "set the ontology IRI");
    o.addOption("V", "version-iri", true, "set the ontology version IRI");
    o.addOption("i", "input", true, "convert ontology from a file");
    o.addOption("I", "input-iri", true, "convert ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    options = o;

    // This option should be used with boolean values
    // Originally, this option had no args, so the arg is optional for backwards compatibility
    Option a =
        new Option("R", "remove-annotations", true, "remove all annotations on the ontology");
    a.setOptionalArg(true);
    o.addOption(a);

    // Annotate with a property and plain literal - expects 2 args
    a = new Option("a", "annotate ontology with PROP VALUE");
    a.setLongOpt("annotation");
    a.setArgs(2);
    options.addOption(a);

    // Annotate with a property and plain literal - expects 2 args
    a = new Option("k", "annotate ontology with PROP IRI");
    a.setLongOpt("link-annotation");
    a.setArgs(2);
    options.addOption(a);

    // Annotate with a property and a plain literal with a language tag - expects 3 args
    a = new Option("l", "annotate ontology with PROP VALUE LANG");
    a.setLongOpt("language-annotation");
    a.setArgs(3);
    options.addOption(a);

    // Annotate with a property and a typed literal with a language tag - expects 3 args
    a = new Option("t", "annotate ontology with PROP VALUE TYPE");
    a.setLongOpt("typed-annotation");
    a.setArgs(3);
    options.addOption(a);

    // Annotate with a property and a typed literal with a language tag - expects 3 args
    a = new Option("x", "annotate all axioms in the ontology " + "with PROP VALUE");
    a.setLongOpt("axiom-annotation");
    a.setArgs(3);
    options.addOption(a);
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "annotate";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "annotate ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot annotate --input <file> " + "--annotate <property> <value> " + "--output <file>";
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
   * Handle the command-line and file operations for the command.
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
   * Given an input state and command line arguments, add or remove ontology annotations and return
   * the modified state.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with the updated ontology
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

    boolean hasAnnotation = false;
    boolean removeAnnotations =
        CommandLineHelper.getBooleanValue(line, "remove-annotations", false);
    if (removeAnnotations) {
      hasAnnotation = true;
      OntologyHelper.removeOntologyAnnotations(ontology);
    }

    String property = null;
    String value = null;

    // Add annotations with PROP VALUE
    List<String> annotationItems = CommandLineHelper.getOptionValues(line, "annotation");
    while (annotationItems.size() > 0) {
      hasAnnotation = true;
      // Check for valid input
      try {
        property = annotationItems.remove(0);
        value = annotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(annotationFormatError);
      }
      IRI iri = CommandLineHelper.maybeCreateIRI(ioHelper, property, "property");
      OntologyHelper.addOntologyAnnotation(ontology, iri, IOHelper.createLiteral(value));
    }

    // Add link-annotations with PROP LINK
    List<String> linkItems = CommandLineHelper.getOptionValues(line, "link-annotation");
    while (linkItems.size() > 0) {
      hasAnnotation = true;
      // Check for valid input
      try {
        property = linkItems.remove(0);
        value = linkItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(linkAnnotationFormatError);
      }
      IRI propIRI = CommandLineHelper.maybeCreateIRI(ioHelper, property, "property");
      IRI valueIRI = CommandLineHelper.maybeCreateIRI(ioHelper, value, "value");
      OntologyHelper.addOntologyAnnotation(ontology, propIRI, valueIRI);
    }

    // Add language-annotations with PROP VALUE LANG
    List<String> langItems = CommandLineHelper.getOptionValues(line, "language-annotation");
    while (langItems.size() > 0) {
      hasAnnotation = true;
      // Check for valid input
      String lang = null;
      try {
        property = langItems.remove(0);
        value = langItems.remove(0);
        lang = langItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(langAnnotationFormatError);
      }
      IRI iri = CommandLineHelper.maybeCreateIRI(ioHelper, property, "property");
      OntologyHelper.addOntologyAnnotation(
          ontology, iri, IOHelper.createTaggedLiteral(value, lang));
    }

    // Add typed-annotations with PROP VALUE TYPE
    List<String> typedItems = CommandLineHelper.getOptionValues(line, "typed-annotation");
    while (typedItems.size() > 0) {
      hasAnnotation = true;
      // Check for valid input
      String type = null;
      try {
        property = typedItems.remove(0);
        value = typedItems.remove(0);
        type = typedItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(typedAnnotationFormatError);
      }
      IRI iri = CommandLineHelper.maybeCreateIRI(ioHelper, property, "property");
      OntologyHelper.addOntologyAnnotation(ontology, iri, ioHelper.createTypedLiteral(value, type));
    }

    // Add annotations with PROP VALUE
    List<String> axiomItems = CommandLineHelper.getOptionValues(line, "axiom-annotation");
    while (axiomItems.size() > 0) {
      hasAnnotation = true;
      // Check for valid input
      try {
        property = axiomItems.remove(0);
        value = axiomItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(axiomAnnotationFormatError);
      }
      IRI iri = CommandLineHelper.maybeCreateIRI(ioHelper, property, "property");
      OntologyHelper.addAxiomAnnotations(ontology, iri, IOHelper.createLiteral(value));
    }

    // Load any annotation files as ontologies and merge them in
    List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
    List<String> paths = CommandLineHelper.getOptionValues(line, "annotation-file");
    for (String path : paths) {
      ontologies.add(ioHelper.loadOntology(path));
    }
    if (ontologies.size() > 0) {
      hasAnnotation = true;
      MergeOperation.mergeInto(ontologies, ontology, true, false);
    }

    // Set ontology and version IRI
    String ontologyIRI = CommandLineHelper.getOptionalValue(line, "ontology-iri");
    String versionIRI = CommandLineHelper.getOptionalValue(line, "version-iri");
    if (ontologyIRI != null || versionIRI != null) {
      hasAnnotation = true;
      OntologyHelper.setOntologyIRI(ontology, ontologyIRI, versionIRI);
    }

    // Validate that annotations were provided
    if (!hasAnnotation) {
      throw new IllegalArgumentException(missingAnnotationError);
    }

    CommandLineHelper.maybeSaveOutput(line, ontology);

    return state;
  }
}
