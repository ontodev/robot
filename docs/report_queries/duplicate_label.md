# Duplicate Label

Problem: Two different subjects have been assigned the same label. This causes ambiguity.

Solution: Avoid ambiguity by assigning distinct labels to each subject.

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value
WHERE {
 VALUES ?property {rdfs:label}
 ?entity ?property ?value .
 ?entity2 ?property ?value .
 FILTER (?entity != ?entity2)
 FILTER (!isBlank(?entity))
}
ORDER BY ?value
```
