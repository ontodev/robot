# Multiple Asserted Superclasses

Problem: A class has more than one asserted named parent. This filters any axioms with the `is_inferred true` annotation, but inferences may not always be annotated. Double check where the parent is coming from before making changes.

Solution: Use logical axioms to infer dual parentage.

```sparql
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value
WHERE {
 VALUES ?property {rdfs:subClassOf}
 ?entity a owl:Class .
 ?entity ?property ?value .
 ?entity ?property ?value2 .
 FILTER (?value != ?value2)
 FILTER (!isBlank(?value))
 FILTER (!isBlank(?value2))
 FILTER NOT EXISTS {
   ?axiom owl:annotatedSource ?entity .
   ?axiom owl:annotatedTarget ?value .
   ?axiom owl:annotatedProperty rdfs:subClassOf .
   ?axiom oboInOwl:is_inferred ?inf .
   FILTER regex(str(?inf), "true")
 }
}
ORDER BY ?entity
```
