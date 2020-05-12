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

  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("format", null);
    options.put("standalone", "false");
    options.put("output-dir", null);
    return options;
  }

  public static void validate(
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
    boolean standalone = OptionsHelper.optionIsTrue(options, "standalone");

    TableValidator validator =
        new TableValidator(ontology, ioHelper, parser, reasoner, outFormat, outDir);
    validator.validate(tables, standalone);
  }
}
