# Missing Superclass

Problem: A class does not have a superclass. This is not relevant for top-level classes, but may reveal orphaned children.

Solution: Make sure there are no orphaned children - if so, assert a parent.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value
WHERE {
 VALUES ?property {rdfs:subClassOf}
 ?entity a owl:Class .
 FILTER (!isBlank(?entity)) .
 FILTER NOT EXISTS {?entity ?property ?value} .
 FILTER NOT EXISTS {
   FILTER EXISTS {
     ?entity owl:deprecated ?dep .
     FILTER regex(str(?dep), "true")
   }
 }
}
```
