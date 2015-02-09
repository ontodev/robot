package owltools2;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import owltools2.Command;
import owltools2.CommandLineHelper;
import owltools2.IOHelper;
import owltools2.ExtractOperation;

/**
 * Implements the extract command.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractCommand implements Command {
  public static String name = "extract";
  public static String description = "extract terms from an ontology";
  public static String usage = "owltools2 extract --input-file <file> --term-file <file> --output <file>";

  protected Options options;

  public ExtractCommand() {
    options = CommandLineHelper.getCommonOptions();
    options.addOption("i", "input",      true,  "load ontology from a file");
    options.addOption("I", "input-iri",  true,  "load ontology from an IRI");
    options.addOption("o", "output",     true,  "save ontology to a file");
    options.addOption("O", "output-iri", true,  "set OntologyIRI for output");
    options.addOption("t", "terms",      true,  "space-separated terms to extract");
    options.addOption("T", "term-file",  true,  "load terms from a file");
    // TODO: options.addOption("m", "method",     true, "the extraction method");
  }

  /**
   * Handle CLI and IO for the ExtractOperation.
   * TODO: allow for different extraction methods
   */
  public void main(String[] args) {
    try {
      CommandLine line = CommandLineHelper.getCommandLine(usage, options, args);
      IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
      ioHelper.saveOntology(
        ExtractOperation.extract(
          CommandLineHelper.getInputOntology(ioHelper, line),
          CommandLineHelper.getTerms(ioHelper, line),
          CommandLineHelper.getOutputIRI(line)
        ),
        CommandLineHelper.getOutputFile(line)
      );
    } catch(Exception e) {
      CommandLineHelper.handleException(usage, options, e);
    }
  }
}
