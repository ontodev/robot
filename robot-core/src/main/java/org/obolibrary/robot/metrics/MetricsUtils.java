package org.obolibrary.robot.metrics;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.HashSet;
import java.util.Set;

//@SuppressWarnings("unused")
public class MetricsUtils {

  /**
   * @param axioms set of axioms
   * @return filtered set of axioms (only ABox axioms)
   */
  public static Set<OWLAxiom> getABoxAxioms(Set<OWLAxiom> axioms) {
    Set<OWLAxiom> abox = new HashSet<>();
    Set<AxiomType<?>> types = getABoxAxiomTypes();
    for (OWLAxiom ax : axioms) {
      if (types.contains(ax.getAxiomType())) {
        abox.add(ax);
      }
    }
    return abox;
  }

  /**
   * @param ontology ontology to be checked
   * @param includeImportsClosure take into account imports
   * @param skiprules ignore rules (SWRL)
   * @param types AxiomTypes to consider
   * @return all logical axioms in the ontology
   */
  public static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology ontology,
      Imports includeImportsClosure,
      boolean skiprules,
      Set<AxiomType<?>> types) {
    return getLogicalAxioms(ontology, includeImportsClosure, skiprules, false, types);
  }

  /**
   * @param including_rbox whether or not RBox
   * @return all TBox axiom types
   */
  public static Set<AxiomType<?>> getTBoxAxiomTypes(boolean including_rbox) {
    Set<AxiomType<?>> axty = new HashSet<>();
    if (including_rbox) {
      axty.addAll(AxiomType.RBoxAxiomTypes);
    }
    axty.addAll(AxiomType.TBoxAxiomTypes);
    return axty;
  }

  /**
   * @param axioms set of axioms
   * @return filtered set of axioms (only tbox axioms)
   */
  public static Set<OWLAxiom> getTBoxAxioms(Set<OWLAxiom> axioms) {
    Set<OWLAxiom> tbox = new HashSet<>();
    Set<AxiomType<?>> types = getTBoxAxiomTypes(true);
    for (OWLAxiom ax : axioms) {
      if (types.contains(ax.getAxiomType())) {
        tbox.add(ax);
      }
    }
    return tbox;
  }

  private static Set<OWLAxiom> stripAnnotations(Set<OWLAxiom> axioms) {
    Set<OWLAxiom> newAxioms = new HashSet<>();
    for (OWLAxiom ax : axioms) {
      if (ax instanceof OWLAnnotationAssertionAxiom) {
        continue;
      }
      newAxioms.add(ax.getAxiomWithoutAnnotations());
    }
    return newAxioms;
  }

  private static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology ontology,
      Imports includeImportsClosure,
      boolean skiprules,
      boolean stripaxiomanno,
      Set<AxiomType<?>> types) {
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom ax : ontology.getLogicalAxioms()) {
      if (types.contains(ax.getAxiomType())) {
        axioms.add(ax);
      }
    }
    if (includeImportsClosure == Imports.INCLUDED) {
      for (OWLOntology imp : ontology.getImports()) {
        for (OWLAxiom ax : imp.getLogicalAxioms()) {
          if (types.contains(ax.getAxiomType())) {
            axioms.add(ax);
          }
        }
      }
    }
    if (skiprules) {
      stripRules(axioms);
    }
    if (stripaxiomanno) {
      axioms = stripAnnotations(axioms);
    }
    return axioms;
  }

  private static void stripRules(Set<OWLAxiom> axioms) {
    Set<OWLAxiom> tmp = new HashSet<>(axioms);
    axioms.clear();
    for (OWLAxiom ax : tmp) {
      if (!ax.getAxiomType().toString().equals("Rule")) {
        axioms.add(ax);
      }
    }
  }

  private static Set<AxiomType<?>> getABoxAxiomTypes() {
    return new HashSet<>(AxiomType.ABoxAxiomTypes);
  }
}
