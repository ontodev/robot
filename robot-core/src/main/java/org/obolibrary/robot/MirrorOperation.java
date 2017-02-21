package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Mirrors ontologies on filesystem
 *
 * @author cjm
 *
 */
public class MirrorOperation {

    /**
     * Logger.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(MirrorOperation.class);


    /**
     * Mirrors ontologies on local filesystem
     *
     * @param ontologies
     * @param baseFolder
     * @param catalogFile
     * @throws IOException
     * @throws OWLOntologyStorageException
     */
    public static void mirror(OWLOntology rootOntology,
            File baseFolder, File catalogFile) throws IOException, OWLOntologyStorageException {

        logger.info("Mirroring ontologies: "+rootOntology);
        Map<IRI, String> iriMap = new HashMap<>();
        for (OWLOntology ont : rootOntology.getImportsClosure()) {
            logger.info("Mirroring: "+ont);
            validateImports(ont);

            OWLOntologyID ontologyID = ont.getOntologyID();
            Optional<IRI> ontologyIRI = ontologyID.getOntologyIRI();

            // Not really sure why this is here, but apparently we can get
            // an ontology without an IRI, in which case we'll generate one
            // that is 'sort of' unique (only fails if two different machines run
            // this tool at the exact same time).
            //
            if (!ontologyIRI.isPresent()) {
                ontologyIRI = Optional.of(IRI.generateDocumentIRI());
            }
            // Always write the actualIRI
            String localFilePath = getMirrorPathOfOntologyIRI(ontologyIRI.get());
            IRI outputStream = IRI.create(new File(baseFolder, localFilePath));
            ont.getOWLOntologyManager().saveOntology(ont, outputStream);
            iriMap.put(ontologyIRI.get(), localFilePath);

            //
            // In case there is a difference between the source document IRI
            // and the IRI of the resolved target (e.g., there is an HTTP
            // redirect from a legacy IRI to a newer IRI), then write an entry
            // in the catalog that points the legacy IRI to the newer, canonical one.
            // Examples of this include:
            //  http://purl.obolibrary.org/obo/so.owl
            // which redirects to:
            //  http://purl.obolibrary.org/obo/so-xp.obo.owl
            //

            IRI documentIRI = ont.getOWLOntologyManager().getOntologyDocumentIRI(ont);
            if (!documentIRI.toString().startsWith("file:") &&
                    documentIRI != ontologyIRI.get()) {
                String sourceLocalFile = getMirrorPathOfOntologyIRI(ontologyIRI.get());
                logger.info("Mirroring "+documentIRI+" in "+sourceLocalFile);
                iriMap.put(documentIRI, sourceLocalFile);
           }
        }
        writeCatalog(catalogFile, iriMap);

    }

    /**
     * Writes a catalog-v001.xml file
     *
     * @param path
     * @param iriMap
     * @throws IOException
     */
    public static void writeCatalog(File catalogFile, Map<IRI,String> iriMap) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        lines.add("<catalog prefer=\"public\" xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">");
        for (IRI iri : iriMap.keySet()) {
            lines.add("");
            lines.add("  <!-- generated mapping -->");
            lines.add("  <uri name=\""+ iri +"\" uri=\""+ iriMap.get(iri) +"\"/>");
        }
        lines.add("</catalog>\n");
        FileUtils.writeLines(catalogFile, lines);
    }

    /**
     * Generates a local file path that should be in a 1:1 relationship with the ontology IRI
     *
     * The path can the point to a file that mirrors an individual ontology
     *
     * @param iri
     * @return path
     */
    private static String getMirrorPathOfOntologyIRI(IRI iri) {
        String iriString = iri.toString();
        iriString = iriString.replaceFirst("http://", "").replaceFirst("https://", "");
        iriString = iriString.replace(':', '_');
        iriString = iriString.replace('\\', '_');
        return iriString;
    }


    private static void validateImports(OWLOntology ontology) throws IOException {
        Set<IRI> directImportDocuments = ontology.getDirectImportsDocuments();
        Set<OWLOntology> directImports = ontology.getDirectImports();
        if (directImports.size() < directImportDocuments.size()) {
            // less imports than actually declared
            // assume something went wrong, throw Exception
            throw new IOException(
                "The ontology has less actual imports then declared.\nActual: "
                + directImports
                + "\n Declared: "
                + directImportDocuments
            );
        }
    }
}
