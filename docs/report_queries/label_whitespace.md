# Label Whitespace

**Problem:** A label has leading or trailing whitespace. This may cause issues when trying to reference the entity by label.

**Solution:** Remove the additional whitespace.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 {
  VALUES ?property {rdfs:label}
  ?entity ?property ?value .
  FILTER STRENDS(str(?value), " ")
 }
 UNION
 {
  VALUES ?property {rdfs:label}
  ?entity ?property ?value .
  FILTER STRSTARTS(str(?value), " ")
 }
}
ORDER BY ?entity
```
