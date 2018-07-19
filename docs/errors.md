# Common Error Messages

For command-specific error messages, see the command's doc page.

If you run into any other errors or exceptions not listed, or have any issues with the existing errors, please head over to our <a href="https://github.com/ontodev/robot/issues" target="_blank">GitHub Issues</a> for assistance!

---

## Command & Option Errors

### Boolean Value Error

The command line option provided only accepts `true` or `false` as input.

### Chained Input Error

[Chained commands](/chaining) take the output of the last command as the input of the next. Because of this, `--input` should only be used with the first command in the chain. This excludes [merge](/merge) and [unmerge](/unmerge), which allow multiple inputs.

### File Does Not Exist Error

One of the required files (often `--input`) could not be found. This is often caused by a typo.

### Invalid Format Error

When specifying the `--output` (or `--format` for converting), make sure the file has a valid extension. See [convert](/convert) for a current list of ontology formats.

### Invalid Ontology File Error

ROBOT was expecting an ontology file, and the file exists, but is not in a recognized format. Adding the `-vvv` option will print a stack trace that shows how the underlying OWLAPI library tried to parse the ontology file. This will include details and line numbers that can help isolate the problem.

### Invalid Ontology IRI Error

Either the ontology could not be loaded from the provided IRI, or the file at that IRI was in an unrecognized format. Adding the `-vvv` option may provide helpful details.

### Invalid Ontology Stream Error

Either the ontology could not be loaded from the provided input stream, or the input stream was in an unrecognized format. Adding the `-vvv` option may provide helpful details.

### Invalid IRI Error

Many commands involve creating IRIs from provided string representations of the <a href="https://www.w3.org/TR/2010/NOTE-curie-20101216/" target="_blank">CURIE</a> or full IRI. If the provided field is not a string in valid CURIE or IRI format, the actual IRI cannot be created.

When using CURIEs, make sure the prefix is defined, or add it with `--prefix`.

### Invalid Prefix Error

Prefixes (added with `--prefix`) should be strings in the following format: `"foo: http://foo/bar#"`. See [Prefixes](/prefixes) for more details on adding prefixes.

### Invalid Reasoner Error

[Reason](/reason), [materialize](/materialize), and [reduce](/reduce) all expect `--reasoner` options. All three commands support `structural`, `hermit`, `jfact`, and `elk`. Only the reason command supports `emr`. Click on the command for more details

### JSON-LD Context Creation Error

There was an error creating a JSON-LD context. This could be caused by a bad prefix.

### JSON-LD Context Parsing Error

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

### Ontology Storage Error

The ontology could not be saved to the specified IRI. The most common reasons are: the IRI is not a valid file path; ROBOT does not have write permissions; there is not enough space on the storage device.

### Options Error

Each command requires a set of options, although it is more common to get a `MISSING INPUT ERROR`. See the command-specific documentation for more details.

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
