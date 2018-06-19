# Lowercase Definition

Problem: A definition does not begin with an uppercase letter. This may be indicative of inconsistent formatting.

Solution: Capitalize the first letter of the definition, or disregard this INFO.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>

SELECT DISTINCT ?entity ?property ?value
WHERE {
 VALUES ?property {obo:IAO_0000115}
 ?entity ?property ?value .
 FILTER (!regex(?value, "^[A-Z]"))
}
```
