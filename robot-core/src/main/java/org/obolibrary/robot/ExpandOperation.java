package org.obolibrary.robot;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.vocab.PROVVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expand ontology macro relations.
 *
 * @author <a href="mailto:balhoff@renci.org">Jim Balhoff</a>
 */
public class ExpandOperation {

  private static final Logger logger = LoggerFactory.getLogger(ExpandOperation.class);

  // FIXME this property needs to be created in OMO
  private static final OWLAnnotationProperty definedByConstruct =
      OWLManager.getOWLDataFactory()
          .getOWLAnnotationProperty(
              IRI.create("http://purl.obolibrary.org/obo/OMO_defined_by_construct"));
  private static final OWLAnnotationProperty derivedFrom =
      OWLManager.getOWLDataFactory()
          .getOWLAnnotationProperty(PROVVocabulary.WAS_DERIVED_FROM.getIRI());

  public static class ExpandConfig {

    private boolean createNew = false;
    private boolean annotateExpansions = false;

    public boolean createNewOntology() {
      return createNew;
    }

    public void setCreateNewOntology(boolean shouldCreateNew) {
      this.createNew = shouldCreateNew;
    }

    public boolean annotateExpansionAxioms() {
      return annotateExpansions;
    }

    public void setAnnotateExpansionAxioms(boolean shouldAnnotate) {
      this.annotateExpansions = shouldAnnotate;
    }
  }

  public static void expand(OWLOntology ontology, ExpandConfig config)
      throws OWLOntologyStorageException, IOException {
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, true);
    Set<OWLAxiom> expansions =
        ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION, Imports.INCLUDED).stream()
            .filter(ax -> ax.getProperty().equals(definedByConstruct))
            .filter(ax -> ax.getSubject().isIRI())
            .filter(ax -> ax.getValue().isLiteral())
            .map(
                ax ->
                    executeConstruct(
                        dataset,
                        ax.getValue().asLiteral().orNull().getLiteral(),
                        (IRI) (ax.getSubject()),
                        config.annotateExpansionAxioms()))
            .reduce(ExpandOperation::combine)
            .orElse(Collections.emptySet());
    if (config.createNew) {
      ontology.getOWLOntologyManager().removeAxioms(ontology, ontology.getAxioms());
    }
    ontology.getOWLOntologyManager().addAxioms(ontology, expansions);
  }

  private static Set<OWLAxiom> executeConstruct(
      Dataset dataset, String query, IRI definitionTerm, boolean annotateAxioms) {
    Model expansionTriples = QueryOperation.execConstruct(dataset, query);
    try {
      Set<OWLAxiom> axioms =
          QueryOperation.convertModel(expansionTriples, new IOHelper(), null).getAxioms();
      final Set<OWLAxiom> annotatedAxioms;
      if (annotateAxioms) {
        Set<OWLAnnotation> derivationAnnotation =
            Collections.singleton(
                OWLManager.getOWLDataFactory().getOWLAnnotation(derivedFrom, definitionTerm));
        annotatedAxioms =
            axioms.stream()
                .map(ax -> ax.getAnnotatedAxiom(derivationAnnotation))
                .collect(Collectors.toSet());
      } else {
        annotatedAxioms = axioms;
      }
      return annotatedAxioms;
    } catch (IOException e) {
      logger.error("Unable to parse expansion triples into OWL axioms: " + definitionTerm, e);
      return Collections.emptySet();
    }
  }

  private static <T> Set<T> combine(Set<T> x, Set<T> y) {
    Set<T> combined = new HashSet<>();
    combined.addAll(x);
    combined.addAll(y);
    return combined;
  }
}
