# Deprecated Boolean Datatype

**Problem:** When an entity is deprecated using the owl:deprecated annotation property, the value of the annotation must be a boolean data type.

**Solution:** Replace deprecated value with a boolean data type.

```sparql
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT DISTINCT ?entity ?property ?value WHERE {
  VALUES ?property {
    owl:deprecated
  }
  ?entity ?property ?value .
  FILTER (datatype(?value) != xsd:boolean)
  FILTER (!isBlank(?entity))
}
ORDER BY ?entity
```
