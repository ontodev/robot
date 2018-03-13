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

public class RemoveCommand implements Command {

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
    o.addOption("N", "all-individuals", true, "if true, remove all individuals");
    o.addOption("e", "entity", true, "remove an entity");
    o.addOption("E", "entities", true, "remove a set of entities");
    o.addOption(
        "d",
        "descendants-of-entity",
        true,
        "remove all descendants of an entity, without removing the entity");
    o.addOption(
        "D",
        "descendants-of-entities",
        true,
        "remove all descendants of a set of entities, without removing the entities");
    o.addOption(
        "s",
        "anonymous-classes",
        true,
        "remove anonymous classes associated with a class ('all' for all classes)");
    o.addOption("a", "entity-and-descendants", true, "remove an entity and its descendants");
    o.addOption(
        "A", "entities-and-descendants", true, "remove a set of entities and their descendants");
    options = o;

    Option a;

    // Filter on an annotation (PROP VAL)
    a = new Option("n", "remove entities with annotation PROP VAL");
    a.setLongOpt("annotation");
    a.setArgs(2);
    options.addOption(a);

    // Filter on a link annotation (PROP IRI)
    a = new Option("l", "remove entities with annotation PROP IRI");
    a.setLongOpt("link-annotation");
    a.setArgs(2);
    options.addOption(a);

    // Filter on a typed annotation (PROP VAL TYPE)
    a = new Option("t", "remove entities with annotation PROP VAL TYPE");
    a.setLongOpt("typed-annotation");
    a.setArgs(3);
    options.addOption(a);

    // Filter on a typed annotation (PROP VAL LANG)
    a = new Option("L", "remove entities with annotation PROP VAL LANG");
    a.setLongOpt("language-annotation");
    a.setArgs(3);
    options.addOption(a);
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

    OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();

    // Remove all individuals if true (defaults to false)
    if (CommandLineHelper.getBooleanValue(line, "all-individuals", false)) {
      RemoveOperation.removeIndividuals(ontology);
    }

    // Remove anonymous superclasses if requested (ID or 'all')
    String entityID = CommandLineHelper.getOptionalValue(line, "anonymous-classes");
    if (entityID != null) {
      if ("all".equalsIgnoreCase(entityID)) {
        RemoveOperation.removeAnonymousClasses(ontology);
      } else {
        IRI iri = CommandLineHelper.maybeCreateIRI(ioHelper, entityID, "anonymous-classes");
        RemoveOperation.removeAnonymousClasses(ontology, iri);
      }
    }

    Set<IRI> IRIs;

    // Remove descendants of entities
    IRIs =
        CommandLineHelper.getTerms(
            ioHelper, line, "descendants-of-entity", "descendants-of-entities");
    if (!IRIs.isEmpty()) {
      RemoveOperation.removeDescendants(ontology, IRIs);
    }

    // Remove entities, no descendants
    IRIs = CommandLineHelper.getTerms(ioHelper, line, "entity", "entities");
    if (!IRIs.isEmpty()) {
      RemoveOperation.remove(ontology, IRIs);
    }

    // Remove entities and descendants
    IRIs =
        CommandLineHelper.getTerms(
            ioHelper, line, "entity-and-descendants", "entities-and-descendants");
    if (!IRIs.isEmpty()) {
      RemoveOperation.remove(ontology, IRIs);
      RemoveOperation.removeDescendants(ontology, IRIs);
    }

    // Remove entities with annotations
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
        // TODO: Set up exception
      }
      OWLAnnotationProperty property =
          dataFactory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "annotation"));
      OWLAnnotationValue value = IOHelper.createLiteral(valueString);
      annotations.add(dataFactory.getOWLAnnotation(property, value));
    }

    // Add link annotations
    List<String> linkAnnotationItems = CommandLineHelper.getOptionValues(line, "link-annotation");
    while (linkAnnotationItems.size() > 0) {
      try {
        propertyString = linkAnnotationItems.remove(0);
        valueString = linkAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        // TODO: Set up exception
      }
      OWLAnnotationProperty property =
          dataFactory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "link-annotation"));
      OWLAnnotationValue value =
          CommandLineHelper.maybeCreateIRI(ioHelper, valueString, "link-annotation");
      annotations.add(dataFactory.getOWLAnnotation(property, value));
    }

    // Add typed annotations
    List<String> typedAnnotationItems = CommandLineHelper.getOptionValues(line, "typed-annotation");
    while (typedAnnotationItems.size() > 0) {
      try {
        propertyString = typedAnnotationItems.remove(0);
        valueString = typedAnnotationItems.remove(0);
        typeString = typedAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        // TODO
      }
      OWLAnnotationProperty property =
          dataFactory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "typed-annotation"));
      OWLAnnotationValue value = ioHelper.createTypedLiteral(valueString, typeString);
      annotations.add(dataFactory.getOWLAnnotation(property, value));
    }

    // Add language annotations
    List<String> languageAnnotationItems =
        CommandLineHelper.getOptionValues(line, "language-annotation");
    while (languageAnnotationItems.size() > 0) {
      try {
        propertyString = languageAnnotationItems.remove(0);
        valueString = languageAnnotationItems.remove(0);
        typeString = languageAnnotationItems.remove(0);
      } catch (IndexOutOfBoundsException e) {
        // TODO
      }
      OWLAnnotationProperty property =
          dataFactory.getOWLAnnotationProperty(
              CommandLineHelper.maybeCreateIRI(ioHelper, propertyString, "language-annotation"));
      OWLAnnotationValue value = ioHelper.createTypedLiteral(valueString, typeString);
      annotations.add(dataFactory.getOWLAnnotation(property, value));
    }

    RemoveOperation.removeWithAnnotations(ontology, annotations);

    CommandLineHelper.maybeSaveOutput(line, ontology);
    state.setOntology(ontology);
    return state;
  }
}
