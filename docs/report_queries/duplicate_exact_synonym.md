# Duplicate Exact Synonym

**Problem:** Two entities share an exact synonym (case-insensitive). This causes ambiguity. Excludes deprecated entities and synonyms annotated as abbreviation or acronym.

**Solution:** Avoid ambiguity by assigning unique exact synonyms, changing the exact synonym to a different annotation (e.g. broad synonym), or annotating it as an abbreviation or acronym.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value
WHERE {
  {
    SELECT DISTINCT ?syn_std ?property (COUNT(DISTINCT ?entity) AS ?cnt)
    WHERE {
      VALUES ?property { obo:IAO_0000118 oboInOwl:hasExactSynonym }
      ?entity ?property ?syn .
      OPTIONAL {
        VALUES ?syn_type { obo:OMO_0003000 obo:OMO_0003012 }
        ?exclude a owl:Axiom ;
              owl:annotatedSource ?entity ;
              owl:annotatedProperty ?property ;
              owl:annotatedTarget ?syn ;
              oboInOwl:hasSynonymType ?syn_type .
      }

      BIND(UCASE(?syn) AS ?syn_std)
      FILTER (!isBlank(?entity) && !BOUND(?exclude))
      FILTER NOT EXISTS { ?entity owl:deprecated true }
    } GROUP BY ?syn_std ?property HAVING (?cnt > 1)
  }
  ?entity ?property ?value .
  FILTER (!isBlank(?entity))
  FILTER NOT EXISTS { ?entity owl:deprecated true }
  FILTER (UCASE(?value) = ?syn_std)
}
ORDER BY DESC(UCASE(str(?value)))
```