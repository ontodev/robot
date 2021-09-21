# Equivalent Class Axiom with no Genus

**Problem:** An equivalent class axiom of the kind C = R some A is nearly always an indication of a problem. It basically means that anything that is R related to an A is equal to the class. While this is sometimes legit, most often this is accidental.

**Solution:** Add a genus to the class expression like: C = B and R some A.

```
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 ?entity owl:equivalentClass ?ec .
 ?entity owl:equivalentClass [ rdf:type owl:Restriction ;
                              owl:someValuesFrom ?y ;
                              owl:onProperty ?property ] .
  FILTER (!isBlank(?entity))
  FILTER (!isBlank(?y))
}
ORDER BY ?entity
```
