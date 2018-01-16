package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLClass;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * A {@link MetadataViolation} where annotations at the level of a class are
 * problematic
 * 
 * @author cjm
 *
 */public class ClassMetadataViolation extends MetadataViolation implements CheckViolation {

  @JsonSerialize(using = ToStringSerializer.class)
  OWLClass cls;

  public ClassMetadataViolation(OWLClass cls, String name, int severity) {
    super(name, severity);
    this.cls = cls;
  }
  
  @Override
  public String getType() {
    return "class metadata violation";
  }
  
  /**
   * @return OWLClass that has the problematic annotation / lack of annotations
   */
  public OWLClass getCls() {
    return cls;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ClassMetadataViolation [cls="
        + cls
        + ", getSeverity()="
        + getSeverity()
        + ", getName()="
        + getDescription()
        + "]";
  }
}
