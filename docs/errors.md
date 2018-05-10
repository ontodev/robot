# Common Error Messages

For command-specific error messages, see the command's doc page.

If you run into any other errors or exceptions not listed, or have any issues with the existing errors, please head over to our <a href="https://github.com/ontodev/robot/issues" target="_blank">GitHub Issues</a> for assistance!

---

## Command & Option Errors

### Boolean Value Error

The command line option provided only accepts `true` or `false` as input.

### Chained Input Error

[Chained commands](/chaining) take the output of the last command as the input of the next. Because of this, `--input` should only be used with the first command in the chain. This excludes [merge](/merge) and [unmerge](/unmerge), which allow multiple inputs.

### Invalid Format Error

When specifying the `--output` (or `--format` for converting), make sure the file has a valid extension. See [convert](/convert) for a current list of ontology formats.

### Invalid IRI Error

Many commands involve creating IRIs from provided string representations of the <a href="https://www.w3.org/TR/2010/NOTE-curie-20101216/" target="_blank">CURIE</a> or full IRI. If the provided field is not a string in valid CURIE or IRI format, the actual IRI cannot be created.

When using CURIEs, make sure the prefix is defined, or add it with `--prefix`.

### Invalid Prefix Error

Prefixes (added with `--prefix`) should be strings in the following format: `"foo: http://foo/bar#"`. See [Prefixes](/prefixes) for more details on adding prefixes.

### Invalid Reasoner Error

[Reason](/reason), [materialize](/materialize), and [reduce](/reduce) all expect `--reasoner` options. All three commands support `structural`, `hermit`, `jfact`, and `elk`. Only the reason command supports `emr`. Click on the command for more details

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
See <a href="" target="_blank">GitHub issue #67</a> for more details.

### Empty Terms Error

For commands that take input terms, ROBOT will check the ontology to ensure those terms exist before proceeding. If the terms do not exist, then there are no tasks to complete so an error message is returned.

If at least one term exists in the ontology, the task can still proceed, but the results may not be as expected. If you run into problems, run these types of commands with the `--verbose` flag, as warnings will be issued when the ontology does not contain a term.
