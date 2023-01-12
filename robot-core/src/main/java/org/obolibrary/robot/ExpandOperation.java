package org.obolibrary.robot;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.openrdf.model.vocabulary.DCTERMS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expand ontology macro relations.
 *
 * @author <a href="mailto:balhoff@renci.org">Jim Balhoff</a>
 */
public class ExpandOperation {

  private static final Logger logger = LoggerFactory.getLogger(ExpandOperation.class);

  private static final OWLAnnotationProperty definedByConstruct =
      OWLManager.getOWLDataFactory()
          .getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/OMO_0002000"));
  private static final OWLAnnotationProperty dctSource =
      OWLManager.getOWLDataFactory()
          .getOWLAnnotationProperty(IRI.create(DCTERMS.SOURCE.stringValue()));

  public static class ExpandConfig {

    private boolean createNew = false;
    private boolean annotateExpansions = false;
    private final Set<IRI> expandProperties = new HashSet<>();
    private final Set<IRI> excludeProperties = new HashSet<>();

    public boolean getCreateNewOntology() {
      return createNew;
    }

    public void setCreateNewOntology(boolean shouldCreateNew) {
      this.createNew = shouldCreateNew;
    }

    public boolean getAnnotateExpansionAxioms() {
      return annotateExpansions;
    }

    public void setAnnotateExpansionAxioms(boolean shouldAnnotate) {
      this.annotateExpansions = shouldAnnotate;
    }

    public Set<IRI> getExpandProperties() {
      return Collections.unmodifiableSet(expandProperties);
    }

    public void setExpandProperties(Set<IRI> properties) {
      expandProperties.clear();
      expandProperties.addAll(properties);
    }

    public Set<IRI> getExcludeProperties() {
      return Collections.unmodifiableSet(excludeProperties);
    }

    public void setExcludeProperties(Set<IRI> properties) {
      excludeProperties.clear();
      excludeProperties.addAll(properties);
    }
  }

  /**
   * Expand macro relations using SPARQL CONSTRUCT
   *
   * @param ontology the ontology to expand from
   * @param config configuration for expansion operation
   * @param includeTerms IRIs of annotation properties whose expansion to perform. If empty, all
   *     found expansion are included.
   * @param excludeTerms IRIs of annotation properties whose expansion to exlude; subtracted from
   *     inclusion list.
   * @throws OWLOntologyStorageException on exception converting OWLOntology to Jena Dataset
   */
  public static void expand(
      OWLOntology ontology, ExpandConfig config, Set<IRI> includeTerms, Set<IRI> excludeTerms)
      throws OWLOntologyStorageException {
    Dataset dataset = QueryOperation.loadOntologyAsDataset(ontology, true);
    Set<OWLAxiom> expansions =
        ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION, Imports.INCLUDED).stream()
            .filter(ax -> ax.getProperty().equals(definedByConstruct))
            .filter(ax -> ax.getSubject().isIRI())
            .filter(ax -> includeTerms.isEmpty() || includeTerms.contains(ax.getSubject()))
            .filter(ax -> !excludeTerms.contains(ax.getSubject()))
            .filter(ax -> ax.getValue().isLiteral())
            .map(
                ax ->
                    executeConstruct(
                        dataset,
                        ax.getValue().asLiteral().orNull().getLiteral(),
                        (IRI) (ax.getSubject()),
                        config.getAnnotateExpansionAxioms()))
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
    expansionTriples.add(ResourceFactory.createResource(), RDF.type, OWL.Ontology);
    try {
      Set<OWLAxiom> axioms =
          QueryOperation.convertModel(expansionTriples, new IOHelper(), null).getAxioms();
      final Set<OWLAxiom> annotatedAxioms;
      if (annotateAxioms) {
        Set<OWLAnnotation> sourceAnnotation =
            Collections.singleton(
                OWLManager.getOWLDataFactory().getOWLAnnotation(dctSource, definitionTerm));
        annotatedAxioms =
            axioms.stream()
                .map(ax -> ax.getAnnotatedAxiom(sourceAnnotation))
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
