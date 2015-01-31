package owltools2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

//import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link OWLOntologyIRIMapper} using the mappings from a catalog.xml file.
 */
public class CatalogXmlIRIMapper implements OWLOntologyIRIMapper {

	//private static final Logger logger = Logger.getLogger(CatalogXmlIRIMapper.class);
	
	private final Map<IRI, IRI> mappings;
	
	CatalogXmlIRIMapper(Map<IRI, IRI> mappings) {
		this.mappings = mappings;
	}
	
	/**
	 * Create an CatalogXmlIRIMapper from the given catalog.xml file.
	 * Assume, that relative paths are relative to the catalog file location.
	 * 
	 * @param catalogFile
	 * @throws IOException
	 */
	public CatalogXmlIRIMapper(String catalogFile) throws IOException {
		this(new File(catalogFile).getAbsoluteFile());
	}
	
	/**
	 * Create an CatalogXmlIRIMapper from the given catalog.xml file.
	 * Assume, that relative paths are relative to the catalog file location.
	 * 
	 * @param catalogFile
	 * @throws IOException
	 */
	public CatalogXmlIRIMapper(File catalogFile) throws IOException {
		this(catalogFile, catalogFile.getAbsoluteFile().getParentFile());
	}
	
	/**
	 * Create an CatalogXmlIRIMapper from the given catalog.xml file. 
	 * Use the parentFolder to resolve relative paths from the catalog file. 
	 * 
	 * @param catalogFile
	 * @param parentFolder
	 * @throws IOException
	 */
	public CatalogXmlIRIMapper(File catalogFile, File parentFolder) throws IOException {
		this(parseCatalogXml(new FileInputStream(catalogFile), parentFolder));
	}
	
	/**
	 * Create an CatalogXmlIRIMapper from the given catalog URL.
	 * Assume, there are no relative paths in the catalog file.
	 * 
	 * @param catalogURL
	 * @throws IOException
	 */
	public CatalogXmlIRIMapper(URL catalogURL) throws IOException {
		if ("file".equals(catalogURL.getProtocol())) {
			try {
				File catalogFile = new File(catalogURL.toURI());
				mappings = parseCatalogXml(new FileInputStream(catalogFile), catalogFile.getParentFile());
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		else {
			mappings = parseCatalogXml(catalogURL.openStream(), null);
		}
	}
	
	/**
	 * Create an CatalogXmlIRIMapper from the given catalog URL.
	 * Use the parentFolder to resolve relative paths from the catalog file. 
	 * 
	 * @param catalogURL
	 * @param parentFolder
	 * @throws IOException
	 */
	public CatalogXmlIRIMapper(URL catalogURL, File parentFolder) throws IOException {
		this(parseCatalogXml(catalogURL.openStream(), parentFolder));
	}
	
	@Override
	public IRI getDocumentIRI(IRI ontologyIRI) {
		return mappings.get(ontologyIRI);
	}
	
	/**
	 * Parse the inputStream as a catalog.xml file and extract IRI mappings.
	 * 
	 * Optional: Resolve relative file paths with the given parent folder.
	 * 
	 * @param inputStream input stream (never null)
	 * @param parentFolder folder or null
	 * @return mappings
	 * @throws IOException
	 * @throws IllegalArgumentException if input stream is null
	 */
	static Map<IRI, IRI> parseCatalogXml(InputStream inputStream, final File parentFolder) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream should never be null, missing resource?");
		}
		
		// use the Java built-in SAX parser 
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    factory.setValidating(false);
	    
	    try {
	    	final Map<IRI, IRI> mappings = new HashMap<IRI, IRI>();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(inputStream, new DefaultHandler(){

				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException 
				{
					// only look at 'uri' tags
					// does not check any parent tags
					if ("uri".equals(qName)) {
						IRI original = null;
						IRI mapped = null;
						String nameString = attributes.getValue("name");
						if (nameString != null) {
							original = IRI.create(nameString);
						}
						String mappedString = attributes.getValue("uri");
						if (mappedString != null) {
							if (parentFolder != null && mappedString.indexOf(":") < 0) {
								// there is a parent folder and the mapping is not an IRI or URL
								File file = new File(mappedString);
								if (!file.isAbsolute()) {
									file = new File(parentFolder, mappedString);
								}
								try {
									file = file.getCanonicalFile();
									mapped = IRI.create(file);
								} catch (IOException e) {
									//logger.warn("Skipping mapping: "+nameString+"   "+mappedString, e);
								}
							}
							else {
								mapped = IRI.create(mappedString);
							}
						}
						
						if (original != null && mapped != null) {
							mappings.put(original, mapped);
						}
					}
				}
			});
			return mappings;
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		} finally {
			inputStream.close();
		}
	}
}
