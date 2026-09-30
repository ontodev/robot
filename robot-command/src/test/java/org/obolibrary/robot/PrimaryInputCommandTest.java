package org.obolibrary.robot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for commands that keep the existing or first input ontology as state. */
public class PrimaryInputCommandTest {

  /** Temporary test files. */
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  /** Test files used by command chains. */
  private static class Fixtures {
    private String primaryCatalogPath;
    private String badCatalogPath;
    private String primaryOntologyPath;
    private String primaryInputsPattern;
    private String auxiliaryOntologyPath;
    private String updatePath;
  }

  /**
   * Test that standalone merge records metadata for its first input.
   *
   * @throws Exception on parsing or ontology loading problems
   */
  @Test
  public void testMergeRecordsStandaloneMetadata() throws Exception {
    Fixtures fixtures = createFixtures();
    CommandState state =
        executeManaged(
            new MergeCommand(),
            new String[] {
              "--catalog",
              fixtures.primaryCatalogPath,
              "merge",
              "--input",
              fixtures.primaryOntologyPath,
              "--collapse-import-closure",
              "false"
            });

    assertEquals(fixtures.primaryCatalogPath, state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
  }

  /**
   * Test that standalone unmerge records metadata for its first input.
   *
   * @throws Exception on parsing or ontology loading problems
   */
  @Test
  public void testUnmergeRecordsStandaloneMetadata() throws Exception {
    Fixtures fixtures = createFixtures();
    CommandState state =
        executeManaged(
            new UnmergeCommand(),
            new String[] {
              "--catalog",
              fixtures.primaryCatalogPath,
              "unmerge",
              "--input",
              fixtures.primaryOntologyPath
            });

    assertEquals(fixtures.primaryCatalogPath, state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
  }

  /**
   * Test that merge records the first wildcard input path for later catalog guessing.
   *
   * @throws Exception on parsing, ontology loading, or query update problems
   */
  @Test
  public void testMergeInputsPatternRecordsMetadataForQueryUpdate() throws Exception {
    Fixtures fixtures = createFixtures();
    CommandState state =
        new MergeCommand()
            .execute(
                null,
                new String[] {
                  "--inputs", fixtures.primaryInputsPattern, "--collapse-import-closure", "false"
                });

    assertNull(state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
    new QueryCommand().execute(state, new String[] {"--update", fixtures.updatePath});
  }

  /**
   * Test that wildcard inputs still honor explicit input format.
   *
   * @throws Exception on parsing, ontology loading, or query update problems
   */
  @Test
  public void testInputsPatternPreservesInputFormat() throws Exception {
    Fixtures fixtures = createFixtures("primary-target.data", "primary*.data");
    CommandState state =
        new MergeCommand()
            .execute(
                null,
                new String[] {
                  "--input-format",
                  "owl",
                  "--inputs",
                  fixtures.primaryInputsPattern,
                  "--collapse-import-closure",
                  "false"
                });

    assertNull(state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
    new QueryCommand().execute(state, new String[] {"--update", fixtures.updatePath});
  }

  /**
   * Test that unmerge records the first wildcard input path for later catalog guessing.
   *
   * @throws Exception on parsing, ontology loading, or query update problems
   */
  @Test
  public void testUnmergeInputsPatternRecordsMetadataForQueryUpdate() throws Exception {
    Fixtures fixtures = createFixtures();
    CommandState state =
        new UnmergeCommand()
            .execute(null, new String[] {"--inputs", fixtures.primaryInputsPattern});

    assertNull(state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
    new QueryCommand().execute(state, new String[] {"--update", fixtures.updatePath});
  }

  /**
   * Test that chained merge preserves metadata for the target ontology used by query update.
   *
   * @throws Exception on parsing, ontology loading, or query update problems
   */
  @Test
  public void testMergePreservesChainedMetadataForQueryUpdate() throws Exception {
    Fixtures fixtures = createFixtures();
    MergeCommand mergeCommand = new MergeCommand();
    CommandState state =
        mergeCommand.execute(
            null,
            new String[] {
              "--catalog",
              fixtures.primaryCatalogPath,
              "--input",
              fixtures.primaryOntologyPath,
              "--collapse-import-closure",
              "false"
            });

    state =
        mergeCommand.execute(
            state,
            new String[] {
              "--catalog",
              fixtures.badCatalogPath,
              "--input",
              fixtures.auxiliaryOntologyPath,
              "--collapse-import-closure",
              "false"
            });

    assertEquals(fixtures.primaryCatalogPath, state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
    new QueryCommand().execute(state, new String[] {"--update", fixtures.updatePath});
  }

  /**
   * Test that chained unmerge preserves metadata for the target ontology used by query update.
   *
   * @throws Exception on parsing, ontology loading, or query update problems
   */
  @Test
  public void testUnmergePreservesChainedMetadataForQueryUpdate() throws Exception {
    Fixtures fixtures = createFixtures();
    UnmergeCommand unmergeCommand = new UnmergeCommand();
    CommandState state =
        unmergeCommand.execute(
            null,
            new String[] {
              "--catalog", fixtures.primaryCatalogPath, "--input", fixtures.primaryOntologyPath
            });

    state =
        unmergeCommand.execute(
            state,
            new String[] {
              "--catalog", fixtures.badCatalogPath, "--input", fixtures.auxiliaryOntologyPath
            });

    assertEquals(fixtures.primaryCatalogPath, state.getCatalogPath());
    assertEquals(fixtures.primaryOntologyPath, state.getOntologyPath());
    new QueryCommand().execute(state, new String[] {"--update", fixtures.updatePath});
  }

  /**
   * Create ontology, catalog, and update files for testing catalog preservation.
   *
   * @return fixture file paths
   * @throws Exception on file creation problems
   */
  private Fixtures createFixtures() throws Exception {
    return createFixtures("primary-target.owl", "primary*.owl");
  }

  /**
   * Create ontology, catalog, and update files for testing catalog preservation.
   *
   * @param primaryOntologyName name of the primary ontology file
   * @param primaryInputsPattern wildcard pattern for the primary ontology
   * @return fixture file paths
   * @throws Exception on file creation problems
   */
  private Fixtures createFixtures(String primaryOntologyName, String primaryInputsPattern)
      throws Exception {
    File directory = temporaryFolder.newFolder();
    File primaryImport = new File(directory, "imported-resource.owl");
    File invalidImport = new File(directory, "invalid-import.owl");
    File primaryOntology = new File(directory, primaryOntologyName);
    File auxiliaryOntology = new File(directory, "auxiliary.owl");
    File primaryCatalog = new File(directory, "catalog-v001.xml");
    File badCatalog = new File(directory, "bad-catalog.xml");
    File update = new File(directory, "update.ru");
    String importIRI = invalidImport.toURI().toString();

    write(
        primaryImport,
        ontology(importIRI, "", "<owl:Class rdf:about=\"http://example.org/Imported\"/>"));
    write(invalidImport, "not an ontology");
    write(
        primaryOntology,
        ontology(
            "http://example.org/primary.owl",
            "<owl:imports rdf:resource=\"" + importIRI + "\"/>",
            "<owl:Class rdf:about=\"http://example.org/Primary\"/>"));
    write(
        auxiliaryOntology,
        ontology(
            "http://example.org/auxiliary.owl",
            "",
            "<owl:Class rdf:about=\"http://example.org/Auxiliary\"/>"));
    write(primaryCatalog, catalog(importIRI, primaryImport));
    write(badCatalog, catalog(importIRI, invalidImport));
    write(
        update,
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "INSERT DATA { <http://example.org/Primary> rdfs:comment \"updated\" . }\n");

    Fixtures fixtures = new Fixtures();
    fixtures.primaryCatalogPath = primaryCatalog.getPath();
    fixtures.badCatalogPath = badCatalog.getPath();
    fixtures.primaryOntologyPath = primaryOntology.getPath();
    fixtures.primaryInputsPattern = new File(directory, primaryInputsPattern).getPath();
    fixtures.auxiliaryOntologyPath = auxiliaryOntology.getPath();
    fixtures.updatePath = update.getPath();
    return fixtures;
  }

  /**
   * Execute one command through CommandManager to test global options.
   *
   * @param command command to register
   * @param args command-line arguments
   * @return the resulting state
   * @throws Exception on command problems
   */
  private CommandState executeManaged(Command command, String[] args) throws Exception {
    CommandManager manager = new CommandManager();
    manager.addCommand(command.getName(), command);
    return manager.execute(null, args);
  }

  /**
   * Write content to a file.
   *
   * @param file the target file
   * @param content the content to write
   * @throws Exception on write problems
   */
  private void write(File file, String content) throws Exception {
    FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
  }

  /**
   * Return a minimal RDF/XML ontology.
   *
   * @param ontologyIRI the ontology IRI
   * @param ontologyContent content inside the ontology element
   * @param entityContent content inside the RDF document
   * @return ontology document content
   */
  private String ontology(String ontologyIRI, String ontologyContent, String entityContent) {
    return "<?xml version=\"1.0\"?>\n"
        + "<rdf:RDF xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
        + "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
        + "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
        + "  <owl:Ontology rdf:about=\""
        + ontologyIRI
        + "\">\n"
        + ontologyContent
        + "\n  </owl:Ontology>\n"
        + entityContent
        + "\n</rdf:RDF>\n";
  }

  /**
   * Return an XML catalog that maps the primary import IRI to a file.
   *
   * @param importIRI the import IRI to map
   * @param importFile the mapped import file
   * @return XML catalog content
   */
  private String catalog(String importIRI, File importFile) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<catalog prefer=\"public\" xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
        + "  <uri name=\""
        + importIRI
        + "\" uri=\""
        + importFile.toURI()
        + "\"/>\n"
        + "</catalog>\n";
  }
}
