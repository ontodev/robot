package org.obolibrary.robot.checks;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * Represents a reference violation
 * 
 * TODO: we may want to make warnings if a class is slated for deprecation in the future:
 * see https://github.com/information-artifact-ontology/ontology-metadata/issues/22
 * 
 * @author cjm
 *
 */
public class InvalidReferenceViolation {
    
    final private OWLAxiom axiom;
    final private OWLEntity referencedObject;
    final private Category category;
    
    
    
    private InvalidReferenceViolation(OWLAxiom axiom,
            OWLEntity referencedObject, Category category) {
        super();
        this.axiom = axiom;
        this.referencedObject = referencedObject;
        this.category = category;
    }
    
    /**
     * @param axiom
     * @param referencedObject
     * @param category
     * @return new instance
     */
    public static InvalidReferenceViolation create(OWLAxiom axiom,
            OWLEntity referencedObject, Category category) {
        return new InvalidReferenceViolation(axiom, referencedObject, category);
    }



    /**
     * @return the axiom that contains the invalid reference
     */
    public OWLAxiom getAxiom() {
        return axiom;
    }



    /**
     * @return the referencedObject that is either deprecated or dangling
     */
    public OWLEntity getReferencedObject() {
        return referencedObject;
    }



    /**
     * @return the category of violation (deprecated or dangling)
     */
    public Category getCategory() {
        return category;
    }



    /**
     * A reference may be invalid for different reasons
     * 
     *  - the referenced object may be deprecated
     *  - the referenced object may be dangling
     * 
     * @author cjm
     *
     */
    public enum Category {
        DANGLING, DEPRECATED
    }



    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // TODO: pretty-printer
        return "InvalidReferenceViolation [axiom=" + axiom
                + ", referencedObject=" + referencedObject + ", category="
                + category + "]";
    }
    
    

}
