package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Sets;

public class RelatedEntitiesHelperTest extends CoreTest {
	
	private static final String INPUT = "/related-entities.owl";
	
	@Test
	public void testGetAncestors() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		Set<IRI> IRIs = Sets.newHashSet(
				IRI.create("http://purl.obolibrary.org/obo/IAO_0000030"),
				IRI.create("http://purl.obolibrary.org/obo/RO_0000091"));
		Set<String> ancestors = new HashSet<>();
		for (OWLObject a :
			RelatedEntitiesHelper.getRelatedEntities(ont, IRIs, "ancestors")) {
			ancestors.add(a.toString());
		}
		String someVals =
				"ObjectSomeValuesFrom("
				+ "<http://purl.obolibrary.org/obo/IAO_0000136> "
				+ "<http://purl.obolibrary.org/obo/BFO_0000001>)";
		Set<String> expected = Sets.newHashSet(someVals,
				"<http://purl.obolibrary.org/obo/BFO_0000002>",
				"<http://purl.obolibrary.org/obo/BFO_0000001>",
				"<http://purl.obolibrary.org/obo/RO_0000053>",
				"<http://purl.obolibrary.org/obo/BFO_0000031>",
				"owl:Thing");
		assertEquals("Check Ancestors", expected, ancestors);
	}
	
	@Test
	public void testGetDescendants() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		Set<IRI> IRIs = Sets.newHashSet(
				IRI.create("http://purl.obolibrary.org/obo/BFO_0000008"),
				IRI.create("http://purl.obolibrary.org/obo/RO_0000057"));
		Set<String> descendants = new HashSet<>();
		for (OWLObject d :
			RelatedEntitiesHelper.getRelatedEntities(ont, IRIs, "descendants")) {
			descendants.add(d.toString());
		}
		Set<String> expected = Sets.newHashSet(
				"<http://purl.obolibrary.org/obo/BFO_0000038>",
				"<http://purl.obolibrary.org/obo/BFO_0000148>",
				"<http://purl.obolibrary.org/obo/OBI_0000293>",
				"<http://purl.obolibrary.org/obo/OBI_0000299>");
		assertEquals("Check Descendants", expected, descendants);
	}
	
	@Test
	public void testGetDisjoints() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000002");
		Set<String> disjoints = new HashSet<>();
		for (OWLObject d :
			RelatedEntitiesHelper.getRelatedEntities(ont, iri, "disjoints")) {
			disjoints.add(d.toString());
		}
		Set<String> expected = Sets.newHashSet(
				"<http://purl.obolibrary.org/obo/BFO_0000003>");
		assertEquals("Check Disjoints", expected, disjoints);
	}
	
	@Test
	public void testGetDomains() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		Set<IRI> IRIs = Sets.newHashSet(
				IRI.create("http://purl.obolibrary.org/obo/IAO_0000004"),
				IRI.create("http://purl.obolibrary.org/obo/RO_0000059"));
		Set<String> domains = new HashSet<>();
		for (OWLObject d :
			RelatedEntitiesHelper.getRelatedEntities(ont, IRIs, "domains")) {
			domains.add(d.toString());
		}
		Set<String> expected = Sets.newHashSet(
				"<http://purl.obolibrary.org/obo/IAO_0000032>",
				"<http://purl.obolibrary.org/obo/BFO_0000020>");
		assertEquals("Check Domains", expected, domains);
	}
	
	@Test
	public void testGetEquivalents() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		Set<IRI> IRIs = Sets.newHashSet(
				IRI.create("http://purl.obolibrary.org/obo/IAO_0000409"),
				IRI.create("http://purl.obolibrary.org/obo/RO_0000057"));
		Set<String> equivalents = new HashSet<>();
		for (OWLObject e :
			RelatedEntitiesHelper.getRelatedEntities(ont, IRIs, "equivalents")) {
			equivalents.add(e.toString());
		}
		String oneOf = 
				"ObjectOneOf(<http://purl.obolibrary.org/obo/IAO_0000410> "
				+ "<http://purl.obolibrary.org/obo/IAO_0000420> "
				+ "<http://purl.obolibrary.org/obo/IAO_0000421>)";
		String eqProp =
				"EquivalentObjectProperties("
				+ "<http://purl.obolibrary.org/obo/RO_0000057> "
				+ "InverseOf(<http://purl.obolibrary.org/obo/RO_0000056>) )";
		Set<String> expected = Sets.newHashSet(oneOf, eqProp);
		assertEquals("Check Equivalents", expected, equivalents);
	}
	
	@Test
	public void testGetInverses() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000067");
		Set<String> inverses = new HashSet<>();
		for (OWLObject i :
			RelatedEntitiesHelper.getRelatedEntities(ont, iri, "inverses")) {
			inverses.add(i.toString());
		}
		Set<String> expected = Sets.newHashSet(
				"<http://purl.obolibrary.org/obo/BFO_0000066>");
		assertEquals("Check Inverses", expected, inverses);
	}
	
	@Test
	public void testGetRanges() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		Set<IRI> IRIs = Sets.newHashSet(
				IRI.create("http://purl.obolibrary.org/obo/IAO_0000404"),
				IRI.create("http://purl.obolibrary.org/obo/RO_0000057"));
		Set<String> ranges = new HashSet<>();
		for (OWLObject r :
			RelatedEntitiesHelper.getRelatedEntities(ont, IRIs, "ranges")) {
			ranges.add(r.toString());
		}
		
		String dataRange = "DataPropertyRange("
				+ "<http://purl.obolibrary.org/obo/IAO_0000404> xsd:float)";
		Set<String> expected = Sets.newHashSet(dataRange,
				"<http://purl.obolibrary.org/obo/BFO_0000002>");
		assertEquals("Check Ranges", expected, ranges);
	}
	
	@Test
	public void testGetTypes() throws IOException {
		OWLOntology ont = loadOntology(INPUT);
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/IAO_0000229");
		Set<String> types = new HashSet<>();
		for (OWLObject t :
			RelatedEntitiesHelper.getRelatedEntities(ont, iri, "types")) {
			types.add(t.toString());
		}
		Set<String> expected = Sets.newHashSet(
				"<http://purl.obolibrary.org/obo/IAO_0000225>");
		assertEquals("Check Types", expected, types);
	}
}
