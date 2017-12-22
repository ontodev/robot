package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class RemoveOperationTest extends CoreTest {
	
	private static final String INPUT = "/remove.owl";
	private static final String CLASS = "class";
	private static final String INDIV = "individual";
	private static final String OBJ = "object-property";
	private static final String ANN = "annotation-property";
	private static final String DATA = "datatype-property";

	private static Map<String, String> nullEntities;
	static {
		nullEntities = new HashMap<>();
		nullEntities.put(CLASS, null);
		nullEntities.put(INDIV, null);
		nullEntities.put(OBJ, null);
		nullEntities.put(ANN, null);
		nullEntities.put(DATA, null);
	}
	
	/**
	 * Test removal of OWLClass (articular system) from remove.owl.
	 * Result is identical to remove_class.owl.
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException 
	 */
	@Test
	public void testRemoveClass() throws IOException,
			OWLOntologyCreationException {
		OWLOntology ontology = loadOntology(INPUT);
		
		Map<String, String> entities = new HashMap<>();
		entities.putAll(nullEntities);
		entities.put(CLASS, "UBERON:0004770");
		
		RemoveOperation.remove(ontology, entities);
		assertIdentical("/remove_class.owl", ontology);
	}
	
	/**
	 * Test removal of OWLNamedIndividual (skeleton) from remove.owl.
	 * Result is identical to remove_individual.owl.
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException 
	 */
	@Test
	public void testRemoveIndividual() throws IOException,
			OWLOntologyCreationException {
		OWLOntology ontology = loadOntology(INPUT);
		
		Map<String, String> entities = new HashMap<>();
		entities.putAll(nullEntities);
		entities.put(INDIV, "UBERON:9999999");
		
		RemoveOperation.remove(ontology, entities);
		assertIdentical("/remove_individual.owl", ontology);
	}
	
	/**
	 * Test removal of OWLAnnotationProperty (label) from remove.owl.
	 * Result is identical to remove_annotation.owl.
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException 
	 */
	@Test
	public void testRemoveAnnProp() throws IOException,
			OWLOntologyCreationException {
		OWLOntology ontology = loadOntology(INPUT);
		
		Map<String, String> entities = new HashMap<>();
		entities.putAll(nullEntities);
		entities.put(ANN, "rdfs:label");
		
		RemoveOperation.remove(ontology, entities);
		assertIdentical("/remove_annotation.owl", ontology);
	}
	
	/**
	 * Test removal of OWLObjectProperty (has_part) from remove.owl.
	 * Result is identical to remove_obj_prop.owl.
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException 
	 */
	@Test
	public void testRemoveObjProp() throws IOException,
			OWLOntologyCreationException {
		OWLOntology ontology = loadOntology(INPUT);
		
		Map<String, String> entities = new HashMap<>();
		entities.putAll(nullEntities);
		entities.put(OBJ, "BFO:0000051");
		
		RemoveOperation.remove(ontology, entities);
		assertIdentical("/remove_obj_prop.owl", ontology);
	}
	
	/**
	 * Test removal of OWLDataProperty (height) from remove.owl.
	 * Result is identical to remove_data_prop.owl.
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException 
	 */
	@Test
	public void testRemoveDataProp() throws IOException,
			OWLOntologyCreationException {
		OWLOntology ontology = loadOntology(INPUT);
		
		Map<String, String> entities = new HashMap<>();
		entities.putAll(nullEntities);
		entities.put(DATA, "UBERON:8888888");
		
		RemoveOperation.remove(ontology, entities);
		assertIdentical("/remove_data_prop.owl", ontology);
	}

}
