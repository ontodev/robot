package org.obolibrary.robot.checks;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.OntologyHelper;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLOntology;

public abstract class AbstractChecker implements Checker {
  
  OWLOntology ontology;
  IOHelper iohelper;
  private OWLObjectRenderer renderer;
  
  
  public AbstractChecker(OWLOntology ontology, IOHelper iohelper) {
    super();
    this.ontology = ontology;
    this.iohelper = iohelper;
    this.renderer = OntologyHelper.getRenderer(ontology);
  }


  /**
   * @return the renderer
   */
  public OWLObjectRenderer getRenderer() {
  return renderer;}
  
  
}
