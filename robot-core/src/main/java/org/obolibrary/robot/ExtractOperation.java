package org.obolibrary.robot;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Extract a set of OWLEntities from the input ontology to an output ontology. Uses the OWLAPI's
 * SyntacticLocalityModuleExtractor (SLME).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ExtractOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "extract#";

  /** Error message when the source map is not TSV or CSV. */
  private static final String invalidSourceMapError =
      NS + "INVALID SOURCE MAP ERROR --sources input must be .tsv or .csv";

  /** Error message when an invalid intermediates opiton is provided. */
  private static final String invalidIntermediatesError =
      NS + "UNKNOWN INTERMEDIATES ERROR '%s' is not a valid --intermediates arg";

  /** Shared data factory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** RDFS isDefinedBy annotation property. */
  private static OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

  public static Map<String, String> getDefaultOptions() {
    Map<String, String> extractOptions = new HashMap<>();
    extractOptions.put("copy-ontology-annotations", "false");
    extractOptions.put("annotate-with-source", "false");
    extractOptions.put("sources", null);
    extractOptions.put("intermediates", "all");
    extractOptions.put("method", "star");
    extractOptions.put("reasoner", "elk");
    return extractOptions;
  }

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME). The input ontology is not changed.
   *
   * @param inputOntology OWLOntology to extract from
   * @param terms Set of IRIs to extract
   * @param outputIRI IRI for output ontology
   * @param options Map of extract options, or null for defauls
   * @return OWLOntology extracted module
   * @throws OWLOntologyCreationException on problem creating new ontology
   */
  public static OWLOntology extract(
      OWLOntology inputOntology,
      IOHelper ioHelper,
      Set<IRI> terms,
      IRI outputIRI,
      Map<String, String> options)
      throws Exception {
    // Get options
    if (options == null) {
      options = getDefaultOptions();
    }

    String intermediates = OptionsHelper.getOption(options, "intermediates", "all").toLowerCase();
    boolean annotateSource = OptionsHelper.optionIsTrue(options, "annotate-with-source");
    String sourceMapPath = OptionsHelper.getOption(options, "sources");
    Map<IRI, IRI> sourceMap = new HashMap<>();
    if (sourceMapPath != null) {
      sourceMap = getSourceMap(ioHelper, sourceMapPath);
    }

    // Get the method for extraction
    String method = OptionsHelper.getOption(options, "method", "star").toLowerCase();
    ModuleType moduleType;
    switch (method) {
      case "star":
        moduleType = ModuleType.STAR;
        break;
      case "top":
        moduleType = ModuleType.TOP;
        break;
      case "bot":
        moduleType = ModuleType.BOT;
        break;
      default:
        moduleType = ModuleType.STAR;
    }

    // Extract based on the ModuleType
    SyntacticLocalityModuleExtractor extractor = getExtractor(inputOntology, moduleType);
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    Set<OWLEntity> entities = getEntities(inputOntology, terms);
    OWLOntology outputOntology = manager.createOntology(extractor.extract(entities), outputIRI);

    // Maybe add sources
    if (annotateSource) {
      Set<OWLAnnotationAxiom> sourceAxioms = new HashSet<>();
      for (OWLEntity entity : OntologyHelper.getEntities(outputOntology)) {
        // Check if rdfs:isDefinedBy already exists
        Set<OWLAnnotationValue> existingValues =
            OntologyHelper.getAnnotationValues(outputOntology, isDefinedBy, entity.getIRI());
        if (existingValues == null || existingValues.size() == 0) {
          // If not, add it
          sourceAxioms.add(getIsDefinedBy(entity, sourceMap));
        }
      }
      manager.addAxioms(outputOntology, sourceAxioms);
    }

    // Determine what to do based on intermediates
    if ("all".equalsIgnoreCase(intermediates)) {
      return outputOntology;
    } else if ("none".equalsIgnoreCase(intermediates)) {
      removeIntermediates(outputOntology, entities);
      return outputOntology;
    } else if ("minimal".equalsIgnoreCase(intermediates)) {
      OntologyHelper.collapseOntology(outputOntology, ioHelper, terms);
      return outputOntology;
    } else {
      throw new Exception(String.format(invalidIntermediatesError, intermediates));
    }
  }

  /**
   * Extract a set of terms from an ontology using the OWLAPI's SyntacticLocalityModuleExtractor
   * (SLME). The input ontology is not changed. Replaced by {@link #extract(OWLOntology, IOHelper,
   * Set, IRI, Map)}
   *
   * @param inputOntology the ontology to extract from
   * @param terms a set of IRIs for terms to extract
   * @param outputIRI the OntologyIRI of the new ontology
   * @param moduleType determines the type of extraction; defaults to STAR
   * @return a new ontology (with a new manager)
   * @throws OWLOntologyCreationException on any OWLAPI problem
   */
  public static OWLOntology extract(
      OWLOntology inputOntology, Set<IRI> terms, IRI outputIRI, ModuleType moduleType)
      throws OWLOntologyCreationException {
    // TODO - deprecate this class
    // We cannot easily replace the use of extract in ReasonerHelper.createIncoherentModule
    // as it will cause a breaking change (addition of Exception)
    Set<OWLEntity> entities = getEntities(inputOntology, terms);
    SyntacticLocalityModuleExtractor extractor = getExtractor(inputOntology, moduleType);
    return OWLManager.createOWLOntologyManager()
        .createOntology(extractor.extract(entities), outputIRI);
  }

  /**
   * Given an OWLEntity, return an OWLAnnotationAssertionAxiom indicating the source ontology with
   * rdfs:isDefinedBy.
   *
   * @param entity entity to get source of
   * @return OWLAnnotationAssertionAxiom with rdfs:isDefinedBy as the property
   */
  protected static OWLAnnotationAxiom getIsDefinedBy(OWLEntity entity, Map<IRI, IRI> sourceMap) {
    String iri = entity.getIRI().toString();
    IRI base;
    if (sourceMap != null && sourceMap.containsKey(entity.getIRI())) {
      // IRI exists in the prefixes
      base = sourceMap.get(entity.getIRI());
    } else {
      // Brute force edit the IRI string
      // Warning - this may not work with non-OBO Foundry terms, depending on the IRI format!
      if (iri.contains("#")) {
        final String s = iri.substring(0, iri.lastIndexOf("#")).toLowerCase();
        if (iri.contains(".owl#")) {
          base = IRI.create(s);
        } else {
          String baseStr = s + ".owl";
          base = IRI.create(baseStr);
        }
      } else if (iri.contains("_")) {
        String baseStr = iri.substring(0, iri.lastIndexOf("_")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      } else {
        String baseStr = iri.substring(0, iri.lastIndexOf("/")).toLowerCase() + ".owl";
        base = IRI.create(baseStr);
      }
    }
    return dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, entity.getIRI(), base);
  }

  /**
   * Given an ontology and a set of IRIs, get the OWLEntity objects for each IRI.
   *
   * @param ontology OWLOntology to get entities from
   * @param terms IRIs of entities
   * @return set of OWLEntities
   */
  private static Set<OWLEntity> getEntities(OWLOntology ontology, Set<IRI> terms) {
    Set<OWLEntity> entities = new HashSet<>();
    for (IRI term : terms) {
      entities.addAll(ontology.getEntitiesInSignature(term, Imports.INCLUDED));
    }
    return entities;
  }

  /**
   * Given a ontology and a ModuleType, get an SLME extractor.
   *
   * @param ontology OWLOntology to extract from
   * @param moduleType ModuleType for extractor
   * @return SLME object
   */
  private static SyntacticLocalityModuleExtractor getExtractor(
      OWLOntology ontology, ModuleType moduleType) {
    ModuleType type = moduleType;
    if (type == null) {
      type = ModuleType.STAR;
    }

    logger.debug("Extracting using method: " + type.name());

    return new SyntacticLocalityModuleExtractor(ontology.getOWLOntologyManager(), ontology, type);
  }

  /**
   * Given an input ontology, an extracted output ontology, and a set of entities, remove all
   * intermediates. This leaves only the classes directly used in the logic of any input entities.
   *
   * @param outputOntology extracted module
   * @param entities Set of extracted entities
   */
  private static void removeIntermediates(OWLOntology outputOntology, Set<OWLEntity> entities) {
    Set<OWLObject> precious = new HashSet<>();
    OWLOntologyManager manager = outputOntology.getOWLOntologyManager();
    for (OWLEntity e : entities) {
      if (!e.isOWLClass()) {
        continue;
      }
      OWLClass cls = e.asOWLClass();
      precious.add(cls);
      for (OWLClassExpression expr : EntitySearcher.getSuperClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
      for (OWLClassExpression expr : EntitySearcher.getEquivalentClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
      for (OWLClassExpression expr : EntitySearcher.getDisjointClasses(cls, outputOntology)) {
        precious.addAll(expr.getClassesInSignature());
      }
    }
    Set<OWLAxiom> removeAxioms =
        RelatedObjectsHelper.getPartialAxioms(
            outputOntology,
            RelatedObjectsHelper.selectClasses(
                RelatedObjectsHelper.selectComplement(outputOntology, precious)),
            null);
    manager.removeAxioms(outputOntology, removeAxioms);
  }

  /**
   * Given an IOHelper and the path to a term-to-source map, return a map of term IRI to source IRI.
   *
   * @param ioHelper IOHelper to handle prefixes
   * @param sourceMapPath path of the term-to-source map
   * @return map of term IRI to source IRI
   * @throws Exception on file reading issue
   */
  protected static Map<IRI, IRI> getSourceMap(IOHelper ioHelper, String sourceMapPath)
      throws Exception {
    File sourceMapFile = new File(sourceMapPath);
    if (!sourceMapFile.exists()) {
      throw new Exception(
          String.format(
              "MISSING FILE ERROR file %s for option %s does not exist",
              sourceMapPath, "--sources"));
    }

    char separator;
    if (sourceMapPath.endsWith(".tsv")) {
      separator = '\t';
    } else if (sourceMapPath.endsWith(".csv")) {
      separator = ',';
    } else {
      throw new Exception(invalidSourceMapError);
    }

    DefaultPrefixManager pm = ioHelper.getPrefixManager();

    Reader reader = new FileReader(sourceMapFile);
    CSVReader csv =
        new CSVReaderBuilder(reader)
            .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
            .build();
    // Skip first line
    csv.skip(1);

    Map<IRI, IRI> sourceMap = new HashMap<>();
    for (String[] line : csv) {
      IRI entity = ioHelper.createIRI(line[0]);

      // Maybe create a source IRI from a prefix
      // Otherwise the full IRI should be provided
      IRI source;
      String sourceStr = line[1];
      String namespace = pm.getPrefix(sourceStr + ":");
      if (namespace != null) {
        if (namespace.endsWith("_") || namespace.endsWith("#") || namespace.endsWith("/")) {
          namespace = namespace.substring(0, namespace.length() - 1);
        }
        source = IRI.create(namespace.toLowerCase() + ".owl");
      } else {
        source = IRI.create(sourceStr);
      }
      sourceMap.put(entity, source);
    }

    return sourceMap;
  }
}
