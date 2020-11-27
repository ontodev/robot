package org.obolibrary.robot;

import com.google.common.base.Optional;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.semanticweb.owl.explanation.api.*;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.rootderived.StructuralRootDerivedReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.explanation.ProtegeExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

/**
 * Compute an explanation for an entailed axiom.
 *
 * @author <a href="mailto:balhoff@renci.org">Jim Balhoff</a>
 */
public class ExplainOperation {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ExplainOperation.class);

  private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

  /**
   * Compute explanations for an entailed axiom.
   *
   * @param axiom entailed axiom to explain
   * @param ontology ontology to search for explanation
   * @param reasonerFactory reasoner factory used to create reasoners to test entailments
   * @param maxExplanations maximum number of explanations to compute
   * @return explanations
   */
  public static Set<Explanation<OWLAxiom>> explain(
      OWLAxiom axiom,
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      int maxExplanations) {
    logger.debug("Explaining: " + axiom);

    ExplanationGeneratorFactory<OWLAxiom> genFac =
        ExplanationManager.createExplanationGeneratorFactory(reasonerFactory);
    ExplanationGenerator<OWLAxiom> gen = genFac.createExplanationGenerator(ontology);
    return gen.getExplanations(axiom, maxExplanations);
  }

  /**
   * Compute explanations for inconsistent ontology
   *
   * @param ontology the ontology to be tested
   * @param reasonerFactory the reasoner factory to be used for determining the explanation
   * @param max maximum number of explanations to be computed
   * @return a set of explanations for inconsistent ontology
   */
  public static Set<Explanation<OWLAxiom>> explainInconsistent(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, int max) {
    InconsistentOntologyExplanationGeneratorFactory igf =
        new InconsistentOntologyExplanationGeneratorFactory(reasonerFactory, 10000);
    ExplanationGenerator<OWLAxiom> generator = igf.createExplanationGenerator(ontology);
    OWLAxiom entailment = df.getOWLSubClassOfAxiom(df.getOWLThing(), df.getOWLNothing());
    return generator.getExplanations(entailment, max);
  }

  /**
   * Compute explanations for all unsatisfiable classes
   *
   * @param ontology the ontology to be tested
   * @param reasoner the reasoner to be used to determine the unsatisfiable classes
   * @param reasonerFactory the reasoner factory to be used to compute the explanations
   * @param max maximum number of explanations to be computed
   * @return a set of explanations
   */
  public static Set<Explanation<OWLAxiom>> explainUnsatisfiableClasses(
      OWLOntology ontology, OWLReasoner reasoner, OWLReasonerFactory reasonerFactory, int max) {
    return explainUnsatisfiableClasses(ontology, reasoner, reasonerFactory, max, -1);
  }

  /**
   * Compute explanations for all unsatisfiable classes
   *
   * @param ontology the ontology to be tested
   * @param reasoner the reasoner to be used to determine the unsatisfiable classes
   * @param reasonerFactory the reasoner factory to be used to compute the explanations
   * @param max maximum number of explanations to be computed
   * @param maxUnsat cutoff - limit number of tested unsatisfiable classes to maxUnsat classes
   * @return a set of explanations
   */
  public static Set<Explanation<OWLAxiom>> explainUnsatisfiableClasses(
      OWLOntology ontology,
      OWLReasoner reasoner,
      OWLReasonerFactory reasonerFactory,
      int max,
      int maxUnsat) {
    List<OWLClass> unsatisfiable_classes =
        new ArrayList<>(reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom());
    if (maxUnsat > 0 && unsatisfiable_classes.size() > maxUnsat) {
      unsatisfiable_classes = unsatisfiable_classes.subList(0, maxUnsat);
    }
    return getUnsatExplanationsForClasses(ontology, reasonerFactory, max, unsatisfiable_classes);
  }

  /**
   * Compute explanations for all root unsatisfiable classes
   *
   * @param ontology the ontology to be tested
   * @param reasoner the reasoner to be used to determine the unsatisfiable classes
   * @param reasonerFactory the reasoner factory to be used to compute the explanations
   * @param max maximum number of explanations to be computed
   * @return a set of explanations
   */
  public static Set<Explanation<OWLAxiom>> explainRootUnsatisfiableClasses(
      OWLOntology ontology, OWLReasoner reasoner, OWLReasonerFactory reasonerFactory, int max) {
    RootDerivedReasoner rootReasoner =
        new StructuralRootDerivedReasoner(
            ontology.getOWLOntologyManager(), reasoner, reasonerFactory);
    List<OWLClass> unsatisfiable_classes =
        new ArrayList<>(rootReasoner.getRootUnsatisfiableClasses());
    return getUnsatExplanationsForClasses(ontology, reasonerFactory, max, unsatisfiable_classes);
  }

  /**
   * Render an Explanation object as Markdown text, linking text labels to term IRIs and indenting
   * axioms.
   *
   * @param explanation explanation to render
   * @param manager OWLOntologyManager containing source ontologies for explanation axioms
   * @return Markdown-formatted explanation text
   */
  public static String renderExplanationAsMarkdown(
      Explanation<OWLAxiom> explanation, OWLOntologyManager manager) {
    ExplanationOrderer orderer = new ProtegeExplanationOrderer(manager);
    ExplanationTree tree =
        orderer.getOrderedExplanation(explanation.getEntailment(), explanation.getAxioms());
    ShortFormProvider labelProvider =
        new AnnotationValueShortFormProvider(
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap(),
            manager);
    ShortFormProvider linkProvider = new MarkdownLinkShortFormProvider(labelProvider);
    ManchesterOWLSyntaxOWLObjectRendererImpl axiomRenderer =
        new ManchesterOWLSyntaxOWLObjectRendererImpl();
    axiomRenderer.setShortFormProvider(linkProvider);
    return renderTree(tree, axiomRenderer);
  }

  public static String renderAxiomImpactSummary(
      Map<OWLAxiom, Integer> axiomMap, OWLOntology ontology, OWLOntologyManager manager) {
    ShortFormProvider labelProvider =
        new AnnotationValueShortFormProvider(
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap(),
            manager);
    ShortFormProvider linkProvider = new MarkdownLinkShortFormProvider(labelProvider);
    ManchesterOWLSyntaxOWLObjectRendererImpl axiomRenderer =
        new ManchesterOWLSyntaxOWLObjectRendererImpl();
    axiomRenderer.setShortFormProvider(linkProvider);
    Map<Integer, Set<OWLAxiom>> mapInversed = new HashMap<>();
    Map<OWLAxiom, Set<String>> associatedOntologyIds = new HashMap<>();
    Map<OWLOntologyID, String> ontologyIdAbbreviation = new HashMap<>();

    for (Map.Entry<OWLAxiom, Integer> e : axiomMap.entrySet()) {
      associatedOntologyIds.put(e.getKey(), new HashSet<>());
      if (!mapInversed.containsKey(e.getValue())) {
        mapInversed.put(e.getValue(), new HashSet<>());
      }
      mapInversed.get(e.getValue()).add(e.getKey());

      /*
      Determine source ontologies (if imports are present).
       */
      Set<OWLOntologyID> oids = getOntologyIds(e.getKey(), ontology);
      for (OWLOntologyID oid : oids) {
        if (!ontologyIdAbbreviation.containsKey(oid)) {
          String soid = getAbbreviationForOntologyID(oid);
          ontologyIdAbbreviation.put(oid, soid);
        }
        associatedOntologyIds.get(e.getKey()).add(ontologyIdAbbreviation.get(oid));
      }
    }
    List<Integer> sorted = new ArrayList<>(mapInversed.keySet());
    sorted.sort(Collections.reverseOrder());
    StringBuilder sb = new StringBuilder();
    sb.append("\n\n" + "# Axiom Impact " + "\n");
    for (Integer i : sorted) {
      sb.append(renderAxiomWithImpact(mapInversed.get(i), i, axiomRenderer, associatedOntologyIds));
    }
    sb.append("\n\n" + "# Ontologies used: " + "\n");
    for (OWLOntologyID oid : ontologyIdAbbreviation.keySet()) {
      String soid = ontologyIdAbbreviation.get(oid);
      String oiri = oid.getOntologyIRI().or(IRI.create("unknown.iri")).toString();
      sb.append("- ").append(soid).append(" (").append(oiri).append(")\n");
    }
    return sb.toString();
  }

  private static int ontologyCounter = 1;

  private static String getAbbreviationForOntologyID(OWLOntologyID oid) {
    String soid = "O" + ontologyCounter;
    Optional<IRI> oiri = oid.getOntologyIRI();
    if (oiri.isPresent()) {
      IRI iri = oiri.get();
      String shortform = iri.getShortForm();
      if (!shortform.isEmpty()) {
        return shortform;
      } else {
        ontologyCounter++;
      }
    }
    return soid;
  }

  private static Set<OWLOntologyID> getOntologyIds(OWLAxiom ax, OWLOntology o) {
    Set<OWLOntologyID> importsClosure = new HashSet<>();
    for (OWLOntology closure : o.getImportsClosure()) {
      if (closure.getAxioms(Imports.EXCLUDED).contains(ax)) {
        importsClosure.add(closure.getOntologyID());
      }
    }
    return importsClosure;
  }

  /**
   * Render axiom tree in indented Markdown using the provided renderer.
   *
   * @param tree indented collection of axioms
   * @param renderer renderer for displaying axioms and entities
   * @return markdown string
   */
  private static String renderTree(Tree<OWLAxiom> tree, OWLObjectRenderer renderer) {
    StringBuilder builder = new StringBuilder();
    if (tree.isRoot()) {
      builder.append("## ");
      builder.append(renderer.render(tree.getUserObject()));
      builder.append(" ##");
      builder.append("\n");
    } else {
      String padding =
          tree.getPathToRoot().stream().skip(1).map(x -> "  ").collect(Collectors.joining());
      builder.append(padding);
      builder.append("- ");
      builder.append(renderer.render(tree.getUserObject()));
    }
    if (!tree.isLeaf()) builder.append("\n");
    String children =
        tree.getChildren()
            .stream()
            .map(child -> renderTree(child, renderer))
            .collect(Collectors.joining("\n"));
    builder.append(children);
    return builder.toString();
  }

  private static String renderAxiomWithImpact(
      Set<OWLAxiom> axioms,
      int impact,
      OWLObjectRenderer renderer,
      Map<OWLAxiom, Set<String>> associatedOntologyIds) {
    StringBuilder builder = new StringBuilder();
    builder.append("## Axioms used " + impact + " times" + "\n");
    for (OWLAxiom ax : axioms) {
      builder.append(
          "- "
              + renderer.render(ax)
              + " ["
              + String.join(",", associatedOntologyIds.get(ax))
              + "]§\n");
    }
    builder.append("\n");
    return builder.toString();
  }

  private static Set<Explanation<OWLAxiom>> getUnsatExplanationsForClasses(
      OWLOntology ontology,
      OWLReasonerFactory reasonerFactory,
      int max,
      List<OWLClass> unsatisfiable_classes) {
    Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
    for (OWLClass unsat_cl : unsatisfiable_classes) {
      OWLAxiom axiom = df.getOWLSubClassOfAxiom(unsat_cl, df.getOWLNothing());
      explanations.addAll(ExplainOperation.explain(axiom, ontology, reasonerFactory, max));
    }
    return explanations;
  }

  /**
   * A ShortFormProvider which renders entities as Markdown links, using another provided
   * ShortFormProvider to render entity labels.
   */
  private static class MarkdownLinkShortFormProvider implements ShortFormProvider {

    final ShortFormProvider labelProvider;

    public MarkdownLinkShortFormProvider(ShortFormProvider labelProvider) {
      this.labelProvider = labelProvider;
    }

    @Nonnull
    @Override
    public String getShortForm(@Nonnull OWLEntity entity) {
      String label = labelProvider.getShortForm(entity);
      return "[" + label + "](" + entity.getIRI().toString() + ")";
    }

    @Override
    public void dispose() {}
  }
}
