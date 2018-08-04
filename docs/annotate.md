# Annotate

It's important to add metadata to an ontology before releasing it, and to update the ontology version IRI.

General annotations can be added one-by-one with `--anotation`, and the IRIs can be set with `--version-iri` and `--ontology-iri`. You can specify as many annotation flags as you'd like in one command:

    robot annotate --input edit.owl \
      --ontology-iri "https://github.com/ontodev/robot/examples/annotated.owl" \
      --version-iri "https://github.com/ontodev/robot/examples/annotated-1.owl" \
      --annotation rdfs:comment "Comment" \
      --annotation rdfs:label "Label" \
      --annotation-file annotations.ttl \
      --output results/annotated.owl

Or all at once from a turtle (.ttl) file with `--annotation-file`:
<!-- DO NOT TEST -->
```
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl:     <http://www.w3.org/2002/07/owl#> .
@prefix example: <https://github.com/ontodev/robot/examples/> .

example:annotated.owl
  rdf:type owl:Ontology ;
  rdfs:comment "Comment from annotations.ttl file." .
```

Including at least the following annotations is recommended:
  * Ontology & version IRIs
  * Title (<a href="http://purl.org/dc/terms/title" target="_blank">`dcterms:title`</a>)
  * Description (<a href="http://purl.org/dc/terms/description" target="_blank">`dcterms:description`</a>)
  * License (<a href="http://purl.org/dc/terms/license" target="_blank">`dcterms:license`</a>)

This command can also remove all ontology annotations from your file with `--remove-annotations`. You can combine this with options to add new annotations:

    robot annotate --input annotated.owl \
      --remove-annotations \
      --annotation-file annotations.ttl \
      --output results/annotated_2.owl

---

## Error Messages

### Annotation Format Error

Arguments to `--annotation`  or `--axiom-annotation` option must follow the `PROP VALUE` format:
```
--annotation rdfs:comment "this is a comment"`
```

Arguments to `--link-annotation` option must follow the `PROP LINK` format:
```
--link-annotation dc:license https://creativecommons.org/publicdomain/zero/1.0/
```

Arguments to `--language-annotation` option must follow the `PROP VALUE LANG` format:
```
--language-annotation rdfs:label "label" en
```

Arguments to `--typed-annotation` option must follow the `PROP VALUE TYPE` format:
```
--typed-annotation rdfs:label "label" xsd:string
```

### Missing Annotation Error

No annotations were provided for the `annotate` command. The `--remove-annotations` option is also accepted in place of annotations.
