package org.obolibrary.robot;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.checks.InvalidReferenceChecker;
import org.obolibrary.robot.checks.InvalidReferenceViolation;
import org.obolibrary.robot.checks.InvalidReferenceViolation.Category;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Repair an ontology */
public class RepairOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RepairOperation.class);

  /**
   * Return a map from option name to default option value, for all the available repair options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<String, String>();
    // options.put("remove-redundant-subclass-axioms", "true");

    return options;
  }

  /**
   * Repairs ontology
   *
   * @param ontology the OWLOntology to repair
   * @param iohelper IOHelper to work with the ontology
   */
  public static void repair(OWLOntology ontology, IOHelper iohelper) {
    repair(ontology, iohelper, getDefaultOptions());
  }
  /**
   * Repairs ontology
   *
   * @param ontology the OWLOntology to repair
   * @param iohelper IOHelper to work with the ontology
   * @param options map of repair options
   */
  public static void repair(OWLOntology ontology, IOHelper iohelper, Map<String, String> options) {

    Set<InvalidReferenceViolation> violations =
        InvalidReferenceChecker.getInvalidReferenceViolations(ontology, true);
    repairInvalidReferences(iohelper, ontology, violations);
  }

  /**
   * Repairs invalid references
   *
   * <p>Currently only able to repair references to deprecated classes.
   *
   * <p>Assumes OBO vocabulary
   *
   * @param iohelper IOHelper to work with the ontology
   * @param ontology the OWLOntology to repair
   * @param violations set of references violations
   */
  public static void repairInvalidReferences(
      IOHelper iohelper, OWLOntology ontology, Set<InvalidReferenceViolation> violations) {
    logger.info("Invalid references: " + violations.size());
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    OWLEntityRenamer renamer = new OWLEntityRenamer(manager, ontology.getImportsClosure());
    Map<OWLEntity, IRI> renameMap = new HashMap<>();
    Set<OWLAxiom> axiomsToPreserve = new HashSet<>();

    for (InvalidReferenceViolation v : violations) {
      if (v.getCategory().equals(Category.DEPRECATED)) {
        OWLEntity obsObj = v.getReferencedObject();
        logger.info("Finding replacements for: " + v);
        IRI replacedBy = null;
        for (OWLOntology o : ontology.getImportsClosure()) {
          for (OWLAnnotationAssertionAxiom aaa : o.getAnnotationAssertionAxioms(obsObj.getIRI())) {
            // TODO: use a vocabulary class
            if (aaa.getProperty()
                .getIRI()
                .equals(IRI.create("http://purl.obolibrary.org/obo/IAO_0100001"))) {
              OWLAnnotationValue val = aaa.getValue();
              Optional<IRI> valIRI = val.asIRI();
              if (valIRI.isPresent()) {
                logger.info("Using URI replacement: " + valIRI);
                replacedBy = valIRI.get();
              } else {
                Optional<OWLLiteral> valLit = val.asLiteral();
                if (valLit.isPresent()) {
                  logger.info("Using CURIE replacement: " + valLit);
                  replacedBy = iohelper.createIRI(valLit.get().getLiteral());
                }
              }
            }
          }
        }
        if (replacedBy != null) {
          renameMap.put(obsObj, replacedBy);
        }
      }
    }

    for (OWLEntity obsObj : renameMap.keySet()) {
      IRI replacedBy = renameMap.get(obsObj);
      if (obsObj instanceof OWLClass) {
        axiomsToPreserve.addAll(ontology.getAxioms((OWLClass) obsObj, Imports.EXCLUDED));
      }
      if (obsObj instanceof OWLObjectProperty) {
        axiomsToPreserve.addAll(ontology.getAxioms((OWLObjectProperty) obsObj, Imports.EXCLUDED));
      }
      axiomsToPreserve.addAll(ontology.getDeclarationAxioms(obsObj));
      axiomsToPreserve.addAll(ontology.getAnnotationAssertionAxioms(obsObj.getIRI()));
      logger.info("Replacing: " + obsObj + " -> " + replacedBy);
    }

    logger.info("PRESERVE: " + axiomsToPreserve);
    manager.removeAxioms(ontology, axiomsToPreserve);
    List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    changes.addAll(renamer.changeIRI(renameMap));
    manager.applyChanges(changes);
    manager.addAxioms(ontology, axiomsToPreserve);
  }
}
