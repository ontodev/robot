# Missing Subset Declaration

**Problem:** A subset is used in an annotation (via oboInOwl:inSubset), but is not properly declared as a child of oboInOwl:SubsetProperty. This can cause problems with conversions to OBO format, and should be avoided as all subsets should have metadata.

**Solution:** Make the subset a child of oboInOwl:SubsetProperty.

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
    VALUES ?property { rdfs:subPropertyOf }
    ?x oboInOwl:inSubset ?entity .
    FILTER NOT EXISTS { ?entity ?property oboInOwl:SubsetProperty }
}
ORDER BY ?entity
```
