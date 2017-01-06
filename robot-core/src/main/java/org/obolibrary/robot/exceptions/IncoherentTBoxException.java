package org.obolibrary.robot.exceptions;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Ontology contains unsatisfiable classes
 * 
 * @author cjm
 *
 */
public class IncoherentTBoxException extends OntologyLogicException {

    public IncoherentTBoxException(Set<OWLClass> unsatisfiableClasses) {
        // TODO Auto-generated constructor stub
    }

}
