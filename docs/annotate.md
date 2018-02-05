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