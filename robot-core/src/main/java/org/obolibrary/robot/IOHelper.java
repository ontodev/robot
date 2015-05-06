package org.obolibrary.robot;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.Context;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

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
     * Path to default context as a resource.
     */
    private static String defaultContextPath = "/obo_context.jsonld";

    /**
     * Store the currently loaded prefixes.
     */
    private Map<String, String> prefixes = new HashMap<String, String>();

    /**
     * Create a new IOHelper with the default prefixes.
     */
    public IOHelper() {
        try {
            setPrefixes(loadContext());
        } catch (IOException e) {
            logger.warn("Could not load default prefixes.");
        }
    }

    /**
     * Create a new IOHelper with or without the default prefixes.
     *
     * @param defaults false if defaults should not be used
     */
    public IOHelper(boolean defaults) {
        try {
            if (defaults) {
                setPrefixes(loadContext());
            } else {
                setPrefixes(null);
            }
        } catch (IOException e) {
            logger.warn("Could not load default prefixes.");
        }
    }

    /**
     * Create a new IOHelper with the specified prefixes.
     *
     * @param map the prefixes to use
     */
    public IOHelper(Map<String, String> map) {
        setPrefixes(map);
    }

    /**
     * Create a new IOHelper with prefixes from a file path.
     *
     * @param path to a JSON-LD file with a @context
     */
    public IOHelper(String path) {
        try {
            setPrefixes(loadContext(path));
        } catch (IOException e) {
            logger.warn("Could not load default prefixes from " + path);
        }
    }

    /**
     * Create a new IOHelper with prefixes from a file.
     *
     * @param file a JSON-LD file with a @context
     */
    public IOHelper(File file) {
        try {
            setPrefixes(loadContext(file));
        } catch (IOException e) {
            logger.warn("Could not load prefixes from " + file);
        }
    }

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
     * Load an ontology from an InputStream, without a catalog file.
     *
     * @param ontologyStream the ontology stream to load
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(InputStream ontologyStream)
            throws IOException {
        OWLOntology ontology = null;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);
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
     * Save an ontology in the given format to a file.
     *
     * @param ontology the ontology to save
     * @param format the ontology format to use
     * @param ontologyFile the file to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology,
            OWLOntologyFormat format, File ontologyFile)
            throws IOException {
        return saveOntology(ontology, format, IRI.create(ontologyFile));
    }

    /**
     * Save an ontology in the given format to an IRI.
     *
     * @param ontology the ontology to save
     * @param format the ontology format to use
     * @param ontologyIRI the IRI to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology,
            OWLOntologyFormat format, IRI ontologyIRI)
            throws IOException {
        logger.debug("Saving ontology {} as {} with to IRI {}",
                ontology, format, ontologyIRI);
        try {
            ontology.getOWLOntologyManager().saveOntology(
                    ontology, format, ontologyIRI);
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
            if (line.trim().startsWith("#")) {
                continue;
            }
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
        PrefixManager prefixManager = getPrefixManager();
        for (String term: terms) {
            try {
                iris.add(prefixManager.getIRI(term));
            } catch (Exception e) {
                iris.add(IRI.create(term));
            }
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

    /**
     * Load a map of prefixes from "@context" of the default JSON-LD file.
     *
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Map<String, String> loadContext() throws IOException {
        return loadContext(
                IOHelper.class.getResourceAsStream(defaultContextPath));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD file
     * at the given path.
     *
     * @param path the path to the JSON-LD file
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Map<String, String> loadContext(String path)
            throws IOException {
        return loadContext(new File(path));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD file.
     *
     * @param file the JSON-LD file
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Map<String, String> loadContext(File file)
            throws IOException {
        return loadContext(new FileInputStream(file));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD InputStream.
     *
     * @param stream the JSON-LD content as an InputStream
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Map<String, String> loadContext(InputStream stream)
            throws IOException {
        String content = new Scanner(stream).useDelimiter("\\Z").next();
        return parseContext(content);
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD string.
     *
     * @param json the JSON-LD string
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Map<String, String> parseContext(String json)
            throws IOException {
        Map<String, String> prefixes = new HashMap<String, String>();
        try {
            Object context = JsonUtils.fromString(json);
            if (context instanceof Map
                && ((Map<String, Object>) context).containsKey("@context")) {
                context = ((Map<String, Object>) context).get("@context");
            }
            prefixes = new Context().parse(context).getPrefixes(false);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return prefixes;
    }

    /**
     * Make an OWLAPI PrefixManager from a map of prefixes.
     *
     * @param prefixes a map from prefix name strings to prefix IRI strings
     * @return a PrefixManager
     */
    public static PrefixManager makePrefixManager(
            Map<String, String> prefixes) {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        for (Map.Entry<String, String> entry: prefixes.entrySet()) {
            pm.setPrefix(entry.getKey() + ":", entry.getValue());
        }
        return pm;
    }

    /**
     * Load an OWLAPI PrefixManager from the default JSON-LD file.
     *
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager() throws IOException {
        return makePrefixManager(loadContext());
    }

    /**
     * Load an OWLAPI PrefixManager from the given JSON-LD file path.
     *
     * @param path to the JSON-LD file
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager(String path)
            throws IOException {
        return makePrefixManager(loadContext(path));
    }

    /**
     * Load an OWLAPI PrefixManager from the given JSON-LD file.
     *
     * @param file the JSON-LD file
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager(File file)
            throws IOException {
        return makePrefixManager(loadContext(file));
    }

    /**
     * Get a prefix manager with the current prefixes.
     *
     * @return a new PrefixManager
     */
    public PrefixManager getPrefixManager() {
        return makePrefixManager(prefixes);
    }

    /**
     * Add a prefix mapping as a single string "foo: http://example.com#".
     *
     * @param combined both prefix and target
     * @throws IllegalArgumentException on malformed input
     */
    public void addPrefix(String combined) throws IllegalArgumentException {
        String[] results = combined.split(":", 2);
        if (results.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid prefix string: " + combined);
        }
        addPrefix(results[0], results[1]);
    }

    /**
     * Add a prefix mapping as a prefix string and target string.
     *
     * @param prefix the short prefix to add; should not include ":"
     * @param target the IRI string that is the target of the prefix
     */
    public void addPrefix(String prefix, String target) {
        prefixes.put(prefix.trim(), target.trim());
    }

    /**
     * Get a copy of the current prefix map.
     *
     * @return a copy of the current prefix map
     */
    public Map<String, String> getPrefixes() {
        return new HashMap<String, String>(prefixes);
    }

    /**
     * Set the current prefix map.
     *
     * @param map the new map of prefixes to use
     */
    public void setPrefixes(Map<String, String> map) {
        if (map != null) {
            prefixes = new HashMap<String, String>(map);
        } else {
            prefixes = new HashMap<String, String>();
        }
    }

    /**
     * Return the current prefixes as a JSON-LD Context.
     *
     * @return the current JSON-LD Context
     */
    public Context getJSONLDContext() {
        return new Context(new HashMap<String, Object>(getPrefixes()));
    }

    /**
     * Return the current prefixes as a JSON-LD string.
     *
     * @return the current prefixes as a JSON-LD string
     * @throws IOException on any error
     */
    public String getJSONLDContextString() throws IOException {
        try {
            Object compact = JsonLdProcessor.compact(
                    JsonUtils.fromString("{}"),
                    new HashMap<String, Object>(getPrefixes()),
                    new JsonLdOptions());
            return JsonUtils.toPrettyString(compact);
        } catch (Exception e) {
            throw new IOException("JSON-LD could not be generated", e);
        }
    }

    /**
     * Write the current prefixes as a JSON-LD file.
     *
     * @param path the path to write the context
     * @throws IOException on any error
     */
    public void saveJSONLDContext(String path) throws IOException {
        saveJSONLDContext(new File(path));
    }

    /**
     * Write the current prefixes as a JSON-LD file.
     *
     * @param file the file to write the context
     * @throws IOException on any error
     */
    public void saveJSONLDContext(File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(getJSONLDContextString());
        writer.close();
    }

}
