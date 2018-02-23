# Convert

Ontologies are shared in different formats. The default format used by ROBOT is RDF/XML, but there are other OWL formats, RDF formats, and also the OBO file format.

    robot convert --input annotated.owl --output results/annotated.obo

The file format is determined by the extension of the output file (e.g. `.obo`), but it can also be declared with the `--format` option. Valid file formats are:
  - json - [OBO Graphs JSON](https://github.com/geneontology/obographs/)
  - obo - [OBO Format](http://purl.obolibrary.org/obo/oboformat)
  - ofn - [OWL Functional](http://www.w3.org/TR/owl2-syntax/)
  - omn - [Manchester](https://www.w3.org/TR/owl2-manchester-syntax/)
  - owl - [RDF/XML](https://www.w3.org/TR/rdf-syntax-grammar/)
  - owx - [OWL/XML](https://www.w3.org/TR/owl2-xml-serialization/)
  - ttl - [Turtle](https://www.w3.org/TR/turtle/)

By default, the OBO writer strictly enforces <a href="http://owlcollab.github.io/oboformat/doc/obo-syntax.html#4" target="_blank">document structure rules</a>. If an ontology violates these, the convert to OBO operation will fail. These checks can be ignored by including `--check false`.
