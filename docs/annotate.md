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

<a name="error-1"/>
### 1. Annotation Format Error

Argument to `--annotation` option does not follow the `PROP VALUE` format.

Correct:
```
--annotation rdfs:comment "this is a comment"`
```
Incorrect:
```
--annotation "this is a comment"`
--annotation rdfs:comment this is a comment`
```

<a name="error-2"/>
### 2. Link Annotation Format Error 

Argument to `--link-annotation` option does not follow the `PROP LINK` format.

Correct:
```
--link-annotation dc:license https://creativecommons.org/publicdomain/zero/1.0/
```
Incorrect:
```
--link-annotation dc:license "CC0 1.0"
```

<a name="error-3"/>
### 3. Language Annotation Format Error

Argument to `--language-annotation` option does not follow the `PROP VALUE LANG` format. 

Correct: 
```
--language-annotation rdfs:label "label" en
```
Incorrect:
```
--language-annotation rdfs:label "label"
--language-annotation rdfs:label "label"@en
```

<a name="error-4"/>
### 4. Typed Annotation Format Error

Argument to `--typed-annotation` option does not follow the `PROP VALUE TYPE` format. 

Correct: 
```
--typed-annotation rdfs:label "label" xsd:string
```
Incorrect:
```
--typed-annotation rdfs:label "label"
--typed-annotation rdfs:label "label"^^xsd:string
```

<a name="error-5"/>
### 5. Axiom Annotation Format Error

Argument to `--axiom-annotation` option does not follow the `PROP VALUE` format, same as [Annotation Format Error](#error-1).

<a name="error-6"/>
### 6. Missing Annotation Error

No annotations were provided for the `annotate` command. The `--remove-annotations` option is also accepted in place of annotations.
