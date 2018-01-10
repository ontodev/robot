package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

public class ImportOperation {
	
	/**
	 * 
	 * @param  ontology
	 * @param  source
	 * @param  rows
	 * @throws Exception
	 */
	public static void importTerms(OWLOntology ontology, OWLOntology source,
			List<String> rows) throws Exception {
		for (String row : rows) {
			importTerm(ontology, source, row.split(","));
		}
	}

    /**
     * 
     * @param  ontology
     * @param  source
     * @param  row
     * @throws Exception
     */
	public static void importTerm(OWLOntology ontology, OWLOntology source,
			String[] row) throws Exception {
		IOHelper ioHelper = new IOHelper();
		IRI sourceIRI = source.getOntologyID().getOntologyIRI().orNull();

		// ID,method,relation options (optional)
		IRI iri = ioHelper.createIRI(row[0]);
		String method = row[1];
		Set<IRI> imports = new HashSet<>();
		imports.add(iri);
		if (row.length == 3) {
			List<String> relationOptions = Arrays.asList(row[2].split(" "));
			// Get related entities to import
			imports.addAll(getImportIRIs(source, iri, relationOptions));
		}
		
		System.out.println(imports);
		
		ModuleType module = null;
		if ("mireot".equals(method.toLowerCase())) {
			// TODO: determine top and bottom terms
		} else {
			if ("star".equals(method.toLowerCase())) {
	            module = ModuleType.STAR;
	        } else if ("top".equals(method.toLowerCase())) {
	        	module = ModuleType.TOP;
	        } else if ("bot".equals(method.toLowerCase())) {
	        	module = ModuleType.BOT;
	        } else {
	        	throw new Exception("Method for " + iri 
	        			+ " must be MIREOT, STAR, TOP, or BOT.");
	        }
			// Extract and then merge
			MergeOperation.mergeInto(
					ExtractOperation.extract(source, imports, sourceIRI, module),
					ontology);
		}
	}
	
	/**
	 * 
	 * @param  ontology
	 * @param  iri
	 * @param  relationOptions
	 * @return
	 */
	private static Set<IRI> getImportIRIs(OWLOntology ontology, IRI iri,
			List<String> relationOptions) {
		Set<OWLObject> relatedEntities = 
				RelatedEntitiesHelper.getRelatedEntities(ontology, iri,
						relationOptions);
		Set<IRI> imports = new HashSet<>();
		for (OWLObject re : relatedEntities) {
			Set<OWLEntity> entities = re.getSignature();
			// One entity in the signature, it's just an OWLEntity
			if (entities.size() == 1) {
				for (OWLEntity e : entities) {
					imports.add(e.getIRI());
				}
			}
		}
		return imports;
	}
}
