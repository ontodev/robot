# Multiple Labels

Problem: An entity has more than one label. This may cause confusion or misuse, and will prevent translation to OBO format.

Solution: Remove extra labels.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value
WHERE {
 VALUES ?property {rdfs:label}
 ?entity ?property ?value .
 ?entity ?property ?value2 .
 FILTER (?value != ?value2) .
 FILTER NOT EXISTS {?entity owl:deprecated true}
 FILTER (!isBlank(?entity))
}
ORDER BY ?entity
```
