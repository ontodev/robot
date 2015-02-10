package owltools2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Provides convenience methods for working with ontology and term files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class IOHelper {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(IOHelper.class);

    /**
     * Try to guess the location of the catalog.xml file.
     * Looks in the directory of the given ontology file for a catalog file.
     *
     * @param ontologyFile the
     * @return the guessed catalog File; may not exist!
     */
    public File guessCatalogFile(File ontologyFile) {
        String path = ontologyFile.getParent();
        String catalogPath = path + "/catalog-v001.xml";
        return new File(catalogPath);
    }

    /**
     * Load an ontology from a String path, using a catalog file if available.
     *
     * @param ontologyPath the path to the ontology file
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile = guessCatalogFile(ontologyFile);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a String path, with option to use catalog file.
     *
     * @param ontologyPath the path to the ontology file
     * @param useCatalog when true, a catalog file will be used if one is found
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath, boolean useCatalog)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile = null;
        if (useCatalog) {
            catalogFile = guessCatalogFile(ontologyFile);
        }
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a String path, with optional catalog file.
     *
     * @param ontologyPath the path to the ontology file
     * @param catalogPath the path to the catalog file
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath, String catalogPath)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile  = new File(catalogPath);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, using a catalog file if available.
     *
     * @param ontologyFile the ontology file to load
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile)
            throws IOException {
        File catalogFile = guessCatalogFile(ontologyFile);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, with option to use a catalog file.
     *
     * @param ontologyFile the ontology file to load
     * @param useCatalog when true, a catalog file will be used if one is found
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile, boolean useCatalog)
            throws IOException {
        File catalogFile = null;
        if (useCatalog) {
            catalogFile = guessCatalogFile(ontologyFile);
        }
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, with optional catalog File.
     *
     * @param ontologyFile the ontology file to load
     * @param catalogFile the catalog file to use
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile, File catalogFile)
            throws IOException {
        logger.debug("Loading ontology {} with catalog file {}",
                ontologyFile, catalogFile);

        OWLOntology ontology = null;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            if (catalogFile != null && catalogFile.isFile()) {
                manager.addIRIMapper(new CatalogXmlIRIMapper(catalogFile));
            }
            ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        } catch (OWLOntologyCreationException e) {
            throw new IOException(e);
        }
        return ontology;
    }

    /**
     * Save an ontology to a String path.
     *
     * @param ontology the ontology to save
     * @param ontologyPath the path to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(OWLOntology ontology, String ontologyPath)
            throws IOException {
        return saveOntology(ontology, new File(ontologyPath));
    }

    /**
     * Save an ontology to a File.
     *
     * @param ontology the ontology to save
     * @param ontologyFile the file to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(OWLOntology ontology, File ontologyFile)
            throws IOException {
        return saveOntology(ontology, IRI.create(ontologyFile));
    }

    /**
     * Save an ontology to an IRI.
     *
     * @param ontology the ontology to save
     * @param ontologyIRI the IRI to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology, IRI ontologyIRI)
            throws IOException {
        logger.debug("Saving ontology {} with to IRI {}",
                ontology, ontologyIRI);

        try {
            ontology.getOWLOntologyManager().saveOntology(
                    ontology, ontologyIRI);
        } catch (OWLOntologyStorageException e) {
            throw new IOException(e);
        }
        return ontology;
    }


    /**
     * Extract a set of term identifiers from an input string
     * by removing comments, trimming lines, and removing empty lines.
     * A comment is a space or newline followed by a '#',
     * to the end of the line. This excludes '#' characters in IRIs.
     *
     * @param input the String containing the term identifiers
     * @return a set of term identifier strings
     */
    public Set<String> extractTerms(String input) {
        Set<String> results = new HashSet<String>();
        List<String> lines = Arrays.asList(
                input.replaceAll("\\r", "").split("\\n"));
        for (String line: lines) {
            String result = line.replaceFirst("($|\\s)#.*$", "").trim();
            if (!result.isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Given a set of term identifier strings, return a set of IRIs.
     *
     * @param terms the set of term identifier strings
     * @return the set of IRIs
     * @throws IllegalArgumentException if term identifier is not a valid IRI
     */
    public Set<IRI> createIRIs(Set<String> terms)
            throws IllegalArgumentException {
        Set<IRI> iris = new HashSet<IRI>();
        for (String term: terms) {
            iris.add(IRI.create(term));
        }
        return iris;
    }

    /**
     * Parse a set of IRIs from a space-separated string, ignoring '#' comments.
     *
     * @param input the string containing the IRI strings
     * @return the set of IRIs
     * @throws IllegalArgumentException if term identifier is not a valid IRI
     */
    public Set<IRI> parseTerms(String input) throws IllegalArgumentException {
        return createIRIs(extractTerms(input));
    }

    /**
     * Load a set of IRIs from a file.
     *
     * @param path the path to the file containing the terms
     * @return the set of IRIs
     * @throws IOException on any problem
     */
    public Set<IRI> loadTerms(String path) throws IOException {
        return loadTerms(new File(path));
    }

    /**
     * Load a set of IRIs from a file.
     *
     * @param file the File containing the terms
     * @return the set of IRIs
     * @throws IOException on any problem
     */
    public Set<IRI> loadTerms(File file) throws IOException {
        String content = new Scanner(file).useDelimiter("\\Z").next();
        return parseTerms(content);
    }

}
