# Invalid Entity URI

**Problem:** OBO entities are formatted http://purl.obolibrary.org/obo/IDSPACE_0000000. This format is assumed by many OBO tools. Often, accidentally typos cause entity to be ignored during processing.

**Solution:** Fix the entity OBO URI.

```sparql
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT DISTINCT ?entity ?property ?value WHERE {
  ?entity rdf:type ?value .
  FILTER (!isBlank(?entity))
  FILTER(
    STRSTARTS(str(?entity),"https://purl.obolibrary.org/obo/") ||
    STRSTARTS(str(?entity),"http://purl.org/obo/") ||
    STRSTARTS(str(?entity),"http://www.obofoundry.org/") ||
    (STRSTARTS(str(?entity),"https://purl.obolibrary.org/") && regex(str(?entity),"http[:][/][/]purl[.]obolibrary[.]org[/][^o][^b][^o]"))
  )
}
ORDER BY ?entity
```
