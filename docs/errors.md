# Common Error Messages

For command-specific error messages, see the command's doc page.

If you run into any other errors or exceptions not listed, or have any issues with the existing errors, please head over to our <a href="https://github.com/ontodev/robot/issues" target="_blank">GitHub Issues</a> for assistance!

---

## Input Errors

<a name="input-1"/>
### 1. Chained Input Error

[Chained commands](/chaining) take the output of the last command as the input of the next. Because of this, `--input` should only be used with the first command in the chain. This excludes [merge](/merge) and [unmerge](/unmerge), which allow multiple inputs.

<a name="input-2"/>
### 2. Invalid Format Error

When specifying the `--output` (or `--format` for converting), make sure the file has a valid extension. See [convert](/convert) for a current list of ontology formats.

<a name="input-3"/>
### 3. Invalid IRI Error

Many commands involve creating IRIs from provided string representations of the <a href="https://www.w3.org/TR/2010/NOTE-curie-20101216/" target="_blank">CURIE</a> or full IRI. If the provided field is not a string in valid CURIE or IRI format, the actual IRI cannot be created.

When using CURIEs, make sure the prefix is defined, or add it with `--prefix`.

<a name="input-4"/>
### 4. Invalid Prefix Error

Prefixes (added with `--prefix`) should be strings in the following format: `"foo: http://foo/bar#"`. See [Prefixes](/prefixes) for more details on adding prefixes.

<a name="input-5"/>
### 5. Invalid Reasoner Error

[Reason](/reason), [materialize](/materialize), and [reduce](/reduce) all expect `--reasoner` options. All three commands support `structural`, `hermit`, `jfact`, and `elk`. Only the reason command supports `emr`. Click on the command for more details

<a name="input-6"/>
### 6. Missing Input Error

An input ontology must be specified with `--input`. In the case of [merge](/merge) and [unmerge](/unmerge), at least one input is required.

<a name="input-7"/>
### 7. Missing Term(s) Error

Some commands ([extract](/extract) and [filter](/filter)) require terms as input in addition to the ontology. Click on the commands for more details.

---

## Ontology Errors

<a name="ontology-1"/>
### 1. Axiom Type Error

Currently, ROBOT can only annotate subClassOf axioms. See <a href="" target="_blank">GitHub issue #67</a> for more details.

<a name="ontology-2"/>
### 2. Empty Terms Error

For commands that take input terms, ROBOT will check the ontology to ensure those terms exist before proceeding. If the terms do not exist, then there are no tasks to complete so an error message is returned.

If at least one term exists in the ontology, the task can still proceed, but the results may not be as expected. If you run into problems, run these types of commands with the `--verbose` flag, as warnings will be issued when the ontology does not contain a term.
