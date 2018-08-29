package org.obolibrary.robot;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxInlineAxiomParser;
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

  /** Store the command-line options for the command. */
  private Options options;

  public ExplainCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("r", "reasoner", true, "reasoner to use: ELK, HermiT, JFact");
    o.addOption("a", "axiom", true, "the axiom to explain");
    o.addOption("m", "max", true, "the maximum number of explanations to retrieve");
    o.addOption("f", "format", true, "the explanation result format: MD, OFN, etc.");
    o.addOption("o", "output", true, "save explanation to a file");
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
    return "robot explain --input <file> --reasoner <reasoner> --axiom <axiom> <output>";
  }

  @Override
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations for the relaxOperation.
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
    if (ontology != null) {
      checker.addAll(ontology);
    }
    int max = 1;
    if (line.hasOption("max")) {
      max = Integer.parseInt(line.getOptionValue("max"));
    }
    File output = CommandLineHelper.getOutputFile(line);
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);
    ManchesterOWLSyntaxInlineAxiomParser parser =
        new ManchesterOWLSyntaxInlineAxiomParser(OWLManager.getOWLDataFactory(), checker);
    String expression = line.getOptionValue("axiom");
    OWLAxiom axiom = parser.parse(expression);

    Set<Explanation<OWLAxiom>> explanations =
        ExplainOperation.explain(axiom, ontology, reasonerFactory, max);
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

    return state;
  }
}
