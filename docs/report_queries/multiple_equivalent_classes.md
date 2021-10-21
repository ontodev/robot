# Multiple Equivalent Classes

**Problem:** A class has more than one asserted equivalent classes, anonymous or named. This is probably a mistake, as equivalent statements can be intersections.

**Solution:** Combine the equivalent class statements.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { owl:equivalentClass }
 ?entity ?property ?value1 .
 ?entity ?property ?value2 .
 FILTER NOT EXISTS {
    ?value1 owl:unionOf ?x .
 }
 FILTER NOT EXISTS {
    ?value2 owl:unionOf ?x .
 }
 FILTER (?value1 != ?value2)
 FILTER (!isBlank(?entity))
 BIND (if(isIRI(?value1), ?value1, "blank node" ) as ?value)
}
ORDER BY ?entity
```
