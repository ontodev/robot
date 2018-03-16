package org.obolibrary.robot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
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

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public FilterCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("e", "entity", true, "filter on an entity");
    o.addOption("E", "entities", true, "filter on a set of entities");
    o.addOption("d", "descendants-of-entity", true, "filter for the descendants of an entity");
    o.addOption(
        "D", "descendants-of-entities", true, "filter for the descendants of a set of entities");
    o.addOption("a", "entity-and-descendants", true, "filter for an entity and its descendants");
    o.addOption(
        "A",
        "entities-and-descendants",
        true,
        "filter for a set of entities and their descendants");
    options = o;

    Option a;

    // Filter on an annotation (PROP VAL)
    a = new Option("a", "filter on classes with annotation PROP VAL");
    a.setLongOpt("annotation");
    a.setArgs(2);
    options.addOption(a);

    // Filter on a link annotation (PROP IRI)
    a = new Option("l", "filter on classes with annotation PROP IRI");
    a.setLongOpt("link-annotation");
    a.setArgs(2);
    options.addOption(a);

    // Filter on a typed annotation (PROP VAL TYPE)
    a = new Option("t", "filter on classes with annotation PROP VAL TYPE");
    a.setLongOpt("typed-annotation");
    a.setArgs(3);
    options.addOption(a);
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
    OWLOntology ontology = state.getOntology();
    OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

    // Get IRIs of entities to filter
    Set<IRI> entityIRIs =
        OntologyHelper.filterExistingTerms(
            ontology, CommandLineHelper.getTerms(ioHelper, line, "entity", "entities"), true);
    Set<IRI> descendantIRIs =
        OntologyHelper.filterExistingTerms(
            ontology,
            CommandLineHelper.getTerms(
                ioHelper, line, "descendants-of-entity", "descendants-of-entities"),
            true);
    Set<IRI> nodeIRIs =
        OntologyHelper.filterExistingTerms(
            ontology,
            CommandLineHelper.getTerms(
                ioHelper, line, "entity-and-desendants", "entities-and-descendants"),
            true);
    FilterOperation.filter(ontology, entityIRIs, descendantIRIs, nodeIRIs);

    // Check for annotations to filter on
    Set<OWLAnnotation> annotations = new HashSet<>();
    List<String> annotationItems = CommandLineHelper.getOptionValues(line, "annotation");
    String propertyString = null;
    String valueString = null;
    String typeString = null;
    while (annotationItems.size() > 0) {
      try {
        propertyString = annotationItems.remove(0);
        valueString = annotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(annotationFormatError);
      }
      OWLAnnotationProperty property =
          factory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "annotation"));
      OWLAnnotationValue value =
          factory.getOWLLiteral(
              valueString, factory.getOWLDatatype(ioHelper.createIRI("rdf:PlainLiteral")));
      annotations.add(factory.getOWLAnnotation(property, value));
    }

    // Check for link annotations to filter on
    List<String> linkAnnotationItems = CommandLineHelper.getOptionValues(line, "link-annotation");
    while (linkAnnotationItems.size() > 0) {
      try {
        propertyString = linkAnnotationItems.remove(0);
        valueString = linkAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(linkAnnotationFormatError);
      }
      OWLAnnotationProperty property =
          factory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "link-annotation"));
      OWLAnnotationValue value =
          CommandLineHelper.maybeCreateIRI(ioHelper, valueString, "link-annotation");
      annotations.add(factory.getOWLAnnotation(property, value));
    }

    // Check for typed annotations to filter on
    List<String> typedAnnotationItems = CommandLineHelper.getOptionValues(line, "typed-annotation");
    while (typedAnnotationItems.size() > 0) {
      try {
        propertyString = typedAnnotationItems.remove(0);
        valueString = typedAnnotationItems.remove(0);
        typeString = typedAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(typedAnnotationFormatError);
      }
      OWLAnnotationProperty property =
          factory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "typed-annotation"));
      OWLAnnotationValue value = ioHelper.createTypedLiteral(valueString, typeString);
      annotations.add(factory.getOWLAnnotation(property, value));
    }

    // Check for language annotations to filter on
    List<String> languageAnnotationItems =
        CommandLineHelper.getOptionValues(line, "language-annotation");
    while (languageAnnotationItems.size() > 0) {
      try {
        propertyString = languageAnnotationItems.remove(0);
        valueString = languageAnnotationItems.remove(0);
        typeString = languageAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException(langAnnotationFormatError);
      }
      OWLAnnotationProperty property =
          factory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "language-annotation"));
      OWLAnnotationValue value = ioHelper.createTypedLiteral(valueString, typeString);
      annotations.add(factory.getOWLAnnotation(property, value));
    }
    OWLOntology outputOntology = FilterOperation.filterAnnotations(ontology, annotations);
    CommandLineHelper.maybeSaveOutput(line, outputOntology);
    return state;
  }
}
