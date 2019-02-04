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

### Handling Compressed Files

All ROBOT commands support [gzip](https://www.gzip.org/) format files ending with the extension `.gz`. Any of the above formats can be compressed as long as the output ends with `.gz` (format must be specified for a compressed output):

	robot convert --input annotated.owl --format obo \
	 --output results/annotated.obo.gz

Gzip format files can also be used as input:

	robot convert --input annotated.obo.gz \
	 --output results/annotated.owl

### Converting to OBO Format

By default, the OBO writer strictly enforces [document structure rules](http://owlcollab.github.io/oboformat/doc/obo-syntax.html#4). If an ontology violates these, the convert to OBO operation will fail. These checks can be ignored by including `--check false`.

As a document is converted to OBO, you may see `ERROR MASKING ERROR` exceptions. This does not indicate failure, but it should be noted that these axioms will not be translated to OBO format. Rather, they will be included in the ontology header under `owl-axioms`. See [Untranslatable OWL axioms](http://owlcollab.github.io/oboformat/doc/obo-syntax.html#5.0.4) for more details.

You can choose to keep these in the file, or remove them with:
```
grep -v ^owl-axioms
```

---

## Error Messages

### Check Arg Error

`--check` only accepts `true` or `false` (not case sensitive) as arguments. By default, `--check` is true and the OBO document structure checks are performed.

### OBO Structure Error

If `--check` is true (which, by default, it is), the [document structure rules](http://owlcollab.github.io/oboformat/doc/obo-syntax.html#4) are strictly enforced. You may choose to review the exception message by running the command again with `--very-very-verbose`, or run `convert` with the `--check false` option to ignore the errors.

Please note that `--check false` may result in some unintended output. For example, for terms with more than one definition annotation, a defintion will be chosen at random.

### Output Error

`convert` requires exactly one `--output`. If you do not specify the `--output`, or specify more than one, ROBOT cannot proceed. If chaining commands, place `convert` last.

### Format Error

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

