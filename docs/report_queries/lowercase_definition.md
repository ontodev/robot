# Lowercase Definition

**Problem:** A definition or elucidation does not begin with an uppercase letter. This may be indicative of inconsistent formatting.

**OBO Foundry Principle:** [6 - Textual Definitions](http://www.obofoundry.org/principles/fp-006-textual-definitions.html)

**Solution:** Capitalize the first letter of the definition, or disregard this INFO.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { obo:IAO_0000115
                    obo:IAO_0000600 }
 ?entity ?property ?value .
 FILTER (!regex(?value, "^[A-Z]"))
}
ORDER BY ?entity
```
