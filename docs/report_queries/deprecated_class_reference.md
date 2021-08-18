# Deprecated Class Reference

**Problem:** A deprecated class is used in a logical axiom. A deprecated class can be the child of another class (e.g. ObsoleteClass), but it cannot have children or be used in blank nodes or equivalent class statements. Additionally, a deprecated class should not have any equivalent classes or anonymous parents.

**Solution:** Replace deprecated class.

```sparql
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT DISTINCT ?entity ?property ?value WHERE {
  {
   VALUES ?property {
     rdfs:subClassOf
   }
   ?entity a owl:Class;
           owl:deprecated true ;
           ?property ?value .
   FILTER ( ?value NOT IN (oboInOwl:ObsoleteClass, owl:Thing) )
  }
  UNION
  {
   VALUES ?property {
     owl:equivalentClass
     owl:disjointWith
   }
   ?entity a owl:Class;
           owl:deprecated true ;
           ?property ?value .
  }
  UNION
  {
     VALUES ?property {
       rdfs:subClassOf
       owl:equivalentClass
       owl:disjointWith
     }
     ?entity a owl:Class;
             owl:deprecated true .
     ?value ?property ?entity .
  }
  UNION
  {
   VALUES ?property {
     owl:ObjectProperty
     owl:DataProperty
   }
   ?entity a owl:Class ;
           owl:deprecated true ;
           ?property ?value .
  }
  UNION
  {
   VALUES ?property {
     owl:someValuesFrom
     owl:allValuesFrom
   }
   ?value a owl:Class ;
          owl:deprecated true .
   ?rest a owl:Restriction ;
         ?property ?value .
   BIND("blank node" as ?entity)
  }
}
ORDER BY ?entity
```
