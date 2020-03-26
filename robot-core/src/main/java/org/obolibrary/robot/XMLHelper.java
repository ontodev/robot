package org.obolibrary.robot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
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
  private final String AXIOM_TAG = "{http://www.w3.org/2002/07/owl#}Axiom";

  // Other tags
  private final String ONTOLOGY_TAG = "{http://www.w3.org/2002/07/owl#}Ontology";
  private final String LABEL_TAG = "{http://www.w3.org/2000/01/rdf-schema#}label";
  private final String LANG_TAG = "{http://www.w3.org/XML/1998/namespace}lang";
  private final String RDFS_DATATYPE_TAG = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}datatype";
  private final String SUBCLASS_OF_TAG = "{http://www.w3.org/2000/01/rdf-schema#}subClassOf";
  private final String ANNOTATED_TARGET_TAG = "{http://www.w3.org/2002/07/owl#}annotatedTarget";
  private final String ANNOTATED_SOURCE_TAG = "{http://www.w3.org/2002/07/owl#}annotatedSource";
  private final String ANNOTATED_PROPERTY_TAG = "{http://www.w3.org/2002/07/owl#}annotatedProperty";

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

  // TODO - change to use XML writer instead of OWLAPI
  // Keep root element with all prefixes etc
  // If you see an element that you want, copy it in output stream as XML
  // Maybe method for cloning an element?

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
    if (fileName.endsWith(".gz")) {
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(fileName))) {
        getBasicDetails(fis);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(fileName)) {
        getBasicDetails(fis);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
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
    if (fileName.endsWith(".gz")) {
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(fileName))) {
        getBasicDetails(fis);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(fileName)) {
        getBasicDetails(fis);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
    }
  }

  /**
   * Perform an extraction on contents of file at fileName to create a subset containing the target
   * IRIs with their target annotation properties.
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
    // Add declarations and hierarchy structure
    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    initOntology(targets, intermediates);

    // Add desired annotations on targets
    // If 'all' or 'minimal' intermediates, more targets have been added
    // If 'none' intermediates, targets are the same as the provided targets
    // Also add OWLAxioms
    if (this.fileName.endsWith(".gz")) {
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(this.fileName))) {
        addAnnotations(fis, annotationProperties);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(this.fileName))) {
        addOWLAxioms(fis, annotationProperties);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(this.fileName)) {
        addAnnotations(fis, annotationProperties);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
      try (FileInputStream fis = new FileInputStream(this.fileName)) {
        addOWLAxioms(fis, annotationProperties);
      } catch (XMLStreamException e) {
        throw new IOException("Unable to parse XML from " + fileName, e);
      }
    }
    // Finally, build the ontology
    return extract(options);
  }

  /**
   * Perform an extraction on contents of fis to create a subset containing the target IRIs with
   * their target annotation properties.
   *
   * @param options Map of extract options
   * @return extracted subset
   * @throws OWLOntologyCreationException on problem creating empty ontology
   */
  private OWLOntology extract(Map<String, String> options) throws OWLOntologyCreationException {

    boolean annotateSource = OptionsHelper.optionIsTrue(options, "annotate-with-source");

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
   * @return set of IRIs (maybe empty if no parents or it doesn't exist)
   */
  public Set<IRI> getParents(IRI child) {
    return childParentMap.getOrDefault(child, new HashSet<>());
  }

  /**
   * Get the basic details for all entities in an ontology. This includes parent-child
   * relationships, entity types, and labels.
   *
   * @param fis InputStream of XML
   * @throws XMLStreamException on issue parsing XML
   */
  private void getBasicDetails(InputStream fis) throws XMLStreamException {
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
    Set<IRI> doesNotExist = new HashSet<>();
    for (IRI iri : targets) {
      EntityType<?> et = getEntityType(iri);
      if (et == null) {
        // Entity does not exist in the target ontology
        logger.warn(String.format("<%s> does not exist in input ontology", iri.toString()));
        doesNotExist.add(iri);
        continue;
      }
      // Add declaration
      OWLEntity e = dataFactory.getOWLEntity(et, iri);
      manager.addAxiom(outputOntology, dataFactory.getOWLDeclarationAxiom(e));
    }

    // Remove any that do not exist from the target set
    targets.removeAll(doesNotExist);

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
   * Add OWLAxioms to their source entity.
   *
   * @param annotations Set of OWLAnnotations to add from OWLAxiom
   * @param source String source IRI
   * @param property String property IRI
   * @param target String target IRI or null
   * @param targetContent String target content or null
   */
  private void addOWLAxiomsToSource(
      Set<OWLAnnotation> annotations,
      String source,
      String property,
      String target,
      String targetContent) {
    if (!annotations.isEmpty() && source != null && property != null) {
      // We must have a source, a property, and annotations
      // Target (resource) might be null - requires target content
      // Target content might be null - requires target (resource)
      // If both target content and target are not null, target is an IRI for a datatype

      IRI sourceIRI = IRI.create(source.replace("{", "").replace("}", ""));
      if (!allTargets.contains(sourceIRI)) {
        // Only add if the source of axiom is in the targets we are extracting
        return;
      }

      // Target property - potentially an annotation property, or a logical property
      IRI propertyIRI = IRI.create(property.replace("{", "").replace("}", ""));

      // This is either an IRI, a datatype, or null
      IRI targetIRI = null;
      if (target != null) {
        targetIRI = IRI.create(target);
      }

      if (targetContent != null && !targetContent.trim().equals("")) {
        // Create a parent annotation axiom using IRI as a datatype
        OWLAnnotationProperty ap = dataFactory.getOWLAnnotationProperty(propertyIRI);
        OWLLiteral lit;
        if (targetIRI != null) {
          lit = dataFactory.getOWLLiteral(targetContent, dataFactory.getOWLDatatype(targetIRI));
        } else {
          lit = dataFactory.getOWLLiteral(targetContent);
        }
        OWLAnnotation parentAnnotation = dataFactory.getOWLAnnotation(ap, lit);
        OWLAxiom ax =
            dataFactory.getOWLAnnotationAssertionAxiom(sourceIRI, parentAnnotation, annotations);
        manager.addAxiom(outputOntology, ax);

      } else if (targetIRI != null) {
        // Target is an IRI which means the axiom is targeting a logical axiom
        OWLClass sourceClass = dataFactory.getOWLClass(sourceIRI);
        OWLClass targetClass = dataFactory.getOWLClass(targetIRI);
        Set<OWLClass> cls = new HashSet<>();
        cls.add(sourceClass);
        cls.add(targetClass);

        if (propertyIRI
            .toString()
            .equalsIgnoreCase("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
          OWLSubClassOfAxiom subClassOfAxiom =
              dataFactory.getOWLSubClassOfAxiom(sourceClass, targetClass, annotations);
          manager.addAxiom(outputOntology, subClassOfAxiom);
        } else if (propertyIRI
            .toString()
            .equalsIgnoreCase("http://www.w3.org/2002/07/owl#equivalentClasses")) {
          OWLDisjointClassesAxiom disjointClassesAxiom =
              dataFactory.getOWLDisjointClassesAxiom(cls, annotations);
          manager.addAxiom(outputOntology, disjointClassesAxiom);
        } else if (propertyIRI
            .toString()
            .equalsIgnoreCase("http://www.w3.org/2002/07/owl#disjointClasses")) {
          OWLEquivalentClassesAxiom eqClassesAxiom =
              dataFactory.getOWLEquivalentClassesAxiom(cls, annotations);
          manager.addAxiom(outputOntology, eqClassesAxiom);
        }
      }
    }
  }

  /**
   * Add OWLAxiom objects to output ontology for all targets that are sources of OWLAxioms.
   *
   * @param fis InputStream of XML
   * @param annotationProperties set of IRIs for annotation properties to include
   * @throws XMLStreamException on issue parsing XML
   */
  private void addOWLAxioms(InputStream fis, Set<IRI> annotationProperties)
      throws XMLStreamException {
    if (annotationProperties == null || annotationProperties.isEmpty()) {
      // Add all annotation properties if they were not provided
      annotationProperties = allAnnotationProperties;
    }
    XMLInputFactory2 inf = (XMLInputFactory2) XMLInputFactory2.newInstance();
    XMLStreamReader2 sr = (XMLStreamReader2) inf.createXMLStreamReader(fileName, fis);

    int e;
    String node = null;

    // Parts of the OWLAxiom
    String source = null;
    String property = null;
    String target = null;
    String annotationNode = null;

    // Annotation content
    StringBuilder targetContentBuilder = null;
    String targetContent = null;
    StringBuilder annotationContentBuilder = null;

    Set<OWLAnnotation> annotations = new HashSet<>();

    // Datatype of annotation
    String annotationDt = null;

    try {
      while (sr.hasNext()) {
        e = sr.next();
        if (e == XMLEvent.START_ELEMENT && sr.getName().toString().equalsIgnoreCase(AXIOM_TAG)) {
          // Start of OWLAxiom - the only element we care about
          boolean inAx = true;
          while (inAx) {
            // Loop through elements until we hit end of AXIOM_TAG
            // When we do, break out of this while loop
            if (sr.hasNext()) {
              e = sr.next();
              if (e == XMLEvent.END_ELEMENT) {
                node = sr.getName().toString();
                if (!node.equalsIgnoreCase(AXIOM_TAG)) {
                  // End of OWL Axiom, reset all values and break
                  if (node.equalsIgnoreCase(ANNOTATED_TARGET_TAG) && targetContentBuilder != null) {
                    // End of target with content
                    targetContent = targetContentBuilder.toString();
                    targetContentBuilder = null;

                  } else if (node.equalsIgnoreCase(annotationNode)
                      && annotationContentBuilder != null) {
                    // End of annotation with content
                    String annotationContent = annotationContentBuilder.toString();

                    // Create annotation and add to set, as there may be more than one
                    OWLLiteral literal;
                    if (annotationDt != null) {
                      OWLDatatype dt = dataFactory.getOWLDatatype(IRI.create(annotationDt));
                      literal = dataFactory.getOWLLiteral(annotationContent, dt);
                    } else {
                      literal = dataFactory.getOWLLiteral(annotationContent);
                    }

                    IRI annotationIRI =
                        IRI.create(annotationNode.replace("{", "").replace("}", ""));
                    if (annotationProperties.contains(annotationIRI)) {
                      // Only include the annotation if the annotation property is in our set
                      OWLAnnotationProperty ap =
                          dataFactory.getOWLAnnotationProperty(annotationIRI);
                      OWLAnnotation a = dataFactory.getOWLAnnotation(ap, literal);
                      annotations.add(a);
                      annotationContentBuilder = null;
                      annotationNode = null;
                    }
                  }
                } else {
                  // End of OWL Axiom
                  // Add the annotations
                  addOWLAxiomsToSource(annotations, source, property, target, targetContent);
                  inAx = false;
                  annotations = new HashSet<>();
                }

              } else if (e == XMLEvent.START_ELEMENT) {
                node = sr.getName().toString();
                switch (node) {
                  case ANNOTATED_SOURCE_TAG:
                    if (sr.getAttributeCount() > 0) {
                      // Source should always have a resource
                      source = sr.getAttributeValue(0);
                    }
                    break;

                  case ANNOTATED_PROPERTY_TAG:
                    if (sr.getAttributeCount() > 0) {
                      // Property should always have a resource
                      property = sr.getAttributeValue(0);
                    }
                    break;

                  case ANNOTATED_TARGET_TAG:
                    if (sr.getAttributeCount() > 0) {
                      // This may or may not have a resource
                      // This is either a datatype or another entity IRI
                      target = sr.getAttributeValue(0);
                    }
                    break;

                  default:
                    // The node is the annotation property
                    annotationNode = node;
                    if (sr.getAttributeCount() > 0) {
                      // This might have a datatype
                      annotationDt = sr.getAttributeValue(0);
                    }
                }

              } else if (e == XMLEvent.CHARACTERS) {
                if (node == null) {
                  // node should never be null here
                  // but just in case, continue to next element
                  continue;
                }

                String content = sr.getText();
                if (node.equalsIgnoreCase(ANNOTATED_TARGET_TAG)) {
                  // Target content (if the OWLAxiom is targeting another annotation)
                  if (targetContentBuilder == null) {
                    targetContentBuilder = new StringBuilder();
                  }
                  if (!content.trim().isEmpty()) {
                    targetContentBuilder.append(content);
                  }

                } else if (annotationNode != null) {
                  // Annotation content
                  if (annotationContentBuilder == null) {
                    annotationContentBuilder = new StringBuilder();
                  }
                  if (!content.trim().isEmpty()) {
                    annotationContentBuilder.append(content);
                  }
                }
              }
            } else {
              // End of OWL Axiom
              // Add the annotations
              addOWLAxiomsToSource(annotations, source, property, target, targetContent);
              inAx = false;
              annotations = new HashSet<>();
            }
          }
        }
      }
    } finally {
      sr.closeCompletely();
    }
  }

  /**
   * Add annotations to output ontology for all targets.
   *
   * @param fis InputStream of XML
   * @param annotationProperties set of IRIs for annotation properties to include
   * @throws XMLStreamException on issue parsing XML
   */
  private void addAnnotations(InputStream fis, Set<IRI> annotationProperties)
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
              apIRI = null;
              currentContent = null;
              annotationDatatype = null;
              lang = null;
              continue;
            }

            // Could be an annotation with a datatype or language
            // We only care about the annotations on target IRIs
            if (sr.getAttributeCount() > 0 && !isEntity && iri != null) {
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
