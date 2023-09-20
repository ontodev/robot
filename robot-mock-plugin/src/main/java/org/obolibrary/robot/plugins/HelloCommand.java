package org.obolibrary.robot.plugins;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.obolibrary.robot.Command;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/** A dummy pluggable command for testing and demonstration purposes. */
public class HelloCommand implements Command {

  private Options options;

  public HelloCommand() {
    options = CommandLineHelper.getCommonOptions();
    options.addOption("i", "input", true, "load ontology from a file");
    options.addOption("I", "input-iri", true, "load ontology from an IRI");
    options.addOption("o", "output", true, "save ontology to a file");
    options.addOption("r", "recipient", true, "set the recipient of the hello message");
  }

  @Override
  public String getName() {
    return "hello";
  }

  @Override
  public String getDescription() {
    return "inject a hello annotation into the ontology";
  }

  @Override
  public String getUsage() {
    return "robot hello -r <RECIPIENT>";
  }

  @Override
  public Options getOptions() {
    return options;
  }

  @Override
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(e);
    }
  }

  @Override
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), options, args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);

    String recipient = line.getOptionValue("recipient", "World");

    OWLOntology ontology = state.getOntology();
    OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

    OWLAnnotation annot =
        factory.getOWLAnnotation(
            factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()),
            factory.getOWLLiteral(String.format("Hello, %s", recipient)));
    ontology.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ontology, annot));

    CommandLineHelper.maybeSaveOutput(line, state.getOntology());

    return state;
  }
}
