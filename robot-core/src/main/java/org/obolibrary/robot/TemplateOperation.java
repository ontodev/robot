package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.coode.owlapi.manchesterowlsyntax.
           ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Generate OWL from tables.
 * Based on "Overcoming the ontology enrichment bottleneck
 * with Quick Term Templates"
 * (<a href="http://dx.doi.org/10.3233/AO-2011-0086">link</a>).
 * See template.md for details.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class TemplateOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(TemplateOperation.class);

    /**
     * Shared DataFactory.
     */
    private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();

    /**
     * Error message when the number of header columns does not
     * match the number of template columns.
     * Expects: header count, template count.
     */
    private static String columnMismatchError =
        "The number of header columns (%1$d) must match "
      + "the number of template columns (%2$d).";

    /**
     * Error message when a required column is missing from a row.
     * Expects: row number, row id, columns number, column name.
     */
    private static String columnError =
        "Missing column %3$d (\"%4$s\") from row %1$d (\"%2$s\").";

    /**
     * Error message when a required cell is null.
     * Expects: row number, row id, columns number, column name,
     * template.
     */
    private static String nullCellError =
        "Null value at row %1$d (\"%2$s\"), column %3$d (\"%4$s\") "
      + "for template \"%5$s\".";

    /**
     * Error message when a template cannot be understood.
     * Expects: column number, column name, template.
     */
    private static String unknownTemplateError =
        "Could not interpret template string \"%3$s\" "
      + "for column %1$d (\"%2$s\").";

    /**
     * Error message when content cannot be parsed.
     * Expects: row number, row id, columns number, column name,
     * content, message.
     */
    private static String parseError =
        "Error while parsing \"%5$s\" at row %1$d (\"%2$s\"), "
      + "column %3$d (\"%4$s\"): \"%6$s\".";

    /**
     * Error message when we cannot create an IRI for a row ID.
     * Expects: row number, row id.
     */
    private static String nullIDError =
        "Could not create IRI for row %1$d with ID \"%2$s\".";

    /**
     * Error message when a row for a class does not have a type.
     * Should be "subclass" or "equivalent".
     * Expects: row number, row id.
     */
    private static String missingTypeError =
        "No class type found for row %1$d (\"%2$s\").";

    /**
     * Error message when a class type is not recognized.
     * Should be "subclass" or "equivalent".
     * Expects: row number, row id, value.
     */
    private static String unknownTypeError =
        "Unknown class type \"%3$s\" for row %1$d (\"%2$s\").";

    /**
     * Find an annotation property with the given name or create one.
     *
     * @param checker used to search by rdfs:label (for example)
     * @param name the name to search for
     * @return an annotation property
     * @throws Exception if the name cannot be resolved
     */
    public static OWLAnnotationProperty getAnnotationProperty(
            QuotedEntityChecker checker, String name) throws Exception {
        OWLAnnotationProperty property =
            checker.getOWLAnnotationProperty(name, true);
        if (property != null) {
            return property;
        }
        throw new Exception("Could not find annotation property: " + name);
    }

    /**
     * Find a datatype with the given name or create one.
     *
     * @param checker used to search by rdfs:label (for example)
     * @param name the name to search for
     * @return a datatype
     * @throws Exception if the name cannot be resolved
     */
    public static OWLDatatype getDatatype(QuotedEntityChecker checker,
            String name) throws Exception {
        OWLDatatype datatype = checker.getOWLDatatype(name, true);
        if (datatype != null) {
            return datatype;
        }
        throw new Exception("Could not find datatype: " + name);
    }

    /**
     * Return a string annotation for the given template string and value.
     * The template string format is "A [name]" and the value is any string.
     *
     * @param checker used to resolve the annotation property
     * @param template the template string
     * @param value the value for the annotation
     * @return a new annotation with property and string literal value
     * @throws Exception if the annotation property cannot be found
     */
    public static OWLAnnotation getStringAnnotation(QuotedEntityChecker checker,
            String template, String value) throws Exception {
        String name = template.substring(1).trim();
        OWLAnnotationProperty property = getAnnotationProperty(checker, name);
        return dataFactory.getOWLAnnotation(
            property, dataFactory.getOWLLiteral(value));
    }

    /**
     * Return a typed annotation for the given template string and value.
     * The template string format is "AT [name]^^[datatype]"
     * and the value is any string.
     *
     * @param checker used to resolve the annotation property and datatype
     * @param template the template string
     * @param value the value for the annotation
     * @return a new annotation with property and typed literal value
     * @throws Exception if the annotation property cannot be found
     */
    public static OWLAnnotation getTypedAnnotation(QuotedEntityChecker checker,
            String template, String value) throws Exception {
        template = template.substring(2).trim();
        String name = template.substring(0, template.indexOf("^^")).trim();
        String typeName = template.substring(template.indexOf("^^") + 2,
                template.length()).trim();
        OWLAnnotationProperty property = getAnnotationProperty(checker, name);
        OWLDatatype datatype = getDatatype(checker, typeName);
        return dataFactory.getOWLAnnotation(
            property, dataFactory.getOWLLiteral(value, datatype));
    }

    /**
     * Return a language tagged annotation for the given template and value.
     * The template string format is "AL [name]@[lang]"
     * and the value is any string.
     *
     * @param checker used to resolve the annotation property
     * @param template the template string
     * @param value the value for the annotation
     * @return a new annotation with property and language tagged literal value
     * @throws Exception if the annotation property cannot be found
     */
    public static OWLAnnotation getLanguageAnnotation(
            QuotedEntityChecker checker,
            String template, String value) throws Exception {
        template = template.substring(2).trim();
        String name = template.substring(0, template.indexOf("@")).trim();
        String lang = template.substring(template.indexOf("@") + 1,
                template.length()).trim();
        OWLAnnotationProperty property = getAnnotationProperty(checker, name);
        return dataFactory.getOWLAnnotation(
            property, dataFactory.getOWLLiteral(value, lang));
    }

    /**
     * Return an IRI annotation for the given template string and value.
     * The template string format is "AI [name]"
     * and the value is a string that can be interpreted as an IRI.
     *
     * @param checker used to resolve the annotation property
     * @param template the template string
     * @param value the IRI value for the annotation
     * @return a new annotation with property and an IRI value
     * @throws Exception if the annotation property cannot be found
     */
    public static OWLAnnotation getIRIAnnotation(QuotedEntityChecker checker,
            String template, IRI value) throws Exception {
        String name = template.substring(2).trim();
        OWLAnnotationProperty property = getAnnotationProperty(checker, name);
        return dataFactory.getOWLAnnotation(property, value);
    }

    /**
     * Return true if the tempalte string is valid, false otherwise.
     *
     * @param template the template string to check
     * @return true if valid, false otherwise
     */
    public static boolean validateTemplateString(String template) {
        template = template.trim();
        if (template.equals("ID")) {
            return true;
        }
        if (template.equals("CLASS_TYPE")) {
            return true;
        }
        if (template.matches("^(A|AT|AL|AI|C) .*")) {
            return true;
        }
        if (template.equals("CI")) {
            return true;
        }
        return false;
    }

    /**
     * Use a table to generate an ontology.
     * With this signature we use all defaults.
     *
     * @param rows the table of data
     * @return a new ontology generated from the table
     * @throws Exception when names or templates cannot be handled
     */
    public static OWLOntology template(List<List<String>> rows)
            throws Exception {
        return template(rows, null, null, null);
    }

    /**
     * Use a table to generate an ontology.
     *
     * @param rows the table of data
     * @param inputOntology the ontology to use to resolve names
     * @return a new ontology generated from the table
     * @throws Exception when names or templates cannot be handled
     */
    public static OWLOntology template(List<List<String>> rows,
            OWLOntology inputOntology) throws Exception {
        return template(rows, inputOntology, null, null);
    }

    /**
     * Use a table to generate an ontology.
     *
     * @param rows the table of data
     * @param inputOntology the ontology to use to resolve names
     * @param checker used to find entities by name
     * @return a new ontology generated from the table
     * @throws Exception when names or templates cannot be handled
     */
    public static OWLOntology template(List<List<String>> rows,
            OWLOntology inputOntology, QuotedEntityChecker checker)
            throws Exception {
        return template(rows, inputOntology, checker, null);
    }

    /**
     * Use a table to generate an ontology.
     *
     * @param rows the table of data
     * @param inputOntology the ontology to use to resolve names
     * @param ioHelper used to find entities by name
     * @return a new ontology generated from the table
     * @throws Exception when names or templates cannot be handled
     */
    public static OWLOntology template(List<List<String>> rows,
            OWLOntology inputOntology, IOHelper ioHelper)
            throws Exception {
        return template(rows, inputOntology, null, ioHelper);
    }

    /**
     * Use a table to generate an ontology.
     * The first row of the table must be header names.
     * The second row of the table must be template strings,
     * including ID and CLASS_TYPE columns.
     * The new ontology is created in two passes:
     * first terms are declared and annotations added,
     * then logical axioms are added.
     * This allows annotations from the first pass to be used as names
     * in the logical axioms.
     *
     * @param rows the table of data
     * @param inputOntology the ontology to use to resolve names
     * @param checker used to find entities by name
     * @param ioHelper used to find entities by name
     * @return a new ontology generated from the table
     * @throws Exception when names or templates cannot be handled
     */
    public static OWLOntology template(List<List<String>> rows,
            OWLOntology inputOntology, QuotedEntityChecker checker,
            IOHelper ioHelper) throws Exception {
        logger.debug("Templating...");

        List<String> headers = rows.get(0);
        List<String> templates = rows.get(1);
        if (headers.size() != templates.size()) {
            throw new Exception(
                String.format(columnMismatchError,
                    headers.size(), templates.size()));
        }

        // Check templates and find the ID column.
        int idColumn = -1;
        for (int column = 0; column < templates.size(); column++) {
            String template = templates.get(column);
            if (template == null) {
                continue;
            }
            template = template.trim();
            if (!validateTemplateString(template)) {
                throw new Exception(
                    String.format(unknownTemplateError,
                        column + 1, headers.get(column), template));
            }
            if (template.equals("ID")) {
                idColumn = column;
            }
        }
        if (idColumn == -1) {
            throw new Exception("Template row must include an \"ID\" column");
        }

        OWLOntologyManager outputManager =
            OWLManager.createOWLOntologyManager();
        OWLOntology outputOntology = outputManager.createOntology();

        if (ioHelper == null) {
            ioHelper = new IOHelper();
        }
        if (checker == null) {
            checker = new QuotedEntityChecker();
            checker.useIOHelper(ioHelper);
            checker.addProvider(new SimpleShortFormProvider());
            checker.addProperty(dataFactory.getRDFSLabel());
        }
        if (inputOntology != null) {
            checker.addAll(inputOntology);
        }

        // Process the table in two passes.
        // The first pass adds declarations and annotations to the ontology,
        // then adds the term to the EntityChecker so it can be used
        // by the parser for logical definitions.
        for (int row = 2; row < rows.size(); row++) {
            addAnnotations(outputOntology, rows, row, idColumn, checker,
                ioHelper);
        }

        // Add the entity to the QuotedEntityChecker.
        checker.addAll(outputOntology);

        // Second pass: add logic to existing entities.
        ManchesterOWLSyntaxClassExpressionParser parser =
            new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        for (int row = 2; row < rows.size(); row++) {
            addLogic(outputOntology, rows, row, idColumn, parser, ioHelper);
        }

        return outputOntology;
    }

    /**
     * Use templates to add entities and their annotations to an ontology.
     *
     * @param ontology the ontology to add axioms to
     * @param rows the table to use
     * @param row the current row to use
     * @param idColumn the column that holds the ID for the entity
     * @param checker used to find annotation properties by name
     * @param ioHelper used to generate IRIs
     * @throws Exception when names or templates cannot be handled
     */
    private static void addAnnotations(OWLOntology ontology,
            List<List<String>> rows, int row, int idColumn,
            QuotedEntityChecker checker, IOHelper ioHelper)
            throws Exception {
        List<String> headers = rows.get(0);
        List<String> templates = rows.get(1);

        String id = null;
        try {
            id = rows.get(row).get(idColumn);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            return;
        }

        List<OWLAnnotation> annotations = new ArrayList<OWLAnnotation>();

        // For each column, collect information.
        for (int column = 0; column < headers.size(); column++) {
            String template = templates.get(column);
            if (template == null) {
                return;
            }
            template = template.trim();
            if (template.isEmpty()) {
                continue;
            }

            String header = headers.get(column);
            String cell = null;
            try {
                cell = rows.get(row).get(column);
            } catch (IndexOutOfBoundsException e) {
                throw new Exception(
                    String.format(columnError, row + 1, id,
                        column + 1, header));
            }

            if (cell == null) {
                throw new Exception(
                    String.format(nullCellError, row + 1, id,
                        column + 1, header, template));
            }
            if (cell.trim().isEmpty()) {
                continue;
            }
            String content = QuotedEntityChecker.wrap(cell);

            if (template.startsWith("A ")) {
                annotations.add(getStringAnnotation(checker, template, cell));
            } else if (template.startsWith("AT ")
                    && template.indexOf("^^") > -1) {
                annotations.add(getTypedAnnotation(checker, template, cell));
            } else if (template.startsWith("AL ")
                    && template.indexOf("@") > -1) {
                annotations.add(getLanguageAnnotation(checker, template, cell));
            } else if (template.startsWith("AI ")) {
                IRI value = ioHelper.createIRI(cell);
                annotations.add(getIRIAnnotation(checker, template, value));
            }
        }

        // Now validate and build the class.
        IRI iri = ioHelper.createIRI(id);
        if (iri == null) {
            throw new Exception(String.format(nullIDError, row + 1, id));
        }
        OWLClass cls = dataFactory.getOWLClass(iri);
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, dataFactory.getOWLDeclarationAxiom(cls));

        for (OWLAnnotation annotation: annotations) {
            manager.addAxiom(ontology,
                dataFactory.getOWLAnnotationAssertionAxiom(
                    iri, annotation));
        }

    }

    /**
     * Use templates to add logical axioms to an ontology.
     *
     * @param ontology the ontology to add axioms to
     * @param rows the table to use
     * @param row the current row to use
     * @param idColumn the column that holds the ID for the entity
     * @param parser used parse expressions
     * @param ioHelper used to generate IRIs
     * @throws Exception when names or templates cannot be handled
     */
    private static void addLogic(OWLOntology ontology,
            List<List<String>> rows, int row, int idColumn,
            ManchesterOWLSyntaxClassExpressionParser parser,
            IOHelper ioHelper)
            throws Exception {
        List<String> headers = rows.get(0);
        List<String> templates = rows.get(1);

        String id = null;
        try {
            id = rows.get(row).get(idColumn);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            return;
        }

        String classType = null;
        List<OWLClassExpression> classExpressions =
            new ArrayList<OWLClassExpression>();

        // For each column, collect information.
        for (int column = 0; column < headers.size(); column++) {
            String template = templates.get(column);
            if (template == null) {
                continue;
            }
            template = template.trim();
            if (template.isEmpty()) {
                continue;
            }

            String header = headers.get(column);
            String cell = null;
            try {
                cell = rows.get(row).get(column);
            } catch (IndexOutOfBoundsException e) {
                throw new Exception(
                    String.format(columnError, row + 1, id,
                        column + 1, header));
            }

            if (cell == null) {
                throw new Exception(
                    String.format(nullCellError, row + 1, id,
                        column + 1, header, template));
            }
            if (cell.trim().isEmpty()) {
                continue;
            }
            String content = QuotedEntityChecker.wrap(cell);

            if (template.equals("CLASS_TYPE")) {
                classType = cell.trim().toLowerCase();
            } else if (template.startsWith("C ")) {
                String sub = template.substring(2)
                                     .trim()
                                     .replaceAll("%", content);
                try {
                    classExpressions.add(parser.parse(sub));
                } catch (ParserException e) {
                    throw new Exception(
                        String.format(parseError, row + 1, id,
                            column + 1, header, sub, e.getMessage()));
                }
            } else if (template.startsWith("CI")) {
                IRI iri = ioHelper.createIRI(cell);
                classExpressions.add(dataFactory.getOWLClass(iri));
            }
        }

        // Now validate and build the class.
        IRI iri = ioHelper.createIRI(id);
        OWLClass cls = dataFactory.getOWLClass(iri);

        if (classType == null) {
            throw new Exception(String.format(missingTypeError, row + 1, id));
        }
        classType = classType.trim().toLowerCase();

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        if (classType.equals("subclass")) {
            for (OWLClassExpression expression: classExpressions) {
                manager.addAxiom(ontology,
                    dataFactory.getOWLSubClassOfAxiom(cls, expression));
            }
        } else if (classType.equals("equivalent")) {
            OWLObjectIntersectionOf intersection =
                dataFactory.getOWLObjectIntersectionOf(
                   new HashSet<OWLClassExpression>(classExpressions));
            manager.addAxiom(ontology,
                dataFactory.getOWLEquivalentClassesAxiom(
                    cls, intersection));
        } else {
            throw new Exception(
                String.format(unknownTypeError, row + 1, id));
        }
    }
}
