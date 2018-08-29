package org.obolibrary.robot;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

public class ExplainOperation {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ExplainOperation.class);

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

  private static class MarkdownLinkShortFormProvider implements ShortFormProvider {

    final ShortFormProvider labelProvider;

    public MarkdownLinkShortFormProvider(ShortFormProvider labelProvider) {
      this.labelProvider = labelProvider;
    }

    @Override
    public String getShortForm(OWLEntity entity) {
      String label = labelProvider.getShortForm(entity);
      return "[" + label + "](" + entity.getIRI().toString() + ")";
    }

    @Override
    public void dispose() {}
  }
}
