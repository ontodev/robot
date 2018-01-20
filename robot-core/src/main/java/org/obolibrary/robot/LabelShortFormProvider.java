package org.obolibrary.robot;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ShortFormProvider;

import com.google.common.base.Optional;

public class LabelShortFormProvider implements ShortFormProvider  {

    OWLOntology ontology;
    boolean hideIds = false;
    boolean quoteLabels = true;
    IOHelper iohelper;


    public LabelShortFormProvider(OWLOntology ont) {
      super();
      this.ontology = ont;
    }
    
   
    public LabelShortFormProvider(OWLOntology ontology, IOHelper iohelper) {
      super();
      this.ontology = ontology;
      this.iohelper = iohelper;
    }


    public String getShortForm(OWLEntity entity) {
      String label = getLabel(entity);
      if (label == null) {
        return getTruncatedId(entity);
      }
      else {
        if (hideIds) {
          return label;
        }
        return getTruncatedId(entity) + " "+ label;
      }
    }

    public String getLabel(OWLEntity entity) {

      for (OWLAnnotationAssertionAxiom a : ontology.getAnnotationAssertionAxioms(entity.getIRI())) {
        if (a.getProperty().isLabel()) {
          Optional<OWLLiteral> v = a.getValue().asLiteral();
         
          if (v.isPresent()) {
            
            return v.get().getLiteral();
          }
        }
      }
      return null;
    }

    public String getTruncatedId(OWLEntity entity) {
      // TODO : use iohelper to reduce to CURIE
      return entity.getIRI().getFragment();
    }


    @Override
    public void dispose() {
      // TODO Auto-generated method stub
      
    }
}
