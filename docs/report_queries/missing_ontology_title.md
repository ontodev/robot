# Missing Ontology Title

**Problem:** The ontology header is missing required metadata: [title](http://dublincore.org/documents/dcmi-terms/#elements-title).

**Solution:** Add the missing title. See link for appropriate property.

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 ?entity a owl:Ontology .
 OPTIONAL { ?entity ?property ?value }
 FILTER (!bound(?value))
  VALUES ?property { dc:title dcterms:title }
}
```
