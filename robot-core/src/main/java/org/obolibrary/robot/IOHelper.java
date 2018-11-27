package org.obolibrary.robot;

import com.github.jsonldjava.core.Context;
import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.collect.Sets;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.geneontology.obographs.io.OboGraphJsonDocumentFormat;
import org.geneontology.obographs.io.OgJsonGenerator;
import org.geneontology.obographs.model.GraphDocument;
import org.geneontology.obographs.owlapi.FromOwl;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.XMLWriterPreferences;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Provides convenience methods for working with ontology and term files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class IOHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(IOHelper.class);

  /** Namespace for error messages. */
  private static final String NS = "errors#";

  /** Error message when the specified file does not exist. Expects file name. */
  private static final String fileDoesNotExistError =
      NS + "FILE DOES NOT EXIST ERROR File does not exist: %s";

  /** Error message when an invalid extension is provided (file format). Expects the file format. */
  static final String invalidFormatError = NS + "INVALID FORMAT ERROR unknown format: %s";

  /** Error message when the specified file cannot be loaded. Expects the file name. */
  private static final String invalidOntologyFileError =
      NS + "INVALID ONTOLOGY FILE ERROR Could not load a valid ontology from file: %s";

  /** Error message when the specified IRI cannot be loaded. Expects the IRI string. */
  private static final String invalidOntologyIRIError =
      NS + "INVALID ONTOLOGY IRI ERROR Could not load a valid ontology from IRI: %s";

  /** Error message when the specified input stream cannot be loaded. */
  private static final String invalidOntologyStreamError =
      NS + "INVALID ONTOLOGY STREAM ERROR Could not load a valid ontology from InputStream.";

  /** Error message when an invalid prefix is provided. Expects the combined prefix. */
  static final String invalidPrefixError = NS + "INVALID PREFIX ERROR Invalid prefix string: %s";

  /** Error message when a JSON-LD context cannot be created, for any reason. */
  private static final String jsonldContextCreationError =
      NS + "JSON-LD CONTEXT CREATION ERROR Could not create the JSON-LD context.";

  /** Error message when a JSON-LD context cannot be read, for any reason. */
  private static final String jsonldContextParseError =
      NS + "JSON-LD CONTEXT PARSE ERROR Could not parse the JSON-LD context.";

  /** Error message when the ontology cannot be saved. Expects the IRI string. */
  private static final String ontologyStorageError =
      NS + "ONTOLOGY STORAGE ERROR Could not save ontology to IRI: %s";

  /** Error message when a prefix cannot be loaded. Expects the prefix and target. */
  private static final String prefixLoadError =
      NS + "PREFIX LOAD ERROR Could not load prefix '%s' for '%s'";

  /** Path to default context as a resource. */
  private static String defaultContextPath = "/obo_context.jsonld";

  /** Store the current JSON-LD context. */
  private Context context = new Context();

  /** Store xml entities flag. */
  private Boolean useXMLEntities = false;

  /**
   * Create a new IOHelper with the default prefixes.
   *
   * @throws IOException on problem getting default context
   */
  public IOHelper() throws IOException {
    setContext(getDefaultContext());
  }

  /**
   * Create a new IOHelper with or without the default prefixes.
   *
   * @param defaults false if defaults should not be used
   * @throws IOException on problem getting default context
   */
  public IOHelper(boolean defaults) throws IOException {
    if (defaults) {
      setContext(getDefaultContext());
    } else {
      setContext();
    }
  }

  /**
   * Create a new IOHelper with the specified prefixes.
   *
   * @param map the prefixes to use
   * @throws IOException on issue parsing map
   */
  public IOHelper(Map<String, Object> map) throws IOException {
    setContext(map);
  }

  /**
   * Create a new IOHelper with prefixes from a file path.
   *
   * @param path to a JSON-LD file with a @context
   * @throws IOException on issue parsing JSON-LD file as context
   */
  public IOHelper(String path) throws IOException {
    String jsonString = FileUtils.readFileToString(new File(path));
    setContext(jsonString);
  }

  /**
   * Create a new IOHelper with prefixes from a file.
   *
   * @param file a JSON-LD file with a @context
   * @throws IOException on issue reading file or setting context from file
   */
  public IOHelper(File file) throws IOException {
    String jsonString = FileUtils.readFileToString(file);
    setContext(jsonString);
  }

  /**
   * Try to guess the location of the catalog.xml file. Looks in the directory of the given ontology
   * file for a catalog file.
   *
   * @param ontologyFile the
   * @return the guessed catalog File; may not exist!
   */
  public File guessCatalogFile(File ontologyFile) {
    String path = ontologyFile.getParent();
    String catalogPath = "catalog-v001.xml";
    if (path != null) {
      catalogPath = path + "/catalog-v001.xml";
    }
    return new File(catalogPath);
  }

  /**
   * Load an ontology from a String path, using a catalog file if available.
   *
   * @param ontologyPath the path to the ontology file
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(String ontologyPath) throws IOException {
    File ontologyFile = new File(ontologyPath);
    File catalogFile = guessCatalogFile(ontologyFile);
    if (!catalogFile.isFile()) {
      // If the catalog file does not exist, do not use catalog
      catalogFile = null;
    }
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
  public OWLOntology loadOntology(String ontologyPath, boolean useCatalog) throws IOException {
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
  public OWLOntology loadOntology(String ontologyPath, String catalogPath) throws IOException {
    File ontologyFile = new File(ontologyPath);
    File catalogFile = new File(catalogPath);
    return loadOntology(ontologyFile, catalogFile);
  }

  /**
   * Load an ontology from a File, using a catalog file if available.
   *
   * @param ontologyFile the ontology file to load
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(File ontologyFile) throws IOException {
    File catalogFile = guessCatalogFile(ontologyFile);
    if (!catalogFile.isFile()) {
      // If the catalog file does not exist, do not use catalog
      catalogFile = null;
    }
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
  public OWLOntology loadOntology(File ontologyFile, boolean useCatalog) throws IOException {
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
  public OWLOntology loadOntology(File ontologyFile, File catalogFile) throws IOException {
    logger.debug("Loading ontology {} with catalog file {}", ontologyFile, catalogFile);
    Object jsonObject = null;
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    try {
      String extension = FilenameUtils.getExtension(ontologyFile.getName());
      extension = extension.trim().toLowerCase();
      if (extension.equals("yml") || extension.equals("yaml")) {
        logger.debug("Converting from YAML to JSON");
        String yamlString = FileUtils.readFileToString(ontologyFile);
        jsonObject = new Yaml().load(yamlString);
      } else if (extension.equals("js") || extension.equals("json") || extension.equals("jsonld")) {
        String jsonString = FileUtils.readFileToString(ontologyFile);
        jsonObject = JsonUtils.fromString(jsonString);
      }

      // Use Jena to convert a JSON-LD string to RDFXML, then load it
      if (jsonObject != null) {
        logger.debug("Converting from JSON to RDF");
        jsonObject = new JsonLdApi().expand(getContext(), jsonObject);
        String jsonString = JsonUtils.toString(jsonObject);
        Model model = ModelFactory.createDefaultModel();
        model.read(IOUtils.toInputStream(jsonString), null, "JSON-LD");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // model.write(System.out);
        model.write(output);
        byte[] data = output.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        return loadOntology(input);
      }
      // Handle catalog file
      if (catalogFile != null && catalogFile.isFile()) {
        manager.setIRIMappers(Sets.newHashSet(new CatalogXmlIRIMapper(catalogFile)));
      }
      // Maybe unzip
      if (ontologyFile.getPath().endsWith(".gz")) {
        if (catalogFile == null) {
          return loadCompressedOntology(ontologyFile, null);
        } else {
          return loadCompressedOntology(ontologyFile, catalogFile.getAbsolutePath());
        }
      }
      // Otherwise load from file using default method
      return manager.loadOntologyFromOntologyDocument(ontologyFile);
    } catch (JsonLdError | OWLOntologyCreationException e) {
      throw new IOException(String.format(invalidOntologyFileError, ontologyFile.getName()), e);
    }
  }

  /**
   * Load an ontology from an InputStream, without a catalog file.
   *
   * @param ontologyStream the ontology stream to load
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(InputStream ontologyStream) throws IOException {
    return loadOntology(ontologyStream, null);
  }

  /**
   * Load an ontology from an InputStream with a catalog file.
   *
   * @param ontologyStream the ontology stream to load
   * @param catalogPath the catalog file to use or null
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(InputStream ontologyStream, String catalogPath)
      throws IOException {
    OWLOntology ontology;
    // Maybe load a catalog file
    File catalogFile = null;
    if (catalogPath != null) {
      catalogFile = new File(catalogPath);
      if (!catalogFile.isFile()) {
        throw new IOException(String.format(fileDoesNotExistError, catalogPath));
      }
    }
    try {
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      if (catalogFile != null) {
        manager.setIRIMappers(Sets.newHashSet(new CatalogXmlIRIMapper(catalogFile)));
      }
      ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);
    } catch (OWLOntologyCreationException e) {
      throw new IOException(invalidOntologyStreamError, e);
    }
    return ontology;
  }

  /**
   * Given an ontology IRI, load the ontology from the IRI.
   *
   * @param ontologyIRI the ontology IRI to load
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(IRI ontologyIRI) throws IOException {
    return loadOntology(ontologyIRI, null);
  }

  /**
   * Given an IRI and a path to a catalog file, load the ontology from the IRI with the catalog.
   *
   * @param ontologyIRI the ontology IRI to load
   * @param catalogPath the catalog file to use or null
   * @return a new ontology object, with a new OWLManager
   * @throws IOException on any problem
   */
  public OWLOntology loadOntology(IRI ontologyIRI, String catalogPath) throws IOException {
    OWLOntology ontology;
    // Maybe load a catalog file
    File catalogFile = null;
    if (catalogPath != null) {
      catalogFile = new File(catalogPath);
      if (!catalogFile.isFile()) {
        throw new IOException(String.format(fileDoesNotExistError, catalogPath));
      }
    }
    try {
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      // If a catalog file was loaded, set IRI mappers
      if (catalogFile != null) {
        manager.setIRIMappers(Sets.newHashSet(new CatalogXmlIRIMapper(catalogFile)));
      }
      // Maybe load a zipped ontology
      if (ontologyIRI.toString().endsWith(".gz")) {
        ontology = loadCompressedOntology(new URL(ontologyIRI.toString()), catalogPath);
      } else {
        // Otherwise load ontology as normal
        ontology = manager.loadOntologyFromOntologyDocument(ontologyIRI);
      }
    } catch (OWLOntologyCreationException e) {
      throw new IOException(e);
    }
    return ontology;
  }

  /**
   * Given the name of a file format, return an instance of it.
   *
   * <p>Suported formats:
   *
   * <ul>
   *   <li>OBO as 'obo'
   *   <li>RDFXML as 'owl'
   *   <li>Turtle as 'ttl'
   *   <li>OWLXML as 'owx'
   *   <li>Manchester as 'omn'
   *   <li>OWL Functional as 'ofn'
   * </ul>
   *
   * @param formatName the name of the format
   * @return an instance of the format
   * @throws IllegalArgumentException if format name is not recognized
   */
  public static OWLDocumentFormat getFormat(String formatName) throws IllegalArgumentException {
    formatName = formatName.trim().toLowerCase();
    switch (formatName) {
      case "obo":
        return new OBODocumentFormat();
      case "owl":
        return new RDFXMLDocumentFormat();
      case "ttl":
        return new TurtleDocumentFormat();
      case "owx":
        return new OWLXMLDocumentFormat();
      case "omn":
        return new ManchesterSyntaxDocumentFormat();
      case "ofn":
        return new FunctionalSyntaxDocumentFormat();
      case "json":
        return new OboGraphJsonDocumentFormat();
      default:
        throw new IllegalArgumentException(String.format(invalidFormatError, formatName));
    }
  }

  /**
   * Save an ontology to a String path.
   *
   * @param ontology the ontology to save
   * @param ontologyPath the path to save the ontology to
   * @return the saved ontology
   * @throws IOException on any problem
   */
  public OWLOntology saveOntology(OWLOntology ontology, String ontologyPath) throws IOException {
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
  public OWLOntology saveOntology(OWLOntology ontology, File ontologyFile) throws IOException {
    return saveOntology(ontology, IRI.create(ontologyFile));
  }

  /**
   * Save an ontology to an IRI, using the file extension to determine the format.
   *
   * @param ontology the ontology to save
   * @param ontologyIRI the IRI to save the ontology to
   * @return the saved ontology
   * @throws IOException on any problem
   */
  public OWLOntology saveOntology(final OWLOntology ontology, IRI ontologyIRI) throws IOException {
    try {
      String path = ontologyIRI.toString();
      if (path.endsWith(".gz")) {
        path = path.substring(0, path.lastIndexOf("."));
      }
      String formatName = FilenameUtils.getExtension(path);
      OWLDocumentFormat format = getFormat(formatName);
      return saveOntology(ontology, format, ontologyIRI, true);
    } catch (Exception e) {
      throw new IOException(String.format(ontologyStorageError, ontologyIRI.toString()), e);
    }
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
  public OWLOntology saveOntology(
      final OWLOntology ontology, OWLDocumentFormat format, File ontologyFile) throws IOException {
    return saveOntology(ontology, format, ontologyFile, true);
  }

  /**
   * Save an ontology in the given format to a file, with the option to ignore OBO document checks.
   *
   * @param ontology the ontology to save
   * @param format the ontology format to use
   * @param ontologyFile the file to save the ontology to
   * @param checkOBO if false, ignore OBO document checks
   * @return the saved ontology
   * @throws IOException on any problem
   */
  public OWLOntology saveOntology(
      final OWLOntology ontology, OWLDocumentFormat format, File ontologyFile, boolean checkOBO)
      throws IOException {
    return saveOntology(ontology, format, IRI.create(ontologyFile), checkOBO);
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
  public OWLOntology saveOntology(
      final OWLOntology ontology, OWLDocumentFormat format, IRI ontologyIRI) throws IOException {
    return saveOntology(ontology, format, ontologyIRI, true);
  }

  /**
   * Save an ontology in the given format to a path, with the option to ignore OBO document checks.
   *
   * @param ontology the ontology to save
   * @param format the ontology format to use
   * @param ontologyPath the path to save the ontology to
   * @param checkOBO if false, ignore OBO document checks
   * @return the saved ontology
   * @throws IOException on any problem
   */
  public OWLOntology saveOntology(
      final OWLOntology ontology, OWLDocumentFormat format, String ontologyPath, boolean checkOBO)
      throws IOException {
    return saveOntology(ontology, format, IRI.create(new File(ontologyPath)), checkOBO);
  }

  /**
   * Save an ontology in the given format to an IRI, with the option to ignore OBO document checks.
   *
   * @param ontology the ontology to save
   * @param format the ontology format to use
   * @param ontologyIRI the IRI to save the ontology to
   * @param checkOBO if false, ignore OBO document checks
   * @return the saved ontology
   * @throws IOException on any problem
   */
  public OWLOntology saveOntology(
      final OWLOntology ontology, OWLDocumentFormat format, IRI ontologyIRI, boolean checkOBO)
      throws IOException {
    logger.debug("Saving ontology {} as {} with to IRI {}", ontology, format, ontologyIRI);
    // if (format instanceof PrefixOWLDocumentFormat) {
    //    ((PrefixOWLDocumentFormat) format)
    //        .copyPrefixesFrom(getPrefixManager());
    // }
    XMLWriterPreferences.getInstance().setUseNamespaceEntities(getXMLEntityFlag());
    // If saving in compressed format, get byte data then save to gzip
    if (ontologyIRI.toString().endsWith(".gz")) {
      byte[] data = getOntologyFileData(ontology, format, checkOBO);
      saveCompressedOntology(data, ontologyIRI);
      return ontology;
    }
    // If not compressed, just save the file as-is
    saveOntologyFile(ontology, format, ontologyIRI, checkOBO);
    return ontology;
  }

  /**
   * Extract a set of term identifiers from an input string by removing comments, trimming lines,
   * and removing empty lines. A comment is a space or newline followed by a '#', to the end of the
   * line. This excludes '#' characters in IRIs.
   *
   * @param input the String containing the term identifiers
   * @return a set of term identifier strings
   */
  public Set<String> extractTerms(String input) {
    Set<String> results = new HashSet<>();
    String[] lines = input.replaceAll("\\r", "").split("\\n");
    for (String line : lines) {
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
   * Given a term string, use the current prefixes to create an IRI.
   *
   * @param term the term to convert to an IRI
   * @return the new IRI
   */
  public IRI createIRI(String term) {
    if (term == null) {
      return null;
    }

    try {
      // This is stupid, because better methods aren't public.
      // We create a new JSON map and add one entry
      // with the term as the key and some string as the value.
      // Then we run the JsonLdApi to expand the JSON map
      // in the current context, and just grab the first key.
      // If everything worked, that key will be our expanded iri.
      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put(term, "ignore this string");
      Object expanded = new JsonLdApi().expand(context, jsonMap);
      String result = ((Map<String, Object>) expanded).keySet().iterator().next();
      if (result != null) {
        return IRI.create(result);
      }
    } catch (Exception e) {
      logger.warn("Could not create IRI for {}", term);
      logger.warn(e.getMessage());
    }
    return null;
  }

  /**
   * Given a set of term identifier strings, return a set of IRIs.
   *
   * @param terms the set of term identifier strings
   * @return the set of IRIs
   * @throws IllegalArgumentException if term identifier is not a valid IRI
   */
  public Set<IRI> createIRIs(Set<String> terms) throws IllegalArgumentException {
    Set<IRI> iris = new HashSet<>();
    for (String term : terms) {
      IRI iri = createIRI(term);
      if (iri != null) {
        iris.add(iri);
      } else {
        // Warn and continue
        logger.warn("{} is not a valid IRI.", term);
      }
    }
    return iris;
  }

  /**
   * Create an OWLLiteral.
   *
   * @param value the lexical value
   * @return a literal
   */
  public static OWLLiteral createLiteral(String value) {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    return df.getOWLLiteral(value);
  }

  /**
   * Create an OWLLiteral with a language tag.
   *
   * @param value the lexical value
   * @param lang the language tag
   * @return a literal
   */
  public static OWLLiteral createTaggedLiteral(String value, String lang) {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    return df.getOWLLiteral(value, lang);
  }

  /**
   * Create a typed OWLLiteral.
   *
   * @param value the lexical value
   * @param type the type IRI string
   * @return a literal
   */
  public OWLLiteral createTypedLiteral(String value, String type) {
    IRI iri = createIRI(type);
    return createTypedLiteral(value, iri);
  }

  /**
   * Create a typed OWLLiteral.
   *
   * @param value the lexical value
   * @param type the type IRI
   * @return a literal
   */
  public OWLLiteral createTypedLiteral(String value, IRI type) {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    OWLDatatype datatype = df.getOWLDatatype(type);
    return df.getOWLLiteral(value, datatype);
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
   * Load a map of prefixes from the "@context" of a JSON-LD string.
   *
   * @param jsonString the JSON-LD string
   * @return a map from prefix name strings to prefix IRI strings
   * @throws IOException on any problem
   */
  public static Context parseContext(String jsonString) throws IOException {
    try {
      Object jsonObject = JsonUtils.fromString(jsonString);
      if (!(jsonObject instanceof Map)) {
        throw new IOException(jsonldContextParseError);
      }
      Map<String, Object> jsonMap = (Map<String, Object>) jsonObject;
      if (!jsonMap.containsKey("@context")) {
        throw new IOException(jsonldContextParseError);
      }
      Object jsonContext = jsonMap.get("@context");
      return new Context().parse(jsonContext);
    } catch (Exception e) {
      throw new IOException(jsonldContextParseError, e);
    }
  }

  /**
   * Get a copy of the default context.
   *
   * @return a copy of the current context
   * @throws IOException if default context file cannot be read
   */
  public Context getDefaultContext() throws IOException {
    InputStream stream = IOHelper.class.getResourceAsStream(defaultContextPath);
    String jsonString = IOUtils.toString(stream);
    return parseContext(jsonString);
  }

  /**
   * Get a copy of the current context.
   *
   * @return a copy of the current context
   */
  public Context getContext() {
    return this.context.clone();
  }

  /** Set an empty context. */
  public void setContext() {
    this.context = new Context();
  }

  /**
   * Set the current JSON-LD context to the given context.
   *
   * @param context the new JSON-LD context
   */
  public void setContext(Context context) {
    if (context == null) {
      setContext();
    } else {
      this.context = context;
    }
  }

  /**
   * Set the current JSON-LD context to the given context.
   *
   * @param jsonString the new JSON-LD context as a JSON string
   * @throws IOException on issue parsing JSON
   */
  public void setContext(String jsonString) throws IOException {
    this.context = parseContext(jsonString);
  }

  /**
   * Set the current JSON-LD context to the given map.
   *
   * @param map a map of strings for the new JSON-LD context
   * @throws IOException on issue parsing JSON
   */
  public void setContext(Map<String, Object> map) throws IOException {
    try {
      this.context = new Context().parse(map);
    } catch (JsonLdError e) {
      throw new IOException(jsonldContextParseError, e);
    }
  }

  /**
   * Set whether or not XML entities will be swapped into URIs in saveOntology XML output formats.
   *
   * @param entityFlag value to set
   */
  public void setXMLEntityFlag(Boolean entityFlag) {
    try {
      this.useXMLEntities = entityFlag;
    } catch (Exception e) {
      logger.warn("Could not set useXMLEntities {}", entityFlag);
      logger.warn(e.getMessage());
    }
  }

  /**
   * Get the useXMLEntities flag.
   *
   * @return boolean useXMLEntities flag
   */
  public Boolean getXMLEntityFlag() {
    return this.useXMLEntities;
  }

  /**
   * Make an OWLAPI DefaultPrefixManager from a map of prefixes.
   *
   * @param prefixes a map from prefix name strings to prefix IRI strings
   * @return a new DefaultPrefixManager
   */
  public static DefaultPrefixManager makePrefixManager(Map<String, String> prefixes) {
    DefaultPrefixManager pm = new DefaultPrefixManager();
    for (Map.Entry<String, String> entry : prefixes.entrySet()) {
      pm.setPrefix(entry.getKey() + ":", entry.getValue());
    }
    return pm;
  }

  /**
   * Get a prefix manager with the current prefixes.
   *
   * @return a new DefaultPrefixManager
   */
  public DefaultPrefixManager getPrefixManager() {
    return makePrefixManager(context.getPrefixes(false));
  }

  /**
   * Add a prefix mapping as a single string "foo: http://example.com#".
   *
   * @param combined both prefix and target
   * @throws IllegalArgumentException on malformed input
   * @throws IOException if prefix cannot be parsed
   */
  public void addPrefix(String combined) throws IllegalArgumentException, IOException {
    String[] results = combined.split(":", 2);
    if (results.length < 2) {
      throw new IllegalArgumentException(String.format(invalidPrefixError, combined));
    }
    addPrefix(results[0], results[1]);
  }

  /**
   * Add a prefix mapping to the current JSON-LD context, as a prefix string and target string.
   * Rebuilds the context.
   *
   * @param prefix the short prefix to add; should not include ":"
   * @param target the IRI string that is the target of the prefix
   * @throws IOException if prefix cannot be parsed
   */
  public void addPrefix(String prefix, String target) throws IOException {
    try {
      context.put(prefix.trim(), target.trim());
      context.remove("@base");
      setContext((Map<String, Object>) context);
    } catch (Exception e) {
      throw new IOException(String.format(prefixLoadError, prefix, target), e);
    }
  }

  /**
   * Get a copy of the current prefix map.
   *
   * @return a copy of the current prefix map
   */
  public Map<String, String> getPrefixes() {
    return this.context.getPrefixes(false);
  }

  /**
   * Set the current prefix map.
   *
   * @param map the new map of prefixes to use
   * @throws IOException on issue parsing map to context
   */
  public void setPrefixes(Map<String, Object> map) throws IOException {
    setContext(map);
  }

  /**
   * Return the current prefixes as a JSON-LD string.
   *
   * @return the current prefixes as a JSON-LD string
   * @throws IOException on any error
   */
  public String getContextString() throws IOException {
    try {
      Object compact =
          JsonLdProcessor.compact(
              JsonUtils.fromString("{}"), context.getPrefixes(false), new JsonLdOptions());
      return JsonUtils.toPrettyString(compact);
    } catch (Exception e) {
      throw new IOException(jsonldContextCreationError, e);
    }
  }

  /**
   * Write the current context as a JSON-LD file.
   *
   * @param path the path to write the context
   * @throws IOException on any error
   */
  public void saveContext(String path) throws IOException {
    saveContext(new File(path));
  }

  /**
   * Write the current context as a JSON-LD file.
   *
   * @param file the file to write the context
   * @throws IOException on any error
   */
  public void saveContext(File file) throws IOException {
    FileWriter writer = new FileWriter(file);
    writer.write(getContextString());
    writer.close();
  }

  /**
   * Read comma-separated values from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(String path) throws IOException {
    return TemplateHelper.readCSV(path);
  }

  /**
   * Read comma-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(InputStream stream) throws IOException {
    return TemplateHelper.readCSV(stream);
  }

  /**
   * Read comma-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readCSV(Reader reader) throws IOException {
    return TemplateHelper.readCSV(reader);
  }

  /**
   * Read tab-separated values from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(String path) throws IOException {
    return TemplateHelper.readTSV(path);
  }

  /**
   * Read tab-separated values from a stream to a list of lists of strings.
   *
   * @param stream the stream to read from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(InputStream stream) throws IOException {
    return TemplateHelper.readTSV(stream);
  }

  /**
   * Read tab-separated values from a reader to a list of lists of strings.
   *
   * @param reader a reader to read data from
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTSV(Reader reader) throws IOException {
    return TemplateHelper.readTSV(reader);
  }

  /**
   * Read a table from a path to a list of lists of strings.
   *
   * @param path file path to the CSV file
   * @return a list of lists of strings
   * @throws IOException on file or reading problems
   */
  public static List<List<String>> readTable(String path) throws IOException {
    return TemplateHelper.readTable(path);
  }

  /**
   * Given an ontology, a document format, and a boolean indicating to check OBO formatting, return
   * the ontology file in the OWLDocumentFormat as a byte array.
   *
   * @param ontology OWLOntology to save
   * @param format OWLDocumentFormat to save in
   * @param checkOBO boolean indiciating to check OBO formatting
   * @return byte array of formatted ontology data
   * @throws IOException on any problem
   */
  private byte[] getOntologyFileData(
      final OWLOntology ontology, OWLDocumentFormat format, boolean checkOBO) throws IOException {
    byte[] data;
    // first handle any non-official output formats.
    // currently this is just OboGraphs JSON format
    if (format instanceof OboGraphJsonDocumentFormat) {
      FromOwl fromOwl = new FromOwl();
      GraphDocument gd = fromOwl.generateGraphDocument(ontology);
      String doc = OgJsonGenerator.render(gd);
      data = doc.getBytes();
    } else if (format instanceof OBODocumentFormat && !checkOBO) {
      OWLAPIOwl2Obo bridge = new OWLAPIOwl2Obo(ontology.getOWLOntologyManager());
      OBODoc oboOntology = bridge.convert(ontology);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos))) {
        OBOFormatWriter oboWriter = new OBOFormatWriter();
        oboWriter.setCheckStructure(checkOBO);
        oboWriter.write(oboOntology, bw);
      }
      data = baos.toByteArray();
    } else {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ontology.getOWLOntologyManager().saveOntology(ontology, format, baos);
        data = baos.toByteArray();
      } catch (IOException | OWLOntologyStorageException e) {
        // TODO
        throw new IOException(e);
      }
    }
    return data;
  }

  /**
   * Given a gzipped ontology file and a catalog path, load the ontology from a zip input stream.
   *
   * @param gzipFile compressed File to load ontology from
   * @param catalogPath the path to the catalog file or null
   * @return a new ontology object with a new OWLManager
   * @throws IOException on any problem
   */
  private OWLOntology loadCompressedOntology(File gzipFile, String catalogPath) throws IOException {
    FileInputStream fis = new FileInputStream(gzipFile);
    GZIPInputStream gis = new GZIPInputStream(fis);
    return loadOntology(gis, catalogPath);
  }

  /**
   * Given the URL to a gzipped ontology and a catalog path, load the ontology from a zip input
   * stream.
   *
   * @param url URL to load from
   * @param catalogPath the path to the catalog file or null
   * @return a new ontology object with a new OWLManager
   * @throws IOException on any problem
   */
  private OWLOntology loadCompressedOntology(URL url, String catalogPath) throws IOException {
    InputStream is;
    try {
      is = new BufferedInputStream(url.openStream(), 1024);
    } catch (FileNotFoundException e) {
      throw new IOException(String.format(invalidOntologyIRIError, url));
    }
    GZIPInputStream gis = new GZIPInputStream(is);
    return loadOntology(gis, catalogPath);
  }

  /**
   * Given an ontology, a format, an IRI to save to, and a boolean indiciating to check OBO
   * formatting, save the ontology in the given format to a file at the IRI.
   *
   * @param ontology OWLOntology to save
   * @param format OWLDocumentFormat to save in
   * @param ontologyIRI IRI to save to
   * @param checkOBO boolean indicating to check OBO formatting
   * @throws IOException on any problem
   */
  private void saveOntologyFile(
      final OWLOntology ontology, OWLDocumentFormat format, IRI ontologyIRI, boolean checkOBO)
      throws IOException {
    // first handle any non-official output formats.
    // currently this is just OboGraphs JSON format
    if (format instanceof OboGraphJsonDocumentFormat) {
      FromOwl fromOwl = new FromOwl();
      GraphDocument gd = fromOwl.generateGraphDocument(ontology);
      String doc = OgJsonGenerator.render(gd);
      File outfile = new File(ontologyIRI.toURI());
      FileUtils.writeStringToFile(outfile, doc);
    } else if (format instanceof OBODocumentFormat && !checkOBO) {
      // only use this method when ignoring OBO checking, otherwise use native save
      OWLAPIOwl2Obo bridge = new OWLAPIOwl2Obo(ontology.getOWLOntologyManager());
      OBODoc oboOntology = bridge.convert(ontology);
      File f = new File(ontologyIRI.toURI());
      boolean newFile = f.createNewFile();
      try (BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
        OBOFormatWriter oboWriter = new OBOFormatWriter();
        oboWriter.setCheckStructure(checkOBO);
        oboWriter.write(oboOntology, bw);
      } catch (IOException e) {
        if (!newFile) {
          f.delete();
        }
      }
    } else {
      // use native save functionality
      try {
        ontology.getOWLOntologyManager().saveOntology(ontology, format, ontologyIRI);
      } catch (OWLOntologyStorageException e) {
        throw new IOException(String.format(ontologyStorageError, ontologyIRI.toString()), e);
      }
    }
  }

  /**
   * Given a formatted ontology as a byte array and an IRI, save the data to the IRI as a gzipped
   * file.
   *
   * @param data byte array of ontology
   * @param ontologyIRI IRI to save to
   * @throws IOException on any problem
   */
  private void saveCompressedOntology(byte[] data, IRI ontologyIRI) throws IOException {
    File f = new File(ontologyIRI.toURI());
    boolean newFile = f.createNewFile();
    FileOutputStream fos = new FileOutputStream(f);
    BufferedOutputStream bos = new BufferedOutputStream(fos);
    try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
      gos.write(data, 0, data.length);
    } catch (IOException e) {
      if (!newFile) {
        f.delete();
      }
    }
  }
}
