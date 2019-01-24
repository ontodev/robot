package org.obolibrary.robot;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxInlineAxiomParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link ExplainOperation}.
 *
 * @author <a href="mailto:balhoff@renci.org">Jim Balhoff</a>
 */
public class ExplainCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExplainCommand.class);

  private static final String NS = "explain#";
  private static final String maxTypeError = NS + "MAX TYPE ERROR --max ('%s') must be an integer";

  /** Store the command-line options for the command. */
  private Options options;

  public ExplainCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("r", "reasoner", true, "reasoner to use: ELK, HermiT, JFact");
    o.addOption("a", "axiom", true, "the axiom to explain");
    o.addOption("m", "max", true, "the maximum number of explanations to retrieve");
    o.addOption("e", "explanation", true, "save explanation to a Markdown file");
    o.addOption("f", "format", true, "the format: obo, owl, ttl, owx, omn, ofn, json");
    o.addOption("o", "output", true, "save ontology containing only explanation axioms to a file");
    options = o;
  }

  @Override
  public String getName() {
    return "explain";
  }

  @Override
  public String getDescription() {
    return "explain derivation of an inferred axiom";
  }

  @Override
  public String getUsage() {
    return "robot explain --input <file> --axiom <axiom> --explanation <output>";
  }

  @Override
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations for the ExplainOperation.
   *
   * @param args strings to use as arguments
   */
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
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
    checker.addAll(ontology);

    String maxString = CommandLineHelper.getDefaultValue(line, "max", "1");
    final int max;
    try {
      max = Integer.parseInt(maxString);
    } catch (NumberFormatException e) {
      throw new Exception(String.format(maxTypeError, maxString));
    }
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);
    ManchesterOWLSyntaxInlineAxiomParser parser =
        new ManchesterOWLSyntaxInlineAxiomParser(OWLManager.getOWLDataFactory(), checker);
    String expression = CommandLineHelper.getRequiredValue(line, "axiom", "an --axiom is required");
    OWLAxiom axiom = parser.parse(expression);

    Set<Explanation<OWLAxiom>> explanations =
        ExplainOperation.explain(axiom, ontology, reasonerFactory, max);

    if (line.hasOption("explanation")) {
      File output = new File(line.getOptionValue("explanation"));
      String result =
          explanations
              .stream()
              .map(
                  e ->
                      ExplainOperation.renderExplanationAsMarkdown(
                          e, ontology.getOWLOntologyManager()))
              .collect(Collectors.joining("\n\n\n"));
      Writer writer = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8);
      writer.write(result);
      writer.close();
    }

    Set<OWLAxiom> explanationsAxioms =
        explanations.stream().flatMap(e -> e.getAxioms().stream()).collect(Collectors.toSet());
    Set<IRI> explanationTerms =
        explanationsAxioms
            .stream()
            .flatMap(ax -> ax.getSignature().stream().map(e -> e.getIRI()))
            .collect(Collectors.toSet());
    Set<OWLAnnotationAssertionAxiom> annotations =
        OntologyHelper.getAnnotationAxioms(
            ontology,
            Collections.singleton(OWLManager.getOWLDataFactory().getRDFSLabel()),
            explanationTerms);
    explanationsAxioms.addAll(annotations);
    state.setOntology(ontology.getOWLOntologyManager().createOntology(explanationsAxioms));
    CommandLineHelper.maybeSaveOutput(line, ontology);
    return state;
  }
}
