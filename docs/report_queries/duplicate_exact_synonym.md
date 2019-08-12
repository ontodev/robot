# Duplicate Exact Synonym

**Problem:** Two entities share an exact synonym. This causes ambiguity. Excludes deprecated entities.

**Solution:** Avoid ambiguity by assigning unique exact synonyms or changing the exact synonym to a different annotation (e.g. broad synonym).

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property {
   obo:IAO_0000118
   oboInOwl:hasExactSynonym
 }
 ?entity ?property ?value.
 ?entity2 ?property ?value .
 FILTER NOT EXISTS { ?entity owl:deprecated true }
 FILTER NOT EXISTS { ?entity2 owl:deprecated true }
 FILTER (?entity != ?entity2)
}
ORDER BY DESC(UCASE(str(?value)))
```
