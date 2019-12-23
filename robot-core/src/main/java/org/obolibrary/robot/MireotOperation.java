package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Implements several variations on MIREOT, as first described in "MIREOT: The minimum information
 * to reference an external ontology term" (<a
 * href="http://dx.doi.org/10.3233/AO-2011-0087">link</a>).
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MireotOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MireotOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "extract#";

  /** Error message when only upper terms are specified with MIREOT. */
  private static final String missingLowerTermError =
      NS
          + "MISSING LOWER TERMS ERROR "
          + "lower term(s) must be specified with upper term(s) for MIREOT";

  /** Shared data factory. */
  private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

  /** RDFS isDefinedBy annotation property. */
  private static OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

  /**
   * Extract a module from an input ontology using the MIREOT method.
   *
   * @param inputOntology OWLOntology to extract from
   * @param lowerIRIs set of low-level IRIs
   * @param upperIRIs set of top-level IRIs
   * @param branchIRIs set of IRIs to branch from
   * @param extractOptions map of options
   * @param sourceMap map of entity IRI to source IRI
   * @return OWLOntology extracted module
   * @throws OWLOntologyCreationException on issue creating ontology
   */
  public static OWLOntology mireot(
      OWLOntology inputOntology,
      Set<IRI> lowerIRIs,
      Set<IRI> upperIRIs,
      Set<IRI> branchIRIs,
      Map<String, String> extractOptions,
      Map<IRI, IRI> sourceMap,
      Set<OWLAnnotationProperty> annotationProperties)
      throws OWLOntologyCreationException {
    List<OWLOntology> outputOntologies = new ArrayList<>();

    // First check for lower IRIs, upper IRIs can be null or not
    if (lowerIRIs != null) {
      outputOntologies.add(
          MireotOperation.getAncestors(
              inputOntology,
              upperIRIs,
              lowerIRIs,
              annotationProperties,
              extractOptions,
              sourceMap));
      // If there are no lower IRIs, there shouldn't be any upper IRIs
    } else if (upperIRIs != null) {
      throw new IllegalArgumentException(missingLowerTermError);
    }
    // Check for branch IRIs
    if (branchIRIs != null) {
      outputOntologies.add(
          MireotOperation.getDescendants(
              inputOntology, branchIRIs, annotationProperties, extractOptions, sourceMap));
    }

    return MergeOperation.merge(outputOntologies);
  }

  public static OWLOntology mireotFromConfig(IOHelper ioHelper, Map<String, List<String>> options)
      throws Exception {
    // Make sure we have terms to extract
    if (!options.containsKey("lower-terms")) {
      throw new Exception("The 'lower-terms' option is required for MIEROT!");
    }
    if (options.containsKey("terms")) {
      throw new Exception("The 'terms' option should not be used for MIREOT!");
    }

    // Get an input
    OWLOntology inputOntology = null;
    if (options.containsKey("input")) {
      String inputPath = options.get("input").get(0);
      inputOntology = ioHelper.loadOntology(inputPath);
    } else if (options.containsKey("input-iri")) {
      IRI inputIRI = IRI.create(options.get("input-iri").get(0));
      inputOntology = ioHelper.loadOntology(inputIRI);
    } else {
      throw new Exception("An input or input IRI is required!");
    }

    IRI inputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();

    // Maybe get a target
    OWLOntology target = null;
    if (options.containsKey("target")) {
      String targetPath = options.get("target").get(0);
      target = ioHelper.loadOntology(targetPath);
    } else if (options.containsKey("target-iri")) {
      IRI targetIRI = IRI.create(options.get("target-iri").get(0));
      target = ioHelper.loadOntology(targetIRI);
    }

    // Init the checker for label processing
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProperty(dataFactory.getRDFSLabel());
    checker.addAll(inputOntology);
    if (target != null) {
      checker.addAll(target);
    }

    Map<OWLEntity, Set<OWLEntity>> replaceParents = new HashMap<>();
    Map<IRI, IRI> sourceMap = new HashMap<>();

    // Parse terms
    Set<IRI> lowerTerms = new HashSet<>();
    for (String termLine : options.get("lower-terms")) {
      List<String> split = Lists.newArrayList(termLine.split("\t"));
      String termString = split.remove(0).trim();

      OWLEntity e = checker.getOWLEntity(termString);
      if (e == null) {
        logger.error(String.format("Unable to create entity from '%s'", termString));
        continue;
      }
      lowerTerms.add(e.getIRI());
      if (inputIRI != null) {
        sourceMap.put(e.getIRI(), inputIRI);
      }

      if (split.isEmpty()) {
        continue;
      }

      // IF there are remaining splits, add them as replacement parents
      Set<OWLEntity> replaceParentsForCurrent = new HashSet<>();
      for (String s : split) {
        if (s.trim().equals("")) {
          continue;
        }
        OWLEntity e2 = checker.getOWLEntity(termString);
        if (e2 == null) {
          logger.error(String.format("Unable to create entity from '%s'", termString));
          continue;
        }
        replaceParentsForCurrent.add(e2);
      }
      if (!replaceParentsForCurrent.isEmpty()) {
        replaceParents.put(e, replaceParentsForCurrent);
      }
    }

    Set<IRI> upperTerms = new HashSet<>();
    for (String termLine : options.getOrDefault("upper-terms", new ArrayList<>())) {
      List<String> split = Lists.newArrayList(termLine.split("\t"));
      String termString = split.remove(0).trim();

      OWLEntity e = checker.getOWLEntity(termString);
      if (e == null) {
        logger.error(String.format("Unable to create entity from '%s'", termString));
        continue;
      }
      upperTerms.add(e.getIRI());
      if (inputIRI != null) {
        sourceMap.put(e.getIRI(), inputIRI);
      }

      if (split.isEmpty()) {
        continue;
      }

      // IF there are remaining splits, add them as replacement parents
      Set<OWLEntity> replaceParentsForCurrent = new HashSet<>();
      for (String s : split) {
        if (s.trim().equals("")) {
          continue;
        }
        OWLEntity e2 = checker.getOWLEntity(termString);
        if (e2 == null) {
          logger.error(String.format("Unable to create entity from '%s'", termString));
          continue;
        }
        replaceParentsForCurrent.add(e2);
      }
      if (!replaceParentsForCurrent.isEmpty()) {
        replaceParents.put(e, replaceParentsForCurrent);
      }
    }

    // Parse annotation properties
    Set<OWLAnnotationProperty> annotationProperties = new HashSet<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> mapToAnnotations = new HashMap<>();
    Map<OWLAnnotationProperty, OWLAnnotationProperty> copyToAnnotations = new HashMap<>();
    for (String apLine : options.getOrDefault("annotations", new ArrayList<>())) {
      List<String> split = Lists.newArrayList(apLine.split("\t"));
      String apString = split.remove(0).trim();

      // Try to create an annotation property from the string
      OWLAnnotationProperty ap = checker.getOWLAnnotationProperty(apString);
      if (ap == null) {
        logger.error(String.format("Unable to create annotation property from '%s'", apString));
        continue;
      }
      annotationProperties.add(ap);

      if (split.isEmpty()) {
        continue;
      }

      String apOpt = split.remove(0);
      for (String s : split) {
        OWLAnnotationProperty ap2 = checker.getOWLAnnotationProperty(s);
        if (ap2 == null) {
          logger.error(String.format("Unable to create annotation property from '%s'", s));
          continue;
        }
        if (apOpt.equalsIgnoreCase("mapto")) {
          mapToAnnotations.put(ap, ap2);
        } else if (apOpt.equalsIgnoreCase("copyto")) {
          copyToAnnotations.put(ap, ap2);
        }
      }
    }

    // Map options from list to string
    Map<String, String> extractOptions = new HashMap<>();
    for (String key : ExtractOperation.getDefaultOptions().keySet()) {
      if (options.containsKey(key)) {
        String o = options.get(key).get(0);
        extractOptions.put(key, o);
      } else {
        extractOptions.put(key, ExtractOperation.getDefaultOptions().get(key));
      }
    }
    OWLOntology outputOntology =
        mireot(
            inputOntology,
            lowerTerms,
            upperTerms,
            null,
            extractOptions,
            sourceMap,
            annotationProperties);
    Set<IRI> allTerms = new HashSet<>(upperTerms);
    allTerms.addAll(lowerTerms);
    ExtractOperation.updateExtractedModule(
        outputOntology, allTerms, copyToAnnotations, mapToAnnotations, replaceParents);
    return outputOntology;
  }

  /**
   * Get a set of default annotation properties. Currenly includes only RDFS label.
   *
   * @return a set of annotation properties
   */
  public static Set<OWLAnnotationProperty> getDefaultAnnotationProperties() {
    Set<OWLAnnotationProperty> annotationProperties = new HashSet<>();
    annotationProperties.add(dataFactory.getRDFSLabel());
    return annotationProperties;
  }

  /**
   * Given an ontology, a set of upper-level IRIs, a set of lower-level IRIs, and a set of
   * annotation properties, return a new ontology with just the named ancestors of those terms,
   * their subclass relations, and the selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs ancestors will be copied up to and including these terms
   * @param lowerIRIs copy these terms and their superclasses
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getAncestors(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<IRI> lowerIRIs,
      Set<OWLAnnotationProperty> annotationProperties)
      throws OWLOntologyCreationException {
    return getAncestors(inputOntology, upperIRIs, lowerIRIs, annotationProperties, null, null);
  }

  /**
   * Given an input ontology, a set of upper IRIs, a set of lower IRIs, a set of annotation
   * properties (or null for all), and a map of extract options, get the ancestors of the lower IRIs
   * up to the upper IRIs. Include the specified annotation properties.
   *
   * @param inputOntology OWLOntology to extract from
   * @param upperIRIs top level IRIs
   * @param lowerIRIs bottom level IRIs
   * @param annotationProperties annotation properties to copy, or null for all
   * @param options map of extract options or null
   * @param inputSourceMap map of source IRIs to targets
   * @return extracted module
   * @throws OWLOntologyCreationException on problems creating the new ontology
   */
  public static OWLOntology getAncestors(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<IRI> lowerIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      Map<String, String> options,
      Map<IRI, IRI> inputSourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();

    // Get options
    if (options == null) {
      options = ExtractOperation.getDefaultOptions();
    }
    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    boolean annotateSource = OptionsHelper.optionIsTrue(options, "annotate-with-source");
    if (!annotateSource) {
      // Make sure to set source map to null if not annotate-with-source
      inputSourceMap = null;
    }

    // The other OWLAPI extract methods use the source ontology IRI
    // so we'll use it here too.
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());

    // Directly copy all upper entities
    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, inputSourceMap);
      }
    }

    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inputOntology);

    // For each lower entity, get the ancestors (all or none)
    Set<OWLEntity> lowerEntities = OntologyHelper.getEntities(inputOntology, lowerIRIs);
    for (OWLEntity entity : lowerEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if ("none".equals(intermediates)) {
        copyAncestorsNoIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            entity,
            entity,
            annotationProperties,
            inputSourceMap);
      } else {
        copyAncestorsAllIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            entity,
            annotationProperties,
            inputSourceMap);
      }
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, inputSourceMap);
      }
    }

    // Maybe remove unnecessary intermediates
    if (intermediates.equalsIgnoreCase("minimal")) {
      Set<IRI> precious = new HashSet<>();
      if (upperIRIs != null) {
        precious.addAll(upperIRIs);
      }
      precious.addAll(lowerIRIs);
      OntologyHelper.collapseOntology(outputOntology, precious);
    }

    return outputOntology;
  }

  /**
   * Given an ontology, a set of upper-level IRIs, a set of lower-level IRIs, a set of annotation
   * properties, and a boolean indiciating if rdfs:isDefinedBy should be added to copied classes,
   * return a new ontology with just the named ancestors of those terms, their subclass relations,
   * and the selected annotations. The input ontology is not changed.
   *
   * @deprecated replaced by {@link #getAncestors(OWLOntology, Set, Set, Set)}
   * @param inputOntology the ontology to extract from
   * @param upperIRIs ancestors will be copied up to and including these terms
   * @param lowerIRIs copy these terms and their superclasses
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   * @param sourceMap map of term IRI to source IRI
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  @Deprecated
  public static OWLOntology getAncestors(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<IRI> lowerIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createReasoner(inputOntology);

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    // The other OWLAPI extract methods use the source ontology IRI
    // so we'll use it here too.
    OWLOntology outputOntology = outputManager.createOntology(inputOntology.getOntologyID());

    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
    }

    Set<OWLEntity> lowerEntities = OntologyHelper.getEntities(inputOntology, lowerIRIs);
    for (OWLEntity entity : lowerEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
      copyAncestorsAllIntermediates(
          inputOntology,
          outputOntology,
          reasoner,
          upperEntities,
          entity,
          annotationProperties,
          sourceMap);
    }
    return outputOntology;
  }

  /**
   * Given an ontology, a set of upper-level IRIs, and a set of annotation properties, return a new
   * ontology with just those terms and their named descendants, their subclass relations, and the
   * selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs these terms and their descendants will be copied
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getDescendants(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<OWLAnnotationProperty> annotationProperties)
      throws OWLOntologyCreationException {
    return getDescendants(inputOntology, upperIRIs, annotationProperties, null, null);
  }

  /**
   * Given an ontology, a set of upper-level IRIs, and a set of annotation properties, return a new
   * ontology with just those terms and their named descendants, their subclass relations, and the
   * selected annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to extract from
   * @param upperIRIs these terms and their descendants will be copied
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @param options map of options
   * @param inputSourceMap map of source IRIs (or null)
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  public static OWLOntology getDescendants(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      Map<String, String> options,
      Map<IRI, IRI> inputSourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    // Get options
    if (options == null) {
      options = ExtractOperation.getDefaultOptions();
    }

    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    boolean annotateSource = OptionsHelper.optionIsTrue(options, "annotate-with-source");
    if (!annotateSource) {
      inputSourceMap = null;
    }

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology = outputManager.createOntology();

    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if ("none".equals(intermediates)) {
        copyDescendantsNoIntermediates(
            inputOntology, outputOntology, entity, entity, annotationProperties);
      } else {
        copyDescendantsAllIntermediates(
            inputOntology, outputOntology, entity, annotationProperties);
      }
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, inputSourceMap);
      }
    }

    if ("minimal".equalsIgnoreCase(intermediates)) {
      OntologyHelper.collapseOntology(outputOntology, upperIRIs);
    }

    return outputOntology;
  }

  /**
   * Given an ontology, a set of upper-level IRIs, and a set of annotation properties, return a new
   * ontology with just those terms and their named descendants, their subclass relations, and the
   * selected annotations. The input ontology is not changed.
   *
   * @deprecated replaced by {@link #getDescendants(OWLOntology, Set, Set, Map, Map)}
   * @param inputOntology the ontology to extract from
   * @param upperIRIs these terms and their descendants will be copied
   * @param annotationProperties the annotation properties to copy; if null, all will be copied
   * @param annotateSource if true, annotate copied classes with rdfs:isDefinedBy
   * @param sourceMap map of term IRI to source IRI
   * @return a new ontology with the target terms and their named ancestors
   * @throws OWLOntologyCreationException on problems creating new ontology
   */
  @Deprecated
  public static OWLOntology getDescendants(
      OWLOntology inputOntology,
      Set<IRI> upperIRIs,
      Set<OWLAnnotationProperty> annotationProperties,
      boolean annotateSource,
      Map<IRI, IRI> sourceMap)
      throws OWLOntologyCreationException {
    logger.debug("Extract with MIREOT ...");

    OWLOntologyManager outputManager = OWLManager.createOWLOntologyManager();
    OWLOntology outputOntology = outputManager.createOntology();

    Set<OWLEntity> upperEntities = OntologyHelper.getEntities(inputOntology, upperIRIs);
    for (OWLEntity entity : upperEntities) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      if (annotateSource) {
        maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
      }
      copyDescendantsAllIntermediates(inputOntology, outputOntology, entity, annotationProperties);
    }

    return outputOntology;
  }

  /**
   * Given input and output ontologies, a set of upper entitities, a target entity, and a set of
   * annotation properties, copy the target entity and all its named ancestors (recursively) from
   * the input ontology to the output ontology, along with the specified annotations. The input
   * ontology is not changed.
   *
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param reasoner OWLReasoner to get superclasses and superproperties while maintaining structure
   * @param upperEntities the top level of entities, or null
   * @param entity the target entity that will have its ancestors copied
   * @param annotationProperties the annotations to copy, or null for all
   */
  private static void copyAncestorsAllIntermediates(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLReasoner reasoner,
      Set<OWLEntity> upperEntities,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties,
      Map<IRI, IRI> sourceMap) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    // If this is an upperEntity, copy it and return.
    if (upperEntities != null && upperEntities.contains(entity)) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      return;
    }

    // Otherwise copy ancestors recursively.
    if (entity.isOWLClass()) {
      Set<OWLClass> superclasses =
          reasoner.getSuperClasses(entity.asOWLClass(), true).getFlattened();
      for (OWLClass superclass : superclasses) {
        OntologyHelper.copy(inputOntology, outputOntology, superclass, annotationProperties);
        outputManager.addAxiom(
            outputOntology, dataFactory.getOWLSubClassOfAxiom(entity.asOWLClass(), superclass));
        copyAncestorsAllIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            superclass,
            annotationProperties,
            sourceMap);
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> superProperties =
          EntitySearcher.getSuperProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty superProperty : superProperties) {
        OntologyHelper.copy(inputOntology, outputOntology, superProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                entity.asOWLAnnotationProperty(), superProperty));
        copyAncestorsAllIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            superProperty,
            annotationProperties,
            sourceMap);
      }
    } else if (entity.isOWLObjectProperty()) {
      Set<OWLObjectPropertyExpression> superProperties =
          reasoner.getSuperObjectProperties(entity.asOWLObjectProperty(), true).getFlattened();
      for (OWLObjectPropertyExpression superexpression : superProperties) {
        if (superexpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty superProperty = superexpression.asOWLObjectProperty();
        OntologyHelper.copy(inputOntology, outputOntology, superProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubObjectPropertyOfAxiom(
                entity.asOWLObjectProperty(), superProperty));
        copyAncestorsAllIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            superProperty,
            annotationProperties,
            sourceMap);
      }
    } else if (entity.isOWLDataProperty()) {
      Set<OWLDataProperty> superProperties =
          reasoner.getSuperDataProperties(entity.asOWLDataProperty(), true).getFlattened();
      for (OWLDataProperty superProperty : superProperties) {
        OntologyHelper.copy(inputOntology, outputOntology, superProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubDataPropertyOfAxiom(entity.asOWLDataProperty(), superProperty));
        copyAncestorsAllIntermediates(
            inputOntology,
            outputOntology,
            reasoner,
            upperEntities,
            superProperty,
            annotationProperties,
            sourceMap);
      }
    }

    // Annotate with rdfs:isDefinedBy (maybe)
    if (sourceMap != null) {
      maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
    }
  }

  /**
   * Given input and output ontologies, a set of upper entities, a target entity, a bottom entity
   * (from lower terms), and a set of annotation properties, copy the bottom entity and any
   * superclasses from the upper entities from the input ontology to the output ontology, along with
   * the specified annotations. No intermediate superclasses are included. The input ontology is not
   * changed.
   *
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param reasoner OWLReasoner to get superclasses and superproperties while maintaining structure
   * @param upperEntities the top level of entities, or null
   * @param entity the target entity to check if included in upper entities
   * @param bottomEntity the entity from lower terms to include
   * @param annotationProperties the annotations to copy, or null for all
   */
  private static void copyAncestorsNoIntermediates(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLReasoner reasoner,
      Set<OWLEntity> upperEntities,
      OWLEntity entity,
      OWLEntity bottomEntity,
      Set<OWLAnnotationProperty> annotationProperties,
      Map<IRI, IRI> sourceMap) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    // If there are no upperEntities or if this is an upperEntity, copy it and return
    if (upperEntities == null || upperEntities.contains(entity)) {
      OntologyHelper.copy(inputOntology, outputOntology, entity, annotationProperties);
      return;
    }

    // Otherwise find the highest level ancestor that was included in upper-terms
    if (entity.isOWLClass()) {
      Set<OWLClass> superclasses =
          reasoner.getSuperClasses(entity.asOWLClass(), true).getFlattened();
      for (OWLClass superclass : superclasses) {
        if (upperEntities.contains(superclass)) {
          OntologyHelper.copyAnnotations(inputOntology, outputOntology, entity, null);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubClassOfAxiom(bottomEntity.asOWLClass(), superclass));
        } else {
          copyAncestorsNoIntermediates(
              inputOntology,
              outputOntology,
              reasoner,
              upperEntities,
              superclass,
              bottomEntity,
              annotationProperties,
              sourceMap);
        }
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> superProperties =
          EntitySearcher.getSuperProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty superProperty : superProperties) {
        if (upperEntities.contains(superProperty)) {
          OntologyHelper.copyAnnotations(inputOntology, outputOntology, entity, null);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                  bottomEntity.asOWLAnnotationProperty(), superProperty));
        } else {
          copyAncestorsNoIntermediates(
              inputOntology,
              outputOntology,
              reasoner,
              upperEntities,
              superProperty,
              bottomEntity,
              annotationProperties,
              sourceMap);
        }
      }
    } else if (entity.isOWLObjectProperty()) {
      Set<OWLObjectPropertyExpression> superProperties =
          reasoner.getSuperObjectProperties(entity.asOWLObjectProperty(), true).getFlattened();
      for (OWLObjectPropertyExpression superexpression : superProperties) {
        if (superexpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty superProperty = superexpression.asOWLObjectProperty();
        if (upperEntities.contains(superProperty)) {
          OntologyHelper.copyAnnotations(inputOntology, outputOntology, entity, null);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubObjectPropertyOfAxiom(
                  bottomEntity.asOWLObjectProperty(), superProperty));
        } else {
          copyAncestorsNoIntermediates(
              inputOntology,
              outputOntology,
              reasoner,
              upperEntities,
              superProperty,
              bottomEntity,
              annotationProperties,
              sourceMap);
        }
      }
    } else if (entity.isOWLDataProperty()) {
      Set<OWLDataProperty> superProperties =
          reasoner.getSuperDataProperties(entity.asOWLDataProperty(), true).getFlattened();
      for (OWLDataProperty superProperty : superProperties) {
        if (upperEntities.contains(superProperty)) {
          OntologyHelper.copyAnnotations(inputOntology, outputOntology, entity, null);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubDataPropertyOfAxiom(
                  bottomEntity.asOWLDataProperty(), superProperty));
        } else {
          copyAncestorsNoIntermediates(
              inputOntology,
              outputOntology,
              reasoner,
              upperEntities,
              superProperty,
              bottomEntity,
              annotationProperties,
              sourceMap);
        }
      }
    }

    // Annotate with rdfs:isDefinedBy (maybe)
    if (sourceMap != null) {
      maybeAnnotateSource(outputOntology, outputManager, entity, sourceMap);
    }
  }

  /**
   * Given input and output ontologies, a target entity, and a set of annotation properties, copy
   * the target entity and all its named ancestors (recursively) from the input ontology to the
   * output ontology, along with the specified annotations. The input ontology is not changed.
   *
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param entity the target entity that will have its ancestors copied
   * @param annotationProperties the annotations to copy, or null for all
   */
  private static void copyDescendantsAllIntermediates(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    // Otherwise copy ancestors recursively.
    if (entity.isOWLClass()) {
      Collection<OWLClassExpression> subClasses =
          EntitySearcher.getSubClasses(entity.asOWLClass(), inputOntology);
      for (OWLClassExpression subExpression : subClasses) {
        if (subExpression.isAnonymous()) {
          continue;
        }
        OWLClass subClass = subExpression.asOWLClass();
        OntologyHelper.copy(inputOntology, outputOntology, subClass, annotationProperties);
        outputManager.addAxiom(
            outputOntology, dataFactory.getOWLSubClassOfAxiom(subClass, entity.asOWLClass()));
        copyDescendantsAllIntermediates(
            inputOntology, outputOntology, subClass, annotationProperties);
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> subProperties =
          EntitySearcher.getSubProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty subProperty : subProperties) {
        OntologyHelper.copy(inputOntology, outputOntology, subProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                subProperty, entity.asOWLAnnotationProperty()));
        copyDescendantsAllIntermediates(
            inputOntology, outputOntology, subProperty, annotationProperties);
      }
    } else if (entity.isOWLObjectProperty()) {
      Collection<OWLObjectPropertyExpression> superProperties =
          EntitySearcher.getSuperProperties(entity.asOWLObjectProperty(), inputOntology);
      for (OWLObjectPropertyExpression subExpression : superProperties) {
        if (subExpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty subProperty = subExpression.asOWLObjectProperty();
        OntologyHelper.copy(inputOntology, outputOntology, subProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubObjectPropertyOfAxiom(subProperty, entity.asOWLObjectProperty()));
        copyDescendantsAllIntermediates(
            inputOntology, outputOntology, subProperty, annotationProperties);
      }
    } else if (entity.isOWLDataProperty()) {
      Collection<OWLDataPropertyExpression> subProperties =
          EntitySearcher.getSubProperties(entity.asOWLDataProperty(), inputOntology);
      for (OWLDataPropertyExpression subExpression : subProperties) {
        OWLDataProperty subProperty = subExpression.asOWLDataProperty();
        OntologyHelper.copy(inputOntology, outputOntology, subProperty, annotationProperties);
        outputManager.addAxiom(
            outputOntology,
            dataFactory.getOWLSubDataPropertyOfAxiom(subProperty, entity.asOWLDataProperty()));
        copyDescendantsAllIntermediates(
            inputOntology, outputOntology, subProperty, annotationProperties);
      }
    }
  }

  /**
   * Given input and output ontologies, a top entity (from upper terms), a target entity, a and a
   * set of annotation properties, copy the bottom entity and any superclasses from the upper
   * entities from the input ontology to the output ontology, along with the specified annotations.
   * No intermediate superclasses are included. The input ontology is not changed.
   *
   * @param inputOntology the ontology to copy from
   * @param outputOntology the ontology to copy to
   * @param topEntity the entity for upper terms to include
   * @param entity the target entity to check if included in upper entities
   * @param annotationProperties the annotations to copy, or null for all
   */
  private static void copyDescendantsNoIntermediates(
      OWLOntology inputOntology,
      OWLOntology outputOntology,
      OWLEntity topEntity,
      OWLEntity entity,
      Set<OWLAnnotationProperty> annotationProperties) {
    OWLOntologyManager outputManager = outputOntology.getOWLOntologyManager();

    // Otherwise find the highest level ancestor that was included in upper-terms
    if (entity.isOWLClass()) {
      Collection<OWLClassExpression> subClasses =
          EntitySearcher.getSubClasses(entity.asOWLClass(), inputOntology);
      for (OWLClassExpression subExpression : subClasses) {
        if (subExpression.isAnonymous()) {
          continue;
        }
        OWLClass subClass = subExpression.asOWLClass();
        // Find out if this class has any subclasses
        Collection<OWLClassExpression> subSubClasses =
            EntitySearcher.getSubClasses(subClass, inputOntology);
        if (subSubClasses.isEmpty()) {
          OntologyHelper.copyAnnotations(
              inputOntology, outputOntology, entity, annotationProperties);
          outputManager.addAxiom(
              outputOntology, dataFactory.getOWLSubClassOfAxiom(subClass, topEntity.asOWLClass()));
        } else {
          copyDescendantsNoIntermediates(
              inputOntology, outputOntology, topEntity, subClass, annotationProperties);
        }
      }
    } else if (entity.isOWLAnnotationProperty()) {
      Collection<OWLAnnotationProperty> subProperties =
          EntitySearcher.getSubProperties(entity.asOWLAnnotationProperty(), inputOntology, true);
      for (OWLAnnotationProperty subProperty : subProperties) {
        // Find out if this property has any subproperties
        Collection<OWLAnnotationProperty> subSubProperties =
            EntitySearcher.getSubProperties(subProperty, inputOntology);
        if (subSubProperties.isEmpty()) {
          OntologyHelper.copyAnnotations(
              inputOntology, outputOntology, entity, annotationProperties);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubAnnotationPropertyOfAxiom(
                  subProperty, topEntity.asOWLAnnotationProperty()));
        } else {
          copyDescendantsNoIntermediates(
              inputOntology, outputOntology, topEntity, subProperty, annotationProperties);
        }
      }
    } else if (entity.isOWLObjectProperty()) {
      Collection<OWLObjectPropertyExpression> subProperties =
          EntitySearcher.getSubProperties(entity.asOWLObjectProperty(), inputOntology);
      for (OWLObjectPropertyExpression subExpression : subProperties) {
        if (subExpression.isAnonymous()) {
          continue;
        }
        OWLObjectProperty subProperty = subExpression.asOWLObjectProperty();
        // Find out if this property has any subproperties
        Collection<OWLObjectPropertyExpression> subSubProperties =
            EntitySearcher.getSubProperties(subProperty, inputOntology);
        if (subSubProperties.isEmpty()) {
          OntologyHelper.copyAnnotations(
              inputOntology, outputOntology, entity, annotationProperties);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubObjectPropertyOfAxiom(
                  subProperty, topEntity.asOWLObjectProperty()));
        } else {
          copyDescendantsNoIntermediates(
              inputOntology, outputOntology, topEntity, subProperty, annotationProperties);
        }
      }
    } else if (entity.isOWLDataProperty()) {
      Collection<OWLDataPropertyExpression> subProperties =
          EntitySearcher.getSubProperties(entity.asOWLDataProperty(), inputOntology);
      for (OWLDataPropertyExpression subExpression : subProperties) {
        OWLDataProperty subProperty = subExpression.asOWLDataProperty();
        // Find out if this property has any subproperties
        Collection<OWLDataPropertyExpression> subSubProperties =
            EntitySearcher.getSubProperties(subProperty, inputOntology);
        if (subSubProperties.isEmpty()) {
          OntologyHelper.copyAnnotations(
              inputOntology, outputOntology, entity, annotationProperties);
          outputManager.addAxiom(
              outputOntology,
              dataFactory.getOWLSubDataPropertyOfAxiom(subProperty, topEntity.asOWLDataProperty()));
        } else {
          copyDescendantsNoIntermediates(
              inputOntology, outputOntology, topEntity, subProperty, annotationProperties);
        }
      }
    }
  }

  /**
   * Given an ontology, a manager for that ontology, an entity to annotate, and a map of source
   * replacements, add the rdfs:isDefinedBy annotation to the entity.
   *
   * @param ontology OWLOntology to add annotation in
   * @param manager OWLManager for the ontology
   * @param entity OWLEntity to add annotation on
   * @param sourceMap term-to-source map
   */
  private static void maybeAnnotateSource(
      OWLOntology ontology, OWLOntologyManager manager, OWLEntity entity, Map<IRI, IRI> sourceMap) {
    Set<OWLAnnotationValue> existingValues =
        OntologyHelper.getAnnotationValues(ontology, isDefinedBy, entity.getIRI());
    if (existingValues == null || existingValues.size() == 0) {
      manager.addAxiom(ontology, ExtractOperation.getIsDefinedBy(entity, sourceMap));
    }
  }
}
