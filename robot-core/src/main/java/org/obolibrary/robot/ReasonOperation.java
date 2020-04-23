package org.obolibrary.robot;

import static org.obolibrary.robot.reason.EquivalentClassReasoningMode.ALL;

import java.util.*;
import java.util.stream.Collectors;
import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.obolibrary.robot.exceptions.*;
import org.obolibrary.robot.reason.EquivalentClassReasoning;
import org.obolibrary.robot.reason.EquivalentClassReasoningMode;
import org.obolibrary.robot.reason.InferredSubClassAxiomGeneratorIncludingIndirect;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reason over an ontology and add axioms.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReasonOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReasonOperation.class);

  private static final String NS = "reason#";

  private static final String equivalentClassAxiomError =
      NS + "EQUIVALENT CLASS AXIOM ERROR one or more equivalent named classes were found.";

  /**
   * Return a map from option name to default option value, for all the available reasoner options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("remove-redundant-subclass-axioms", "true");
    options.put("create-new-ontology", "false");
    options.put("create-new-ontology-with-annotations", "false");
    options.put("annotate-inferred-axioms", "false");
    options.put("exclude-duplicate-axioms", "false");
    options.put("exclude-external-entities", "false");
    options.put("exclude-owl-thing", "false");
    options.put("equivalent-classes-allowed", ALL.written());
    options.put("prevent-invalid-references", "false");
    options.put("preserve-annotated-axioms", "false");
    options.put("dump-unsatisfiable", null);
    options.put("axiom-generators", "subclass");
    options.put("include-indirect", "false");
    options.put("exclude-tautologies", "false");

    return options;
  }

  /**
   * Given an ontology and a reasoner factory, return the ontology with inferred axioms added after
   * reasoning. Use default options.
   *
   * @param ontology the ontology to reason over
   * @param reasonerFactory the factory to create a reasoner instance from
   * @throws OntologyLogicException if the ontology contains unsatisfiable classes, properties or
   *     inconsistencies
   * @throws OWLOntologyCreationException if ontology cannot be created
   * @throws InvalidReferenceException if the reference checker fails
   */
  public static void reason(OWLOntology ontology, OWLReasonerFactory reasonerFactory)
      throws OntologyLogicException, OWLOntologyCreationException, InvalidReferenceException {
    reason(ontology, reasonerFactory, getDefaultOptions());
  }

  /**
   * Given an ontology, a reasoner factory, and a map of options, return the ontology with inferred
   * axioms added after reasoning.
   *
   * @param ontology the ontology to reason over
   * @param reasonerFactory the factory to create a reasoner instance from
   * @param options a map of option strings, or null
   * @throws OntologyLogicException if the ontology contains unsatisfiable classes, properties or
   *     inconsistencies
   * @throws OWLOntologyCreationException if ontology cannot be created
   * @throws InvalidReferenceException if the reference checker fails
   */
  public static void reason(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, Map<String, String> options)
      throws OntologyLogicException, OWLOntologyCreationException, InvalidReferenceException {
    logger.info("Ontology has {} axioms.", ontology.getAxioms().size());

    // Check the ontology for reference violations
    // Maybe fail if prevent-invalid-references
    checkReferenceViolations(ontology, options);

    // Get the reasoner and run initial reasoning
    // No axioms are asserted in this step
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    reason(ontology, reasoner, options);

    // Get the axiom generators
    // If none are provided, just default to subclass=
    List<InferredAxiomGenerator<? extends OWLAxiom>> gens = getInferredAxiomGenerators(options);

    // Assert inferred axioms in the ontology
    assertInferred(ontology, reasoner, gens, options);
  }

  /**
   * Create a tautology checker.
   *
   * @param structural if true, return null - we do not need a checker for the structural patterns
   * @return new OWLReasoner for empty ontology or null
   * @throws OWLOntologyCreationException on issue creating empty ontology
   */
  public static OWLReasoner getTautologyChecker(boolean structural)
      throws OWLOntologyCreationException {
    if (!structural) {
      OWLOntology empty = OWLManager.createOWLOntologyManager().createOntology();
      return new ReasonerFactory().createReasoner(empty);
    } else {
      return null;
    }
  }

  /**
   * Given an OWLAxiom, a tautology checker reasoner, and a boolean, determine if the axiom is a
   * tautology.
   *
   * @param axiom OWLAxiom to check
   * @param tautologyChecker OWLReasoner for empty ontology
   * @param structural if true, only check for hard-coded structural patterns (checker can be null)
   * @return true if axiom is tautological
   */
  public static boolean isTautological(
      OWLAxiom axiom, OWLReasoner tautologyChecker, boolean structural) {
    if (structural) {
      if (axiom instanceof OWLSubClassOfAxiom) {
        OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
        if (subClassOfAxiom.getSuperClass().isOWLThing()) {
          // X subClassOf owl:Thing
          return true;
        } else if (subClassOfAxiom.getSubClass().isOWLNothing()) {
          // owl:Nothing subClassOf X
          return true;
        } else {
          // X subClassOf X
          return subClassOfAxiom.getSubClass().equals(subClassOfAxiom.getSuperClass());
        }
      } else if (axiom instanceof OWLEquivalentClassesAxiom) {
        // X equivalentTo X
        OWLEquivalentClassesAxiom equivAxiom = (OWLEquivalentClassesAxiom) axiom;
        return equivAxiom.getClassExpressions().size() < 2;
      } else if (axiom instanceof OWLClassAssertionAxiom) {
        // X a owl:Thing
        OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
        return classAssertion.getClassExpression().isOWLThing();
      } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
        // X owl:topDataProperty ...
        OWLObjectPropertyAssertionAxiom assertion = (OWLObjectPropertyAssertionAxiom) axiom;
        return assertion.getProperty().isOWLTopObjectProperty();
      } else if (axiom instanceof OWLDataPropertyAssertionAxiom) {
        // X owl:topObjectProperty ...
        OWLDataPropertyAssertionAxiom assertion = (OWLDataPropertyAssertionAxiom) axiom;
        return assertion.getProperty().isOWLTopDataProperty();
      } else if (axiom instanceof OWLDeclarationAxiom) {
        // owl:Thing a owl:Class, owl:Nothing a owl:Class, etc.
        OWLDeclarationAxiom declaration = (OWLDeclarationAxiom) axiom;
        return declaration.getEntity().isBuiltIn();
      }
    } else if (tautologyChecker != null) {
      if (axiom instanceof OWLDeclarationAxiom) {
        // ignore declaration UNLESS it is built in
        OWLDeclarationAxiom declaration = (OWLDeclarationAxiom) axiom;
        return declaration.getEntity().isBuiltIn();
      } else if (axiom instanceof OWLLogicalAxiom) {
        // otherwise check if axiom is entailed by empty ontology
        return tautologyChecker.isEntailed(axiom);
      }
    }
    return false;
  }

  /**
   * Remove subClassAxioms where there is a more direct axiom, and the subClassAxiom does not have
   * any annotations.
   *
   * <p>Example: genotyping assay - asserted in dev: assay - inferred by reasoner: analyte assay -
   * asserted after fill: assay, analyte assay - asserted after removeRedundantSubClassAxioms:
   * analyte assay
   *
   * @param reasoner an OWL reasoner, initialized with a root ontology; the ontology will be
   *     modified
   */
  public static void removeRedundantSubClassAxioms(OWLReasoner reasoner) {
    removeRedundantSubClassAxioms(reasoner, null);
  }

  /**
   * Remove subClassAxioms where there is a more direct axiom, and the subClassAxiom does not have
   * any annotations.
   *
   * <p>Example: genotyping assay - asserted in dev: assay - inferred by reasoner: analyte assay -
   * asserted after fill: assay, analyte assay - asserted after removeRedundantSubClassAxioms:
   * analyte assay
   *
   * @param reasoner an OWL reasoner, initialized with a root ontology; the ontology will be
   *     modified
   * @param options map of options for reasoning
   */
  public static void removeRedundantSubClassAxioms(
      OWLReasoner reasoner, Map<String, String> options) {
    logger.info("Removing redundant subclass axioms...");
    OWLOntology ontology = reasoner.getRootOntology();
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();

    for (OWLClass thisClass : ontology.getClassesInSignature()) {
      if (thisClass.isOWLNothing() || thisClass.isOWLThing()) {
        continue;
      }

      // Use the reasoner to get all
      // the direct superclasses of this class.
      Set<OWLClass> inferredSuperClasses = new HashSet<>();
      for (Node<OWLClass> node : reasoner.getSuperClasses(thisClass, true)) {
        for (OWLClass inferredSuperClass : node) {
          inferredSuperClasses.add(inferredSuperClass);
        }
      }

      // For each subClassAxiom,
      // if the subclass axiom does not have any annotations,
      // and the superclass is named (not anonymous),
      // and the superclass is not in the set of inferred super classes,
      // then remove that axiom.
      for (OWLSubClassOfAxiom subClassAxiom : ontology.getSubClassAxiomsForSubClass(thisClass)) {
        if (OptionsHelper.optionIsTrue(options, "preserve-annotated-axioms")) {

          if (subClassAxiom.getAnnotations().size() > 0) {
            // TODO make this configurable
            continue;
          }
        }
        if (subClassAxiom.getSuperClass().isAnonymous()) {
          continue;
        }
        OWLClass assertedSuperClass = subClassAxiom.getSuperClass().asOWLClass();
        if (inferredSuperClasses.contains(assertedSuperClass)) {
          continue;
        }
        manager.removeAxiom(
            ontology, dataFactory.getOWLSubClassOfAxiom(thisClass, assertedSuperClass));
      }
    }
    logger.info("Ontology now has {} axioms.", ontology.getAxioms().size());
  }

  /**
   * Given an ontology, a reasoner, a list of axiom generators, and a map of options, assert the
   * inferred axioms in the ontology.
   *
   * @param ontology OWLOntology to assert new axioms in
   * @param reasoner OWLReasoner to use to assert axioms
   * @param gens InferredAxiomGenerators to use
   * @param options map of reason options
   * @throws OWLOntologyCreationException on problem creating a new axiom ontology
   */
  private static void assertInferred(
      OWLOntology ontology,
      OWLReasoner reasoner,
      List<InferredAxiomGenerator<? extends OWLAxiom>> gens,
      Map<String, String> options)
      throws OWLOntologyCreationException {
    long startTime = System.currentTimeMillis();
    // Create an ontology with ONLY the newly inferred axioms
    // Use this to check against the updated ontology with both inferred and asserted axioms
    OWLOntology newAxiomOntology = getNewAxiomOntology(ontology, reasoner, gens, options);

    // If create-new-ontology or create-new-ontology-with-annotations
    // remove the axioms from the input ontology (maybe keeping annotations)
    maybeCreateNewOntology(ontology, options);

    // Add the inferred axioms to input ontology from the new axiom ontology
    addInferredAxioms(ontology, newAxiomOntology, options);

    // Maybe use the reasoner to remove redundant subclass axioms (reduce)
    if (OptionsHelper.optionIsTrue(options, "remove-redundant-subclass-axioms")) {
      removeRedundantSubClassAxioms(reasoner, options);
    }
    logger.info("Ontology has {} axioms after all reasoning steps.", ontology.getAxioms().size());

    float elapsedTime = System.currentTimeMillis() - startTime;
    long seconds = (long) Math.ceil(elapsedTime / 1000);
    logger.info("Filling took {} seconds.", seconds);
  }

  /**
   * Given a map of reasoner options, return a list of inferred axiom generators.
   *
   * @param options Map of reasoner options
   * @return list of InferredAxiomGenerators
   */
  private static List<InferredAxiomGenerator<? extends OWLAxiom>> getInferredAxiomGenerators(
      Map<String, String> options) {
    String axGeneratorString = OptionsHelper.getOption(options, "axiom-generators", "subclass");
    List<String> axGenerators = Arrays.asList(axGeneratorString.split(" "));
    boolean direct = !OptionsHelper.optionIsTrue(options, "include-indirect");
    List<InferredAxiomGenerator<? extends OWLAxiom>> gens =
        ReasonerHelper.getInferredAxiomGenerators(axGenerators, direct);
    logger.info("Using these axiom generators:");
    for (InferredAxiomGenerator<?> inf : gens) {
      logger.info("    " + inf);
    }
    return gens;
  }

  /**
   * Given an ontology, a reasoner, a list of axiom generators, and map of reasoner options, create
   * a new ontology containing only the newly generated axioms. If direct (in options) is false,
   * include indirect axioms.
   *
   * @param ontology OWLOntology to reason over
   * @param reasoner OWLReasoner to use
   * @param gens list of InferredAxiomGenerators to generate axioms
   * @param options reasoner options
   * @return ontology containing newly generated axioms
   * @throws OWLOntologyCreationException on problem creating new ontology
   */
  private static OWLOntology getNewAxiomOntology(
      OWLOntology ontology,
      OWLReasoner reasoner,
      List<InferredAxiomGenerator<? extends OWLAxiom>> gens,
      Map<String, String> options)
      throws OWLOntologyCreationException {
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();
    boolean direct = !OptionsHelper.optionIsTrue(options, "include-indirect");
    boolean subClass =
        gens.stream()
            .anyMatch(
                g ->
                    (g instanceof InferredSubClassAxiomGenerator)
                        || (g instanceof InferredSubClassAxiomGeneratorIncludingIndirect));

    // we first place all inferred axioms into a new ontology;
    // these will be later transferred into the main ontology,
    // unless the create new ontology option is passed
    OWLOntology newAxiomOntology = manager.createOntology();
    InferredOntologyGenerator generator = new InferredOntologyGenerator(reasoner, gens);
    generator.fillOntology(dataFactory, newAxiomOntology);

    // If EMR, add expressions instead of just classes, etc...
    if (reasoner instanceof ExpressionMaterializingReasoner) {
      logger.info("Creating expression materializing reasoner...");
      ExpressionMaterializingReasoner emr = (ExpressionMaterializingReasoner) reasoner;
      emr.materializeExpressions();
      // Maybe add direct/indirect class expressions
      if (subClass) {
        for (OWLClass c : ontology.getClassesInSignature(Imports.INCLUDED)) {
          // Look at the superclasses because otherwise we would lose the anonymous exprs
          Set<OWLClassExpression> sces = emr.getSuperClassExpressions(c, direct);
          for (OWLClassExpression sce : sces) {
            if (!sce.getSignature().contains(dataFactory.getOWLThing())) {
              OWLAxiom ax = dataFactory.getOWLSubClassOfAxiom(c, sce);
              logger.debug("NEW:" + ax);
              manager.addAxiom(newAxiomOntology, ax);
            }
          }
        }
      }
    }

    return newAxiomOntology;
  }

  /**
   * Given an input ontology, a new axiom ontology (created from reasoning), and a map of options,
   * maybe remove asserted axioms from the input ontology if create-new-ontology or
   * create-new-ontology-with-annotations. The input ontology is updated in place.
   *
   * @param ontology OWLOntology to maybe update
   * @param options Map of reason options
   */
  private static void maybeCreateNewOntology(OWLOntology ontology, Map<String, String> options) {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    // If we need to "create" a new ontology,
    // we actually just remove any asserted axioms from the input ontology
    if (OptionsHelper.optionIsTrue(options, "create-new-ontology")
        || OptionsHelper.optionIsTrue(options, "create-new-ontology-with-annotations")) {
      if (OptionsHelper.optionIsTrue(options, "create-new-ontology-with-annotations")) {
        logger.info("Placing inferred axioms with annotations into a new ontology");
        manager.removeAxioms(
            ontology,
            ontology
                .getAxioms()
                .stream()
                .filter(nonap -> !(nonap instanceof OWLAnnotationAssertionAxiom))
                .collect(Collectors.toSet()));

      } else {
        logger.info("Placing inferred axioms into a new ontology");
        manager.removeAxioms(ontology, ontology.getAxioms());
      }

      Set<OWLImportsDeclaration> oids = ontology.getImportsDeclarations();
      for (OWLImportsDeclaration oid : oids) {
        RemoveImport ri = new RemoveImport(ontology, oid);
        manager.applyChange(ri);
      }
    }
  }

  /**
   * Given an ontology, an ontology containing only inferred axioms, and a map of options, add the
   * inferred axioms to the input ontology. Updates the input ontology in place.
   *
   * @param ontology OWLOntology to add inferred axioms to
   * @param newAxiomOntology OWLOntology containing inferred axioms
   * @param options Map of reason options
   */
  private static void addInferredAxioms(
      OWLOntology ontology, OWLOntology newAxiomOntology, Map<String, String> options)
      throws OWLOntologyCreationException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();

    // cache the complete set of asserted axioms at the initial state.
    // we will later use this if the -x option is passed, to avoid
    // asserting inferred axioms that are duplicates of existing axioms
    Set<OWLAxiom> existingAxioms = ontology.getAxioms(Imports.INCLUDED);

    // get all entities that 'belong' to the main ontology
    // see: https://github.com/ontodev/robot/issues/296
    Set<OWLEntity> ontologyEntities =
        ontology
            .getAxioms(AxiomType.DECLARATION)
            .stream()
            .map(OWLDeclarationAxiom::getEntity)
            .collect(Collectors.toSet());

    // Get a property and value to annotate inferred axioms
    IRI propertyIRI = null;
    OWLAnnotationValue value = null;
    if (OptionsHelper.optionIsTrue(options, "annotate-inferred-axioms")) {
      // the default is the convention used by OWLTools and GO, which is
      // the property is_inferred with a literal (note: not xsd) "true"
      propertyIRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#is_inferred");
      value = dataFactory.getOWLLiteral("true");
    }

    // If we will need a tautology checker, create it only once
    String tautologiesOption = OptionsHelper.getOption(options, "exclude-tautologies", "false");
    boolean excludeTautologies = !tautologiesOption.equalsIgnoreCase("false");
    boolean structural = tautologiesOption.equalsIgnoreCase("structural");
    OWLReasoner tautologyChecker = getTautologyChecker(structural);

    // Look at each inferred axiom
    // Check the options, and maybe add the inferred axiom to the ontology
    for (OWLAxiom a : newAxiomOntology.getAxioms()) {

      if (OptionsHelper.optionIsTrue(options, "exclude-external-entities")) {
        if (a instanceof OWLClassAxiom) {
          boolean overlapsSignature = false;
          // Check that each class in the axiom is DECLARED in the input ontology
          for (OWLClass c : a.getClassesInSignature()) {
            if (ontologyEntities.contains(c)) {
              overlapsSignature = true;
              break;
            }
          }
          // If one or more class(es) is not in the input ontology, skip this axiom
          if (!overlapsSignature) {
            logger.debug("Excluding axiom as class signatures do not overlap: " + a);
            continue;
          }
        }
      }

      if (OptionsHelper.optionIsTrue(options, "exclude-duplicate-axioms")) {
        // if this option is passed, do not add any axioms that are
        // duplicates of existing axioms present at initial state.
        // It may seem this is redundant with the
        // remove-redundant-axioms step, but this is not always the
        // case, particularly when the -n option is used.
        // See: https://github.com/ontodev/robot/issues/85

        // TODO to a check that ignores annotations
        if (existingAxioms.contains(a)) {
          logger.debug("Already have: " + a);
          continue;
        }
      }

      if (OptionsHelper.optionIsTrue(options, "exclude-duplicate-axioms")
          || OptionsHelper.optionIsTrue(options, "exclude-owl-thing")) {
        if (a.containsEntityInSignature(dataFactory.getOWLThing())) {
          // If axiom contains owl:Thing, skip it
          logger.debug("Ignoring trivial axioms with " + "OWLThing in signature: " + a);
          continue;
        }
      }

      // Maybe exclude tautologies
      if (excludeTautologies && isTautological(a, tautologyChecker, structural)) {
        continue;
      }

      // If the axiom has not been skipped, add it to the ontology
      manager.addAxiom(ontology, a);
      // If propertyIRI isn't null, we are annotating the inferred axioms
      // Add that annotation here
      if (propertyIRI != null) {
        OntologyHelper.addAxiomAnnotation(ontology, a, propertyIRI, value);
      }
    }
  }

  /**
   * Given an ontology and a map of options, find any reference violations in the ontology. If
   * prevent-invalid-references, fail on any invalid reference violations.
   *
   * @param ontology OWLOntology to check
   * @param options map of reason options
   * @throws InvalidReferenceException on invalid reference, if prevent-invalid-references
   */
  private static void checkReferenceViolations(OWLOntology ontology, Map<String, String> options)
      throws InvalidReferenceException {
    Set<InvalidReferenceViolation> referenceViolations =
        InvalidReferenceChecker.getInvalidReferenceViolations(ontology, false);
    Set<InvalidReferenceViolation> filteredViolations = new HashSet<>();

    if (referenceViolations.size() > 0) {
      for (InvalidReferenceViolation v : referenceViolations) {
        // Don't log errors for:
        // - annotations
        // - subclass of ObsoleteClass
        // - subproperty of ObsoleteProperty
        // - rdf:type owl:Thing
        if (v.getAxiom() instanceof OWLAnnotationAxiom) {
          continue;
        }
        if (v.getAxiom() instanceof OWLSubClassOfAxiom) {
          OWLSubClassOfAxiom sub = (OWLSubClassOfAxiom) v.getAxiom();
          OWLClassExpression sup = sub.getSuperClass();
          if (!sup.isAnonymous()
              && sup.asOWLClass()
                  .getIRI()
                  .toString()
                  .equals("http://www.geneontology.org/formats/oboInOwl#ObsoleteClass")) {
            continue;
          }
        }
        if (v.getAxiom() instanceof OWLSubObjectPropertyOfAxiom) {
          OWLSubObjectPropertyOfAxiom sub = (OWLSubObjectPropertyOfAxiom) v.getAxiom();
          OWLObjectPropertyExpression sup = sub.getSuperProperty();
          if (!sup.isAnonymous()
              && sup.asOWLObjectProperty()
                  .getIRI()
                  .toString()
                  .equals("http://www.geneontology.org/formats/oboInOwl#ObsoleteProperty")) {
            continue;
          }
        }
        if (v.getAxiom() instanceof OWLClassAssertionAxiom) {
          OWLClassAssertionAxiom sub = (OWLClassAssertionAxiom) v.getAxiom();
          if (sub.getClassExpression().isOWLThing()) {
            continue;
          }
        }

        filteredViolations.add(v);
      }
    }

    if (filteredViolations.size() > 0) {
      logger.warn(
          "Reference violations found: "
              + filteredViolations.size()
              + " - reasoning may be incomplete");

      int maxDanglings = 10;
      int danglings = 0;
      for (InvalidReferenceViolation v : filteredViolations) {
        if (v.getCategory().equals(InvalidReferenceViolation.Category.DANGLING)
            && danglings < maxDanglings) {
          logger.warn("Reference violation: " + v);
          danglings++;
        } else if (!v.getCategory().equals(InvalidReferenceViolation.Category.DANGLING)) {
          logger.warn("Reference violation: " + v);
        }
      }

      if (OptionsHelper.optionIsTrue(options, "prevent-invalid-references")) {
        throw new InvalidReferenceException(referenceViolations);
      }
    }
  }

  /**
   * Given an ontology, a reasoner, and a map of options, use the reasoner to validate the ontology,
   * compute class hierarchy, and find equivalencies.
   *
   * @param ontology OWLOntology to reason over
   * @param reasoner OWLReasoner to use
   * @param options Map of reason options
   * @throws OntologyLogicException on invalid ontology
   */
  private static void reason(
      OWLOntology ontology, OWLReasoner reasoner, Map<String, String> options)
      throws OntologyLogicException {
    long startTime = System.currentTimeMillis();
    logger.info("Starting reasoning...");

    // Validate and maybe dump the unsat classes into a file
    String dumpFilePath = OptionsHelper.getOption(options, "dump-unsatisfiable", null);
    ReasonerHelper.validate(reasoner, dumpFilePath);

    logger.info("Precomputing class hierarchy...");
    reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

    EquivalentClassReasoningMode mode =
        EquivalentClassReasoningMode.from(options.getOrDefault("equivalent-classes-allowed", ""));
    logger.info("Finding equivalencies...");

    EquivalentClassReasoning equivalentReasoning =
        new EquivalentClassReasoning(ontology, reasoner, mode);
    boolean passesEquivalenceTests = equivalentReasoning.reason();
    equivalentReasoning.logReport(logger);
    if (!passesEquivalenceTests) {
      throw new OntologyLogicException(equivalentClassAxiomError);
    }

    float elapsedTime = System.currentTimeMillis() - startTime;
    long seconds = (int) Math.ceil(elapsedTime / 1000);
    logger.info("Reasoning took {} seconds.", seconds);
  }
}
