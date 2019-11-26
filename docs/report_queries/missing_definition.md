# Missing Definition

**Problem:** An entity does not have a definition or elucidation. This may cause confusion or misuse of the entity. Excludes deprecated entities.

**OBO Foundry Principle:** [6 - Textual Definitions](http://obofoundry.org/principles/fp-006-textual-definitions.html)

**Solution:** Add a definition.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { obo:IAO_0000115 }
 ?entity ?any ?o
 FILTER NOT EXISTS { ?entity ?property ?value }
 FILTER NOT EXISTS { ?entity obo:IAO_0000600 ?elucidation }
 FILTER NOT EXISTS { ?entity a owl:Ontology }
 FILTER NOT EXISTS { ?entity a owl:NamedIndividual }
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
 FILTER (!isBlank(?entity)) }
ORDER BY ?entity
```
