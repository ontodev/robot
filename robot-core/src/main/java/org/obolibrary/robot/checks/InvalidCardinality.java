package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLAnnotationProperty;

public class InvalidCardinality {
  public enum Op {
    LESS_THAN,
    MORE_THAN
  };

  private final OWLAnnotationProperty property;
  private final int num;
  private final Op op;
  private final int expected;

  public InvalidCardinality(OWLAnnotationProperty property, int num, Op op, int expected) {
    super();
    this.property = property;
    this.num = num;
    this.op = op;
    this.expected = expected;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return 
        "|"+property+"|=" + num +" " + op +" " + expected;
       
  }
  
  
}
