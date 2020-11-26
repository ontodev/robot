package org.obolibrary.robot.metrics;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

public class MetricsUtils {

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

  private static boolean isIntegerParsable(String id) {
    try {
      Integer.parseInt(id);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isLongParsable(String id) {
    try {
      Long.parseLong(id);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isDoubleParsable(String id) {
    try {
      Double d = Double.parseDouble(id);
      return !d.isNaN();
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isNumeric(String id) {
    return (isIntegerParsable(id) || isLongParsable(id) || isDoubleParsable(id));
  }

  private static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology o, Imports includeImportsClosure, boolean skiprules, boolean leavedeclarations) {
    Set<AxiomType<?>> types = new HashSet<>();
    types.addAll(AxiomType.TBoxAxiomTypes);
    types.addAll(AxiomType.RBoxAxiomTypes);
    types.addAll(AxiomType.ABoxAxiomTypes);
    if (leavedeclarations) {
      types.add(AxiomType.DECLARATION);
    }
    return getLogicalAxioms(o, includeImportsClosure, skiprules, types);
  }

  private static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology o, Imports includeImportsClosure, boolean skiprules) {
    return getLogicalAxioms(o, includeImportsClosure, skiprules, false);
  }

  public static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology o, Imports includeImportsClosure, boolean skiprules, Set<AxiomType<?>> types) {
    return getLogicalAxioms(o, includeImportsClosure, skiprules, false, types);
  }

  private static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology o,
      Imports includeImportsClosure,
      boolean skiprules,
      boolean stripaxiomanno,
      Set<AxiomType<?>> types) {
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLAxiom ax : o.getLogicalAxioms()) {
      if (types.contains(ax.getAxiomType())) {
        axioms.add(ax);
      }
    }
    if (includeImportsClosure == Imports.INCLUDED) {
      for (OWLOntology imp : o.getImports()) {
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

  private static Set<OWLAxiom> getLogicalAxioms(
      OWLOntology o, boolean includeImportsClosure, boolean skiprules, boolean stripaxiomanno) {
    Set<OWLAxiom> axioms = new HashSet<>(o.getLogicalAxioms());
    if (includeImportsClosure) {
      for (OWLOntology imp : o.getImports()) {
        axioms.addAll(imp.getLogicalAxioms());
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

  public static File getResourcePath(Object o) {
    return new File(o.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
  }

  private static Map<? extends String, ? extends String> getFileMetrics(File file) {
    Map<String, String> data = new HashMap<>();
    data.put(MetricsLabels.FILEPATH, file.getParent());
    data.put(MetricsLabels.FILESIZE, FileUtils.sizeOf(file) + "");
    return data;
  }

  private static boolean isNaturalNumber(String value) {
    try {
      Double d = Double.valueOf(value);
      long l = d.longValue();
      return (l >= 0 && (double) l == d);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static long getLong(String sl) {
    Double d = Double.valueOf(sl);
    long s = d.longValue();
    if (d != (double) s) {
      throw new NumberFormatException(sl + " not valid long!");
    }
    return s;
  }

  private static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  private static int getNaturalNumberValue(String metrics_rec) {
    try {
      int i = Integer.parseInt(metrics_rec);
      return i;
    } catch (Exception e) {
      return -1;
    }
  }

  public static Set<AxiomType<?>> getTBoxAxiomTypes(boolean including_rbox) {
    Set<AxiomType<?>> axty = new HashSet<>();
    if (including_rbox) {
      axty.addAll(AxiomType.RBoxAxiomTypes);
    }
    axty.addAll(AxiomType.TBoxAxiomTypes);
    return axty;
  }

  private static Set<AxiomType<?>> getABoxAxiomTypes() {
    Set<AxiomType<?>> axty = new HashSet<>();
    axty.addAll(AxiomType.ABoxAxiomTypes);
    return axty;
  }

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
}
