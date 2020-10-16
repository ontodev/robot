package org.obolibrary.robot;

import java.io.Writer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A version of ManchesterOWLSyntaxObjectRenderer adapted to output named objects as hyperlinks
 *
 * @author <a href="mailto:consulting@michaelcuffaro">Michael E. Cuffaro</a>
 */
public class ManchesterOWLSyntaxObjectHTMLRenderer extends ManchesterOWLSyntaxObjectRenderer {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(IOHelper.class);

  /**
   * Initialize the renderer with the given Writer and ShortFormProvider
   *
   * @param writer Writer to use
   * @param sfProvider ShortFormProvider to use
   */
  public ManchesterOWLSyntaxObjectHTMLRenderer(Writer writer, ShortFormProvider sfProvider) {
    super(writer, sfProvider);
  }

  // In order to implement the HTML rendering for named objects, we override all of the visit
  // methods of the parent class that are applicable to bottom-level instances of OWLNamedObject.
  // These are OWLAnnotationProperty, OWLClass, OWLDataProperty, OWLDatatype, OWLNamedIndividual,
  // and OWLObjectProperty.

  /** Given an OWLAnnotationProperty, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLAnnotationProperty property) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            property.getIRI().toString(), getShortFormProvider().getShortForm(property)));
  }

  /** Given an OWLClass, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLClass cls) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            cls.getIRI().toString(), getShortFormProvider().getShortForm(cls)));
  }

  /** Given an OWLDataProperty, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLDataProperty property) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            property.getIRI().toString(), getShortFormProvider().getShortForm(property)));
  }

  /** Given an OWLDataType, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLDatatype node) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            node.getIRI().toString(), getShortFormProvider().getShortForm(node)));
  }

  /** Given an OWLNamedIndividual, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLNamedIndividual individual) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            individual.getIRI().toString(), getShortFormProvider().getShortForm(individual)));
  }

  /** Given an OWLObjectProperty, write a hyperlink describing it to the writer. */
  @Override
  public void visit(OWLObjectProperty property) {
    write(
        String.format(
            "<a href=\"%s\">%s</a>",
            property.getIRI().toString(), getShortFormProvider().getShortForm(property)));
  }

  /**
   * Given an OWLClassExpression, determine the particular type of OWLClassExpression that it is,
   * and then call the appropriate visit() function for it.
   *
   * @param ce OWLClassExpression to visit
   * @throws ClassNotFoundException when a Class does not exist
   */
  public void visit(OWLClassExpression ce) throws ClassNotFoundException {
    if (ce instanceof OWLClass) {
      visit((OWLClass) ce);
    } else if (ce instanceof OWLObjectSomeValuesFrom) {
      visit((OWLObjectSomeValuesFrom) ce);
    } else if (ce instanceof OWLObjectAllValuesFrom) {
      visit((OWLObjectAllValuesFrom) ce);
    } else if (ce instanceof OWLObjectMinCardinality) {
      visit((OWLObjectMinCardinality) ce);
    } else if (ce instanceof OWLObjectMaxCardinality) {
      visit((OWLObjectMaxCardinality) ce);
    } else if (ce instanceof OWLObjectExactCardinality) {
      visit((OWLObjectExactCardinality) ce);
    } else if (ce instanceof OWLObjectHasValue) {
      visit((OWLObjectHasValue) ce);
    } else if (ce instanceof OWLObjectHasSelf) {
      visit((OWLObjectHasSelf) ce);
    } else if (ce instanceof OWLDataSomeValuesFrom) {
      visit((OWLDataSomeValuesFrom) ce);
    } else if (ce instanceof OWLDataAllValuesFrom) {
      visit((OWLDataAllValuesFrom) ce);
    } else if (ce instanceof OWLDataMinCardinality) {
      visit((OWLDataMinCardinality) ce);
    } else if (ce instanceof OWLDataMaxCardinality) {
      visit((OWLDataMaxCardinality) ce);
    } else if (ce instanceof OWLDataExactCardinality) {
      visit((OWLDataExactCardinality) ce);
    } else if (ce instanceof OWLDataHasValue) {
      visit((OWLDataHasValue) ce);
    } else if (ce instanceof OWLObjectIntersectionOf) {
      visit((OWLObjectIntersectionOf) ce);
    } else if (ce instanceof OWLObjectUnionOf) {
      visit((OWLObjectUnionOf) ce);
    } else if (ce instanceof OWLObjectComplementOf) {
      visit((OWLObjectComplementOf) ce);
    } else if (ce instanceof OWLObjectOneOf) {
      visit((OWLObjectOneOf) ce);
    } else {
      logger.error(
          "Could not visit class expression: {} of type: {}",
          ce.toString(),
          ce.getClass().toString());
    }
  }
}
