package org.obolibrary.robot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.*;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides convenience methods for working with RDF/XML ontologies with a streaming XML processor.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class XMLHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(XMLHelper.class);

  // Entity types
  private final String ANNOTATION_PROPERTY_TAG =
      "{http://www.w3.org/2002/07/owl#}AnnotationProperty";
  private final String CLASS_TAG = "{http://www.w3.org/2002/07/owl#}Class";
  private final String DATA_PROPERTY_TAG = "{http://www.w3.org/2002/07/owl#}DatatypeProperty";
  private final String OWL_DATATYPE_TAG = "{http://www.w3.org/2002/07/owl#}Datatype";
  private final String NAMED_INDIVIDUAL_TAG = "{http://www.w3.org/2002/07/owl#}NamedIndividual";
  private final String OBJECT_PROPERTY_TAG = "{http://www.w3.org/2002/07/owl#}ObjectProperty";

  // Other tags
  private final String ONTOLOGY_TAG = "{http://www.w3.org/2002/07/owl#}Ontology";
  private final String LABEL_TAG = "{http://www.w3.org/2000/01/rdf-schema#}label";
  private final String LANG_TAG = "{http://www.w3.org/XML/1998/namespace}lang";
  private final String RDFS_DATATYPE_TAG = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}datatype";
  private final String SUBCLASS_OF_TAG = "{http://www.w3.org/2000/01/rdf-schema#}subClassOf";

  // Shared data factory & manager
  private final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
  private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  // Extracted module
  private OWLOntology outputOntology;

  // User parameters
  private String fileName;
  private IRI outputIRI;

  // Maps to resolve labels, relationships, and types from IRIs
  private BiMap<IRI, String> labelMap = HashBiMap.create();
  private Map<IRI, Set<IRI>> childParentMap = new HashMap<>();
  private Map<IRI, EntityType> typeMap = new HashMap<>();

  // Tracking added targets (if intermediates = all or minimal)
  private Set<IRI> allTargets = new HashSet<>();

  // Tracking all annotation properties, if none are specified
  private Set<IRI> allAnnotationProperties = new HashSet<>();

  /**
   * Create a new XMLHelper for parsing an ontology in a local file.
   *
   * @param fileName path to XML file
   * @throws IOException if file does not exist or XML cannot be parsed
   * @throws OWLOntologyCreationException if empty ontology cannot be created
   */
  public XMLHelper(String fileName, IRI outputIRI)
      throws IOException, OWLOntologyCreationException {
    this.fileName = fileName;
    // Create an empty ontology
    outputOntology = manager.createOntology();
    if (outputIRI != null) {
      this.outputIRI = outputIRI;
    }

    // Create maps: IRI -> label, child -> parents
    try (FileInputStream fis = new FileInputStream(this.fileName)) {
      getBasicDetails(fis);
    } catch (XMLStreamException e) {
      throw new IOException("Unable to parse XML from " + fileName, e);
    }
  }

  /**
   * Create a new XMLHelper for parsing an ontology from an IRI. This will create a temporary file,
   * which will be deleted on exit.
   *
   * @param iri IRI of XML file
   * @throws IOException if temp file cannot be created or downloaded, or if XML cannot be parsed
   * @throws OWLOntologyCreationException if empty ontology cannot be created
   */
  public XMLHelper(IRI iri, IRI outputIRI) throws IOException, OWLOntologyCreationException {
    // We will create a temporary file
    String fileName = iri.toString().substring(iri.toString().lastIndexOf("/"));
    String ext = fileName.substring(fileName.lastIndexOf("."));
    String fn = fileName.substring(0, fileName.lastIndexOf("."));
    File temp = File.createTempFile(fn, ext);

    // Make sure this file is removed on exit
    temp.deleteOnExit();
    this.fileName = temp.getAbsolutePath();

    // Download ontology, following redirects
    logger.info(String.format("Starting download from <%s>", iri.toString()));
    CloseableHttpClient httpclient =
        HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    try {
      HttpGet get = new HttpGet(iri.toURI());
      HttpResponse r = httpclient.execute(get);
      InputStream source = r.getEntity().getContent();
      FileUtils.copyInputStreamToFile(source, temp);
    } catch (Exception e) {
      throw new IOException("Unable to download content from " + iri, e);
    } finally {
      IOUtils.closeQuietly(httpclient);
    }

    logger.info("Temporary file created for input ontology: " + this.fileName);

    // Create an empty ontology
    outputOntology = manager.createOntology();
    if (outputIRI != null) {
      this.outputIRI = outputIRI;
    }

    // Create map of IRI -> label
    try (FileInputStream fis = new FileInputStream(this.fileName)) {
      getBasicDetails(fis);
    } catch (XMLStreamException e) {
      throw new IOException("Unable to parse XML from " + this.fileName, e);
    }
  }

  /**
   * Perform an extraction to create a subset containing the target IRIs with their target
   * annotation properties.
   *
   * @param targets IRIs to extract
   * @param annotationProperties IRIs of annotation properties to include
   * @param options Map of extract options
   * @return extracted subset
   * @throws IOException on problem parsing XML
   * @throws OWLOntologyCreationException on problem creating empty ontology
   */
  public OWLOntology extract(
      Set<IRI> targets, Set<IRI> annotationProperties, Map<String, String> options)
      throws IOException, OWLOntologyCreationException {
    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    boolean annotateSource = OptionsHelper.optionIsTrue(options, "annotate-with-source");

    // Add declarations and hierarchy structure
    initOntology(targets, intermediates);

    // Add desired annotations on targets
    // If 'all' or 'minimal' intermediates, more targets have been added
    // If 'none' intermediates, targets are the same as the provided targets
    try (FileInputStream fis = new FileInputStream(this.fileName)) {
      addAnnotations(fis, annotationProperties);
    } catch (XMLStreamException e) {
      throw new IOException("Unable to parse XML from " + this.fileName, e);
    }

    // Maybe add source annotations
    if (annotateSource && outputIRI != null) {
      OWLAnnotationProperty isDefinedBy = dataFactory.getRDFSIsDefinedBy();
      for (IRI iri : allTargets) {
        manager.addAxiom(
            outputOntology,
            dataFactory.getOWLAnnotationAssertionAxiom(isDefinedBy, iri, outputIRI));
      }
    }

    // This is a stupid way to set the IRI
    // But manager.setOntologyDocumentIRI(...) does not copy over to the output
    if (outputIRI == null) {
      return outputOntology;
    }
    OWLOntologyManager manager = outputOntology.getOWLOntologyManager();
    Set<OWLOntology> ont = new HashSet<>();
    ont.add(outputOntology);
    return manager.createOntology(outputIRI, ont);
  }

  /**
   * Get the entity type of an entity by its IRI
   *
   * @param iri IRI of entity
   * @return EntityType or null
   */
  public EntityType getEntityType(IRI iri) {
    return typeMap.getOrDefault(iri, null);
  }

  /**
   * Get the IRI of an entity by its label.
   *
   * @param label String label of entity to get IRI of
   * @return IRI or null
   */
  public IRI getIRI(String label) {
    return labelMap.inverse().getOrDefault(label, null);
  }

  /**
   * Get the set of parent IRIs for a child entity.
   *
   * @param child IRI to get parents of
   * @return set of IRIs (maybe empty if no parents), or null if child IRI does not exist
   */
  public Set<IRI> getParents(IRI child) {
    if (typeMap.containsKey(child)) {
      return childParentMap.getOrDefault(child, new HashSet<>());
    } else {
      // Entity does not exist in input ontology
      return null;
    }
  }

  /**
   * Get the basic details for all entities in an ontology. This includes parent-child
   * relationships, entity types, and labels.
   *
   * @param fis FileInputStream of XML
   * @throws XMLStreamException on issue parsing XML
   */
  private void getBasicDetails(FileInputStream fis) throws XMLStreamException {
    XMLInputFactory2 inf = (XMLInputFactory2) XMLInputFactory2.newInstance();
    XMLStreamReader2 sr = (XMLStreamReader2) inf.createXMLStreamReader(fileName, fis);

    // XML event integer
    int e;

    // Name of current node tag
    String node = null;

    // Ignore anonymous closing events
    boolean anonymous = false;

    // Track long strings over multiple CHARACTER events for labels
    StringBuilder currentContent = null;

    // Add label to set of annotation properties
    allAnnotationProperties.add(dataFactory.getRDFSLabel().getIRI());

    // Track current entity by IRI
    IRI iri = null;
    try {
      while (sr.hasNext()) {
        e = sr.next();
        switch (e) {
          case XMLEvent.START_ELEMENT:
            node = sr.getName().toString();

            // Maybe add child -> parents
            if (node.equals(SUBCLASS_OF_TAG) && iri != null) {
              // We only care about named classes right now
              if (sr.getAttributeCount() == 0) {
                continue;
              }
              IRI parentIRI = IRI.create(sr.getAttributeValue(0));
              Set<IRI> parents = childParentMap.getOrDefault(iri, new HashSet<>());
              parents.add(parentIRI);
              childParentMap.put(iri, parents);
              continue;
            }

            // Check if the node is for an entity declaration
            // Add the IRI
            switch (node) {
              case ONTOLOGY_TAG:
                // Maybe get an ontology IRI
                if (sr.getAttributeCount() == 0) {
                  continue;
                }
                if (outputIRI == null) {
                  String ontologyIRI = sr.getAttributeValue(0);
                  outputIRI = IRI.create(ontologyIRI);
                }
                break;

              case CLASS_TAG:
              case OBJECT_PROPERTY_TAG:
                if (sr.getAttributeCount() == 0) {
                  // anonymous
                  anonymous = true;
                  continue;
                }
                iri = IRI.create(sr.getAttributeValue(0));
                if (node.equals(CLASS_TAG)) {
                  typeMap.put(iri, EntityType.CLASS);
                } else {
                  typeMap.put(iri, EntityType.OBJECT_PROPERTY);
                }
                break;
              case ANNOTATION_PROPERTY_TAG:
              case DATA_PROPERTY_TAG:
              case OWL_DATATYPE_TAG:
              case NAMED_INDIVIDUAL_TAG:
                iri = IRI.create(sr.getAttributeValue(0));
                switch (node) {
                  case ANNOTATION_PROPERTY_TAG:
                    typeMap.put(iri, EntityType.ANNOTATION_PROPERTY);
                    allAnnotationProperties.add(iri);
                    break;
                  case DATA_PROPERTY_TAG:
                    typeMap.put(iri, EntityType.DATA_PROPERTY);
                    break;
                  case OWL_DATATYPE_TAG:
                    typeMap.put(iri, EntityType.DATATYPE);
                    break;
                  default:
                    typeMap.put(iri, EntityType.NAMED_INDIVIDUAL);
                    break;
                }
                break;
            }

            break;
          case XMLEvent.CHARACTERS:
            if (node == null || iri == null) {
              // Node should never be null here
              continue;
            }
            if (node.equals(LABEL_TAG)) {
              String content = sr.getText();
              if (content.trim().equals("")) {
                continue;
              }
              if (currentContent != null) {
                currentContent.append(content);
              } else {
                currentContent = new StringBuilder(content);
              }
            }
            break;
          case XMLEvent.END_ELEMENT:
            node = sr.getName().toString();
            // Check if the node is for an entity declaration
            switch (node) {
              case ANNOTATION_PROPERTY_TAG:
              case CLASS_TAG:
              case DATA_PROPERTY_TAG:
              case OWL_DATATYPE_TAG:
              case NAMED_INDIVIDUAL_TAG:
              case OBJECT_PROPERTY_TAG:
                if (!anonymous) {
                  iri = null;
                } else {
                  anonymous = false;
                }
                continue;
            }
            if (node.equals(LABEL_TAG) && currentContent != null) {
              String content = currentContent.toString();

              if (labelMap.containsValue(content)) {
                logger.debug(String.format("Duplicate label '%s' - appending IRI!", content));
                IRI dupIRI = labelMap.inverse().get(content);
                content = String.format("%s <%s>", content, iri);
                String dupContent = String.format("%s <%s>", content, dupIRI);
                labelMap.put(dupIRI, dupContent);
                labelMap.put(iri, content);
              } else {
                labelMap.put(iri, content);
              }
              currentContent = null;
            }
            break;
        }
      }
    } finally {
      sr.closeCompletely();
    }
  }

  /**
   * Create a new ontology by extracting target IRIs. This ontology only includes the declarations
   * and the hierarchy.
   *
   * @param targets set of IRIs to include in output ontology
   * @param intermediates 'none', 'all', or 'minimal'
   * @throws OWLOntologyCreationException on issue creating empty ontology
   */
  private void initOntology(Set<IRI> targets, String intermediates)
      throws OWLOntologyCreationException {
    for (IRI iri : targets) {
      EntityType<?> et = getEntityType(iri);
      if (et == null) {
        logger.error(String.format("Unable to create entity from <%s>", iri.toString()));
        continue;
      }
      // Add declaration
      OWLEntity e = dataFactory.getOWLEntity(et, iri);
      manager.addAxiom(outputOntology, dataFactory.getOWLDeclarationAxiom(e));
    }

    // Handle parents
    for (IRI iri : targets) {
      if (intermediates.equals("none")) {
        // Assert what relationships we can between existing terms
        addAncestorsNoIntermediates(targets, iri, iri);
      } else if (intermediates.equals("all") || intermediates.equals("minimal")) {
        // Add all terms between existing terms
        addAncestorsAllIntermediates(targets, iri);
      } else {
        throw new IllegalArgumentException("Unknown intermediates option: " + intermediates);
      }
    }

    if (intermediates.equals("minimal")) {
      // Minimal intermediates, collapse ontology
      OntologyHelper.collapseOntology(outputOntology, targets);
    }

    // Add all entities referenced in ontology to targets for annotations
    OntologyHelper.getEntities(outputOntology).forEach(e -> allTargets.add(e.getIRI()));
  }

  /**
   * Add annotations to output ontology for all targets.
   *
   * @param fis FileInputStream of XML
   * @param annotationProperties set of IRIs for annotation properties to include
   * @throws XMLStreamException on issue parsing XML
   */
  private void addAnnotations(FileInputStream fis, Set<IRI> annotationProperties)
      throws XMLStreamException {
    if (annotationProperties == null || annotationProperties.isEmpty()) {
      // Add all annotation properties if they were not provided
      annotationProperties = allAnnotationProperties;
    }
    XMLInputFactory2 inf = (XMLInputFactory2) XMLInputFactory2.newInstance();
    XMLStreamReader2 sr = (XMLStreamReader2) inf.createXMLStreamReader(fileName, fis);

    // XML Event int
    int e;

    // Current XML node
    String node = null;

    // Ignore anonymous nodes
    boolean anonymous = false;

    // Current entity IRI
    IRI iri = null;

    // Current annotation property IRI
    IRI apIRI = null;

    // Current content of annotation
    StringBuilder currentContent = null;

    // Maybe track a datatype
    OWLDatatype annotationDatatype = null;

    // Maybe track a language
    String lang = null;

    try {
      while (sr.hasNext()) {
        e = sr.next();
        switch (e) {
          case XMLEvent.START_ELEMENT:
            // Start of an XML tag
            node = sr.getName().toString();
            boolean isEntity = false;

            // Check if the node is for an entity declaration
            switch (node) {
              case ANNOTATION_PROPERTY_TAG:
              case DATA_PROPERTY_TAG:
              case OWL_DATATYPE_TAG:
              case NAMED_INDIVIDUAL_TAG:
                iri = IRI.create(sr.getAttributeValue(0));
                isEntity = true;
                break;
              case CLASS_TAG:
              case OBJECT_PROPERTY_TAG:
                if (sr.getAttributeCount() == 0) {
                  // Skip anonymous for now
                  anonymous = true;
                  continue;
                }
                iri = IRI.create(sr.getAttributeValue(0));
                isEntity = true;
                break;
            }

            // IRI = null for entities we don't care about
            if (!allTargets.contains(iri) && isEntity) {
              iri = null;
              continue;
            }

            // Could be an annotation with a datatype or language
            if (sr.getAttributeCount() > 0 && !isEntity) {
              String attr = sr.getAttributeName(0).toString();
              if (attr.equals(RDFS_DATATYPE_TAG)) {
                IRI dtIRI = IRI.create(sr.getAttributeValue(0));
                annotationDatatype = dataFactory.getOWLDatatype(dtIRI);
              } else if (attr.equals(LANG_TAG)) {
                lang = sr.getAttributeValue(0);
              }
            }
            break;

          case XMLEvent.CHARACTERS:
            // Content inside of XML tags
            if (node == null || iri == null) {
              // Node should never be null here
              // If IRI is null, we don't care about this entity
              continue;
            }
            String content = sr.getText();
            if (content.trim().equals("")) {
              // Skip empty content
              continue;
            }

            // Create an IRI from the node and check if it's in the desired APs
            node = node.replace("{", "").replace("}", "");
            IRI maybeIRI = IRI.create(node);
            if (annotationProperties.contains(maybeIRI) && apIRI == null) {
              // Set AP IRI to track and init content builder
              apIRI = maybeIRI;
              currentContent = new StringBuilder(content);
            } else if (maybeIRI == apIRI) {
              // Continue to add to content builder
              currentContent.append(content);
            }
            break;

          case XMLEvent.END_ELEMENT:
            // XML closing tag
            if (iri == null) {
              continue;
            }
            node = sr.getName().toString();

            // Check if the node is for an entity declaration
            switch (node) {
              case ANNOTATION_PROPERTY_TAG:
              case CLASS_TAG:
              case DATA_PROPERTY_TAG:
              case OWL_DATATYPE_TAG:
              case NAMED_INDIVIDUAL_TAG:
              case OBJECT_PROPERTY_TAG:
                if (!anonymous) {
                  iri = null;
                } else {
                  anonymous = false;
                }
                continue;
            }

            // If not entity, it might be closing of an annotation
            node = node.replace("{", "").replace("}", "");
            if (apIRI != null && node.equals(apIRI.toString())) {
              // Closing of an annotation property
              // Add the content to the ontology
              content = currentContent.toString();
              OWLAnnotationProperty ap = dataFactory.getOWLAnnotationProperty(apIRI);
              OWLLiteral lit;
              if (annotationDatatype != null) {
                lit = dataFactory.getOWLLiteral(content, annotationDatatype);
              } else if (lang != null) {
                lit = dataFactory.getOWLLiteral(content, lang);
              } else {
                lit = dataFactory.getOWLLiteral(content);
              }
              manager.addAxiom(
                  outputOntology, dataFactory.getOWLAnnotationAssertionAxiom(ap, iri, lit));

              // Reset tracking variables
              apIRI = null;
              currentContent = null;
              annotationDatatype = null;
              lang = null;
            }
            break;
        }
      }
    } finally {
      sr.closeCompletely();
    }
  }

  /**
   * Create a hiearchy within the output ontology with all intermediates. Terms not included in
   * target set will be used to fill in gaps. Update the set of targets to include added entities.
   *
   * @param targets set of target IRIs
   * @param childIRI IRI for current term to be asserted as child of something else
   */
  private void addAncestorsAllIntermediates(Set<IRI> targets, IRI childIRI) {
    Set<IRI> parents = getParents(childIRI);
    for (IRI piri : parents) {
      addSubXOfAxiom(childIRI, piri);
      if (!targets.contains(piri)) {
        addAncestorsAllIntermediates(targets, piri);
      }
    }
  }

  /**
   * Create a hierarchy within the output ontology with no intermediates. Only terms within the
   * target set will be included, and relationships will be made between ancestors an descendants to
   * fill in gaps.
   *
   * @param targets set of target IRIs
   * @param bottomIRI bottom-level IRI
   * @param currentIRI current-level IRI
   */
  private void addAncestorsNoIntermediates(Set<IRI> targets, IRI bottomIRI, IRI currentIRI) {
    Set<IRI> parents = getParents(currentIRI);
    for (IRI piri : parents) {
      if (targets.contains(piri)) {
        addSubXOfAxiom(bottomIRI, piri);
      } else {
        addAncestorsNoIntermediates(targets, bottomIRI, piri);
      }
    }
  }

  /**
   * Add a 'subXOf' axiom (subClass, subProperty) to the output ontology.
   *
   * @param childIRI child in SubX axiom
   * @param parentIRI parent in SubX axiom
   */
  private void addSubXOfAxiom(IRI childIRI, IRI parentIRI) {
    EntityType<?> et = getEntityType(childIRI);
    if (et == EntityType.CLASS) {
      OWLClass child = dataFactory.getOWLClass(childIRI);
      OWLClass parent = dataFactory.getOWLClass(parentIRI);
      manager.addAxiom(outputOntology, dataFactory.getOWLSubClassOfAxiom(child, parent));
    } else if (et == EntityType.ANNOTATION_PROPERTY) {
      OWLAnnotationProperty child = dataFactory.getOWLAnnotationProperty(childIRI);
      OWLAnnotationProperty parent = dataFactory.getOWLAnnotationProperty(parentIRI);
      manager.addAxiom(
          outputOntology, dataFactory.getOWLSubAnnotationPropertyOfAxiom(child, parent));
    } else if (et == EntityType.DATA_PROPERTY) {
      OWLDataProperty child = dataFactory.getOWLDataProperty(childIRI);
      OWLDataProperty parent = dataFactory.getOWLDataProperty(parentIRI);
      manager.addAxiom(outputOntology, dataFactory.getOWLSubDataPropertyOfAxiom(child, parent));
    } else if (et == EntityType.OBJECT_PROPERTY) {
      OWLObjectProperty child = dataFactory.getOWLObjectProperty(childIRI);
      OWLObjectProperty parent = dataFactory.getOWLObjectProperty(parentIRI);
      manager.addAxiom(outputOntology, dataFactory.getOWLSubObjectPropertyOfAxiom(child, parent));
    }
  }
}
