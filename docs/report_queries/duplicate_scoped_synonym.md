# Duplicate Scoped Synonyms

**Problem:** An entity has duplicate synonyms with different properties (e.g. the same broad and related synonym). This causes ambiguity.

**Solution:** Remove duplicate synonyms and determine the correct scope.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property {
   obo:IAO_0000118
   oboInOwl:hasExactSynonym
   oboInOwl:hasBroadSynonym
   oboInOwl:hasRelatedSynonym
   oboInOwl:hasNarrowSynonym
 }
 VALUES ?property2 {
   obo:IAO_0000118
   oboInOwl:hasExactSynonym
   oboInOwl:hasBroadSynonym
   oboInOwl:hasRelatedSynonym
   oboInOwl:hasNarrowSynonym
 }
 ?entity ?property ?value .
 ?entity ?property2 ?value .
 FILTER (?property != ?property2)
}
ORDER BY ?entity
```
