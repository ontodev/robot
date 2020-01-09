# Missing Label

**Problem:** An entity does not have a label. This may cause confusion or misuse. Excludes deprecated entities.

**OBO Foundry Principle:** [12 - Naming Conventions](http://www.obofoundry.org/principles/fp-012-naming-conventions.html)

**Solution:** Add a label.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { rdfs:label }
 ?entity ?any ?o .
 FILTER NOT EXISTS { ?entity ?property ?value }
 FILTER NOT EXISTS { ?entity a owl:Ontology }
 FILTER NOT EXISTS { ?entity owl:deprecated true }
 FILTER NOT EXISTS {
   ?entity rdfs:subPropertyOf oboInOwl:SubsetProperty .
 }
 FILTER EXISTS {
   ?entity ?prop2 ?object .
   FILTER (?prop2 != rdf:type)
   FILTER (?prop2 != owl:equivalentClass)
   FILTER (?prop2 != owl:disjointWith)
   FILTER (?prop2 != owl:equivalentProperty)
   FILTER (?prop2 != owl:sameAs)
   FILTER (?prop2 != owl:differentFrom)
   FILTER (?prop2 != owl:inverseOf)
 }
 FILTER (!isBlank(?entity))
}
ORDER BY ?entity
```
