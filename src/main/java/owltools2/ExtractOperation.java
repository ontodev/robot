package owltools2;

import java.util.Set;
import java.util.HashSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * Implements the extract operation.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ExtractOperation {

  /**
   * Extract a set of terms from an ontology.
   * TODO: implement other methods for cleaning results.
   *
   * @return a new ontology (with a new manager)
   */
  public static OWLOntology extract(OWLOntology fromOntology,
      Set<IRI> terms, IRI outputIRI)
      throws OWLOntologyCreationException {
    System.out.println("Extracting");
    Set<OWLEntity> entities = new HashSet<OWLEntity>();
    for(IRI term: terms) {
      entities.addAll(fromOntology.getEntitiesInSignature(term, true));
    }
    SyntacticLocalityModuleExtractor extractor =
      new SyntacticLocalityModuleExtractor(
        fromOntology.getOWLOntologyManager(), fromOntology, ModuleType.STAR);
    return OWLManager.createOWLOntologyManager().createOntology(
        extractor.extract(entities),
        outputIRI);
  }
}
