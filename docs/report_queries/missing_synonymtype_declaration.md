# Missing Synonym Type Declaration

**Problem:** A synonym type is used in an annotation, but is not properly declared as a child of oboInOwl:SynonymTypeProperty. This can cause problems with conversions to OBO format.

**Solution:** Make the synonym type a child of oboInOwl:SynonymTypeProperty.

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
   VALUES ?property { oboInOwl:hasSynonymType }
   ?value ?property ?entity .
   FILTER NOT EXISTS { ?entity rdfs:subPropertyOf oboInOwl:SynonymTypeProperty }
}
ORDER BY ?entity
```