package org.obolibrary.robot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/** {@link OWLOntologyIRIMapper} using the mappings from a catalog.xml file. */
public class CatalogXmlIRIMapper implements OWLOntologyIRIMapper {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(CatalogXmlIRIMapper.class);

  /**
   * Mappings from OntologyIRIs to (usually) file IRIs, allowing a layer of indirection when
   * importing ontologies.
   */
  private final Map<IRI, IRI> mappings;

  /**
   * Initialize with IRI mappings.
   *
   * @param mappings initial mappings from IRIs to IRIs
   */
  CatalogXmlIRIMapper(Map<IRI, IRI> mappings) {
    this.mappings = mappings;
  }

  /**
   * Given a String path to a catalog file, create a CatalogXmlIRIMapper that interprets any
   * relative paths as relative to the catalog file location.
   *
   * @param catalogFile the String path to the catalog file
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(String catalogFile) throws IOException {
    this(new File(catalogFile).getAbsoluteFile());
  }

  /**
   * Given a catalog File, create a CatalogXmlIRIMapper that interprets any relative paths as
   * relative to the catalog file location.
   *
   * @param catalogFile the catalog File
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(File catalogFile) throws IOException {
    this(catalogFile, catalogFile.getAbsoluteFile().getParentFile());
  }

  /**
   * Given a catalog File and a parent folder, create a CatalogXmlIRIMapper that interprets any
   * relative paths as relative to the parent folder.
   *
   * @param catalogFile the catalog File
   * @param parentFolder the File for the parent folder
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(File catalogFile, File parentFolder) throws IOException {
    this(parseCatalogXml(new FileInputStream(catalogFile), parentFolder));
  }

  /**
   * Given an IRI, create a CatalogXmlIRIMapper that interprets any relative paths as relative to
   * the catalog file location.
   *
   * @param catalogIRI the IRI of the catalog file
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(IRI catalogIRI) throws IOException {
    this(catalogIRI.toURI().toURL());
  }

  /**
   * Given an URL, create a CatalogXmlIRIMapper that interprets any relative paths as relative to
   * the catalog file location.
   *
   * @param catalogURL the URL of the catalog file
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(URL catalogURL) throws IOException {
    if ("file".equals(catalogURL.getProtocol())) {
      try {
        File catalogFile = new File(catalogURL.toURI());
        mappings = parseCatalogXml(new FileInputStream(catalogFile), catalogFile.getParentFile());
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }
    } else {
      mappings = parseCatalogXml(catalogURL.openStream(), null);
    }
  }

  /**
   * Given an URL and a parent folder, create a CatalogXmlIRIMapper that interprets any relative
   * paths as relative to the parent folder.
   *
   * @param catalogURL the URL of the catalog file
   * @param parentFolder the File for the parent folder
   * @throws IOException on any problem
   */
  public CatalogXmlIRIMapper(URL catalogURL, File parentFolder) throws IOException {
    this(parseCatalogXml(catalogURL.openStream(), parentFolder));
  }

  /**
   * Given an IRI, return the mapped IRI.
   *
   * @param ontologyIRI the IRI that we want to look up
   * @return the mapped IRI, usually to a local file
   */
  @Override
  public IRI getDocumentIRI(IRI ontologyIRI) {
    return mappings.get(ontologyIRI);
  }

  /**
   * Parse the inputStream as a catalog.xml file and extract IRI mappings.
   *
   * <p>Optional: Resolve relative file paths with the given parent folder.
   *
   * @param inputStream input stream (never null)
   * @param parentFolder folder or null
   * @return mappings
   * @throws IOException on general IO problems
   * @throws IllegalArgumentException if input stream is null
   */
  static Map<IRI, IRI> parseCatalogXml(InputStream inputStream, final File parentFolder)
      throws IOException, IllegalArgumentException {
    if (inputStream == null) {
      throw new IllegalArgumentException("InputStream should never be null, missing resource?");
    }

    // use the Java built-in SAX parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);

    try {
      final Map<IRI, IRI> mappings = new HashMap<IRI, IRI>();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(inputStream, new CatalogElementHandler(parentFolder, mappings));
      return mappings;
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    } finally {
      inputStream.close();
    }
  }
}
