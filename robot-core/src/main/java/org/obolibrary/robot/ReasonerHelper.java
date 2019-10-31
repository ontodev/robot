package org.obolibrary.robot;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.obolibrary.robot.exceptions.IncoherentRBoxException;
import org.obolibrary.robot.exceptions.IncoherentTBoxException;
import org.obolibrary.robot.exceptions.InconsistentOntologyException;
import org.obolibrary.robot.reason.InferredClassAssertionAxiomGeneratorDirectOnly;
import org.obolibrary.robot.reason.InferredSubClassAxiomGeneratorIncludingIndirect;
import org.obolibrary.robot.reason.InferredSubObjectPropertyAxiomGeneratorIncludingIndirect;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * Provides convenience methods for working with OWL reasoning.
 *
 * @author cjm
 */
public class ReasonerHelper {

  /** Namespace for error messages. */
  private static final String NS = "reason#";

  /** Error message when an unknown axiom generator is provided. */
  private static final String axiomGeneratorError =
      NS + "UNKNOWN AXIOM GENERATOR %s is not a valid inferred axiom generator";

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReasonerHelper.class);

  /**
   * Validates ontology.
   *
   * @param reasoner OWLReasoner being used.
   * @throws IncoherentTBoxException on unsatisfiable classes
   * @throws InconsistentOntologyException on logical inconsistencies
   * @throws IncoherentRBoxException on unsatisfiable properties
   */
  public static void validate(OWLReasoner reasoner)
      throws IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {
    validate(reasoner, null, null);
  }

  /**
   * Validates ontology.
   *
   * @param reasoner OWLReasoner being used.
   * @param unsatisfiableModulePath path to unsatisfiable module as string
   * @throws IncoherentTBoxException on unsatisfiable classes
   * @throws InconsistentOntologyException on logical inconsistencies
   * @throws IncoherentRBoxException on unsatisfiable properties
   */
  public static void validate(OWLReasoner reasoner, String unsatisfiableModulePath)
      throws InconsistentOntologyException, IncoherentRBoxException, IncoherentTBoxException {
    validate(reasoner, unsatisfiableModulePath, null);
  }

  /**
   * Validates ontology, writes unsatisfiable module.
   *
   * @param reasoner OWLReasoner being used
   * @param unsatisfiableModulePath path to unsatisfiable module as string
   * @param ioHelper IOHelper to use
   * @throws IncoherentTBoxException on unsatisfiable classes
   * @throws InconsistentOntologyException on logical inconsistencies
   * @throws IncoherentRBoxException on unsatisfiable properties
   */
  public static void validate(
      OWLReasoner reasoner, String unsatisfiableModulePath, IOHelper ioHelper)
      throws IncoherentTBoxException, InconsistentOntologyException, IncoherentRBoxException {

    OWLOntology ont = reasoner.getRootOntology();
    OWLOntologyManager manager = ont.getOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();
    OWLClass nothing = dataFactory.getOWLNothing();
    OWLClass thing = dataFactory.getOWLThing();

    logger.info("Checking for inconsistencies");
    if (!reasoner.isConsistent()) {
      logger.error(
          "The ontology is inconsistent. TIP: use a tool like Protege to find explanations");
      if (unsatisfiableModulePath != null) {
        logger.error(
            "Unfortunately, robot is not able to generate an unsatisfiable "
                + "minimal model for inconsistent ontologies at this time.\n");
        logger.error("TIP: remove individuals from ontology and try again");
      }
      throw new InconsistentOntologyException();
    }

    logger.info("Checking for unsatisfiable classes...");
    Set<OWLClass> unsatisfiableClasses =
        reasoner.getUnsatisfiableClasses().getEntitiesMinus(nothing);
    if (unsatisfiableClasses.size() > 0) {
      logger.error(
          "There are {} unsatisfiable classes in the ontology.", unsatisfiableClasses.size());
      for (OWLClass cls : unsatisfiableClasses) {
        logger.error("    unsatisfiable: " + cls.getIRI());
      }
      if (unsatisfiableModulePath != null) {
        // normally we would not catch an exception and carry on,
        // but in this case the primary exception is IncoherentTBoxException,
        // we want to ensure this is thrown
        try {
          saveIncoherentModule(ont, unsatisfiableClasses, unsatisfiableModulePath, ioHelper);
        } catch (OWLOntologyCreationException | IOException e) {
          e.printStackTrace();
        }
      }
      throw new IncoherentTBoxException(unsatisfiableClasses);
    }

    // TODO: can this be done by checking for equivalence to bottomObjectProperty?
    logger.info("Checking for unsatisfiable object properties...");

    Set<OWLAxiom> tempAxioms = new HashSet<>();
    Map<OWLClass, OWLObjectProperty> probeFor = new HashMap<>();
    for (OWLObjectProperty p : ont.getObjectPropertiesInSignature(Imports.INCLUDED)) {
      UUID uuid = UUID.randomUUID();
      IRI probeIRI = IRI.create(p.getIRI().toString() + "-" + uuid.toString());
      OWLClass probe = dataFactory.getOWLClass(probeIRI);
      probeFor.put(probe, p);
      tempAxioms.add(dataFactory.getOWLDeclarationAxiom(probe));
      tempAxioms.add(
          dataFactory.getOWLSubClassOfAxiom(
              probe, dataFactory.getOWLObjectSomeValuesFrom(p, thing)));
    }
    manager.addAxioms(ont, tempAxioms);
    reasoner.flush();

    Set<OWLClass> unsatisfiableProbeClasses =
        reasoner.getUnsatisfiableClasses().getEntitiesMinus(nothing);

    // leave no trace
    manager.removeAxioms(ont, tempAxioms);
    reasoner.flush();

    if (unsatisfiableProbeClasses.size() > 0) {
      logger.error(
          "There are {} unsatisfiable properties in the ontology.",
          unsatisfiableProbeClasses.size());
      Set<OWLObjectProperty> unsatPs = new HashSet<>();
      for (OWLClass cls : unsatisfiableProbeClasses) {
        OWLObjectProperty unsatP = probeFor.get(cls);
        unsatPs.add(unsatP);
        logger.error("    unsatisfiable property: " + unsatP.getIRI());
      }
      throw new IncoherentRBoxException(unsatPs);
    }
  }

  /**
   * @param reasoner OWLReasoner being used
   * @param outputIRI IRI of output
   * @return minimal incoherent module
   * @throws OWLOntologyCreationException on issue creating incoherent module
   */
  public static OWLOntology createIncoherentModule(OWLReasoner reasoner, IRI outputIRI)
      throws OWLOntologyCreationException {
    OWLOntology ontology = reasoner.getRootOntology();
    OWLClass nothing = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLNothing();
    Set<OWLClass> unsatisfiableClasses =
        reasoner.getUnsatisfiableClasses().getEntitiesMinus(nothing);
    return createIncoherentModule(ontology, unsatisfiableClasses, outputIRI);
  }

  /**
   * Create an incoherent module from an ontology based on a set of unsatisfiable classes.
   *
   * @param ontology OWLOntology to create module from
   * @param unsatisfiableClasses set of unsatisfiable OWLClasses
   * @param outputIRI IRI for the output, or null
   * @return incoherent module as OWLOntology
   * @throws OWLOntologyCreationException on problem extracting incoherent module
   */
  private static OWLOntology createIncoherentModule(
      OWLOntology ontology, Set<OWLClass> unsatisfiableClasses, IRI outputIRI)
      throws OWLOntologyCreationException {
    if (outputIRI == null) {
      outputIRI = IRI.generateDocumentIRI();
    }
    Set<IRI> terms =
        unsatisfiableClasses.stream().map(OWLNamedObject::getIRI).collect(Collectors.toSet());
    OWLOntology module = ExtractOperation.extract(ontology, terms, outputIRI, ModuleType.BOT);

    if (ontology.getImportsClosure().size() > 1) {
      logger.info("Tagging axioms in unsatisfiable module with their source");
      OWLOntologyManager manager = ontology.getOWLOntologyManager();
      OWLDataFactory dataFactory = manager.getOWLDataFactory();
      OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();

      // create an index of where each axiom comes from -
      // this will be useful for debugging
      Map<OWLAxiom, Set<OWLOntologyID>> axiomToOntologyMap = new HashMap<>();
      for (OWLOntology subont : ontology.getImportsClosure()) {
        OWLOntologyID ontid = subont.getOntologyID();
        for (OWLAxiom axiom : subont.getAxioms()) {
          OWLAxiom axiomWithoutAnns = axiom.getAxiomWithoutAnnotations();
          if (!axiomToOntologyMap.containsKey(axiomWithoutAnns)) {
            axiomToOntologyMap.put(axiomWithoutAnns, new HashSet<>());
          }
          axiomToOntologyMap.get(axiomWithoutAnns).add(ontid);
        }
      }
      Set<OWLAxiom> newAxioms = new HashSet<>();
      Set<OWLAxiom> rmAxioms = new HashSet<>();
      for (OWLAxiom axiom : module.getAxioms()) {
        OWLAxiom axiomWithoutAnns = axiom.getAxiomWithoutAnnotations();

        Set<OWLAnnotation> anns = new HashSet<>();
        if (!axiomToOntologyMap.containsKey(axiomWithoutAnns)) {
          logger.warn("Unexpected: module has axiom not in source ontologies: " + axiomWithoutAnns);
        } else {
          for (OWLOntologyID ontid : axiomToOntologyMap.get(axiomWithoutAnns)) {
            anns.add(
                dataFactory.getOWLAnnotation(
                    isDefinedBy, dataFactory.getOWLLiteral(ontid.toString())));
          }
        }
        newAxioms.add(axiom.getAnnotatedAxiom(anns));
        rmAxioms.add(axiom);
      }
      manager.removeAxioms(module, rmAxioms);
      manager.addAxioms(module, newAxioms);
    }
    return module;
  }

  /**
   * Given a list of axiom generator strings, return a list of InferredAxiomGenerator objects.
   *
   * @param axGenerators list of strings to get InferredAxiomGenerators
   * @param direct return axiom generators which include only direct
   *     superclass/superproperties/types
   * @return list of InferredAxiomGenerators
   */
  public static List<InferredAxiomGenerator<? extends OWLAxiom>> getInferredAxiomGenerators(
      List<String> axGenerators, boolean direct) {
    List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<>();
    if (axGenerators == null || axGenerators.isEmpty()) {
      if (direct) {
        gens.add(new InferredSubClassAxiomGenerator());
      } else {
        gens.add(new InferredSubClassAxiomGeneratorIncludingIndirect());
      }
      return gens;
    }
    for (String ax : axGenerators) {
      gens.add(getInferredAxiomGenerator(ax, direct));
    }
    return gens;
  }

  /**
   * Given a list of axiom generator strings, return a list of InferredAxiomGenerator objects.
   *
   * @param axGenerators list of strings to get InferredAxiomGenerators
   * @return list of InferredAxiomGenerators
   */
  public static List<InferredAxiomGenerator<? extends OWLAxiom>> getInferredAxiomGenerators(
      List<String> axGenerators) {
    return getInferredAxiomGenerators(axGenerators, true);
  }

  /**
   * Given an axiom generator as a string, return the InferredAxiomGenerator object.
   *
   * @param axGenerator name of InferredAxiomGenerator
   * @return InferredAxiomGenerator
   */
  public static InferredAxiomGenerator<? extends OWLAxiom> getInferredAxiomGenerator(
      String axGenerator) {
    return getInferredAxiomGenerator(axGenerator, true);
  }

  /**
   * Given an axiom generator as a string, return the InferredAxiomGenerator object.
   *
   * @param axGenerator name of InferredAxiomGenerator
   * @param direct return axiom generators which include only direct
   *     superclass/superproperties/types
   * @return InferredAxiomGenerator
   */
  public static InferredAxiomGenerator<? extends OWLAxiom> getInferredAxiomGenerator(
      String axGenerator, boolean direct) {
    switch (axGenerator.toLowerCase()) {
      case "subclass":
      case "":
        if (direct) {
          return new InferredSubClassAxiomGenerator();
        } else {
          return new InferredSubClassAxiomGeneratorIncludingIndirect();
        }
      case "disjointclasses":
        return new InferredDisjointClassesAxiomGenerator();
      case "equivalentclass":
        return new InferredEquivalentClassAxiomGenerator();
      case "datapropertycharacteristic":
        return new InferredDataPropertyCharacteristicAxiomGenerator();
      case "equivalentdataproperties":
        return new InferredEquivalentDataPropertiesAxiomGenerator();
      case "subdataproperty":
        return new InferredSubDataPropertyAxiomGenerator();
      case "classassertion":
        if (direct) {
          return new InferredClassAssertionAxiomGeneratorDirectOnly();
        } else {
          return new InferredClassAssertionAxiomGenerator();
        }
      case "propertyassertion":
        return new InferredPropertyAssertionGenerator();
      case "equivalentobjectproperty":
        return new InferredEquivalentObjectPropertyAxiomGenerator();
      case "inverseobjectproperties":
        return new InferredInverseObjectPropertiesAxiomGenerator();
      case "objectpropertycharacteristic":
        return new InferredObjectPropertyCharacteristicAxiomGenerator();
      case "subobjectproperty":
        if (direct) {
          return new InferredSubObjectPropertyAxiomGenerator();
        } else {
          return new InferredSubObjectPropertyAxiomGeneratorIncludingIndirect();
        }
      default:
        throw new IllegalArgumentException(String.format(axiomGeneratorError, axGenerator));
    }
  }

  /**
   * Save an incoherent ontology module to a path.
   *
   * @param ontology OWLOntology to create module from
   * @param unsatisfiableClasses set of unsatisfiable OWLClasses
   * @param path path to save module to
   * @param ioHelper IOHelper to use, or null
   * @throws OWLOntologyCreationException on problem creating incoherent module
   * @throws IOException on problem saving module
   */
  private static void saveIncoherentModule(
      OWLOntology ontology, Set<OWLClass> unsatisfiableClasses, String path, IOHelper ioHelper)
      throws OWLOntologyCreationException, IOException {
    if (ioHelper == null) {
      ioHelper = new IOHelper();
    }
    OWLOntology module = createIncoherentModule(ontology, unsatisfiableClasses, null);
    ioHelper.saveOntology(module, path);
  }
}
