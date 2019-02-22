# Missing Ontology License

**Problem:** The ontology header is missing required metadata: [license](http://dublincore.org/documents/dcmi-terms/#terms-license).

**OBO Foundry Principle:** [1 - Open](http://obofoundry.org/principles/fp-001-open.html)

**Solution:** Add the missing license. See link for appropriate property.

```sparql
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { dcterms:license }
 ?entity a owl:Ontology .
 OPTIONAL { ?entity ?property ?value }
 FILTER (!bound(?value))
}
```
