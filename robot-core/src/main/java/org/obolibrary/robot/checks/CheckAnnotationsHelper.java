package org.obolibrary.robot.checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.checks.InvalidCardinality.Op;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Utils for checking annotations
 *
 * @author cjm
 */
public class CheckAnnotationsHelper {

  /**
   * @param anns
   * @return Map keyed by property
   */
  public static Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> collectAnnotations(
      Set<OWLAnnotation> anns) {
    Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> m = new HashMap<>();
    for (OWLAnnotation ann : anns) {
      OWLAnnotationProperty p = ann.getProperty();
      OWLAnnotationValue v = ann.getValue();
      if (!m.containsKey(p)) m.put(p, new HashSet<>());
      m.get(p).add(v);
    }
    return m;
  }

  public static Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> collectAnnotationsFromAxioms(
      Set<OWLAnnotationAssertionAxiom> aas) {
    Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> m = new HashMap<>();
    for (OWLAnnotationAssertionAxiom ax : aas) {
      OWLAnnotationProperty p = ax.getProperty();
      OWLAnnotationValue v = ax.getValue();
      if (!m.containsKey(p)) m.put(p, new HashSet<>());
      m.get(p).add(v);
    }
    return m;
  }

  /**
   * @param ontology
   * @param pname
   * @param minCardinality
   * @param maxCardinality
   * @param amap
   * @return InvalidCardinality or null
   */
  public static InvalidCardinality checkCardinality(
      IOHelper helper,
      OWLOntology ontology,
      String pname,
      int minCardinality,
      Integer maxCardinality,
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap) {

    IRI iri = helper.createIRI(pname);
    OWLAnnotationProperty p =
        ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(iri);
    return checkCardinality(p, minCardinality, maxCardinality, amap);
  }

  /**
   * @param p
   * @param minCardinality
   * @param maxCardinality
   * @param amap
   * @return InvalidCardinality or null
   */
  public static InvalidCardinality checkCardinality(
      OWLAnnotationProperty p,
      int minCardinality,
      Integer maxCardinality,
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap) {
    if (!amap.containsKey(p)) {
      if (minCardinality > 0) {
        return new InvalidCardinality(p, 0, Op.LESS_THAN, minCardinality);
      } else return null;
    }
    int num = amap.get(p).size();
    if (num < minCardinality) {
      return new InvalidCardinality(p, num, Op.LESS_THAN, minCardinality);
    }
    if (maxCardinality != null && num > maxCardinality) {
      return new InvalidCardinality(p, num, Op.MORE_THAN, maxCardinality);
    }
    return null;
  }
}
