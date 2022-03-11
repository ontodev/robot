package org.obolibrary.robot;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compute metrics for the ontology.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MigrateOperation {
  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "migrate#";

  /** Error message when metric type is illegal. Expects: metric type. */
  private static final String METRICS_TYPE_ERROR =
      NS + "METRICS TYPE ERROR unknown metrics type: %s";

  private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
  private static final OWLAnnotationProperty exclusionReason = df.getRDFSComment();
  private static final String excludedSuperClassOfIRI = "http://w3id.org/robot/excludedAxiom";
  private static final OWLAnnotationProperty excludedSuperClassOfAP =
      df.getOWLAnnotationProperty(IRI.create(excludedSuperClassOfIRI));
  private static final String excludedAxiomIRI = "http://w3id.org/robot/excludedAxiom";
  private static final OWLAnnotationProperty excludedAxiomAP =
      df.getOWLAnnotationProperty(IRI.create(excludedAxiomIRI));

  /**
   * @param targetOntology
   * @param sourceOntology
   * @param migrateTerms
   * @param source_id
   * @param mappings
   * @param axiomSelectors
   * @param excludeTerms
   * @param excludedAxiomsFromSourceOntology An ontology with all the axioms that should be ignored
   *     by the migration process.
   * @param reasonerFactory
   * @param updateExclusionReasons
   * @param tagAxiomsWithSource
   * @param mergeSource
   * @param ioHelper
   * @return
   */
  public static OWLOntology migrate(
      OWLOntology targetOntology,
      OWLOntology sourceOntology,
      Set<OWLEntity> migrateTerms,
      String source_id,
      Map<String, String> mappings,
      List<String> axiomSelectors,
      Set<OWLEntity> excludeTerms,
      OWLOntology excludedAxiomsFromSourceOntology,
      OWLReasonerFactory reasonerFactory,
      boolean updateExclusionReasons,
      boolean tagAxiomsWithSource,
      boolean mergeSource,
      IOHelper ioHelper) {
    /**
     * Tag axioms with dc:source (PARAM --tag-axioms-with-source) Rename local IDs to external IDs:
     * ROBOT rename -m PARAMETER Merge axioms into local ontology 1 by 1: if reasoner provided,
     * check entailment violations and print warnings. PARAM
     */
    OWLOntology mergedSourceOntology = MergeOperation.merge(sourceOntology);
    System.out.println("Size: " + mergedSourceOntology.getAxiomCount());
    if (migrateTerms.isEmpty()) {
      migrateTerms.addAll(mergedSourceOntology.getClassesInSignature());
    }
    if (axiomSelectors.isEmpty()) {
      axiomSelectors.add("logical");
      axiomSelectors.add("annotation");
    }
    try {
      /*
       * Only select all the axioms that are related to terms in the migration set
       * and filter out anything not related to one of the axiom types provided.
       */
      filterAxiomsToMigrate(mergedSourceOntology, axiomSelectors, migrateTerms, excludeTerms);
      System.out.println("filterAxiomsToMigrate. Size: " + mergedSourceOntology.getAxiomCount());
      /*
       * Removing all the axioms from the source that should be ignored according to the
       * axioms provided by the excludedAxiomsFromSourceOntology.
       */
      filterAxiomsThatShouldBeIgnored(excludedAxiomsFromSourceOntology, mergedSourceOntology);
      System.out.println(
          "filterAxiomsThatShouldBeIgnored. Size: " + mergedSourceOntology.getAxiomCount());

      // If dc source mapping provided, use that for mapping the data
      String sourceIRI = "http://purl.org/dc/elements/1.1/source";
      for (Map.Entry<String, String> e : mappings.entrySet()) {
        if (e.getKey().equals(sourceIRI)) {
          sourceIRI = e.getValue();
          break;
        }
      }
      OWLAnnotationProperty sourceAP = df.getOWLAnnotationProperty(IRI.create(sourceIRI));
      OWLAnnotationProperty sourceTermAP = df.getOWLAnnotationProperty(IRI.create(sourceIRI));
      OWLAnnotation sourceOfAnnotation = df.getOWLAnnotation(sourceAP, df.getOWLLiteral(source_id));

      if (updateExclusionReasons) {
        /*
         * Updating the exclusion reasons for the given source by
         * 1. Removing old exclusion reasons
         * 2. Adding new ones
         *
         * This method changes the target ontology.
         */
        updateExclusionReasons(
            targetOntology,
            mergedSourceOntology,
            excludedAxiomsFromSourceOntology,
            sourceOfAnnotation,
            source_id);
      }

      System.out.println("updateExclusionReasons. Size: " + mergedSourceOntology.getAxiomCount());

      /*
       Rename all the entities in the source ontology to the entities they should be migrated to.
      */
      RenameOperation.renameFull(mergedSourceOntology, ioHelper, mappings, true);
      CURIEShortFormProvider curieProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
      Map<IRI, String> iriMappings = new HashMap<>();
      for (Map.Entry<String, String> entry : mappings.entrySet()) {
        IRI sourceEntityIRI = IRI.create(entry.getKey());
        IRI targetEntityIRI = IRI.create(entry.getValue());
        iriMappings.put(targetEntityIRI, curieProvider.getShortForm(sourceEntityIRI));
      }
      for (OWLEntity e : mergedSourceOntology.getSignature()) {
        IRI eiri = e.getIRI();
        if (!iriMappings.containsKey(eiri)) {
          iriMappings.put(eiri, curieProvider.getShortForm(eiri));
        }
      }

      /*
      Remove all "evidence" from source ontology
       */
      List<OWLAxiomChange> changes = new ArrayList<>();
      if(tagAxiomsWithSource) {
        for (OWLAxiom ax : targetOntology.getAxioms(Imports.EXCLUDED)) {
          OWLAxiom withoutAnnotation = ax.getAxiomWithoutAnnotations();
          Set<OWLAnnotation> axiomAnnotations = new HashSet<>(ax.getAnnotations());
          Set<OWLAnnotation> newAnnotations = new HashSet<>(ax.getAnnotations());

            // Since the axiom in the target ontology (for loop) exists in the
            // set of axioms to be imported, we remove it, i.e. not trying to import it again.
          changes.add(
              new RemoveAxiom(
                targetOntology,
                withoutAnnotation.getAnnotatedAxiom(axiomAnnotations)));
          changes.add(
            new AddAxiom(
              targetOntology,
              withoutAnnotation.getAnnotatedAxiom(newAnnotations)));
        }
      }

      /*
             * Remove all the axioms from the set to be imported that are already present in the
             * base ontology.Think of a way to avoid injecting redundant axioms.
      run reasoner prior to injection and check whether entailed.

             *
             */
/*
      List<RemoveAxiom> removals = new ArrayList<>();
      List<OWLAxiomChange> addAnnotatedAxioms =
          new ArrayList<>(); // Adding axiom annotations where axiom supported by source
      HashMap<OWLAxiom, OWLAxiom> mapAxiomWithAnnosToAxiomWithoutAnnos =
          mapAxiomsWithAnnosToAxiomsWithoutAnnos(mergedSourceOntology);

      for (OWLAxiom ax : targetOntology.getAxioms(Imports.EXCLUDED)) {
        OWLAxiom withoutAnnotation = ax.getAxiomWithoutAnnotations();
        Set<OWLAnnotation> axiomAnnotations = new HashSet<>(ax.getAnnotations());
        if (mapAxiomWithAnnosToAxiomWithoutAnnos.containsKey(withoutAnnotation)) {

          // Since the axiom in the target ontology (for loop) exists in the
          // set of axioms to be imported, we remove it, i.e. not trying to import it again.
          removals.add(
              new RemoveAxiom(
                  mergedSourceOntology,
                  mapAxiomWithAnnosToAxiomWithoutAnnos.get(withoutAnnotation)));

          //The following line add a evidence support to the
          if (tagAxiomsWithSource) {
            axiomAnnotations.add(sourceOfAnnotation);
            addAnnotatedAxioms.add(new RemoveAxiom(targetOntology, ax));
            addAnnotatedAxioms.add(
                new AddAxiom(targetOntology, ax.getAnnotatedAxiom(axiomAnnotations)));
          }

        }
      }
      mergedSourceOntology.getOWLOntologyManager().applyChanges(removals);
      targetOntology.getOWLOntologyManager().applyChanges(addAnnotatedAxioms);
      removals.clear();
      System.out.println("removedPresent. Size: " + mergedSourceOntology.getAxiomCount());
*/
      /*
       * Tagging remaining axioms
       */
      if (tagAxiomsWithSource) {
        Set<OWLAnnotation> axiomAnnotationsSource = new HashSet<>();
        axiomAnnotationsSource.add(sourceOfAnnotation);

        List<OWLAxiomChange> changes = new ArrayList<>();

        // Go through the axioms in the source to be migrated
        for (OWLAxiom ax : mergedSourceOntology.getAxioms(Imports.INCLUDED)) {
          Optional<String> subject = getSubject(ax, iriMappings);
          // System.out.println("AXIOM: " + ax);
          // System.out.println("SUBJECT: " + subject);
          Set<OWLAnnotation> axiomAnnotationsAxiom = new HashSet<>();
          // TODO: consider migrating the existing axiom annotations
          // if (false) {
          //  axiomAnnotationsAxiom.addAll(ax.getAnnotations());
          // }
          // System.out.println(ax);
          if (subject.isPresent()) {
            axiomAnnotationsAxiom.add(
                df.getOWLAnnotation(sourceTermAP, df.getOWLLiteral(subject.get())));
          } else {
            axiomAnnotationsAxiom.addAll(axiomAnnotationsSource);
          }
          changes.add(new RemoveAxiom(mergedSourceOntology, ax));
          changes.add(
              new AddAxiom(mergedSourceOntology, ax.getAnnotatedAxiom(axiomAnnotationsAxiom)));
        }

        mergedSourceOntology.getOWLOntologyManager().applyChanges(changes);
        System.out.println("updated. Size: " + mergedSourceOntology.getAxiomCount());

        changes.clear();
      }

      /*
       * What about when we introduce redundant axioms?
       */

      if (mergeSource) {
        targetOntology
            .getOWLOntologyManager()
            .addAxioms(targetOntology, mergedSourceOntology.getAxioms());
        if (reasonerFactory != null) {
          OWLReasoner r = reasonerFactory.createReasoner(targetOntology);
          if (r.isConsistent()) {
            LOGGER.info("Ontology is consistent after migration.");
          }
          if (r.getUnsatisfiableClasses().getEntitiesMinusBottom().isEmpty()) {
            LOGGER.info("No unsatisfiable classes added.");
          } else {
            int ct_unsat = r.getUnsatisfiableClasses().getEntitiesMinusBottom().size();
            LOGGER.warn("The merge caused " + ct_unsat + " unsatisfiable classes.");
            Set<Explanation<OWLAxiom>> explanations =
                ExplainOperation.explainUnsatisfiableClasses(
                    targetOntology, r, reasonerFactory, 100, 1);
            Map<OWLAxiom, Integer> mapMostUsedAxioms = new HashMap<>();
            explanations.forEach(e -> e.getAxioms().forEach(ax -> countUp(ax, mapMostUsedAxioms)));
            String summary =
                ExplainOperation.renderAxiomImpactSummary(
                    mapMostUsedAxioms, targetOntology, targetOntology.getOWLOntologyManager());
            LOGGER.warn(summary);
          }
        }
        return targetOntology;
      } else {
        return mergedSourceOntology;
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static void filterAxiomsThatShouldBeIgnored(
      OWLOntology excludedAxiomsFromSourceOntology, OWLOntology mergedSourceOntology) {
    Set<OWLAxiom> excludedAxiomsWithoutAnnotations = new HashSet<>();
    List<RemoveAxiom> removals = new ArrayList<>();
    excludedAxiomsFromSourceOntology
        .getAxioms(Imports.INCLUDED)
        .forEach(a -> excludedAxiomsWithoutAnnotations.add(a.getAxiomWithoutAnnotations()));
    for (OWLAxiom ax : mergedSourceOntology.getAxioms(Imports.INCLUDED)) {
      if (excludedAxiomsWithoutAnnotations.contains(ax.getAxiomWithoutAnnotations())) {
        removals.add(new RemoveAxiom(mergedSourceOntology, ax));
      }
    }
    mergedSourceOntology.getOWLOntologyManager().applyChanges(removals);
    removals.clear();
  }

  private static void filterAxiomsToMigrate(
    OWLOntology mergedSourceOntology,
    List<String> axiomSelectors,
      Set<OWLEntity> migrateTerms,
      Set<OWLEntity> excludeTerms)
      throws OWLOntologyCreationException {
    // Obtain all axioms from the source ontology that contains references to the termsToMigrate
    Set<OWLObject> termsToMigrate =
        migrateTerms.stream()
            .filter(Objects::nonNull)
            .filter(t -> !excludeTerms.contains(t))
            .map(obj -> (OWLObject) obj)
            .collect(Collectors.toSet());
    System.out.println("termsToMigrate. Size: " + termsToMigrate.size());

    Set<OWLAxiom> axiomsInSourceThatReferenceTermsToMigrate =
        RelatedObjectsHelper.filterAxioms(
            mergedSourceOntology.getAxioms(Imports.INCLUDED),
            termsToMigrate,
            axiomSelectors,
            new ArrayList<>(),
            true,
            false);

    System.out.println(
        "axiomsInSourceThatReferenceTermsToMigrate. Size: "
            + axiomsInSourceThatReferenceTermsToMigrate.size());
    /*
     * Removing all the axioms from the source ontology that are not in the set of the
     * filtered axioms from the filter process above.
     * TODO: replace by ROBOT.unmerge()?
     */
    List<RemoveAxiom> removals = new ArrayList<>();
    mergedSourceOntology.getAxioms(Imports.INCLUDED).stream()
        .filter(a -> !axiomsInSourceThatReferenceTermsToMigrate.contains(a))
        .forEach(a -> removals.add(new RemoveAxiom(mergedSourceOntology, a)));
    mergedSourceOntology.getOWLOntologyManager().applyChanges(removals);
    removals.clear();
  }

  @Nonnull
  private static HashMap<OWLAxiom, OWLAxiom> mapAxiomsWithAnnosToAxiomsWithoutAnnos(
      OWLOntology mergedSourceOntology) {
    HashMap<OWLAxiom, OWLAxiom> mapAxiomWithAnnosToAxiomWithoutAnnos = new HashMap<>();
    for (OWLAxiom ax : mergedSourceOntology.getAxioms(Imports.INCLUDED)) {
      mapAxiomWithAnnosToAxiomWithoutAnnos.put(ax.getAxiomWithoutAnnotations(), ax);
    }
    return mapAxiomWithAnnosToAxiomWithoutAnnos;
  }

  private static void updateExclusionReasons(
      OWLOntology targetOntology,
      OWLOntology sourceOntology,
      OWLOntology excludedAxiomsFromSourceOntology,
      OWLAnnotation sourceOfAnnotation,
      String source_id) {
    /*
     updateExclusionReasons changes the target ontology. It first removes the old exclusion reasons, then
     adding updates ones.
    */
    List<RemoveAxiom> removals = new ArrayList<>();
    for (OWLAxiom ax : targetOntology.getAxioms()) {
      if (isExclusionDocumentationForSource(ax, source_id)) {
        removals.add(new RemoveAxiom(sourceOntology, ax));
      }
    }
    targetOntology.getOWLOntologyManager().applyChanges(removals);
    removals.clear();

    List<AddAxiom> exclusionDocAxioms = new ArrayList<>();
    for (OWLAxiom ax : excludedAxiomsFromSourceOntology.getAxioms()) {
      String reasonAxiom = "Excluded";
      Set<OWLAnnotation> exclusionsReasoner = ax.getAnnotations(exclusionReason);
      if (!exclusionsReasoner.isEmpty()) {
        reasonAxiom += " for the following reasons:\n";
      }
      for (OWLAnnotation anno : exclusionsReasoner) {
        reasonAxiom += "-" + anno.getValue().asLiteral().orNull() + "\n";
      }

      boolean processed = false;
      OWLAnnotation exclusionReasonAnnotation =
          df.getOWLAnnotation(exclusionReason, df.getOWLLiteral(reasonAxiom));

      Set<OWLAnnotation> exclusionReasonAnnotations = new HashSet<>();
      exclusionReasonAnnotations.add(sourceOfAnnotation);
      exclusionReasonAnnotations.add(exclusionReasonAnnotation);

      if (ax instanceof OWLSubClassOfAxiom) {
        OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom) ax;
        if (!sax.getSubClass().isAnonymous() && !sax.getSuperClass().isAnonymous()) {
          OWLClass subClass = sax.getSubClass().asOWLClass();
          OWLClass superClass = sax.getSuperClass().asOWLClass();

          OWLAnnotation excludedSuperClassOfAnnotation =
              df.getOWLAnnotation(excludedSuperClassOfAP, superClass.getIRI());
          OWLAnnotationAssertionAxiom excludedSubClass =
              df.getOWLAnnotationAssertionAxiom(subClass.getIRI(), excludedSuperClassOfAnnotation);
          exclusionDocAxioms.add(
              new AddAxiom(
                  targetOntology, excludedSubClass.getAnnotatedAxiom(exclusionReasonAnnotations)));
          processed = true;
        }
      }
      if (!processed) {
        String readableAxiom = OntologyHelper.renderManchester(ax, new SimpleShortFormProvider());
        for (OWLEntity e : ax.getSignature()) {
          if (e instanceof OWLClass || e instanceof OWLNamedIndividual) {
            OWLAnnotation excludedAxiomAnnotation =
                df.getOWLAnnotation(excludedAxiomAP, df.getOWLLiteral(readableAxiom));
            OWLAnnotationAssertionAxiom excludedAxiom =
                df.getOWLAnnotationAssertionAxiom(e.getIRI(), excludedAxiomAnnotation);
            exclusionDocAxioms.add(
                new AddAxiom(
                    targetOntology, excludedAxiom.getAnnotatedAxiom(exclusionReasonAnnotations)));
          }
        }
      }
      targetOntology.getOWLOntologyManager().applyChanges(exclusionDocAxioms);
    }
  }

  private static Optional<String> getSubject(OWLAxiom ax, Map<IRI, String> stringMappings) {
    // TODO
    IRI iri = null;
    if (ax instanceof OWLSubClassOfAxiom) {
      OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom) ax;
      if (!sax.getSuperClass().isAnonymous()) {
        iri = sax.getSuperClass().asOWLClass().getIRI();
      }
    } else if (ax instanceof OWLAnnotationAssertionAxiom) {
      OWLAnnotationAssertionAxiom aax = (OWLAnnotationAssertionAxiom) ax;

      if (aax.getSubject().isIRI()) {
        iri = (IRI) aax.getSubject();
      }
    }
    if (iri != null) {
      if (stringMappings.containsKey(iri)) {
        return Optional.of(stringMappings.get(iri));
      }
    }
    return Optional.empty();
  }

  private static boolean isExclusionDocumentationForSource(OWLAxiom ax, String source_id) {
    return false;
  }

  private static void countUp(OWLAxiom ax, Map<OWLAxiom, Integer> mapMostUsedAxioms) {
    if (!mapMostUsedAxioms.containsKey(ax)) {
      mapMostUsedAxioms.put(ax, 0);
    }
    mapMostUsedAxioms.put(ax, mapMostUsedAxioms.get(ax) + 1);
  }
}
