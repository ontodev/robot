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

---

## Error Messages

<a name="error-1"/>
### 1. Missing Output Error

An output file must be specified for `convert`. If chaining commands, place `convert` last.

<a name="error-2"/>
### 2. Multiple Outputs Error

Only one `--output` option may be specified for `convert`.

<a name="error-3"/>
### 3. Missing Format Error

The `convert` command expects either the `--output` file name to include an extension, or a format specified by `--format`.

Correct:
```
--output release.owl
--format owl --output release
```
Incorrect:
```
--output release
```
