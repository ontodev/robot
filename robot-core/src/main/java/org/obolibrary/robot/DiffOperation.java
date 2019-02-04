package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the differences between two ontology files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class DiffOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(DiffOperation.class);

  /** The base IRI for OBO terms. */
  private static String oboBase = "http://purl.obolibrary.org/obo/";

  /** A pattern for matching IRI strings. */
  private static Pattern iriPattern = Pattern.compile("<(http\\S+)>");

  /**
   * Return default diff options.
   *
   * @return diff options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("labels", "false");
    return options;
  }

  /**
   * Given two ontologies, compare their sets of axiom strings, returning true if they are identical
   * and false otherwise.
   *
   * @param ontology1 the first ontology
   * @param ontology2 the second ontology
   * @return true if the ontologies are the same, false otherwise
   */
  public static boolean equals(OWLOntology ontology1, OWLOntology ontology2) {
    try {
      return compare(ontology1, ontology2, null);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Given two ontologies and a Writer, get the differences between axiom strings and write then to
   * the writer. The ontologies are not changed.
   *
   * @param ontology1 the first ontology
   * @param ontology2 the second ontology
   * @param writer the Writer for the report, or null
   * @return true if the ontologies are the same, false otherwise
   * @throws IOException on writer failure
   */
  public static boolean compare(OWLOntology ontology1, OWLOntology ontology2, Writer writer)
      throws IOException {
    return compare(ontology1, ontology2, new IOHelper(), writer, getDefaultOptions());
  }

  /**
   * Given two ontologies, a Writer, and a set of diff options, get the differences between axiom
   * strings and write then to the writer. The ontologies are not changed.
   *
   * @param ontology1 the first ontology
   * @param ontology2 the second ontology
   * @param ioHelper IOHelper to use for prefixes
   * @param writer the Writer for the report, or null
   * @param options diff options
   * @return true if the ontologies are the same, false otherwise
   * @throws IOException on writer failure
   */
  public static boolean compare(
      OWLOntology ontology1,
      OWLOntology ontology2,
      IOHelper ioHelper,
      Writer writer,
      Map<String, String> options)
      throws IOException {

    boolean useLabels = OptionsHelper.optionIsTrue(options, "labels");

    Map<IRI, String> labels = OntologyHelper.getLabels(ontology1);
    labels.putAll(OntologyHelper.getLabels(ontology2));

    Set<String> strings1 = getAxiomStrings(ontology1);
    Set<String> strings2 = getAxiomStrings(ontology2);
    Set<String> sorted;

    Set<String> strings1minus2 = new HashSet<>(strings1);
    strings1minus2.removeAll(strings2);
    Set<String> strings2minus1 = new HashSet<>(strings2);
    strings2minus1.removeAll(strings1);

    if (strings1minus2.size() == 0 && strings2minus1.size() == 0) {
      if (writer != null) {
        writer.write("Ontologies are identical\n");
      }
      return true;
    }

    if (writer == null) {
      return false;
    }

    writer.write(strings1minus2.size() + " axioms in Ontology 1 but not in Ontology 2:\n");
    sorted = new TreeSet<>();
    for (String axiom : strings1minus2) {
      if (useLabels) {
        sorted.add("- " + addLabels(ioHelper, labels, axiom) + "\n");
      } else {
        sorted.add("- " + axiom + "\n");
      }
    }
    for (String axiom : sorted) {
      writer.write(axiom);
    }

    writer.write("\n");

    writer.write(strings2minus1.size() + " axioms in Ontology 2 but not in Ontology 1:\n");
    sorted = new TreeSet<>();
    for (String axiom : strings2minus1) {
      if (useLabels) {
        sorted.add("+ " + addLabels(ioHelper, labels, axiom) + "\n");
      } else {
        sorted.add("+ " + axiom + "\n");
      }
    }
    for (String axiom : sorted) {
      writer.write(axiom);
    }

    return false;
  }

  /**
   * Given a map from IRIs to labels and an axiom string, add labels next to any IRIs in the string,
   * shorten OBO IRIs, and return the updated string.
   *
   * @param labels a map from IRIs to label strings
   * @param axiom a string representation of an OWLAxiom
   * @return a string with labels inserted next to IRIs
   */
  public static String addLabels(Map<IRI, String> labels, String axiom) {
    Matcher matcher = iriPattern.matcher(axiom);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      IRI iri = IRI.create(matcher.group(1));
      String id = iri.toString();
      if (id.startsWith(oboBase)) {
        id = id.substring(oboBase.length()).replace("_", ":");
      }
      String replacement = "<" + id + ">";
      if (labels.containsKey(iri)) {
        replacement = "<" + id + ">[" + labels.get(iri) + "]";
      }
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Given an IOHelper to get short form of IRIs, a map from IRIs to labels, and an axiom string,
   * add labels next to any IRIs in the string, shorten OBO IRIs, and return the updated string.
   *
   * @param ioHelper IOHelper to use for prefixes
   * @param labels a map from IRIs to label strings
   * @param axiom a string representation of an OWLAxiom
   * @return a string with labels inserted next to CURIEs
   */
  public static String addLabels(IOHelper ioHelper, Map<IRI, String> labels, String axiom) {
    DefaultPrefixManager pm = ioHelper.getPrefixManager();
    Matcher matcher = iriPattern.matcher(axiom);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      IRI iri = IRI.create(matcher.group(1));
      String id = pm.getShortForm(iri);
      if (id.startsWith("obo:")) {
        id = id.substring(4).replace("_", ":");
      }
      if (!id.startsWith("<") && !id.endsWith(">")) {
        id = "<" + id + ">";
      }
      String replacement = id;
      if (labels.containsKey(iri)) {
        replacement = id + "[" + labels.get(iri) + "]";
      }
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Given an ontology, return a list of strings for all the axioms of that ontology.
   *
   * @param ontology the ontology to use
   * @return a set of strings, one for each axiom in the ontology
   */
  public static Set<String> getAxiomStrings(OWLOntology ontology) {
    Set<String> strings = new HashSet<>();
    strings.add(ontology.getOntologyID().toString());
    for (OWLAxiom axiom : ontology.getAxioms()) {
      strings.add(axiom.toString().replaceAll("\\n", "\\n"));
    }
    for (OWLAnnotation annotation : ontology.getAnnotations()) {
      strings.add(annotation.toString().replaceAll("\\n", "\\n"));
    }
    return strings;
  }
}
