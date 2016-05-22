package org.obolibrary.robot;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

public class ExtractOperationTest extends CoreTest {

	@Test
	public void testExtractStar()
			throws IOException, OWLOntologyCreationException {
		
		testExtract(ModuleType.STAR, "/star.owl");
	}
	@Test
	public void testExtractBot()
			throws IOException, OWLOntologyCreationException {
		
		testExtract(ModuleType.BOT, "/bot.owl");
	}
	@Test
	public void testExtractTop()
			throws IOException, OWLOntologyCreationException {
		
		testExtract(ModuleType.TOP, "/top.owl");
	}

	public void testExtract(ModuleType moduleType, String expectedPath)
			throws IOException, OWLOntologyCreationException {
		OWLOntology simple = loadOntology("/filtered.owl");

		IRI outputIRI = IRI.create("http://purl.obolibrary.org/obo/uberon.owl");
		
		Set<IRI> terms = 
				Collections.singleton(
						IRI.create("http://purl.obolibrary.org/obo/UBERON_0001235"));
		OWLOntology module = 
				ExtractOperation.extract(simple, terms, outputIRI, moduleType);
		
		for (OWLAxiom ax : module.getAxioms()) {
			System.out.println(ax);
		}
		
		OWLOntology expected = loadOntology(expectedPath);
		removeDeclarations(expected);
		removeDeclarations(module);
		assertIdentical(expected, module);
	}
	
	
}
