package owltools2;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Get the differences between two ontology files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class DiffOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(DiffOperation.class);

    /**
     * The base IRI for OBO terms.
     */
    private static String oboBase = "http://purl.obolibrary.org/obo/";

    /**
     * A pattern for matching IRI strings.
     */
    private static Pattern iriPattern = Pattern.compile("<(http\\S+)>");

    /**
     * Given two ontologies, compare their sets of axiom strings,
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
     * Given two ontologies and a Writer, get the differences between
     * axiom strings and write then to the writer.
     *
     * @param ontology1 the first ontology
     * @param ontology2 the second ontology
     * @param writer the Writer for the report, or null
     * @return true if the ontologies are the same, false otherwise
     * @throws IOException on writer failure
     */
    public static boolean compare(OWLOntology ontology1,
            OWLOntology ontology2, Writer writer) throws IOException {

        Map<IRI, String> labels = OntologyHelper.getLabels(ontology1);
        labels.putAll(OntologyHelper.getLabels(ontology2));

        Set<String> strings1 = getAxiomStrings(ontology1);
        Set<String> strings2 = getAxiomStrings(ontology2);

        Set<String> strings1minus2 = new HashSet<String>(strings1);
        strings1minus2.removeAll(strings2);
        Set<String> strings2minus1 = new HashSet<String>(strings2);
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

        writer.write(strings1minus2.size()
                + " axioms in Ontology 1 but not in Ontology 2:\n");
        for (String axiom: strings1minus2) {
            writer.write("- " + addLabels(labels, axiom) + "\n");
        }

        writer.write("\n");

        writer.write(strings2minus1.size()
                + " axioms in Ontology 2 but not in Ontology 1:\n");
        for (String axiom: strings2minus1) {
            writer.write("+ " + addLabels(labels, axiom) + "\n");
        }

        return false;
    }

    /**
     * Given a map from IRIs to labels and an axiom string,
     * add labels next to any IRIs in the string,
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
            String iri = matcher.group(1);
            String id = iri;
            if (id.startsWith(oboBase)) {
                id = id.substring(oboBase.length());
            }
            String replacement = "<" + iri + ">";
            if (labels.containsKey(iri)) {
                replacement = "<" + id + ">[" + labels.get(iri) + "]";
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Given an ontology, return a list of strings for
     * all the axioms of that ontology.
     *
     * @param ontology the ontology to use
     * @return a set of strings, one for each axiom in the ontology
     */
    public static Set<String> getAxiomStrings(OWLOntology ontology) {
        Set<OWLAxiom> axioms = ontology.getAxioms();
        Set<String> strings = new HashSet<String>();
        for (OWLAxiom axiom: axioms) {
            strings.add(axiom.toString().replaceAll("\\n", "\\n"));
        }
        return strings;
    }


}
