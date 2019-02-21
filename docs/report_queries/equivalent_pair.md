# Equivalent Pair

**Problem:** An entity has a one-to-one equivalency with another entity. This may be intentional (assuming it is not annotated with `is_inferred true`), or it may be due to incorrect logic and the reasoner asserting the equivalency.

**Solution:** Ensure the reasoner is not incorrectly inferring equivalency. If so, update the logical axioms.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property {owl:equivalentClass}
 ?entity ?property ?value .
 FILTER (!isBlank(?value))
}
ORDER BY ?entity
```
