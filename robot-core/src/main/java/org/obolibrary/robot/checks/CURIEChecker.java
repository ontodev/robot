package org.obolibrary.robot.checks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.OntologyHelper;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Check all CURIEs/IDs
 * 
 * Note that in theory CURIEs are syntactic-level entities that should be expanded
 * to URIs in parsing by the OWLAPI. In practice many ontologies denote external
 * entities using a CURIE in a string literal.
 * 
 * The prefix of a CURIE typically comes from a standard registry. For example
 * 
 * - https://github.com/geneontology/go-site/blob/master/metadata/db-xrefs.yaml
 * - https://github.com/prefixcommons/biocontext
 * 
 * The checks here are intended for
 * 
 *  - checking syntax of CURIE - e.g. no unintential spaces
 *  - checking prefix is within allowed subset and can be expanded
 * 
 * TODO: many of these operations can be repaired. Implement corresponding RepairOperation
 * 
 * @author cjm
 *
 */
public class CURIEChecker extends AbstractChecker implements Checker {
  
  public CURIEChecker(OWLOntology ontology, IOHelper iohelper) {
    super(ontology, iohelper);
  }
  
  public static Set<CURIEViolation> getInvalidCURIEs(OWLOntology ontology, IOHelper iohelper) {
    CURIEChecker checker = new CURIEChecker(ontology, iohelper);
    return checker.getInvalidCURIEs();
  }
  public Set<CURIEViolation> getInvalidCURIEs() {

    Set<CURIEViolation> violations = new HashSet<>();
    for (OWLAnnotationAssertionAxiom axiom : ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
      OWLAnnotationSubject s = axiom.getSubject();
      
      OWLAnnotationProperty p = axiom.getProperty();
      if (isCURIEValuedProperty(p)) {
        OWLAnnotationValue v = axiom.getValue();
        violations.addAll(checkCURIE(v, axiom));  
      }
    }
    for (OWLAxiom axiom : ontology.getAxioms(Imports.EXCLUDED)) {
      for (OWLAnnotation a : axiom.getAnnotations()) {
        OWLAnnotationProperty p = a.getProperty();
        if (isCURIEValuedProperty(p)) {
          OWLAnnotationValue v = a.getValue();
          violations.addAll(checkCURIE(v, axiom));
        }
      }
    }
    return violations;
    
  }
  
  public static boolean isCURIEValuedProperty(OWLAnnotationProperty p) {
    IRI pIRI = p.getIRI();
    return (pIRI.toString().equals("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));
  }
  
  public Collection<CURIEViolation> checkCURIE(OWLAnnotationValue v, OWLAxiom axiom ) {
    Set<CURIEViolation> violations = new HashSet<>();
    if (v.asLiteral().isPresent()) {
      OWLLiteral vlit = v.asLiteral().get();
      String id = vlit.getLiteral();
      violations.addAll(checkCURIE(id, axiom)); 
    }
    return violations;
  }

  public Collection<CURIEViolation> checkCURIE(String id, OWLAxiom axiom) {
    Set<CURIEViolation> violations = new HashSet<>();
    
    String axiomLabel = getRenderer().render(axiom);
    
    // consider bringing in guava.
    // See https://stackoverflow.com/questions/4067809/how-can-i-find-whitespace-space-in-a-string
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(id);
    boolean found = matcher.find();
    if (found) {
      violations.add(new CURIEViolation(axiomLabel, "id/curie contains whitespace: '"+id+"'", 4));
    }
    if (id.contains(":")) {
      String[] idparts = id.split(":");
      if (idparts.length == 2) {
        String prefix = idparts[0];
        String frag = idparts[1];
        // TODO - check prefix against registry
        if (prefix.equals("")) {
          violations.add(new CURIEViolation(axiomLabel, "blank prefix in '"+id+"'", 4));                             
        }
        
        // hardcoded checks for particular prefix types
        // this could be made more generic and driven by metadata,
        // for now encode common classes of error here
        if (prefix.equals("PMID")) {
          if (frag.replaceAll("[0-9]+", "").length() > 0) {
            violations.add(new CURIEViolation(axiomLabel, "local id must be numeric '"+id+"'", 4));                                        
          }
        }
      }
      else {
        violations.add(new CURIEViolation(axiomLabel, "id/curie should contain exactly one ':' separator - '"+id+"'", 3));                   
      }
    }
    else {
      violations.add(new CURIEViolation(axiomLabel, "id/curie does not contain ':' separator - '"+id+"'", 3));            
    }
    return violations;
  }

  @Override
  public String getName() {
    return "CURIE checker";
  }
  
}
