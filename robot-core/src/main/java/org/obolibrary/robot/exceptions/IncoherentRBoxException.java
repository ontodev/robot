package org.obolibrary.robot.exceptions;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * Ontology contains unsatisfiable properties
 * 
 * @author cjm
 *
 */
public class IncoherentRBoxException extends OntologyLogicException {

    public IncoherentRBoxException(Set<OWLObjectProperty> unsatisfiableProperties) {
        // TODO Auto-generated constructor stub
    }

}
