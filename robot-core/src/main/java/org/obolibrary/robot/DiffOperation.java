package org.obolibrary.robot;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.geneontology.owl.differ.Differ;
import org.geneontology.owl.differ.render.BasicDiffRenderer;
import org.geneontology.owl.differ.render.HTMLDiffRenderer;
import org.geneontology.owl.differ.render.MarkdownGroupedDiffRenderer;
import org.geneontology.owl.differ.shortform.DoubleShortFormProvider;
import org.geneontology.owl.differ.shortform.OBOShortenerShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
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
   * Given two ontologies, compare their sets of axioms, annotations, imports, and ontology IDs,
   * returning true if they are identical and false otherwise.
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
   * Given two ontologies and a Writer, get their differences and write then to the writer. The
   * ontologies are not changed.
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
   * Given two ontologies, a Writer, and a set of diff options, get their differences and write then
   * to the writer. The ontologies are not changed.
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
    String format = OptionsHelper.getOption(options, "format", "plain");
    format = format.toLowerCase();
    if (useLabels && format.equals("plain")) {
      format = "pretty";
    }

    Differ.BasicDiff diff = Differ.diff(ontology1, ontology2);

    if (diff.isEmpty()) {
      if (writer != null) {
        writer.write("Ontologies are identical\n");
      }
      return true;
    }

    if (writer == null) {
      return false;
    }

    OWLOntologySetProvider ontologyProvider =
        new DualOntologySetProvider(
            ontology1.getOWLOntologyManager(), ontology2.getOWLOntologyManager());

    switch (format) {
      case "plain":
        writer.write(BasicDiffRenderer.renderPlain(diff));
        break;
      case "pretty":
        DefaultPrefixManager pm = ioHelper.getPrefixManager();
        AnnotationValueShortFormProvider labelProvider =
            new AnnotationValueShortFormProvider(
                ontologyProvider,
                pm,
                pm,
                Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
                Collections.emptyMap());
        OBOShortenerShortFormProvider iriProvider = new OBOShortenerShortFormProvider(pm);
        DoubleShortFormProvider doubleProvider =
            new DoubleShortFormProvider(iriProvider, labelProvider);
        writer.write(BasicDiffRenderer.render(diff, doubleProvider));
        break;
      case "markdown":
        Differ.GroupedDiff groupedForMarkdown = Differ.groupedDiff(diff);
        writer.write(MarkdownGroupedDiffRenderer.render(groupedForMarkdown, ontologyProvider));
        break;
      case "html":
        Differ.GroupedDiff groupedForHTML = Differ.groupedDiff(diff);
        writer.write(HTMLDiffRenderer.render(groupedForHTML, ontologyProvider));
        break;
      default:
        throw new IOException("Unknown diff format: " + format);
    }

    return false;
  }

  /** OWLOntologySetProvider for two (left and right) ontologies. */
  private static class DualOntologySetProvider implements OWLOntologySetProvider {

    private static final long serialVersionUID = -8942374248162307075L;
    private final Set<OWLOntology> ontologies = new HashSet<>();

    /**
     * Init a new DualOntologySetProvider for a left and right ontology.
     *
     * @param left OWLOntologySetProvider for left ontology
     * @param right OWLOntologySetProvider for right ontology
     */
    public DualOntologySetProvider(OWLOntologySetProvider left, OWLOntologySetProvider right) {
      ontologies.addAll(left.getOntologies());
      ontologies.addAll(right.getOntologies());
    }

    /**
     * Get the ontologies in the provider.
     *
     * @return Set of OWLOntologies
     */
    @Nonnull
    @Override
    public Set<OWLOntology> getOntologies() {
      return Collections.unmodifiableSet(ontologies);
    }
  }

  /**
   * Given a map from IRIs to labels and an axiom string, add labels next to any IRIs in the string,
   * shorten OBO IRIs, and return the updated string.
   *
   * @param labels a map from IRIs to label strings
   * @param axiom a string representation of an OWLAxiom
   * @return a string with labels inserted next to IRIs
   * @deprecated This functionality is now provided by the owl-diff library and no longer used to
   *     format results in DiffOperation.
   */
  @Deprecated
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
   * @deprecated This functionality is now provided by the owl-diff library and no longer used to
   *     format results in DiffOperation.
   */
  @Deprecated
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
   * @deprecated This functionality is no longer used within DiffOperation.
   */
  @Deprecated
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
