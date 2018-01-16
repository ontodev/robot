package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLOntology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * A {@link MetadataViolation} where annotations at the level of the ontology are
 * problematic
 * 
 * @author cjm
 *
 */
public class OntologyMetadataViolation extends MetadataViolation implements CheckViolation {

  @JsonIgnore
  private final OWLOntology ontology;

  public OntologyMetadataViolation(OWLOntology ontology, String name, int severity) {
    super(name, severity);
    this.ontology = ontology;
  }

  /**
   * @return the ontology that has the problem
   */
  public OWLOntology getOntology() {
    return ontology;
  }
  
  @Override
  public String getType() {
    return "ontology metadata violation";
  }
  
  
}
