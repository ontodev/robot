# Deprecated Class Reference

**Problem:** A deprecated class is used in a logical axiom. A deprecated class can be the child of another class (e.g. ObsoleteClass), but it cannot have children or be used in blank nodes or equivalent class statements. Additionally, a deprecated class should not have any equivalent classes or anonymous parents.

**Solution:** Replace deprecated class.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 {
  VALUES ?property {
    owl:equivalentClass
    rdfs:subClassOf
  }
  ?entity owl:deprecated true .
  ?value ?property ?entity .
  FILTER NOT EXISTS { ?entity rdfs:subClassOf oboInOwl:ObsoleteClass }
 }
 UNION
 {
  ?entity owl:deprecated true .
  ?entity ?property ?value .
  FILTER (?property != obo:IAO_0000231)
  FILTER (?property != rdf:type)
  FILTER NOT EXISTS { ?property a owl:AnnotationProperty }
  FILTER NOT EXISTS {
    ?entity rdfs:subClassOf ?value .
    FILTER (!isBlank(?value))
  }
  FILTER NOT EXISTS {
    ?entity rdfs:subPropertyOf ?value .
    FILTER (!isBlank(?value))
  }
  FILTER NOT EXISTS { ?entity rdfs:subClassOf oboInOwl:ObsoleteClass }
 }
}
ORDER BY ?entity
```
