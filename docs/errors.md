# Common Error Messages

For command-specific error messages, see the command's doc page.

If you run into any other errors or exceptions not listed, or have any issues with the existing errors, please head over to our <a href="https://github.com/ontodev/robot/issues" target="_blank">GitHub Issues</a> for assistance!

---

## Java Errors

### Out of Memory Error

Sometimes when working with very large ontologies, the JVM will run out of memory (`OutOfMemoryError`). You can increase the max heap size using the Java option `-Xmx4G` before running `robot`, which increases the heap to 4G. Any size can be specified here, e.g. `-Xmx8G`. For details on setting these options based on platform, see [Java Options](http://robot.obolibrary.org/global#java-options).

---

## Command and Option Errors

### Boolean Value Error

The command line option provided only accepts `true` or `false` as input.

### Chained Input Error

[Chained commands](/chaining) take the output of the last command as the input of the next. Because of this, `--input` should only be used with the first command in the chain. This excludes [merge](/merge) and [unmerge](/unmerge), which allow multiple inputs.

### File Does Not Exist Error

One of the required files (often `--input`) could not be found. This is often caused by a typo.

### Invalid Format Error

When specifying the `--output` (or `--format` for converting), make sure the file has a valid extension. See [convert](/convert) for a current list of ontology formats.

### Invalid Element Error

This error occurs when ROBOT tries to convert an IRI to an XML element name for writing but encounters an illegal character. Common illegal characters include `/` and `:`. This error usually occurs when creating new ontology terms with [`template`](/template). See [Namespaces in XML](https://www.w3.org/TR/REC-xml-names/) for full details on legal XML element names.

The solution is usually to add a new [prefix](/global#prefixes) so that the illegal character is no longer part of the element name. For example, the prefix `ex` for `http://example.com/` is valid, and `http://example.com/foo/bar` is a valid IRI, but `ex:foo/bar` is not a valid element name. By defining a new prefix `foo` for `http://example.com/foo/` we can now use `foo:bar` as a valid element name for the same IRI `http://example.com/foo/bar`.

### Invalid Ontology File Error

ROBOT was expecting an ontology file, and the file exists, but is not in a recognized format. Adding the `-vvv` option will print a stack trace that shows how the underlying OWLAPI library tried to parse the ontology file. This will include details and line numbers that can help isolate the problem.

### Invalid Ontology IRI Error

Either the ontology could not be loaded from the provided IRI, or the file at that IRI was in an unrecognized format. Adding the `-vvv` option may provide helpful details.

### Invalid Ontology Stream Error

Either the ontology could not be loaded from the provided input stream, or the input stream was in an unrecognized format. Adding the `-vvv` option may provide helpful details.

### Invalid IRI Error

Many commands involve creating IRIs from provided string representations of the <a href="https://www.w3.org/TR/2010/NOTE-curie-20101216/" target="_blank">CURIE</a> or full IRI. If the provided field is not a string in valid CURIE or IRI format, the actual IRI cannot be created.

When using CURIEs, make sure the prefix is defined, or add it with `--prefix`.

### Invalid IRI Pattern Error

When matching an IRI by pattern, the pattern should contain one or more wildcard characters (`*` and/or `?`) or should be a regex pattern preceded by `~`. If you wish to match a full IRI to remove or filter, use the `--term` option.

### Invalid Prefix Error

Prefixes (added with `--prefix`) should be strings in the following format: `"foo: http://foo/bar#"`. See [Prefixes](/global#prefixes) for more details on adding prefixes.

### Invalid Reasoner Error

[Reason](/reason), [materialize](/materialize), and [reduce](/reduce) all expect `--reasoner` options. All three commands support `structural`, `hermit`, `jfact`, and `elk`. Only the reason command supports `emr`. Click on the command for more details

### JSON-LD Context Creation Error

There was an error creating a JSON-LD context. This could be caused by a bad prefix.

### JSON-LD Context Parse Error

There was an error parsing a JSON-LD context. Add the `-vvv` option to see more details, and refer to <https://json-ld.org> for information about that format.

### Missing Command Error

A valid command must be provided, running just `robot` will not perform any action.

### Missing Requirement Error

Some commands require certain options to be present. See the documentation for the specific command.

### Missing Input Error

An input ontology must be specified with `--input`. In the case of [merge](/merge) and [unmerge](/unmerge), at least one input is required.

### Missing Term(s) Error

Some commands ([extract](/extract) and [filter](/filter)) require terms as input in addition to the ontology. Click on the commands for more details.

### Multiple Inputs Error

For all commands other than [merge](/merge) and [unmerge](/unmerge), only one `--input` may be specified.

### OBO Structure Error

When running the [convert](/convert) command, if `--check` is true (default behavior), the [document structure rules](http://owlcollab.github.io/oboformat/doc/obo-syntax.html#4) are strictly enforced. If you are saving an ontology in OBO format from another command, `--check` is always `true`. 

You may run convert (or chain to convert) with the `--check false` option to ignore the errors, e.g.:
```
robot reason --input ont.owl \
  convert --check false --output ont.obo
```

Please note that `--check false` may result in some unintended output. For example, for terms with more than one definition annotation, a definition will be chosen at random.

### Ontology Storage Error

The ontology could not be saved to the specified IRI. The most common reasons are: the IRI is not a valid file path; ROBOT does not have write permissions; there is not enough space on the storage device.

### Options Error

Each command requires a set of options, although it is more common to get a `MISSING INPUT ERROR`. See the command-specific documentation for more details.

### Prefix Load Error

If a prefix is incorrectly formatted, or if the prefix target does not point to an absolute IRI, the prefix cannot be loaded. Prefixes should be formatted as follows:
```
robot -p "robot: http://purl.obolibrary.org/robot/"
```

### Undefined Prefix Error

This error usually occurs when running [`template`](/template). If you use a CURIE in one of the ROBOT template strings as a property (e.g., `A ex:0000115`) but do not define the prefix of that CURIE, ROBOT will be unable to save the ontology file.

To resolve this, make sure all CURIEs use prefixes that are defined. ROBOT includes a set of [default prefixes](https://github.com/ontodev/robot/blob/master/robot-core/src/main/resources/obo_context.jsonld), but you can also define your own prefixes. To include a custom prefix, see [prefixes](/global#prefixes).

When rendering the output, only properties are validated for [QNames](https://en.wikipedia.org/wiki/QName). OWLAPI will allow undefined prefixes to be used in subjects and objects, but the IRI will be the unexpanded version of the CURIE (i.e., the IRI will just be `ex:0000115`).

### Unknown Arg Error

This error message may appear for one of two common reasons:
1. A command or option (the argument) was typed incorrectly, or it not exist.

2. Multiple arguments have been provided to an option that only accepts one argument. For example, the `--term` option for [extract](/extract) only accepts one argument. The following would **not** work:
```
$ robot extract --input foo.owl --term foo:0000001 foo:0000002
UNKNOWN ARG ERROR unknown command or option: foo:0000002
```
Instead, use this (or a `--term-file`):
```
robot extract --input foo.owl --term foo:0000001 --term foo:0000002
```

### Wildcard Error

Any pattern specified with `--inputs` for [merge](/merge) and [unmerge](/unmerge) must be a wildcard pattern, including either `*` (to match any number of characters) or `?` (to match any single character).

---

## Ontology Errors

### Axiom Type Error

Currently, ROBOT can only annotate subClassOf axioms.<br>
See [GitHub issue #67](https://github.com/ontodev/robot/issues/67) for more details.

### Empty Terms Error

For commands that take input terms, ROBOT will check the ontology to ensure those terms exist before proceeding. If the terms do not exist, then there are no tasks to complete so an error message is returned.

If at least one term exists in the ontology, the task can still proceed, but the results may not be as expected. If you run into problems, run these types of commands with the `--verbose` flag, as warnings will be issued when the ontology does not contain a term.

### Null IRI Error

Occurs when an import ontology does not have a valid IRI.

### Syntax Error

This error occurs when an ontology cannot be loaded from file to a TDB dataset (using `--tdb true`). Review your ontology to ensure it is valid RDF/XML or TTL syntax. Jena also supports [some other formats](https://jena.apache.org/documentation/io/#formats).
