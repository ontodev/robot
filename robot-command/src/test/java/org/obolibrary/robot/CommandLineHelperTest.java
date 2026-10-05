package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.junit.Test;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.existentials.CreationOrderStrategy;
import org.semanticweb.HermiT.existentials.IndividualReuseStrategy;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.LoggerFactory;

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
    assertEquals(CreationOrderStrategy.class, getHermitStrategy("hermit"));
    assertEquals(CreationOrderStrategy.class, getHermitStrategy("hermit-creation-order"));
    assertEquals(IndividualReuseStrategy.class, getHermitStrategy("hermit-individual-reuse"));

    // We don't support the "hermit-el" variant, so it should throw an exception
    assertThrows(IllegalArgumentException.class, () -> getHermitStrategy("hermit-el"));
  }

  /**
   * Test that running the reason command with a HermiT variant logs the strategy actually in use.
   *
   * @throws Exception on any problem
   */
  @Test
  public void testReasonCommandLogsHermitStrategy() throws Exception {
    Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Level rootLevel = root.getLevel();
    Logger logger = (Logger) LoggerFactory.getLogger(CommandLineHelper.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      String[] args = {
        "-vv",
        "--input",
        "../robot-core/src/test/resources/simple.owl",
        "--reasoner",
        "hermit-individual-reuse"
      };
      new ReasonCommand().execute(null, args);
    } finally {
      logger.detachAppender(appender);
      root.setLevel(rootLevel);
    }

    List<String> messages =
        appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    assertTrue(messages.contains("HermiT existential strategy: INDIVIDUAL_REUSE"));
  }

  /**
   * Create a reasoner via getReasonerFactory and return the class of the existential expansion
   * strategy that HermiT's tableau is using.
   *
   * @param reasonerName value for the --reasoner option
   * @return class of the existential expansion strategy in use
   * @throws Exception on parsing or ontology creation problem
   */
  private Class<?> getHermitStrategy(String reasonerName) throws Exception {
    Options o = new Options();
    o.addOption("r", "reasoner", true, "reasoner to use");
    String[] args = {"--reasoner", reasonerName};
    CommandLine line = CommandLineHelper.getCommandLine("usage", o, args);
    OWLReasonerFactory factory = CommandLineHelper.getReasonerFactory(line);
    OWLOntology ontology = OWLManager.createOWLOntologyManager().createOntology();
    Reasoner reasoner = (Reasoner) factory.createReasoner(ontology);
    return reasoner.getTableau().getExistentialsExpansionStrategy().getClass();
  }
}
