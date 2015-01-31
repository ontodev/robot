package owltools2;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools2.CatalogXmlIRIMapper;

/**
 * Provides convenience methods for working with ontology and term files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class IOHelper {

  // TODO: add methods for manipulating a shared PrefixManager
  
  /**
   * Try to guess the location of the catalog.xml file.
   */
  public File guessCatalogFile(File ontologyFile) {
    String path = ontologyFile.getParent();
    String catalogPath = path + "/catalog-v001.xml";
    return new File(catalogPath);
  }

  public OWLOntology loadOntology(String ontologyPath)
      throws IOException {
    File ontologyFile = new File(ontologyPath);
    File catalogFile = guessCatalogFile(ontologyFile); 
    return loadOntology(ontologyFile, catalogFile);
  }

  public OWLOntology loadOntology(String ontologyPath, boolean useCatalog)
      throws IOException {
    File ontologyFile = new File(ontologyPath);
    File catalogFile = null;
    if(useCatalog) {
      catalogFile = guessCatalogFile(ontologyFile); 
    }
    return loadOntology(ontologyFile, catalogFile);
  }

  public OWLOntology loadOntology(String ontologyPath, String catalogPath)
      throws IOException {
    File ontologyFile = new File(ontologyPath);
    File catalogFile =  new File(catalogPath);
    return loadOntology(ontologyFile, catalogFile);
  }

  public OWLOntology loadOntology(File ontologyFile)
      throws IOException {
    File catalogFile = guessCatalogFile(ontologyFile); 
    return loadOntology(ontologyFile, catalogFile);
  }

  public OWLOntology loadOntology(File ontologyFile, boolean useCatalog)
      throws IOException {
    File catalogFile = null;
    if(useCatalog) {
      catalogFile = guessCatalogFile(ontologyFile); 
    }
    return loadOntology(ontologyFile, catalogFile);
  }

  public OWLOntology loadOntology(File ontologyFile, File catalogFile)
      throws IOException {
    OWLOntology ontology = null;
    try {
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      if(catalogFile != null && catalogFile.isFile()) {
        manager.addIRIMapper(new CatalogXmlIRIMapper(catalogFile));
      }
      ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
    } catch (OWLOntologyCreationException e) {
      throw new IOException(e);
    }
    return ontology;
  }

  //public OWLOntology loadOntology(IRI ontologyIRI) {
  //  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
  //  return manager.loadOntologyFromOntologyDocument(ontologyIRI);
  //}
  //public OWLOntology loadOntology(IRI ontologyIRI, Bool use-catalog)
  //public OWLOntology loadOntology(IRI ontologyIRI, IRI catalogIRI)

  //public OWLOntology saveOntology(OWLOntology ontology, String ontologyPath)

  public void saveOntology(OWLOntology ontology, File ontologyFile)
      throws IOException {
    try {
    ontology.getOWLOntologyManager().saveOntology(
        ontology, IRI.create(ontologyFile));
    } catch (OWLOntologyStorageException e) {
      throw new IOException(e);
    }
  }
  //public OWLOntology saveOntology(OWLOntology ontology, IRI ontologyIRI)

  
  /**
   * Remove comments, trim lines, remove empty lines.
   * A comment is a space or newline followed by a '#', to the end of the line.
   * This excludes '#' characters in IRIs.
   */
  public Set<String> extractTerms(String input) {
    Set<String> results = new HashSet<String>();
    List<String> lines = Arrays.asList(
        input.replaceAll("\\r", "").split("\\n"));
    for(String line: lines) {
      String result = line.replaceFirst("($|\\s)#.*$", "").trim();
      if(!result.isEmpty()) {
        results.add(result);
      }
    }
    return results;
  }

  public Set<IRI> createIRIs(Set<String> terms) throws IllegalArgumentException {
    Set<IRI> iris = new HashSet<IRI>();
    for(String term: terms) {
      // TODO: use prefix manager to expand IRIs
      iris.add(IRI.create(term));
    }
    return iris;
  }

  /**
   * Parse a set of IRIs from a space-separated string, ignoring '#' comments.
   */
  public Set<IRI> parseTerms(String input) throws IllegalArgumentException {
    return createIRIs(extractTerms(input));
  }

  public Set<IRI> loadTerms(String path) throws IOException {
    return loadTerms(new File(path));
  }

  /**
   * Load a set of IRIs from a file (space-separated, ignoring '#' comments).
   */
  public Set<IRI> loadTerms(File file) throws IOException {
    String content = new Scanner(file).useDelimiter("\\Z").next();
    return parseTerms(content);
  }

}
