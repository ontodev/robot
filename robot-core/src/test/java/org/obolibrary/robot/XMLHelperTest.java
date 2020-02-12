package org.obolibrary.robot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class XMLHelperTest extends CoreTest {

  private final IRI TARGET = IRI.create("http://purl.obolibrary.org/obo/UBERON_0000480");
  private final IRI STRING = IRI.create("http://www.w3.org/2001/XMLSchema#string");
  private final IRI DB_XREF = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
  private final IRI DEFINITION = IRI.create("http://purl.obolibrary.org/obo/IAO_0000115");

  /** @return */
  private OWLAnnotationAssertionAxiom createExpectedAnnotation() {
    OWLDataFactory df = OWLManager.getOWLDataFactory();
    OWLDatatype string = df.getOWLDatatype(STRING);

    OWLAnnotationProperty hasDbXref = df.getOWLAnnotationProperty(DB_XREF);
    OWLLiteral dbXref1 = df.getOWLLiteral("CARO:0000054", string);
    OWLLiteral dbXref2 = df.getOWLLiteral("CARO:MAH", string);
    OWLAnnotation ann1 = df.getOWLAnnotation(hasDbXref, dbXref1);
    OWLAnnotation ann2 = df.getOWLAnnotation(hasDbXref, dbXref2);
    Set<OWLAnnotation> annotations = new HashSet<>(Arrays.asList(ann1, ann2));

    // Main Axiom
    OWLAnnotationProperty definitionProperty = df.getOWLAnnotationProperty(DEFINITION);
    OWLLiteral definitionValue =
        df.getOWLLiteral(
            "Anatomical structure consisting of at least two non-overlapping organs, multi-tissue aggregates or portion of tissues or cells of different types that does not constitute an organism, organ, multi-tissue aggregate, or portion of tissue.",
            string);
    OWLAnnotation parentAnn = df.getOWLAnnotation(definitionProperty, definitionValue, annotations);
    return df.getOWLAnnotationAssertionAxiom(TARGET, parentAnn);
  }

  @Test
  public void testXMLExtract() throws IOException, OWLOntologyCreationException {
    String path = this.getClass().getResource("/uberon.owl").getPath();
    XMLHelper xmlHelper = new XMLHelper(path, null);
    Set<IRI> targets = new HashSet<>(Collections.singletonList(TARGET));
    OWLOntology outputOntology = xmlHelper.extract(targets, null, null);

    OWLAxiom expected = createExpectedAnnotation();
    System.out.println("EXPECTED:");
    System.out.println(expected);

    System.out.println("ACTUAL:");
    System.out.println(outputOntology.getAxioms());

    assert (outputOntology.containsAxiom(expected));
  }
}
