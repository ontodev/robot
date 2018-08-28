package org.obolibrary.robot;

import java.io.File;
import java.io.FileWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.Profiles;

/** */
public class ValidateProfileCommand implements Command {

  /** Store the command-line options for the command. */
  private final Options options;

  /** Namespace for error messages. */
  private static final String NS = "validate#";

  /** Error message when a profile is not provided. */
  private static final String missingProfileError =
      NS + "MISSING PROFILE ERROR a profile is required";

  /** Error message when an invalid profile is provided. Expects profile. */
  private static final String invalidProfileError =
      NS + "INVALID PROFILE ERROR unknown profile: %s";

  /** Error message when the ontology validates provided profile. Expects ontology IRI, profile. */
  private static final String profileViolationError =
      NS + "PROFILE VIOLATION ERROR %s violates profile %s";

  /** Initialize the command. */
  public ValidateProfileCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("o", "output", true, "save validation report to a file");
    o.addOption("p", "profile", true, "OWL profile to validate (DL, EL, RL, QL, or Full)");
    o.addOption("i", "input", true, "validate ontology from a file");
    o.addOption("I", "input-iri", true, "validate ontology from an IRI");
    options = o;
  }

  @Override
  public String getName() {
    return "validate-profile";
  }

  @Override
  public String getDescription() {
    return "validate ontology against an OWL profile";
  }

  @Override
  public String getUsage() {
    return "robot validate-profile --profile <profile> --output <file>";
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
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }
    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();
    String profile = CommandLineHelper.getRequiredValue(line, "profile", missingProfileError);
    profile = profile.toUpperCase();
    final OWLProfile owlProfile;
    switch (profile) {
      case "DL":
        owlProfile = Profiles.OWL2_DL;
        break;
      case "EL":
        owlProfile = Profiles.OWL2_EL;
        break;
      case "RL":
        owlProfile = Profiles.OWL2_RL;
        break;
      case "QL":
        owlProfile = Profiles.OWL2_QL;
        break;
      case "FULL":
        owlProfile = new OWL2Profile();
        break; // #162
      default:
        throw new IllegalArgumentException(String.format(invalidProfileError, profile));
    }
    OWLProfileReport report = owlProfile.checkOntology(ontology);
    File outputFile = CommandLineHelper.getOutputFile(line);
    if (outputFile != null) {
      FileWriter writer = new FileWriter(outputFile);
      writer.write(report.toString());
      writer.close();
    } else {
      System.out.println(report.toString());
    }
    if (!report.isInProfile()) {
      throw new Exception(
          String.format(
              profileViolationError, ontology.getOntologyID().getOntologyIRI().orNull(), profile));
    }
    return state;
  }
}
