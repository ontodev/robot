package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/** A SAX DefaultHandler for reading OWL Catalog files. */
public class CatalogElementHandler extends DefaultHandler {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(CatalogElementHandler.class);

  /** Use the parentFolder to resolve relative paths. */
  private File parentFolder;

  /** Build a map from OntologyIRIs to File IRIs. */
  private Map<IRI, IRI> mappings;

  /**
   * Initialize the element handler with required context.
   *
   * @param parentFolder used to resolve relative file paths
   * @param mappings use to build a map for IRI resolution
   */
  public CatalogElementHandler(File parentFolder, Map<IRI, IRI> mappings) {
    this.parentFolder = parentFolder;
    this.mappings = mappings;
  }

  /**
   * Handle startElement events by looking for 'uri' elements, and their 'name' and 'uri'
   * attributes, and updating 'mappings'. We only handle 'uri' elements, and do not update mappings
   * if any of our checks fail.
   *
   * @param uri the URI of the start element
   * @param localName the local name of the start element
   * @param qName the qualified name of the start element
   * @param attributes the attributes object of the start element
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (!"uri".equals(qName)) {
      return;
    }

    String fromString = attributes.getValue("name");
    if (fromString == null) {
      return;
    }
    IRI fromIRI = IRI.create(fromString);
    String toString = attributes.getValue("uri");
    if (toString == null) {
      return;
    }

    // If there is a parent folder
    // and the 'uri' string does not contain a colon character,
    // then treat this as a "file://" IRI.
    // Otherwise treat this as web IRI.
    IRI toIRI = null;
    if (parentFolder != null && !toString.contains(":")) {
      File toFile = new File(toString);
      if (!toFile.isAbsolute()) {
        toFile = new File(parentFolder, toString);
      }
      try {
        toFile = toFile.getCanonicalFile();
        toIRI = IRI.create(toFile);
      } catch (IOException e) {
        logger.warn("Skipping mapping: {} {} {}", fromString, toString, e);
      }
    } else {
      toIRI = IRI.create(toString);
    }
    if (toIRI == null) {
      return;
    }

    mappings.put(fromIRI, toIRI);
  }
}
