# Misused Replaced-by Annotation

**Problem:** A replaced-by annotation was used on a non-obsolete class.

**Solution:** Remove the replaced-by annotation or obsolete the class.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { <http://purl.obolibrary.org/obo/IAO_0100001> }
 ?entity ?property ?value .
 FILTER NOT EXISTS { ?entity owl:deprecated true }
 FILTER (?entity != oboInOwl:ObsoleteClass)
}
ORDER BY ?entity
```