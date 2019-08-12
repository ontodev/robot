# Missing Obsolete Label

**Problem:** An obsolete entity does not have a label beginning with "obsolete". Adding this prefix ensures that users are aware the entity has been deprecated.

**Solution:** Add the "obsolete" (or "OBSOLETE") prefix.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { rdfs:label }
 ?entity owl:deprecated true .
 ?entity ?property ?value .
 FILTER (!regex(?value, "^obsolete", "i"))
}
ORDER BY ?entity
```
