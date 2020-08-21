package org.obolibrary.robot;

import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * Implements the validate operation on a collection of tables
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {

  /**
   * Return the default Validate options.
   *
   * @return map of default options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("format", null);
    options.put("standalone", "true");
    options.put("output-dir", null);
    options.put("silent", "true");
    options.put("errors", null);
    options.put("skip-row", "0");
    options.put("write-all", "false");
    return options;
  }

  /**
   * Validate tables based on an ontology.
   *
   * @param tables tables to validate (map of table name to table contents)
   * @param ontology OWLOntology to use to validate tables
   * @param ioHelper IOHelper to resolve entities
   * @param reasonerFactory OWLReasonerFactory to create reasoner
   * @param options map of validate options
   * @throws Exception on any problem
   */
  public static List<String> validate(
      Map<String, List<List<String>>> tables,
      OWLOntology ontology,
      IOHelper ioHelper,
      OWLReasonerFactory reasonerFactory,
      Map<String, String> options)
      throws Exception {
    if (options == null) {
      options = getDefaultOptions();
    }

    // Robot's custom quoted entity checker will be used for parsing class expressions:
    QuotedEntityChecker checker = new QuotedEntityChecker();
    // Add the class that will be used for I/O and for handling short-form IRIs by the quoted entity
    // checker:
    checker.setIOHelper(new IOHelper());
    checker.addProvider(new SimpleShortFormProvider());

    // Initialise the dataFactory and use it to add rdfs:label to the list of annotation properties
    // which will be looked up in the ontology by the quoted entity checker when finding names.
    OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(ontology);

    // Create the parser using the data factory and entity checker.
    ManchesterOWLSyntaxClassExpressionParser parser =
        new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);

    // Use the given reasonerFactory to initialise the reasoner based on the given ontology:
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    String outFormat = options.getOrDefault("format", null);
    String outDir = options.getOrDefault("output-dir", ".");

    TableValidator validator =
        new TableValidator(ontology, ioHelper, parser, reasoner, outFormat, outDir);

    boolean silent = OptionsHelper.optionIsTrue(options, "silent");
    if (silent && outFormat != null) {
      // Only toggle to silent if results are written to a file
      validator.toggleLogging();
    }

    // Run validation over all tables
    List<String> result = validator.validate(tables, options);

    // Maybe save errors to their own table
    String errorsPath = OptionsHelper.getOption(options, "errors", null);
    if (errorsPath != null) {
      List<String[]> errors = validator.getErrors();
      if (errors.size() > 1) {
        // Only one item in the errors means it is just the header
        IOHelper.writeTable(errors, errorsPath);
      }
    }
    return result;
  }
}
