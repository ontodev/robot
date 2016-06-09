package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

/**
 * Reason over an ontology and add axioms.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class ReasonOperation {
	/**
	 * Logger.
	 */
	private static final Logger logger =
			LoggerFactory.getLogger(ReasonOperation.class);

	/**
	 * Given a map of options and a key name,
	 * return the value, or null if it is not specified.
	 *
	 * @param options a map of options
	 * @param key the name of the option to get
	 * @return the value, if set, otherwise null
	 */
	private static String getOption(Map<String, String> options,
			String key) {
		return getOption(options, key, null);
	}

	/**
	 * Given a map of options, a key name, and a default value,
	 * if the map contains the key, return its value,
	 * otherwise return the default value.
	 *
	 * @param options a map of options
	 * @param key the name of the option to get
	 * @param defaultValue the value to return if the key is not set
	 * @return the value, if set, otherwise the default value
	 */
	private static String getOption(Map<String, String> options,
			String key, String defaultValue) {
		if (options == null) {
			return defaultValue;
		}
		if (!options.containsKey(key)) {
			return defaultValue;
		}
		return options.get(key);
	}

	/**
	 * Given a map of options and a key name,
	 * return true if the value is "true" or "yes",
	 * otherwise return false.
	 *
	 * @param options a map of options
	 * @param key the name of the option to get
	 * @return true if the value is "true" or "yes", false otherwise
	 */
	private static boolean optionIsTrue(Map<String, String> options,
			String key) {
		String value = getOption(options, key);
		if (value == null) {
			return false;
		}

		value = value.trim().toLowerCase();
		if (value.equals("true") || value.equals("yes")) {
			return true;
		}

		return false;
	}

	/**
	 * Return a map from option name to default option value,
	 * for all the available reasoner options.
	 *
	 * @return a map with default values for all available options
	 */
	public static Map<String, String> getDefaultOptions() {
		Map<String, String> options = new HashMap<String, String>();
		options.put("remove-redundant-subclass-axioms", "true");
		options.put("create-new-ontology", "false");
		options.put("annotated-inferred-axioms", "false");
		return options;
	}

	/**
	 * Given an ontology, the name of a reasoner, and an output IRI,
	 * return the ontology with inferred axioms added,
	 * using the default reasoner options.
	 *
	 * @param ontology the ontology to reason over
	 * @param reasonerFactory the factory to create a reasoner instance from
	 * @throws OWLOntologyCreationException 
	 */
	public static void reason(OWLOntology ontology,
			OWLReasonerFactory reasonerFactory) throws OWLOntologyCreationException {
		reason(ontology, reasonerFactory, getDefaultOptions());
	}

	/**
	 * Given an ontology, the name of a reasoner, an output IRI,
	 * and an optional map of reasoner options,
	 * return the ontology with inferred axioms added.
	 *
	 * @param ontology the ontology to reason over
	 * @param reasonerFactory the factory to create a reasoner instance from
	 * @param options a map of option strings, or null
	 * @throws OWLOntologyCreationException 
	 */
	public static void reason(OWLOntology ontology,
			OWLReasonerFactory reasonerFactory,
			Map<String, String> options) throws OWLOntologyCreationException {
		logger.info("Ontology has {} axioms.", ontology.getAxioms().size());
		logger.info("Starting reasoning...");

		int seconds;
		long elapsedTime;
		long startTime = System.currentTimeMillis();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();

		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		if (!reasoner.isConsistent()) {
			logger.info("Ontology is not consistent!");
			return;
		}

		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		Node<OWLClass> unsatisfiableClasses =
				reasoner.getUnsatisfiableClasses();
		if (unsatisfiableClasses.getSize() > 1) {
			logger.info("There are {} unsatisfiable classes in the ontology.",
					unsatisfiableClasses.getSize());
			for (OWLClass cls : unsatisfiableClasses) {
				if (!cls.isOWLNothing()) {
					logger.info("    unsatisfiable: " + cls.getIRI());
				}
			}
		}

		// Make sure to add the axiom generators in this way!!!
		List<InferredAxiomGenerator<? extends OWLAxiom>> gens =
				new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
		gens.add(new InferredSubClassAxiomGenerator());
		InferredOntologyGenerator generator =
				new InferredOntologyGenerator(reasoner, gens);
		logger.info("Using these axiom generators:");
		for (InferredAxiomGenerator inf: generator.getAxiomGenerators()) {
			logger.info("    " + inf);
		}

		elapsedTime = System.currentTimeMillis() - startTime;
		seconds = (int) Math.ceil(elapsedTime / 1000);
		logger.info("Reasoning took {} seconds.", seconds);

		// we first place all inferred axioms into a new ontology;
		// these will be later transferred into the main ontology,
		// unless the create new ontology option is passed
		OWLOntology newAxiomOntology;
		newAxiomOntology = manager.createOntology();

		startTime = System.currentTimeMillis();
		generator.fillOntology(dataFactory, newAxiomOntology);

		logger.info("Reasoning created {} new axioms.",
				newAxiomOntology.getAxioms().size());


		if (optionIsTrue(options, "create-new-ontology")) {
			// because the ontology is passed by reference, we manipulate it in place
			logger.info("Placing inferred axioms into a new ontology");
			// todo: set ontology id
			manager.removeAxioms(ontology, ontology.getAxioms() );
		}
		for (OWLAxiom a : newAxiomOntology.getAxioms()) {
			if (optionIsTrue(options, "annotate-inferred-axioms")) {
				logger.warn("annotate not implemented!");
			}
			manager.addAxiom(ontology, a);
		}
		
		if (optionIsTrue(options, "remove-redundant-subclass-axioms")) {
			removeRedundantSubClassAxioms(reasoner);
		}
		logger.info("Ontology has {} axioms after all reasoning steps.",
				ontology.getAxioms().size());

		elapsedTime = System.currentTimeMillis() - startTime;
		seconds = (int) Math.ceil(elapsedTime / 1000);
		logger.info("Filling took {} seconds.", seconds);
	}

	/**
	 * Remove subClassAxioms where there is a more direct axiom,
	 * and the subClassAxiom does not have any annotations.
	 *
	 * Example: genotyping assay
	 * - asserted in dev: assay
	 * - inferred by reasoner: analyte assay
	 * - asserted after fill: assay, analyte assay
	 * - asserted after removeRedundantSubClassAxioms: analyte assay
	 *
	 * @param reasoner an OWL reasoner, initialized with a root ontology;
	 *   the ontology will be modified
	 */
	public static void removeRedundantSubClassAxioms(OWLReasoner reasoner) {
		logger.info("Removing redundant subclass axioms...");
		OWLOntology ontology = reasoner.getRootOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();

		for (OWLClass thisClass: ontology.getClassesInSignature()) {
			if (thisClass.isOWLNothing() || thisClass.isOWLThing()) {
				continue;
			}

			// Use the reasoner to get all
			// the direct superclasses of this class.
			Set<OWLClass> inferredSuperClasses = new HashSet<OWLClass>();
			for (Node<OWLClass> node
					: reasoner.getSuperClasses(thisClass, true)) {
				for (OWLClass inferredSuperClass: node) {
					inferredSuperClasses.add(inferredSuperClass);
				}
			}

			// For each subClassAxiom,
			// if the subclass axiom does not have any annotations,
			// and the superclass is named (not anonymous),
			// and the superclass is not in the set of inferred super classes,
			// then remove that axiom.
			for (OWLSubClassOfAxiom subClassAxiom
					: ontology.getSubClassAxiomsForSubClass(thisClass)) {
				if (subClassAxiom.getAnnotations().size() > 0) {
					continue;
				}
				if (subClassAxiom.getSuperClass().isAnonymous()) {
					continue;
				}
				OWLClass assertedSuperClass =
						subClassAxiom.getSuperClass().asOWLClass();
				if (inferredSuperClasses.contains(assertedSuperClass)) {
					continue;
				}
				manager.removeAxiom(ontology,
						dataFactory.getOWLSubClassOfAxiom(
								thisClass, assertedSuperClass));
			}
		}
		logger.info("Ontology now has {} axioms.", ontology.getAxioms().size());
	}

}
