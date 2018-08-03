package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.obolibrary.robot.exceptions.IncoherentRBoxException;
import org.obolibrary.robot.exceptions.IncoherentTBoxException;
import org.obolibrary.robot.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * Provides convenience methods for working with OWL reasoning.
 *
 * @author cjm
 */
public class ReasonerHelper {

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
   * Validates ontology, writes unsatisfiable module
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
        } catch (OWLOntologyCreationException e) {
          e.printStackTrace();
        } catch (IOException e) {
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

  private static OWLOntology createIncoherentModule(
      OWLOntology ontology, Set<OWLClass> unsatisfiableClasses, IRI outputIRI)
      throws OWLOntologyCreationException {
    if (outputIRI == null) {
      outputIRI = IRI.generateDocumentIRI();
    }
    Set<IRI> terms = unsatisfiableClasses.stream().map(x -> x.getIRI()).collect(Collectors.toSet());
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

  private static void saveIncoherentModule(
      OWLOntology ontology, Set<OWLClass> unsatisfiableClasses, String path, IOHelper ioHelper)
      throws OWLOntologyCreationException, IOException {
    OWLOntology module = createIncoherentModule(ontology, unsatisfiableClasses, null);
    ioHelper.saveOntology(module, path);
  }
}
