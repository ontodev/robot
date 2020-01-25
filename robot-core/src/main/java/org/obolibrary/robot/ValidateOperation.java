package org.obolibrary.robot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the validate operation on a collection of tables
 *
 * @author <a href="mailto:consulting@michaelcuffaro.com">Michael E. Cuffaro</a>
 */
public class ValidateOperation {
  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "validate#";

  /** Given a map from IRIs to strings, return its inverse. */
  private static Map<String, IRI> reverse_iri_label_map(Map<IRI, String> source) {
    HashMap<String, IRI> target = new HashMap();
    for (Map.Entry<IRI, String> entry : source.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (target.containsKey(reverseKey)) {
        logger.warn(
            "Duplicate rdfs:label \"{}\". Overwriting value \"{}\" with \"{}\"",
            reverseKey,
            target.get(reverseKey),
            reverseValue);
      }
      target.put(reverseKey, reverseValue);
    }
    return target;
  }

  /**
   * Given a map representing a collection of tables to validate (with the filesystem paths to the
   * table functioning as keys), an ontology, a reasoner factory, an output directory, an output
   * format, and a boolean flag (`standalone`): Create a data factory, parser and reasoner based on
   * the ontology, as well as convenience maps from IRIs to labels and vice versa. Then, for each
   * table in the collection, create a validation instance based on the above, determine where the
   * validation output will be written to, and validate the table. The standalone flag indicates
   * that, if the output format is html, then the generated file should be a standalone HTML file
   * rather than a bare HTML table.
   */
  public static void validate(
      Map<String, List<List<String>>> tables,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      String outDir,
      String outFormat,
      boolean standalone)
      throws Exception {

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

    // Extract from the ontology two convenience maps from rdfs:labels to IRIs and vice versa:
    Map<IRI, String> iri_to_label_map = OntologyHelper.getLabels(ontology);
    Map<String, IRI> label_to_iri_map = reverse_iri_label_map(iri_to_label_map);

    // Validate all of the tables in turn:
    for (Map.Entry<String, List<List<String>>> table : tables.entrySet()) {
      String tblPath = table.getKey();
      List<List<String>> tblData = table.getValue();
      // Generate the output path to write the validation results to, based on the format and the
      // output directory provided. If no format is provided then we pass null as the output path to
      // the TableValidator, which should interpret that as a request to write to STDOUT.
      String outPath = null;
      if (outFormat != null) {
        outPath = outDir + "/" + FilenameUtils.getBaseName(tblPath) + "." + outFormat.toLowerCase();
      }

      TableValidator tableValidator =
          new TableValidator(
              ontology, parser, reasoner, dataFactory, iri_to_label_map, label_to_iri_map);
      tableValidator.validate(tblPath, tblData, standalone, outPath);
    }
  }
}
