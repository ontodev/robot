package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * A {@link MetadataViolation} where annotations at the level of a class are
 * problematic
 * 
 * @author cjm
 *
 */
public class ClassMetadataViolation extends MetadataViolation implements CheckViolation {

  @JsonSerialize(using = ToStringSerializer.class)
  final OWLClass cls;
  final String label;
  
  String property;

  public ClassMetadataViolation(OWLClass cls, String label, String p, String desc, int severity) {
    super(desc, severity);
    this.cls = cls;
    this.label = label;
    this.property = p;
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
  
  

  /**
   * @return the property
   */
  public String getProperty() {
    return property;
  }

  /**
   * @param property the property to set
   */
  public void setProperty(String property) {
    this.property = property;
  }

  /**
   * @return the label
   */
  public String getLabel() {
  return label;}

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
