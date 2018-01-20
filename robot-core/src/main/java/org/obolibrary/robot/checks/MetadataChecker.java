package org.obolibrary.robot.checks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.OntologyHelper;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Checks metadata for ontology headers and all classes.
 * 
 * This is typically called from within a ReportOperation
 *
 * <p>See: https://github.com/ontodev/robot/issues/205
 *
 * @author cjm
 */
public class MetadataChecker extends AbstractChecker implements Checker {

  /**
   * Profile/Style for an ontology
   *
   * <p>This defines specific conformance checks
   */
  public enum Profile {
    /** Generic lax style for a non-foundry ontology */
    LAX,
    
    /** Profile used by the GO and GO-lineage ontologies */
    GO_STRICT,

    /** Profile used by OBI and OBI-lineage ontologies */
    OBI_STRICT
  }

  private Set<OntologyMetadataViolation> ontologyMetadataViolations = new HashSet<>();
  private Set<ClassMetadataViolation> classMetadataViolations = new HashSet<>();
  private Profile profile = Profile.LAX;

  /** @param ontology */
  public MetadataChecker(OWLOntology ontology) {
    super(ontology, null);
    setupIOHelper();
  }
  
  private void setupIOHelper() {
    iohelper = new IOHelper();
    iohelper.addPrefix("dc", "http://purl.org/dc/elements/1.1/");
    iohelper.addPrefix("definition", "http://purl.obolibrary.org/obo/IAO_0000115");
    iohelper.addPrefix("definition_editor", "http://purl.obolibrary.org/obo/IAO_0000117");
    iohelper.addPrefix("obo", "http://purl.obolibrary.org/obo/");
    iohelper.addPrefix("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#");
  }

  /**
   * Convenience static method for checking ontology header
   *
   * @param ontology
   * @return violations in ontology header
   */
  public static Set<OntologyMetadataViolation> getOntologyMetadataViolations(OWLOntology ontology) {
    MetadataChecker checker = new MetadataChecker(ontology);
    checker.runOntologyMetadataChecks();
    return checker.ontologyMetadataViolations;
  }

  /**
   * Convenience static method for checking metadata on classes
   *
   * @param ontology
   * @return violations in class metadata for all classes in ontology
   */
  public static Set<ClassMetadataViolation> getClassMetadataViolations(OWLOntology ontology) {
    MetadataChecker checker = new MetadataChecker(ontology);
    checker.runClassMetadataChecks();
    return checker.classMetadataViolations;
  }

  /** run all ontology header checks */
  public void runOntologyMetadataChecks() {
    Set<OWLAnnotation> anns = ontology.getAnnotations();
    Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap =
        CheckAnnotationsHelper.collectAnnotations(anns);

    checkHeaderCardinality("dc:description", 1, 1, amap);
    checkHeaderCardinality("dc:title", 1, 1, amap);
    checkHeaderCardinality("dc:license", 1, 1, amap, 5);
    checkHeaderCardinality("dc:creator", 1, null, amap);
  }

  /** run all class metadata checks */
  public void runClassMetadataChecks() {

    for (OWLClass c : ontology.getClassesInSignature(Imports.EXCLUDED)) {
      
      if (InvalidReferenceChecker.isDangling(ontology, c)) {
        // when referencing classes in the import chain,
        // a stub class declaration may be entered in the main
        // ontology; do not check this
        continue;
      }

      if (InvalidReferenceChecker.isMergedIRI(ontology, c.getIRI())) {
        // when referencing classes in the import chain,
        // a stub class declaration may be entered in the main
        // ontology; do not check this
        continue;
      }
     
      // set up annotation map
      Set<OWLAnnotationAssertionAxiom> aas = ontology.getAnnotationAssertionAxioms(c.getIRI());
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap =
          CheckAnnotationsHelper.collectAnnotationsFromAxioms(aas);

      // all classes (live or deprecated) MUST have 1 label
      checkClassCardinality("rdfs:label", 1, 1, amap, c);
      
      if (InvalidReferenceChecker.isDeprecated(ontology, c)) {
        // we do not expect a deprecated class to conform to
        // the additional checks below
        continue;
      }
     
 
      if (profile.equals(Profile.LAX)) {
        checkClassCardinality("definition:", 0, 1, amap, c);
      }
      else {
        checkClassCardinality("definition:", 1, 1, amap, c);
        
        // check definition has provenance
        OWLAnnotationProperty defp = getProperty("definition:");
        Set<OWLAnnotation> defAnns = new HashSet<>();

        // GO-style: axiom annotation
        for (OWLAnnotationAssertionAxiom aax : ontology.getAnnotationAssertionAxioms(c.getIRI())) {
          if (aax.getProperty().equals(defp)) {
            defAnns.addAll(aax.getAnnotations());
          }
        }
        if (defAnns.size() == 0) {
          // OBI-style
          checkClassCardinality("definition_editor:", 1, null, amap, c);
        }

        String defn = getStringPropertyValue("definition:", amap);
        if (defn != null) {
          // TODO - style checks on text definition
        }
      }

      // Checks for 'GO lineage' ontologies

      int minNS = 0;
      if (profile.equals(Profile.GO_STRICT)) {
        minNS = 1;
      }
      checkClassCardinality("oboInOwl:hasOBONamespace:", minNS, 1, amap, c);
      checkClassCardinality("oboInOwl:created_by", 0, 1, amap, c);
      checkClassCardinality("oboInOwl:creation_date", 0, 1, amap, c);
    }
  }

  /**
   * @param pname
   * @return property with the same CURIE
   */
  private OWLAnnotationProperty getProperty(String pname) {
    IRI iri = iohelper.createIRI(pname);
    return ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(iri);
  }

  private String getStringPropertyValue(
      String pname, Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap) {
    OWLAnnotationProperty p = getProperty(pname);
    if (amap.containsKey(p)) {
      if (amap.get(p).size() == 1) {
        OWLAnnotationValue v = amap.get(p).iterator().next();
        if (v instanceof OWLLiteral) {
          return v.asLiteral().toString();
        }
      }
    }
    return null;
  }

  private void checkHeaderCardinality(
      String p,
      int minCardinality,
      Integer maxCardinality,
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap) {
    checkHeaderCardinality(p, minCardinality, maxCardinality, amap, 1);
  }

  private void checkHeaderCardinality(
      String p,
      int minCardinality,
      Integer maxCardinality,
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap,
      Integer severity) {
    InvalidCardinality inv =
        CheckAnnotationsHelper.checkCardinality(
            iohelper, ontology, p, minCardinality, maxCardinality, amap);
    if (inv != null) {
      ontologyMetadataViolations.add(
          new OntologyMetadataViolation(ontology, "cardinality of " + p, severity));
    }
  }

  private void checkClassCardinality(
      String p,
      int minCardinality,
      Integer maxCardinality,
      Map<OWLAnnotationProperty, Set<OWLAnnotationValue>> amap,
      OWLClass c) {
    InvalidCardinality inv =
        CheckAnnotationsHelper.checkCardinality(
            iohelper, ontology, p, minCardinality, maxCardinality, amap);
    if (inv != null) {
      String label = getRenderer().render(c);
      classMetadataViolations.add(new ClassMetadataViolation(c, label, p, inv.toString(), 1));
    }
  }

 

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }
}
