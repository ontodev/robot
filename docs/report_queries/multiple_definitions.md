# Multiple Defintions

**Problem:** An entity has more than one definition or elucidation. This may cause confusion or misuse, and will prevent translation to OBO format (in case of multiple definitions). Excludes deprecated entities.

**OBO Foundry Principle:** [6 - Textual Definitions](http://obofoundry.org/principles/fp-006-textual-definitions.html)

**Solution:** Remove extra definitions.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { obo:IAO_0000115
                    obo:IAO_0000600 }
 ?entity ?property ?value .
 ?entity ?property ?value2 .
 FILTER (?value != ?value2)
 FILTER NOT EXISTS { ?entity owl:deprecated true }
 FILTER (!isBlank(?entity))
}
ORDER BY ?entity
```
