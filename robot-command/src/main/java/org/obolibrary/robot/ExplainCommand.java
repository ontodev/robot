package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxInlineAxiomParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
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

  private static final List<String> legal_modes =
      Arrays.asList("entailment", "inconsistency", "unsatisfiability");
  OWLDataFactory df = OWLManager.getOWLDataFactory();

  private static final String maxTypeError = NS + "MAX TYPE ERROR --max ('%s') must be an integer";
  private static final String inconsistentOntologyError =
      NS
          + "INCONSISTENT ONTOLOGY ERROR cannot generate explanations for inconsistent ontology. Consider using --inconsistent explain. Explanation: \n %s";
  private static final String illegalModeError =
      NS
          + "ILLEGAL EXPLANATION MODE ERROR: %s. Must be one of "
          + String.join(" ", legal_modes)
          + ".";
  private static final String missingAxiomArgumentError =
      NS + "MISSING AXIOM ARGUMENT ERROR: must have a valid --axiom.";
  private static final String illegalUnsatisfiableArgumentError =
      NS
          + "ILLEGAL UNSATISFIABLE ARGUMENT ERROR: %s. Must have either a valid --unsatisfiable option (all, root, most_general, random:n), where n is an integer.";

  /** Store the command-line options for the command. */
  private Options options;

  public ExplainCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("r", "reasoner", true, "reasoner to use: ELK, HermiT, JFact");
    o.addOption("a", "axiom", true, "the axiom to explain");
    o.addOption("m", "max", true, "the maximum number of explanations to retrieve");
    o.addOption(
        "u",
        "unsatisfiable",
        true,
        "optional list of unsatisfiable classes to explain: all, root, random:n");
    o.addOption(
        "M",
        "mode",
        true,
        "there are three modes for the explanation service: "
            + String.join(" ", legal_modes).trim()
            + ".");
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

    String maxString = CommandLineHelper.getDefaultValue(line, "max", "1");
    String modeString = CommandLineHelper.getDefaultValue(line, "mode", "entailment");

    final int max = getMaxExplanationValue(maxString);
    OWLReasonerFactory reasonerFactory = CommandLineHelper.getReasonerFactory(line, true);

    Set<Explanation<OWLAxiom>> explanations = new HashSet<>();

    switch (modeString) {
      case "inconsistency":
        explanations.addAll(handleInconsistentOntology(line, ontology, max, reasonerFactory));
        break;
      case "unsatisfiability":
        explanations.addAll(handleUnsatisfiableMode(line, ontology, max, reasonerFactory));
        break;
      case "entailment":
        explanations.addAll(handleEntailmentCheck(line, ontology, max, reasonerFactory));
        break;
      default:
        throw new IllegalStateException(String.format(illegalModeError, modeString));
    }

    state.setOntology(getExplanationOntology(ontology, explanations));
    CommandLineHelper.maybeSaveOutput(line, ontology);
    return state;
  }

  private Set<Explanation<OWLAxiom>> handleUnsatisfiableMode(
      CommandLine line, OWLOntology ontology, int max, OWLReasonerFactory reasonerFactory)
      throws IOException {
    OWLReasoner r = reasonerFactory.createReasoner(ontology);
    Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
    String unsatisfiableString = CommandLineHelper.getOptionalValue(line, "unsatisfiable");
    boolean listmode = false;
    if (unsatisfiableString != null) {
      switch (unsatisfiableString) {
        case "all":
          explanations.addAll(
              ExplainOperation.explainUnsatisfiableClasses(ontology, r, reasonerFactory, max));
          break;
        case "root":
          explanations.addAll(
              ExplainOperation.explainRootUnsatisfiableClasses(ontology, r, reasonerFactory, max));
          break;
        case "most_general":
          explanations.addAll(
              ExplainOperation.explainMostGeneralUnsatisfiableClasses(
                  ontology, r, reasonerFactory, max));
          break;
        case "list":
          handleListMode(line, r);
          listmode = true;
          break;
        default:
          if (unsatisfiableString.startsWith("random:")) {
            try {
              int maxUnsat = getMaxExplanationValue(unsatisfiableString.split(":")[1]);
              explanations.addAll(
                  ExplainOperation.explainUnsatisfiableClasses(
                      ontology, r, reasonerFactory, max, maxUnsat));
            } catch (Exception e) {
              throw new IllegalStateException(
                  String.format(illegalUnsatisfiableArgumentError, unsatisfiableString), e);
            }
          } else {
            throw new IllegalStateException(
                String.format(illegalUnsatisfiableArgumentError, unsatisfiableString));
          }
      }
    }
    if (!listmode) {
      // In listmode, the unsatisfiable classes would have already been written to file.
      if (line.hasOption("explanation")) {
        writeExplanationsToFile(line, ontology, ontology.getOWLOntologyManager(), explanations);
      }
    }
    return explanations;
  }

  private Set<Explanation<OWLAxiom>> handleEntailmentCheck(
      CommandLine line, OWLOntology ontology, int max, OWLReasonerFactory reasonerFactory)
      throws IOException {
    Set<Explanation<OWLAxiom>> explanations = new HashSet<>();

    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(CommandLineHelper.getIOHelper(line));
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(df.getRDFSLabel());
    checker.addAll(ontology);

    ManchesterOWLSyntaxInlineAxiomParser parser =
        new ManchesterOWLSyntaxInlineAxiomParser(df, checker);
    String expression = CommandLineHelper.getOptionalValue(line, "axiom");

    if (expression != null) {
      OWLAxiom axiom = parser.parse(expression);
      explanations.addAll(ExplainOperation.explain(axiom, ontology, reasonerFactory, max));
    } else {
      throw new IllegalStateException(missingAxiomArgumentError);
    }

    if (line.hasOption("explanation")) {
      writeExplanationsToFile(line, ontology, ontology.getOWLOntologyManager(), explanations);
    }
    return explanations;
  }

  private Set<Explanation<OWLAxiom>> handleInconsistentOntology(
      CommandLine line, OWLOntology ontology, int max, OWLReasonerFactory reasonerFactory)
      throws IOException {
    Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
    OWLReasoner r = reasonerFactory.createReasoner(ontology);
    if (!r.isConsistent()) {
      Set<Explanation<OWLAxiom>> inconsistencyExplanations =
          ExplainOperation.explainInconsistent(ontology, reasonerFactory, max);
      explanations.addAll(inconsistencyExplanations);
    } else {
      logger.info("Ontology consistent, nothing to be done.");
    }
    if (line.hasOption("explanation")) {
      writeExplanationsToFile(line, ontology, ontology.getOWLOntologyManager(), explanations);
    }
    return explanations;
  }

  private void handleListMode(CommandLine line, OWLReasoner r) throws IOException {
    if (line.hasOption("explanation")) {
      File output = new File(line.getOptionValue("explanation"));
      CURIEShortFormProvider curieProvider =
          new CURIEShortFormProvider(new IOHelper().getPrefixes());
      List<String> unsatisfiable_classes_iris = new ArrayList<>();
      r.getUnsatisfiableClasses()
          .getEntitiesMinusBottom()
          .forEach(e -> unsatisfiable_classes_iris.add(curieProvider.getShortForm(e)));
      FileUtils.writeLines(output, unsatisfiable_classes_iris);
    }
  }

  private OWLOntology getExplanationOntology(
      OWLOntology ontology, Set<Explanation<OWLAxiom>> explanations)
      throws OWLOntologyCreationException {
    Set<OWLAxiom> explanationsAxioms =
        explanations.stream().flatMap(e -> e.getAxioms().stream()).collect(Collectors.toSet());
    Set<IRI> explanationTerms =
        explanationsAxioms
            .stream()
            .flatMap(ax -> ax.getSignature().stream().map(OWLNamedObject::getIRI))
            .collect(Collectors.toSet());
    Set<OWLAnnotationAssertionAxiom> annotations =
        OntologyHelper.getAnnotationAxioms(
            ontology, Collections.singleton(df.getRDFSLabel()), explanationTerms);
    explanationsAxioms.addAll(annotations);
    return ontology.getOWLOntologyManager().createOntology(explanationsAxioms);
  }

  private void writeExplanationsToFile(
      CommandLine line,
      OWLOntology ontology,
      OWLOntologyManager man,
      Set<Explanation<OWLAxiom>> explanations)
      throws IOException {
    File output = new File(line.getOptionValue("explanation"));
    Map<OWLAxiom, Integer> mapMostUsedAxioms = new HashMap<>();
    explanations.forEach(e -> e.getAxioms().forEach(ax -> countUp(ax, mapMostUsedAxioms)));
    String result =
        explanations
            .stream()
            .map(e -> ExplainOperation.renderExplanationAsMarkdown(e, man))
            .collect(Collectors.joining("\n\n\n"));
    String summary = ExplainOperation.renderAxiomImpactSummary(mapMostUsedAxioms, ontology, man);
    Writer writer = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8);
    writer.write(result);
    if (!explanations.isEmpty()) {
      writer.write(summary);
    } else {
      writer.write("No explanations found.");
    }
    writer.close();
  }

  private void countUp(OWLAxiom ax, Map<OWLAxiom, Integer> mapMostUsedAxioms) {
    if (!mapMostUsedAxioms.containsKey(ax)) {
      mapMostUsedAxioms.put(ax, 0);
    }
    mapMostUsedAxioms.put(ax, mapMostUsedAxioms.get(ax) + 1);
  }

  private int getMaxExplanationValue(String maxString) throws Exception {
    final int max;
    try {
      max = Integer.parseInt(maxString);
    } catch (NumberFormatException e) {
      throw new Exception(String.format(maxTypeError, maxString));
    }
    return max;
  }
}
