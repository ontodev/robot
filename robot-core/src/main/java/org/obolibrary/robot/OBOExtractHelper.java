package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OBOExtractHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(OBOExtractHelper.class);

  private final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
  private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  private OWLOntology outputOntology;

  private IOHelper ioHelper;

  private String fileName;
  private IRI outputIRI;

  // Maps to resolve labels, relationships, and types from IRIs
  private Map<IRI, Set<IRI>> childParentMap = new HashMap<>();
  private Map<IRI, Set<IRI>> instanceTypeMap = new HashMap<>();
  private Map<IRI, EntityType> entityTypeMap = new HashMap<>();

  // Tracking added targets (if intermediates = all or minimal)
  private Set<IRI> allTargets = new HashSet<>();

  // Tracking all annotation properties, if none are specified
  private Set<IRI> allAnnotationProperties = new HashSet<>();

  private String namespace = null;

  // Map of subset names (ID) -> IRIs
  private Map<String, IRI> subsetMap = new HashMap<>();

  // Map of property names (ID) -> IRIs
  private Map<String, IRI> propertyMap = new HashMap<>();

  private final String OBO_IN_OWL = "http://www.geneontology.org/formats/oboInOwl#";
  private final String OBO = "http://purl.obolibrary.org/obo/";

  // Default annotation properties in OBO format
  private final IRI ALT_ID = IRI.create(OBO_IN_OWL + "hasAlternativeId");
  private final IRI ANTI_SYMMETRIC = IRI.create(OBO + "IAO_0000427");
  private final IRI BROAD_SYNONYM = IRI.create(OBO_IN_OWL + "hasBroadSynonym");
  private final IRI COMMENT = dataFactory.getRDFSComment().getIRI();
  private final IRI CREATED_BY = IRI.create(OBO_IN_OWL + "createdBy");
  private final IRI CREATION_DATE = IRI.create(OBO_IN_OWL + "creationDate");
  private final IRI CYCLIC = IRI.create(OBO_IN_OWL + "isCyclic");
  private final IRI DB_XREF = IRI.create(OBO_IN_OWL + "hasDbXref");
  private final IRI DEFINITION = IRI.create(OBO + "IAO_0000115");
  private final IRI EXACT_SYNONYM = IRI.create(OBO_IN_OWL + "hasExactSynonym");
  private final IRI NARROW_SYNONYM = IRI.create(OBO_IN_OWL + "hasNarrowSynonym");
  private final IRI OBSOLETE = IRI.create("http://www.w3.org/2002/07/owl#deprecated");
  private final IRI RELATED_SYNONYM = IRI.create(OBO_IN_OWL + "hasRelatedSynonym");
  private final IRI REPLACED_BY = IRI.create(OBO_IN_OWL + "replacedBy");
  private final IRI SUBSET = IRI.create(OBO_IN_OWL + "inSubset");

  /**
   * @param ioHelper IOHelper to resolve IRIs
   * @param fileName path to OBO file
   * @param outputIRI IRI of output ontology, or null
   * @throws Exception on any problem
   */
  public OBOExtractHelper(IOHelper ioHelper, String fileName, IRI outputIRI) throws Exception {
    this.fileName = fileName;
    outputOntology = manager.createOntology();
    if (outputIRI != null) {
      this.outputIRI = outputIRI;
    }

    this.ioHelper = ioHelper;

    // Create maps: IRI -> label, child -> parents
    if (fileName.endsWith(".gz")) {
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(fileName))) {
        getBasicDetails(fis);
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + fileName, e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(fileName)) {
        getBasicDetails(fis);
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + fileName, e);
      }
    }
  }

  /**
   * Create a new XMLHelper for parsing an ontology from an IRI. This will create a temporary file,
   * which will be deleted on exit.
   *
   * @param ioHelper IOHelper to resolve IRIs
   * @param inputIRI IRI of OBO file
   * @param outputIRI IRI of output ontology, or null
   * @throws Exception on any problem
   */
  public OBOExtractHelper(IOHelper ioHelper, IRI inputIRI, IRI outputIRI) throws Exception {
    this.ioHelper = ioHelper;

    // We will create a temporary file
    String fileName = inputIRI.toString().substring(inputIRI.toString().lastIndexOf("/"));
    String ext = fileName.substring(fileName.lastIndexOf("."));
    String fn = fileName.substring(0, fileName.lastIndexOf("."));
    File temp = File.createTempFile(fn, ext);

    // Make sure this file is removed on exit
    temp.deleteOnExit();
    this.fileName = temp.getAbsolutePath();

    // Download ontology, following redirects
    logger.info(String.format("Starting download from <%s>", inputIRI.toString()));
    CloseableHttpClient httpclient =
        HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    try {
      HttpGet get = new HttpGet(inputIRI.toURI());
      HttpResponse r = httpclient.execute(get);
      InputStream source = r.getEntity().getContent();
      FileUtils.copyInputStreamToFile(source, temp);
    } catch (Exception e) {
      throw new IOException("Unable to download content from " + inputIRI.toString(), e);
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
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + inputIRI.toString(), e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(fileName)) {
        getBasicDetails(fis);
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + inputIRI.toString(), e);
      }
    }
  }

  /**
   * Perform an extraction on contents of file at fileName to create a subset containing the target
   * IRIs with their target annotation properties.
   *
   * @param upperTargets upper IRIs to extract
   * @param lowerTargets lower IRIs to extract
   * @param annotationProperties IRIs of annotation properties to include
   * @param options Map of extract options
   * @return extracted subset
   * @throws IOException on problem parsing OBO
   * @throws OWLOntologyCreationException on problem creating empty ontology
   */
  public OWLOntology extract(
      Set<IRI> upperTargets,
      Set<IRI> lowerTargets,
      Set<IRI> annotationProperties,
      Map<String, String> options)
      throws IOException, OWLOntologyCreationException {
    // Add declarations and hierarchy structure
    String intermediates = OptionsHelper.getOption(options, "intermediates", "all");
    initOntology(upperTargets, lowerTargets, intermediates);

    // Add desired annotations on targets
    // If 'all' or 'minimal' intermediates, more targets have been added
    // If 'none' intermediates, targets are the same as the provided targets
    // Also add OWLAxioms
    if (this.fileName.endsWith(".gz")) {
      try (GZIPInputStream fis = new GZIPInputStream(new FileInputStream(this.fileName))) {
        addAnnotations(fis, annotationProperties);
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + fileName, e);
      }
    } else {
      try (FileInputStream fis = new FileInputStream(this.fileName)) {
        addAnnotations(fis, annotationProperties);
      } catch (IOException e) {
        throw new IOException("Unable to parse OBO from " + fileName, e);
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
      outputIRI = IRI.create(namespace.substring(0, namespace.length() - 1) + ".obo");
    }
    OWLOntologyManager manager = outputOntology.getOWLOntologyManager();
    Set<OWLOntology> ont = new HashSet<>();
    ont.add(outputOntology);
    return manager.createOntology(outputIRI, ont);
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
   * Get the entity type of an entity by its IRI
   *
   * @param iri IRI of entity
   * @return EntityType or null
   */
  public EntityType getEntityType(IRI iri) {
    return entityTypeMap.getOrDefault(iri, null);
  }

  /**
   * Create a hiearchy within the output ontology with all intermediates. Terms not included in
   * target set will be used to fill in gaps. Update the set of targets to include added entities.
   *
   * @param upperTargets set of target IRIs
   * @param childIRI IRI for current term to be asserted as child of something else
   */
  private void addAncestorsAllIntermediates(Set<IRI> upperTargets, IRI childIRI) {
    Set<IRI> parents = getParents(childIRI);
    for (IRI piri : parents) {
      addSubXOfAxiom(childIRI, piri);
      if (!upperTargets.contains(piri)) {
        // Only continue if this isn't an upper target
        addAncestorsAllIntermediates(upperTargets, piri);
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
   * Add annotations to output ontology for all targets.
   *
   * @param fis InputStream of XML
   * @param annotationProperties set of IRIs for annotation properties to include
   * @throws IOException on issue parsing OBO
   */
  private void addAnnotations(InputStream fis, Set<IRI> annotationProperties) throws IOException {
    if (annotationProperties == null || annotationProperties.isEmpty()) {
      // Add all annotation properties if they were not provided
      annotationProperties = allAnnotationProperties;
    }
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));

    // Renamer for updating property IRIs
    OWLEntityRenamer entityRenamer = new OWLEntityRenamer(manager, Sets.newHashSet(outputOntology));

    // Current entity IRI
    IRI iri = null;

    // Track all intersections
    Set<OWLClassExpression> intersections = new HashSet<>();
    int intersectionCount = 0;

    // Track all unions
    Set<OWLClassExpression> unions = new HashSet<>();
    int unionCount = 0;

    // Tracking vars
    boolean inProperty = false;
    boolean inHeader = true;
    int n = 0;

    while (br.ready()) {
      n++;
      String line = br.readLine();

      // TODO - ontology annotations
      // format-version
      // data-version
      // date
      // saved-by
      // auto-generated-by
      // import
      // synonymtypedef
      // default-namespace
      // namespace-id-rule
      // idspace
      // treat-xrefs-as-equivalent
      // treat-xrefs-as-genus-differentia
      // treat-xrefs-as-relationship
      // treat-xrefs-as-is_a
      // remark
      if (inHeader) {
        if (line.isEmpty()) {
          inHeader = false;
          continue;
        }
      }

      if (line.isEmpty()) {
        // End of term, check for logic then reset IRI
        if (!intersections.isEmpty() && intersections.size() == intersectionCount) {
          OWLObjectIntersectionOf intersect = dataFactory.getOWLObjectIntersectionOf(intersections);
          OWLAxiom ax =
              dataFactory.getOWLEquivalentClassesAxiom(dataFactory.getOWLClass(iri), intersect);
          manager.addAxiom(outputOntology, ax);
        }
        if (!unions.isEmpty() && unions.size() == unionCount) {
          OWLObjectUnionOf union = dataFactory.getOWLObjectUnionOf(unions);
          OWLAxiom ax =
              dataFactory.getOWLEquivalentClassesAxiom(dataFactory.getOWLClass(iri), union);
          manager.addAxiom(outputOntology, ax);
        }
        intersectionCount = 0;
        unionCount = 0;
        iri = null;

      } else if (line.equals("[Term]")) {
        line = br.readLine().trim();
        String curie = line.substring(4);
        iri = ioHelper.createIRI(curie);
        if (iri == null) {
          logger.error(String.format("Unable to create IRI from Term '%s' on line %d", curie, n));
        }

      } else if (line.equals("[Typedef]")) {
        inProperty = true;
        line = br.readLine().trim();
        String curie = line.substring(4);
        iri = ioHelper.createIRI(curie);
        if (iri == null) {
          iri = ioHelper.createIRI(namespace + curie);
          if (iri == null) {
            logger.error(
                String.format("Unable to create IRI from Typedef '%s' on line %d", curie, n));
          }
        }

      } else if (line.equals("[Instance]")) {
        // Warning - this is not supported by OBOFormatParser
        line = br.readLine().trim();
        String curie = line.substring(4);
        iri = ioHelper.createIRI(curie);
        if (iri == null) {
          logger.error(
              String.format("Unable to create IRI from Instance '%s' on line %d", curie, n));
        }

      } else if (iri != null && allTargets.contains(iri)) {

        // Logic
        if (line.startsWith("intersection_of: ")) {
          intersectionCount++;
          // These are always equivalent
          String[] parts = line.substring(17).split(" ");
          if (parts.length >= 4) {
            // Anonymous
            String property = parts[0];
            String value = parts[1];
            OWLClassExpression svf = getSomeValuesFrom(property, value, n);
            if (svf != null) {
              intersections.add(svf);
            }
          } else {
            // Named
            String value = parts[0];
            IRI valueIRI = ioHelper.createIRI(value);
            if (valueIRI == null) {
              logger.error(
                  String.format("Unable to create class IRI from '%s' on line %d", value, n));
              continue;
            }
            if (allTargets.contains(valueIRI)) {
              intersections.add(dataFactory.getOWLClass(valueIRI));
            }
          }

        } else if (line.startsWith("union_of: ")) {
          unionCount++;
          // These are always equivalent
          String[] parts = line.substring(10).split(" ");

          if (parts.length >= 4) {
            // Anonymous
            String property = parts[0];
            String value = parts[1];
            OWLClassExpression svf = getSomeValuesFrom(property, value, n);
            if (svf != null) {
              unions.add(svf);
            }
          } else {
            // Named
            String value = parts[0];
            IRI valueIRI = ioHelper.createIRI(value);
            if (valueIRI == null) {
              logger.error(
                  String.format("Unable to create class IRI from '%s' on line %d", value, n));
              continue;
            }
            unions.add(dataFactory.getOWLClass(valueIRI));
          }

        } else if (line.startsWith("disjoint_from: ")) {
          String value = line.substring(15);
          addDisjointAxiom(iri, value, n);

          // Annotations
        } else if (line.startsWith("name: ")
            && annotationProperties.contains(dataFactory.getRDFSLabel().getIRI())) {
          String label = line.substring(6);

          // Add axiom to output
          OWLLiteral l = dataFactory.getOWLLiteral(label);
          OWLAnnotation a = dataFactory.getOWLAnnotation(dataFactory.getRDFSLabel(), l);
          OWLAnnotationAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("alt_id: ") && annotationProperties.contains(ALT_ID)) {
          String altID = line.substring(6);

          // Add axiom to output
          OWLLiteral l = dataFactory.getOWLLiteral(altID);
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(ALT_ID), l);
          OWLAnnotationAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("def: ") && annotationProperties.contains(DEFINITION)) {
          addDefinition(iri, line);

        } else if (line.startsWith("comment: ") && annotationProperties.contains(COMMENT)) {
          // Add an rdfs:comment
          String comment = line.substring(9);
          OWLLiteral l = dataFactory.getOWLLiteral(comment);
          OWLAnnotation a = dataFactory.getOWLAnnotation(dataFactory.getRDFSComment(), l);
          OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("subset: ") && annotationProperties.contains(SUBSET)) {
          String subset = line.substring(8);

          // Get subset IRI
          IRI subsetIRI = subsetMap.getOrDefault(subset, null);
          if (subsetIRI == null) {
            logger.error(String.format("Subset '%s' on line %d is not defined", subset, n));
            continue;
          }

          // Add the axiom to the output ontology
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(SUBSET), subsetIRI);
          OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("synonym: ")
            && line.contains("EXACT [")
            && annotationProperties.contains(EXACT_SYNONYM)) {
          addSynonym(line, iri, EXACT_SYNONYM);

        } else if (line.startsWith("synonym: ")
            && line.contains("NARROW [")
            && annotationProperties.contains(NARROW_SYNONYM)) {
          addSynonym(line, iri, NARROW_SYNONYM);

        } else if (line.startsWith("synonym: ")
            && line.contains("BROAD [")
            && annotationProperties.contains(BROAD_SYNONYM)) {
          addSynonym(line, iri, BROAD_SYNONYM);

        } else if (line.startsWith("synonym: ")
            && line.contains("RELATED [")
            && annotationProperties.contains(RELATED_SYNONYM)) {
          addSynonym(line, iri, RELATED_SYNONYM);

        } else if (line.startsWith("xref: ") && annotationProperties.contains(DB_XREF)) {
          if (inProperty) {
            // First xref will be the ID
            String curie = line.substring(6);
            IRI newIRI = ioHelper.createIRI(curie);
            if (newIRI == null) {
              logger.error(
                  String.format("Unable to create property IRI from '%s' on line %d", curie, n));
              continue;
            }

            // Apply changes to existing axioms to update to the new IRI
            manager.applyChanges(entityRenamer.changeIRI(iri, newIRI));
            iri = newIRI;
            inProperty = false;
          } else {
            // Treat as regular xref
            String xref = line.substring(6);
            OWLLiteral l = dataFactory.getOWLLiteral(xref);
            OWLAnnotation a =
                dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DB_XREF), l);
            OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("created_by: ") && annotationProperties.contains(CREATED_BY)) {
          String cb = line.substring(12);
          OWLLiteral l = dataFactory.getOWLLiteral(cb);
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(CREATED_BY), l);
          OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("creation_date: ")
            && annotationProperties.contains(CREATION_DATE)) {
          String cd = line.substring(15);
          OWLLiteral l = dataFactory.getOWLLiteral(cd);
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(CREATION_DATE), l);
          OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if ((line.startsWith("replaced_by: ") || line.startsWith("consider: "))
            && annotationProperties.contains(REPLACED_BY)) {
          String curie;
          if (line.startsWith("replaced_by: ")) {
            curie = line.substring(13);
          } else {
            curie = line.substring(10);
          }
          IRI replacedIRI = ioHelper.createIRI(curie);
          if (replacedIRI == null) {
            logger.error(String.format("Unable to create IRI from '%s' on line %d", curie, n));
            continue;
          }
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(
                  dataFactory.getOWLAnnotationProperty(REPLACED_BY), replacedIRI);
          OWLAnnotationAssertionAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("property_value: ")) {
          addPropertyValue(iri, line, annotationProperties, n);

        } else if (line.startsWith("domain: ")) {
          String value = line.substring(8).split(" ")[0];
          IRI valueIRI = ioHelper.createIRI(value);
          if (valueIRI == null) {
            logger.error(
                String.format("Unable to create class IRI from '%s' on line %d", value, n));
            continue;
          }
          if (!allTargets.contains(valueIRI)) {
            continue;
          }
          OWLClass c = dataFactory.getOWLClass(valueIRI);
          OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
          OWLAxiom ax = dataFactory.getOWLObjectPropertyDomainAxiom(op, c);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("range: ")) {
          String value = line.substring(7).split(" ")[0];
          IRI valueIRI = ioHelper.createIRI(value);
          if (valueIRI == null) {
            logger.error(
                String.format("Unable to create class IRI from '%s' on line %d", value, n));
            continue;
          }
          if (!allTargets.contains(valueIRI)) {
            continue;
          }
          OWLClass c = dataFactory.getOWLClass(valueIRI);
          OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
          OWLAxiom ax = dataFactory.getOWLObjectPropertyRangeAxiom(op, c);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("inverse_of: ")) {
          // Inverse object properties
          String value = line.substring(12);
          IRI valueIRI = ioHelper.createIRI(value);
          if (valueIRI == null) {
            logger.error(
                String.format("Unable to create property IRI from '%s' on line %d", value, n));
            continue;
          }
          if (!allTargets.contains(valueIRI)) {
            continue;
          }
          OWLObjectProperty subjectOP = dataFactory.getOWLObjectProperty(iri);
          OWLObjectProperty valueOP = dataFactory.getOWLObjectProperty(valueIRI);
          OWLAxiom ax = dataFactory.getOWLInverseObjectPropertiesAxiom(subjectOP, valueOP);
          manager.addAxiom(outputOntology, ax);

        } else if (line.startsWith("is_anti_symmetric: ")) {
          String value = line.substring(19);
          addBooleanAnnotation(iri, ANTI_SYMMETRIC, value);

        } else if (line.startsWith("is_cyclic: ")) {
          String value = line.substring(11);
          addBooleanAnnotation(iri, CYCLIC, value);

        } else if (line.startsWith("is_reflexive: ")) {
          String value = line.substring(15);
          boolean val = value.trim().equalsIgnoreCase("true");
          if (val) {
            OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
            OWLAxiom ax = dataFactory.getOWLReflexiveObjectPropertyAxiom(op);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("is_symmetric: ")) {
          String value = line.substring(15);
          boolean val = value.trim().equalsIgnoreCase("true");
          if (val) {
            OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
            OWLAxiom ax = dataFactory.getOWLSymmetricObjectPropertyAxiom(op);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("is_transitive: ")) {
          String value = line.substring(15);
          boolean val = value.trim().equalsIgnoreCase("true");
          if (val) {
            OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
            OWLAxiom ax = dataFactory.getOWLTransitiveObjectPropertyAxiom(op);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("is_functional: ")) {
          String value = line.substring(15);
          boolean val = value.trim().equalsIgnoreCase("true");
          if (val) {
            OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
            OWLAxiom ax = dataFactory.getOWLFunctionalObjectPropertyAxiom(op);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("is_inverse_functional: ")) {
          String value = line.substring(15);
          boolean val = value.trim().equalsIgnoreCase("true");
          if (val) {
            OWLObjectProperty op = dataFactory.getOWLObjectProperty(iri);
            OWLAxiom ax = dataFactory.getOWLInverseFunctionalObjectPropertyAxiom(op);
            manager.addAxiom(outputOntology, ax);
          }

        } else if (line.startsWith("is_obsolete: ") && annotationProperties.contains(OBSOLETE)) {
          String value = line.substring(13);
          addBooleanAnnotation(iri, OBSOLETE, value);
        }

        // TODO - not supported class
        // - relationship

        // TODO - not supported typedef
        // - relationship
        // - transitive_over
        // - holds_over_chain
        // - equivalent_to_chain
        // - disjoint_over (annotation)
        // - expand_expression_to
        // - expand_assertion_to
      }
    }
  }

  /**
   * Add an annotation with a boolean value to a subject.
   *
   * @param iri IRI of annotation subject
   * @param apIRI IRI of annotation property
   * @param value String value to be converted to boolean
   */
  private void addBooleanAnnotation(IRI iri, IRI apIRI, String value) {
    OWLLiteral l;
    if (value.trim().equalsIgnoreCase("true")) {
      l = dataFactory.getOWLLiteral(true);
    } else {
      l = dataFactory.getOWLLiteral(false);
    }
    OWLAnnotationProperty ap = dataFactory.getOWLAnnotationProperty(apIRI);
    OWLAnnotation a = dataFactory.getOWLAnnotation(ap, l);
    OWLAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
    manager.addAxiom(outputOntology, ax);
  }

  /**
   * Add a definition to the output ontology.
   *
   * @param iri IRI of subject
   * @param line def line from OBO file
   */
  private void addDefinition(IRI iri, String line) {
    // Extract text between quotes
    String def = line.substring(6).split("\"")[0];

    // Check for DB xrefs - if the xrefs are empty, ignore this
    Set<OWLAnnotation> xrefs = new HashSet<>();
    if (!line.endsWith("[]")) {
      Matcher m = Pattern.compile("\".+\"\\[(.+)]").matcher(line);
      if (m.matches()) {
        String xrefString = m.group(1);
        if (xrefString.contains(", ")) {
          for (String x : xrefString.split(", ")) {
            OWLLiteral l = dataFactory.getOWLLiteral(x);
            OWLAnnotation a =
                dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DB_XREF), l);
            xrefs.add(a);
          }
        } else {
          OWLLiteral l = dataFactory.getOWLLiteral(xrefString);
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DB_XREF), l);
          xrefs.add(a);
        }
      }
    }

    // Add definition axiom to output
    OWLLiteral l = dataFactory.getOWLLiteral(def);
    OWLAnnotation a =
        dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DEFINITION), l, xrefs);
    OWLAnnotationAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
    manager.addAxiom(outputOntology, ax);
  }

  /**
   * Add a disjoint axiom between the subject IRI and any expression in the array.
   *
   * @param iri IRI to add disjoint axiom to
   * @param value the disjoint class or class expression
   * @param n int line number
   */
  private void addDisjointAxiom(IRI iri, String value, int n) {
    String[] parts = value.split(" ");
    if (parts.length >= 4) {
      // Anonymous
      String property = parts[0];
      IRI propertyIRI;
      if (propertyMap.containsKey(property)) {
        propertyIRI = propertyMap.get(property);
      } else {
        propertyIRI = ioHelper.createIRI(property);
        if (propertyIRI == null) {
          propertyIRI = ioHelper.createIRI(namespace + property);
        }
      }
      if (propertyIRI == null) {
        logger.error(
            String.format("Unable to create property IRI from '%s' on line %d", property, n));
        return;
      }
      String v = parts[1];
      IRI valueIRI = ioHelper.createIRI(v);
      if (valueIRI == null) {
        logger.error(String.format("Unable to create class IRI from '%s' on line %d", v, n));
        return;
      }
      if (allTargets.contains(valueIRI)) {
        OWLClassExpression svf =
            dataFactory.getOWLObjectSomeValuesFrom(
                dataFactory.getOWLObjectProperty(propertyIRI), dataFactory.getOWLClass(valueIRI));
        OWLAxiom ax = dataFactory.getOWLDisjointClassesAxiom(dataFactory.getOWLClass(iri), svf);
        manager.addAxiom(outputOntology, ax);
      }
    } else {
      // Named
      String v = parts[0];
      IRI valueIRI = ioHelper.createIRI(v);
      if (valueIRI == null) {
        logger.error(String.format("Unable to create class IRI from '%s' on line %d", v, n));
        return;
      }
      if (allTargets.contains(valueIRI)) {
        OWLAxiom ax =
            dataFactory.getOWLDisjointClassesAxiom(
                dataFactory.getOWLClass(iri), dataFactory.getOWLClass(valueIRI));
        manager.addAxiom(outputOntology, ax);
      }
    }
  }

  /**
   * Add a property value to the output ontology.
   *
   * @param iri IRI of subject
   * @param line String line from file
   * @param annotationProperties set of IRIs of annotation properties to include
   * @param n int line number
   */
  private void addPropertyValue(IRI iri, String line, Set<IRI> annotationProperties, int n) {
    String property = line.substring(16).split(" ")[0];

    IRI propertyIRI;
    if (propertyMap.containsKey(property)) {
      propertyIRI = propertyMap.get(property);
    } else {
      propertyIRI = ioHelper.createIRI(property);
      if (propertyIRI == null) {
        propertyIRI = ioHelper.createIRI(namespace + property);
      }
    }

    if (propertyIRI == null) {
      logger.error(
          String.format("Unable to create property IRI from '%s' on line %d", property, n));
      return;
    }

    if (!annotationProperties.contains(propertyIRI)) {
      return;
    }

    OWLAnnotationProperty ap = dataFactory.getOWLAnnotationProperty(propertyIRI);
    OWLAnnotation a;

    String[] parts = line.substring(16).split(" ");
    String last = parts[parts.length - 1];
    if (last.contains("xsd:")) {
      IRI xsdIRI = ioHelper.createIRI(last);
      Matcher m = Pattern.compile(".+\"(.+)\"").matcher(line);
      String value = m.group(1);
      OWLLiteral l = dataFactory.getOWLLiteral(value, dataFactory.getOWLDatatype(xsdIRI));
      a = dataFactory.getOWLAnnotation(ap, l);
    } else {
      // IRI value
      IRI valueIRI = ioHelper.createIRI(last);
      a = dataFactory.getOWLAnnotation(ap, valueIRI);
    }
    OWLAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(iri, a);
    manager.addAxiom(outputOntology, ax);
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
      OWLAxiom ax = dataFactory.getOWLSubClassOfAxiom(child, parent);
      manager.addAxiom(outputOntology, ax);
    } else if (et == EntityType.ANNOTATION_PROPERTY) {
      OWLAnnotationProperty child = dataFactory.getOWLAnnotationProperty(childIRI);
      OWLAnnotationProperty parent = dataFactory.getOWLAnnotationProperty(parentIRI);
      OWLAxiom ax = dataFactory.getOWLSubAnnotationPropertyOfAxiom(child, parent);
      manager.addAxiom(outputOntology, ax);
    } else if (et == EntityType.DATA_PROPERTY) {
      OWLDataProperty child = dataFactory.getOWLDataProperty(childIRI);
      OWLDataProperty parent = dataFactory.getOWLDataProperty(parentIRI);
      OWLAxiom ax = dataFactory.getOWLSubDataPropertyOfAxiom(child, parent);
      manager.addAxiom(outputOntology, ax);
    } else if (et == EntityType.OBJECT_PROPERTY) {
      OWLObjectProperty child = dataFactory.getOWLObjectProperty(childIRI);
      OWLObjectProperty parent = dataFactory.getOWLObjectProperty(parentIRI);
      OWLAxiom ax = dataFactory.getOWLSubObjectPropertyOfAxiom(child, parent);
      manager.addAxiom(outputOntology, ax);
    }
  }

  /**
   * Add a synonym from an OBO line to the output ontology.
   *
   * @param line line from OBO
   * @param subject subject of stanzas
   * @param property synonym property IRI (exact, narrow, broad, related)
   */
  private void addSynonym(String line, IRI subject, IRI property) {
    // Extract text between quotes
    String syn = line.substring(10).split("\"")[0];

    // Check for DB xrefs
    Set<OWLAnnotation> xrefs = new HashSet<>();
    Matcher m = Pattern.compile(".+\\[(.+)]").matcher(line);
    if (m.matches()) {
      String xrefString = m.group(1);
      if (xrefString.contains(", ")) {
        for (String x : xrefString.split(", ")) {
          OWLLiteral l = dataFactory.getOWLLiteral(x);
          OWLAnnotation a =
              dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DB_XREF), l);
          xrefs.add(a);
        }
      } else {
        OWLLiteral l = dataFactory.getOWLLiteral(xrefString);
        OWLAnnotation a =
            dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(DB_XREF), l);
        xrefs.add(a);
      }
    }

    // Add axiom to output
    OWLLiteral l = dataFactory.getOWLLiteral(syn);
    OWLAnnotation a =
        dataFactory.getOWLAnnotation(dataFactory.getOWLAnnotationProperty(property), l, xrefs);
    OWLAnnotationAxiom ax = dataFactory.getOWLAnnotationAssertionAxiom(subject, a);
    manager.addAxiom(outputOntology, ax);
  }

  /**
   * Get the basic details for all entities in an ontology. This includes parent-child
   * relationships, entity types, and labels.
   *
   * @param fis InputStream of OBO
   * @throws IOException on issue parsing OBO
   */
  private void getBasicDetails(InputStream fis) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));

    // Add OBO defaults to set of all annotation properties
    allAnnotationProperties.add(dataFactory.getRDFSLabel().getIRI());
    allAnnotationProperties.add(dataFactory.getRDFSComment().getIRI());
    allAnnotationProperties.add(ALT_ID);
    allAnnotationProperties.add(DEFINITION);
    allAnnotationProperties.add(EXACT_SYNONYM);
    allAnnotationProperties.add(NARROW_SYNONYM);
    allAnnotationProperties.add(BROAD_SYNONYM);
    allAnnotationProperties.add(RELATED_SYNONYM);
    allAnnotationProperties.add(DB_XREF);
    allAnnotationProperties.add(OBSOLETE);
    allAnnotationProperties.add(REPLACED_BY);
    allAnnotationProperties.add(CREATED_BY);
    allAnnotationProperties.add(CREATION_DATE);

    IRI iri = null;
    String propertyID = null;
    boolean inHeader = true;

    int n = 0;
    while (br.ready()) {
      n++;
      String line = br.readLine().trim();

      // Ontology header details
      // If 'ontology' is not defined in ontology header, TEMP_NAMESPACE is used
      String tempNamespace = "http://purl.obolibrary.org/obo/TEMP#";
      if (inHeader) {
        if (line.startsWith("ontology: ")) {
          String value = line.substring(10);
          // This is used to create IRIs from Typedef IDs
          if (!value.startsWith("http")) {
            namespace = "http://purl.obolibrary.org/obo/" + value + "#";
          } else if (value.endsWith(".owl") || value.endsWith(".obo")) {
            namespace = value.substring(0, value.length() - 4);
          }

        } else if (line.startsWith("subsetdef: ")) {
          String subset = line.substring(11);
          IRI subsetIRI;
          if (namespace != null) {
            subsetIRI = ioHelper.createIRI(namespace + subset);
          } else {
            subsetIRI = ioHelper.createIRI(tempNamespace + subset);
          }
          subsetMap.put(subset, subsetIRI);

        } else if (line.isEmpty()) {
          inHeader = false;
          continue;
        }

        // TODO - do we want to handle owl axioms?
      }

      if (line.isEmpty()) {
        // End of term, reset IRI
        if (!entityTypeMap.containsKey(iri)) {
          entityTypeMap.put(iri, EntityType.OBJECT_PROPERTY);
        }
        iri = null;

      } else if (line.equals("[Term]")) {
        line = br.readLine().trim();
        String curie = line.substring(4);
        iri = ioHelper.createIRI(curie);
        entityTypeMap.put(iri, EntityType.CLASS);

      } else if (line.equals("[Typedef]")) {
        line = br.readLine().trim();
        propertyID = line.substring(4);

        // default namespace + ID
        if (namespace != null) {
          iri = ioHelper.createIRI(namespace + propertyID);
        } else {
          iri = ioHelper.createIRI(tempNamespace + propertyID);
        }
        // entityTypeMap.put(iri, EntityType.OBJECT_PROPERTY);
        propertyMap.put(propertyID, iri);

      } else if (line.equals("[Instance]")) {
        line = br.readLine().trim();
        String curie = line.substring(4);
        iri = ioHelper.createIRI(curie);
        entityTypeMap.put(iri, EntityType.NAMED_INDIVIDUAL);

      } else if (line.startsWith("property_value: ")) {
        String curie = line.substring(16).split(" ")[0];
        if (propertyMap.containsKey(curie)) {
          // Already found
          continue;
        }
        IRI annotationPropertyIRI = ioHelper.createIRI(curie);
        if (annotationPropertyIRI == null) {
          annotationPropertyIRI = ioHelper.createIRI(namespace + curie);
          if (annotationPropertyIRI == null) {
            logger.error(
                String.format("Unable to create property IRI from '%s' on line %d", curie, n));
            continue;
          }
        }
        entityTypeMap.put(annotationPropertyIRI, EntityType.ANNOTATION_PROPERTY);
        propertyMap.put(curie, annotationPropertyIRI);
        allAnnotationProperties.add(annotationPropertyIRI);

      } else if (iri != null) {
        if (line.startsWith("is_a: ")) {
          // Get named parents only
          String curie = line.substring(6).split(" ")[0];
          IRI parentIRI = ioHelper.createIRI(curie);
          Set<IRI> parents = childParentMap.getOrDefault(iri, new HashSet<>());
          parents.add(parentIRI);
          childParentMap.put(iri, parents);

        } else if (line.startsWith("instance_of: ")) {
          String curie = line.substring(13).split(" ")[0];
          IRI typeIRI = ioHelper.createIRI(curie);
          Set<IRI> types = instanceTypeMap.getOrDefault(iri, new HashSet<>());
          types.add(typeIRI);
          instanceTypeMap.put(iri, types);

        } else if (propertyID != null && line.startsWith("xref: ")) {
          // Replace property IRI because OBO uses "name" as ID sometimes
          // Then the actual ID becomes "xref"
          String curie = line.substring(6);
          IRI newIRI = ioHelper.createIRI(curie);
          entityTypeMap.remove(iri);
          entityTypeMap.put(newIRI, EntityType.OBJECT_PROPERTY);
          propertyMap.put(propertyID, newIRI);
          iri = newIRI;

          // Only the first xref is used as the ID
          propertyID = null;

        } else if (propertyID != null && line.startsWith("is_metadata_tag: ")) {
          if (line.contains("true")) {
            entityTypeMap.put(iri, EntityType.ANNOTATION_PROPERTY);
          }
        }
      }
    }
  }

  /**
   * Return a ObjectSomeValuesFrom class expression, if the value is in the targets.
   *
   * @param property Object Property to use in SVF expression
   * @param value Class to use in SVF expression
   * @param n int line number of OBO file
   * @return OWLClassExpression {property} some {value}
   */
  private OWLClassExpression getSomeValuesFrom(String property, String value, int n) {
    IRI propertyIRI;
    if (propertyMap.containsKey(property)) {
      propertyIRI = propertyMap.get(property);
    } else {
      propertyIRI = ioHelper.createIRI(property);
      if (propertyIRI == null) {
        propertyIRI = ioHelper.createIRI(namespace + property);
      }
    }
    if (propertyIRI == null) {
      logger.error(
          String.format("Unable to create property IRI from '%s' on line %d", property, n));
      return null;
    }
    IRI valueIRI = ioHelper.createIRI(value);
    if (valueIRI == null) {
      logger.error(String.format("Unable to create class IRI from '%s' on line %d", value, n));
      return null;
    }
    if (allTargets.contains(valueIRI)) {
      return dataFactory.getOWLObjectSomeValuesFrom(
          dataFactory.getOWLObjectProperty(propertyIRI), dataFactory.getOWLClass(valueIRI));
    }
    return null;
  }

  /**
   * Create a new ontology by extracting target IRIs. This ontology only includes the declarations
   * and the hierarchy.
   *
   * @param upperTargets upper IRIs to include in output ontology
   * @param lowerTargets lower IRIs to include in output ontology
   * @param intermediates 'none', 'all', or 'minimal'
   * @throws OWLOntologyCreationException on issue creating empty ontology
   */
  private void initOntology(Set<IRI> upperTargets, Set<IRI> lowerTargets, String intermediates)
      throws OWLOntologyCreationException {
    Set<IRI> doesNotExist = new HashSet<>();
    allTargets.addAll(lowerTargets);
    allTargets.addAll(upperTargets);
    for (IRI iri : allTargets) {
      EntityType<?> et = getEntityType(iri);
      if (et == null) {
        // Entity does not exist in the target ontology
        logger.warn(String.format("<%s> does not exist in input ontology", iri.toString()));
        doesNotExist.add(iri);
        continue;
      }
      // Add declaration
      OWLEntity e = dataFactory.getOWLEntity(et, iri);
      OWLAxiom ax = dataFactory.getOWLDeclarationAxiom(e);
      manager.addAxiom(outputOntology, ax);
    }

    // Remove any that do not exist from the target sets
    lowerTargets.removeAll(doesNotExist);
    upperTargets.removeAll(doesNotExist);
    allTargets.removeAll(doesNotExist);

    // Handle parents
    for (IRI iri : lowerTargets) {
      if (intermediates.equals("none")) {
        // Assert what relationships we can between existing terms
        addAncestorsNoIntermediates(upperTargets, iri, iri);
      } else if (intermediates.equals("all") || intermediates.equals("minimal")) {
        // Add all terms between existing terms
        addAncestorsAllIntermediates(allTargets, iri);
      } else {
        throw new IllegalArgumentException("Unknown intermediates option: " + intermediates);
      }
    }

    if (intermediates.equals("minimal")) {
      // Minimal intermediates, collapse ontology structure
      OntologyHelper.collapseOntology(outputOntology, allTargets);
    }

    // Add all entities referenced in ontology to targets for annotations
    OntologyHelper.getEntities(outputOntology).forEach(e -> allTargets.add(e.getIRI()));
  }
}
