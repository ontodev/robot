package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.junit.Test;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/** Tests for CommandLineHelper. */
public class CommandLineHelperTest {

  /**
   * Test command line splitting.
   *
   * @throws Exception on parsing problem
   */
  @Test
  public void testParseArgs() throws Exception {
    String arg = "";
    List<String> args = new ArrayList<String>();
    assertEquals("Empty arg list", CommandLineHelper.parseArgList(arg), args);

    arg = "command";
    args.clear();
    args.add("command");
    assertEquals("Just a command", CommandLineHelper.parseArgList(arg), args);

    arg = "command -i --foo PARAM 'single quoted' \"double quoted\"";
    args.clear();
    args.add("command");
    args.add("-i");
    args.add("--foo");
    args.add("PARAM");
    args.add("single quoted");
    args.add("double quoted");
    assertEquals("Basic command", CommandLineHelper.parseArgList(arg), args);

    arg = "command 'nested \"quotes\" with \\\" escapes and\nnewlines'";
    args.clear();
    args.add("command");
    args.add("nested \"quotes\" with \\\" escapes and\nnewlines");
    assertEquals("Nested quotations", CommandLineHelper.parseArgList(arg), args);

    // Expect an Exception here
    assertThrows(
        Exception.class,
        () -> {
          CommandLineHelper.parseArgList("unbalanced 'quotes");
        });
  }

  /**
   * Test handling an input ontology that requires a catalog file.
   *
   * @throws Exception on parsing problem
   */
  @Test
  public void testGetInputOntology() throws Exception {
    String[] args = {"--input", "../robot-core/src/test/resources/catalog_test.owl"};
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    CommandLine line = CommandLineHelper.getCommandLine("usage", o, args);
    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    CommandLineHelper.getInputOntology(ioHelper, line);
    assert true;
  }

  /**
   * Test that the HermiT reasoner variants use the expected existential strategy.
   *
   * @throws Exception on parsing or ontology creation problem
   */
  @Test
  public void testGetReasonerFactoryHermitStrategy() throws Exception {
    OWLOntology ontology = OWLManager.createOWLOntologyManager().createOntology();
    Options o = new Options();
    o.addOption("r", "reasoner", true, "reasoner to use");

    String[] args = {"--reasoner", "hermit"};
    CommandLine line = CommandLineHelper.getCommandLine("usage", o, args);
    OWLReasonerFactory factory = CommandLineHelper.getReasonerFactory(line);
    Reasoner reasoner = (Reasoner) factory.createReasoner(ontology);
    assertEquals(
        Configuration.ExistentialStrategyType.CREATION_ORDER,
        reasoner.getConfiguration().existentialStrategyType);

    args = new String[] {"--reasoner", "hermit-individual-reuse"};
    line = CommandLineHelper.getCommandLine("usage", o, args);
    factory = CommandLineHelper.getReasonerFactory(line);
    reasoner = (Reasoner) factory.createReasoner(ontology);
    assertEquals(
        Configuration.ExistentialStrategyType.INDIVIDUAL_REUSE,
        reasoner.getConfiguration().existentialStrategyType);

    args = new String[] {"--reasoner", "hermit-creation-order"};
    line = CommandLineHelper.getCommandLine("usage", o, args);
    factory = CommandLineHelper.getReasonerFactory(line);
    reasoner = (Reasoner) factory.createReasoner(ontology);
    assertEquals(
        Configuration.ExistentialStrategyType.CREATION_ORDER,
        reasoner.getConfiguration().existentialStrategyType);

    // We don't support the "hermit-el" variant, so it should throw an exception
    String[] badArgs = {"--reasoner", "hermit-el"};
    CommandLine badLine = CommandLineHelper.getCommandLine("usage", o, badArgs);
    assertThrows(
        IllegalArgumentException.class, () -> CommandLineHelper.getReasonerFactory(badLine));
  }
}
