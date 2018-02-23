# Annotate

It's important to add metadata to an ontology before releasing it, and to update the ontology version IRI.

Annotations can be added one-by-one with `--anotation`, or all at once from a turtle file with `--annotation-file`. See <a href="/examples/annotations.ttl" target="_blank">`annotations.ttl`</a> for an example.

You can specify as many annotation flags as you'd like in one command:

    robot annotate --input edit.owl \
      --ontology-iri "https://github.com/ontodev/robot/examples/annotated.owl" \
      --version-iri "https://github.com/ontodev/robot/examples/annotated-1.owl" \
      --annotation rdfs:comment "Comment" \
      --annotation rdfs:label "Label" \
      --annotation-file annotations.ttl \
      --output results/annotated.owl

Including at least the following annotations is recommended:
  * Ontology & version IRIs
  * Title (<a href="http://purl.org/dc/terms/title" target="_blank">`dcterms:title`</a>)
  * Description (<a href="http://purl.org/dc/terms/description" target="_blank">`dcterms:description`</a>)
  * License (<a href="http://purl.org/dc/terms/license" target="_blank">`dcterms:license`</a>)

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
