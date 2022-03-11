package org.obolibrary.robot;

import java.io.File;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Handles inputs and outputs for the {@link MigrateOperation}.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MigrateCommand implements Command {

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public MigrateCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("s", "source", true, "ontology file from which axioms should be migrated");
    o.addOption("x", "source-id", true, "ontology file from which axioms should be migrated");
    o.addOption("a", "axioms", true, "filter only for given axiom types");
    o.addOption("p", "properties", true, "list of annotation, data, object properties to include");
    o.addOption("t", "term", true, "term to migrate");
    o.addOption("T", "term-file", true, "load terms to migrate from a file");
    o.addOption("E", "exclude-terms", true, "set of terms in text file to exclude from migration");
    o.addOption(
        "A",
        "exclude-axioms",
        true,
        "a ROBOT template that contains all the axioms that should be excluded and the reasons for them.");
    o.addOption("m", "mappings", true, "Mapping file for renaming");
    o.addOption(
        "r",
        "reasoner",
        true,
        "reasoner to use: (ELK, HermiT). If set, ROBOT will check the exclusion list for entailments and print warnings.");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save updated metrics to a file");
    o.addOption(
        "u",
        "update-exclusion-reasons",
        true,
        "if true, add update the exclusion reason annotations.");
    o.addOption(
        "g",
        "tag-axioms-with-source",
        true,
        "if true, all migrated axioms are tagged with the source id.");

    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "slurp";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "migrates the axioms of one ontology to another";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot migrate --input <file> --mappings <file> --output <output>";
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
   * Handle the command-line and file operations for the MeasureOperation.
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
   * Given an input state and command line arguments, compute metrics in ontology. The input
   * ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the unchanged state
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);

    String mappingsFile = CommandLineHelper.getOptionalValue(line, "mappings");
    Map<String, String> mappings = new HashMap<>();
    if (mappingsFile != null) {
      char separator = RenameCommand.getSeparator(mappingsFile);
      mappings.putAll(RenameCommand.parseTableMappings(new File(mappingsFile), separator, true));
    }

    // String queryPath = CommandLineHelper.getOptionalValue(line, "query");
    // String query = FileUtils.readFileToString(new File(queryPath), Charset.defaultCharset());
    String excludedAxiomsTemplateFile = CommandLineHelper.getOptionalValue(line, "exclude-axioms");

    String sourceFile =
        CommandLineHelper.getRequiredValue(
            line, "source", "a source for the migration must be supplied.");
    OWLOntology sourceOntology = ioHelper.loadOntology(sourceFile);

    String source_id =
        CommandLineHelper.getDefaultValue(line, "source-id", new File(sourceFile).getName());
    boolean updateExclusionReasons =
        CommandLineHelper.getBooleanValue(line, "update-exclusion-reasons", false);
    boolean tagAxiomsWithSource =
        CommandLineHelper.getBooleanValue(line, "tag-axioms-with-source", true);
    boolean mergeSource = CommandLineHelper.getBooleanValue(line, "merge-source", true);

    OWLReasonerFactory reasonerFactory = null;
    if (line.hasOption("reasoner")) {
      reasonerFactory = CommandLineHelper.getReasonerFactory(line);
    }

    List<String> axiomSelectors = CommandLineHelper.cleanAxiomStrings(line);

    if (line.hasOption("property")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "property", "property-file");
      OntologyHelper.getEntities(sourceOntology, entityIRIs);
    }

    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);

    OWLOntology ontology = state.getOntology();

    Set<OWLEntity> excludeObjects = new HashSet<>();
    if (line.hasOption("exclude-terms")) {
      Set<IRI> excludeIRIs =
          CommandLineHelper.getTerms(ioHelper, line, "exclude-term", "exclude-terms");
      excludeObjects.addAll(OntologyHelper.getEntities(sourceOntology, excludeIRIs));
    }

    Set<OWLEntity> migrateTerms = new HashSet<>();
    if (line.hasOption("term") || line.hasOption("term-file")) {
      Set<IRI> entityIRIs = CommandLineHelper.getTerms(ioHelper, line, "term", "term-file");
      if (!entityIRIs.isEmpty()) {
        migrateTerms.addAll(OntologyHelper.getEntities(ontology, entityIRIs));
      }
    }

    OWLOntology excludeAxioms;
    if (excludedAxiomsTemplateFile != null) {
      OWLOntology emptyOntology = OWLManager.createOWLOntologyManager().createOntology();
      List<List<String>> templateTable =
          new ArrayList<>(TemplateHelper.readTable(excludedAxiomsTemplateFile));
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      tables.put(excludedAxiomsTemplateFile, templateTable);
      excludeAxioms =
          TemplateOperation.template(
              emptyOntology, ioHelper, tables, TemplateOperation.getDefaultOptions());
    } else {
      excludeAxioms = OWLManager.createOWLOntologyManager().createOntology();
    }

    OWLOntology migrated =
        MigrateOperation.migrate(
            ontology,
            sourceOntology,
            migrateTerms,
            source_id,
            mappings,
            axiomSelectors,
            excludeObjects,
            excludeAxioms,
            reasonerFactory,
            updateExclusionReasons,
            tagAxiomsWithSource,
            mergeSource,
            ioHelper);

    CommandLineHelper.maybeSaveOutput(line, migrated);
    state.setOntology(migrated);

    return state;
  }
}
