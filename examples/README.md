# ROBOT Examples

This document will walk you through some examples of things you can do with ROBOT. These examples are based on the [OBO tutorial](https://github.com/jamesaoverton/obo-tutorial).

**NOTE**: In the following examples the `\` (backslash) character at the end of a line indicates that the command is continued on the next line. You can always delete the `\` characters and enter the command as a single line. On Windows you should use `^` (caret) character instead of `\` to indicate that the command continues on the next line.

### Commands
* [Comparing](https://github.com/ontodev/robot/tree/master/examples#comparing-files)
* [Merging](https://github.com/ontodev/robot/tree/master/examples#merging)
* [Unmerging](https://github.com/ontodev/robot/tree/master/examples#unmerging)
* [Filtering](https://github.com/ontodev/robot/tree/master/examples#filtering)
* [Removing](https://github.com/ontodev/robot/tree/master/examples#removing)
* [Extracting](https://github.com/ontodev/robot/tree/master/examples#extracting)
* [Querying](https://github.com/ontodev/robot/tree/master/examples#querying)
* [Reasoning](https://github.com/ontodev/robot/tree/master/examples#reasoning)
* [Relaxing](https://github.com/ontodev/robot/tree/master/examples#relaxing-equivalence-axioms)
* [Reducing](https://github.com/ontodev/robot/tree/master/examples#reducing-graph)
* [Materialization](https://github.com/ontodev/robot/tree/master/examples#materialization)
* [Annotating](https://github.com/ontodev/robot/tree/master/examples#annotating)
* [Converting](https://github.com/ontodev/robot/tree/master/examples#converting)
* [Mirroring](https://github.com/ontodev/robot/tree/master/examples#mirroring)
* [Templating](https://github.com/ontodev/robot/tree/master/examples#templating)
* [Validating](https://github.com/ontodev/robot/tree/master/examples#validating-profiles)
* [Repairing](https://github.com/ontodev/robot/tree/master/examples#repairing-ontologies)

### Tips
* [Chaining Commands](https://github.com/ontodev/robot/tree/master/examples#chaining)
* [Prefixes](https://github.com/ontodev/robot/tree/master/examples#prefixes)
* [Makefile](https://github.com/ontodev/robot/tree/master/examples#makefile)
* [Gradle](https://github.com/ontodev/robot/tree/master/examples#gradle)


## Editing

Many ontology projects use an "edit" file for development. Editors modify this file to add terms and fix bugs, often using Protégé. When ready, the edit file is processed and packaged for release. ROBOT provides a range of commands to help with the release process.

We'll use the `edit.owl` file as our running example. It contains a fragment of [Uberon](http://uberon.github.io), a cross-species anatomy ontology with rich logical axioms. You can use [Protégé](https://protege.stanford.edu) to look around.

What follows is a series of example commands that can be used to process `edit.owl` in various ways. The expected results are also provided in files that you can inspect. The example commands will create new files in a new directory, but with similar names, so that you can compare the results you get with the expected results. We use this system for testing ROBOT.


## A Note on Catalog Files

ROBOT is built on [OWLAPI](http://owlcs.github.io/owlapi/). We use the same version of OWLAPI as the current Protégé release. OWLAPI supports "catalog" files (e.g. `catalog-v001.xml`) that can specify when a local file should be used instead of a remote file. This is very useful for ontology development, and many ontology projects include a catalog file.

ROBOT uses catalog files in the same way that Protégé does. When loading a local file, ROBOT will look for a catalog file in the same directory, and use it if found. When loading a remote file (e.g. over HTTP) ROBOT does not look for a catalog file. So if you're using a catalog file in your project, it's best to work in a local directory.


## Comparing Files

To compare two text files and check for differences, you can use the Unix `diff` command on Linux or Mac OS X:

    diff file1.txt file2.txt

or on Windows the `FC` command:

    FC file1.txt file2.txt

Any number of graphical diff tools are also available, for example FileMerge is part of Apple's free XCode tools.

Although OWL ontology files are text, it's possible to have two identical ontologies saved to two files with very different structure. ROBOT provides a `diff` command that compares two ontologies while ignoring these differences:

    robot diff --left edit.owl --right release.owl

If an `output` is provided then a report will be written with any differences between the two ontologies:

    robot diff --left edit.owl --right release.owl --output results/release-diff.txt

See `release-diff.txt` for an example.


## Merging

OWL ontologies are often divided into several `.owl` files, with `owl:imports` statements to bring them together. Sometimes you want to take all those imports and merge them into a single ontology with a single `.owl` file.

    robot merge --input edit.owl --output results/merged.owl

You don't need `owl:import` statements: you can merge any number of ontologies by using multiple `--input` arguments. All the ontologies and their imports are merged into the first ontology.

    robot merge --input edit.owl --input foo.owl --output results/merged2.owl

## Unmerging

A merge can be 'undone' using the unmerge command. This can be useful if we want to perform some operation (for example, reasoning) on a merged file, but later subtract the results.

This will merge in foo, and then subtract out foo from the merged ontology.

    robot merge --input edit.owl --input foo.owl unmerge --input foo.owl --output results/unmerged.owl

## Filtering

Some ontologies contain more axioms than you want to use. You can use the `filter` command to keep only those axioms with ObjectProperties that you specify. For example, Uberon contains rich logical axioms, but sometimes you only want to keep the 'part of' and 'has part' relations. Here we start with a fragment of Uberon and filter for parthood relations:

    robot filter --input uberon_fragment.owl --term obo:BFO_0000050 --term obo:BFO_0000051 --output results/filtered.owl

## Removing

While `filter` specifies what to keep, `remove` allows removal of given entities (classes, individuals, and properties). For example, to remove the `has_part` (BFO:0000051) object property and all axioms that use it:
```
robot remove --input edit.owl --entity BFO:0000051 --output removed.owl
```

More specifically, you can specify removal with non-generic types:
  * `--class <arg>`
  * `--individual <arg>`
  * `--object-property <arg>`
  * `--annotation-property <arg>`
  * `--datatype-property <arg>`

Multiple entities can be removed by providing the path to a text file containing each entity's CURIE on a separate line:
```
robot remove --input edit.owl --entities ids.txt --output removed.owl
```

Other options:
  * Remove all individuals with `--all-individuals`
  * Remove all descendant classes of a class with `--descendant-classes <arg>`
  * Remove all anonymous superclasses of a class with `--anonymous-superclasses <arg>`

## Extracting

The reuse of ontology terms creates links between data, making the ontology and the data more valuable. But often you want to reuse just a subset of terms from a target ontology, not the whole thing. Here we take the filtered ontology from the previous step and extract a "STAR" module for the term 'adrenal cortex' and its supporting terms:

    robot extract --method STAR \
        --input filtered.owl \
        --term-file uberon_module.txt \
        --output results/uberon_module.owl

NOTE: The `extract` command works on the input ontology, not its imports. To extract from imports you should first `merge`.

The `--method` options fall into two groups: Minimal Information for Reuse of External Ontology Term (MIREOT) and Syntactic Locality Module Extractor (SLME).

- MIREOT: extract a simple hierarchy of terms
- STAR: use the SLME to extract a fixpoint-nested module
- TOP: use the SLME to extract a top module
- BOT: use the SLME to extract a bottom module

For MIREOT, both "upper" (ancestor) and "lower" (descendant) limits must be specified, like this:

    robot extract --method MIREOT \
        --input uberon_fragment.owl \
        --upper-term "obo:UBERON_0000465" \
        --lower-term "obo:UBERON_0001017" \
        --lower-term "obo:UBERON_0002369" \
        --output results/uberon_mireot.owl

For more details see:

- [MIREOT](http://dx.doi.org/10.3233/AO-2011-0087)
- [SLME](http://owlcs.github.io/owlapi/apidocs_4/uk/ac/manchester/cs/owlapi/modularity/SyntacticLocalityModuleExtractor.html)
- [ModuleType](http://owlcs.github.io/owlapi/apidocs_4/uk/ac/manchester/cs/owlapi/modularity/ModuleType.html)


## Querying

ROBOT can execute [SPARQL](https://www.w3.org/TR/rdf-sparql-query/)
queries against an ontology. The `verify` command described below is similar, but is used to test that an ontology conforms to the specified rules.

The `query` command can execute SPARQL ASK, SELECT, and CONSTRUCT queries by using the `--query` option with two arguments: a query file and an output file. The output file will only be written if the query returns some results. The output format will be inferred from the output file extension, or you can use the `--format` option.

ASK always produces `true` or `false`.

SELECT produces a table, if there are any results, defaulting to CSV format. For example:

    robot query --input nucleus.owl --query cell_part.sparql results/cell_part.csv

This produces [`cell_part.csv`](cell_part.csv):

    class,label
    http://purl.obolibrary.org/obo/GO_0044424,intracellular part
    http://purl.obolibrary.org/obo/GO_0005622,intracellular

CONSTRUCT produces RDF data, if there are any results, defaulting to Turtle format:

    robot query --format ttl --input nucleus.owl --query part_of.sparql results/part_of.ttl

This produces [`part_of.ttl`](part_of.ttl).

Instead of specifying one or more pairs (query file, output file), you can specify a single `--output-dir` and use the `--queries` option to provide one or more queries of any type. Each output file will be written to the output directory with the same base name as the query file that produced it. For example the `foo.sparql` query file will produce the `foo.csv` file. The output directory must exist.

    robot query --input nucleus.owl --queries cell_part_ask.sparql --output-dir results/


## Verify

The `verify` command is used to check an ontology for violations of rules. Each rule is expressed as a SPARQL SELECT query that matches violations of the rule. If the query produces any results, `verify` will exit with an error message that reports the violations. If the ontology conforms to the rule, the query should find no violations, and the `verify` command will succeed.

You can use `verify` in two ways:

1. `--query` query-file output-file
2. `--queries` query-file-1 query-file-2 ... `--output-dir` some-directory

For example:

```
robot verify --input asserted-equiv.owl --queries equivalent.sparql --output-dir results/
```

Should output as a response:

    Rule /ontodev/robot/examples/equivalent.sparql: 1 violation(s)
    first,second,firstLabel,secondLabel
    http://purl.obolibrary.org/obo/TEST_A,http://purl.obolibrary.org/obo/TEST_B,,

And the CSV file `results/equivalent.csv` should have:

    first,second,firstLabel,secondLabel
    http://purl.obolibrary.org/obo/TEST_A,http://purl.obolibrary.org/obo/TEST_B,,


## Reasoning

One of the main benefits of working with OWL is the availability of powerful automated reasoners. There are several reasoners available, and each has different capabilities and characteristics. For this example we'll be using [ELK](https://code.google.com/p/elk-reasoner/), a very fast reasoner that supports the EL subset of OWL 2.

    robot reason --reasoner ELK --input ribosome.owl --output results/reasoned.owl

It's also possible to place the new inferences in their own ontology:

    robot reason --reasoner ELK --create-new-ontology true --input ribosome.owl --output results/new_axioms.owl

## Relaxing equivalence axioms

Robot can be used to relax Equivalence Axioms to weaker SubClassOf axioms. The resulting axioms will be redundant with the stronger equivalence axioms, but may be useful for applications that only consume SubClassOf axioms

    robot relax  --input ribosome.owl --output results/relaxed.owl

## Reducing Graph

Robot can be used to 'reduce' (i.e. remove redundant subClassOf axioms), independent of this previous step.

    robot reduce --reasoner ELK --input ribosome.owl --output results/reduced.owl

## Materialization

Robot can materialize all parent superclass and superclass expressions using the expression materializing reasoner, which wraps an existing OWL Reasoner

    robot materialize --reasoner ELK --input emr_example.obo --term obo:BFO_0000050  --output results/emr_output.obo

This operation is similar to the reason command, but will also assert parents of the form `P some D`, for all P in the set passed in via `--term`

This can be combined with filter and reduce to create an ontology subset that is complete

    robot materialize --reasoner ELK --input emr_example.obo --term BFO:0000050 filter -t BFO:0000050 reduce --output results/emr_reduced.obo

## Annotating

It's important to add metadata to an ontology before releasing it, and to update the ontology version IRI.

    robot annotate --input edit.owl \
      --ontology-iri "https://github.com/ontodev/robot/examples/annotated.owl" \
      --version-iri "https://github.com/ontodev/robot/examples/annotated-1.owl" \
      --annotation rdfs:comment "Comment" \
      --annotation rdfs:label "Label" \
      --annotation-file annotations.ttl \
      --output results/annotated.owl


## Converting

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

## Mirroring

Many ontologies make use of `owl:imports` to bring in other ontologies, or portions of other ontologies. Large import chains involving multiple large ontologies are more prone to run-time failure due to network errors or latency. It can therefore be beneficial to mirror or cache an external ontology's import chain locally. This can be though of as analogous to what happens with standard dependency management tools for software development.

The following command will mirror a local

    robot mirror -i test.owl -d results/my-cache -o results/my-catalog.xml

This will generate a directory `results/my-cache/purl.obolibrary.org/obo/ro/` with the imported ontologies as files. The file my-catalog.xml is a generated XML catalog mapping the source URIs to local files.

## Templating

ROBOT can also create ontology files from templates. See [template.md](../docs/template.md) for details. Here's a quick example:

    robot template --template template.csv \
      --prefix "ex: http://example.com/" \
      --ontology-iri "https://github.com/ontodev/robot/examples/template.owl" \
      --output results/template.owl


## Validating Profiles

OWL 2 has a number of [profiles](https://www.w3.org/TR/owl2-profiles/) that strike different balances between expressive power and reasoning efficiency. ROBOT can validate an input ontology against a profile (EL, DL, RL, QL, and Full) and generate a report. For example:

    robot validate-profile -profile EL \
      --input merged.owl \
      --output results/merged-validation.txt

## Repairing Ontologies

ROBOT can repair certain problems encountered in ontologies. So far this is limited to updating axioms pointing to deprecated classes with their replacement class

    robot repair \
      --input need-of-repair.owl \
      --output results/repaired.owl

## Reporting

    robot report \
      --input edit.owl \
      --output report.yaml


## Chaining

On Unix platforms it's common to "chain" a series of commands, creating "pipeline" that combines several simple commands to accomplish a complex task. This works because most Unix tools communicate with streams of text. Unfortunately this doesn't work as well for OWL ontologies, because they cannot be streamed between commands, but we can achieve a similar result within ROBOT.

ROBOT allows several commands to be chained by using the output ontology as the input to the next step. Here's an example of a full release pipeline using chained commands:

    robot \
      merge --input edit.owl \
      reason --reasoner ELK \
      annotate --annotation-file annotations.ttl --output results/example.owl \
      convert --output results/example.obo

Each command has been put on its own line, for clarity. Only the first command has an explicit `--input` argument. The following commands use the output of the previous command as their input. Also notice that the first two commands do not specify an `--output` file. Their output is not saved to the filesystem, only sent to the next command. But the last two commands both specify `--output` files, and their results are saved to different files.

Chained commands are powerful but can be tedious to write out. Consider putting them in a `Makefile`.


## Prefixes

Terms in OBO and OWL are identified using [IRIs](https://en.wikipedia.org/wiki/Internationalized_resource_identifier) (Internationalized Resource Identifiers), which generalize the familiar addresses for web pages. IRIs have many advantages, but one of their disadvantages is that they can be pretty long. So we have standard ways to abbreviate IRIs in a particular context by specifying **prefixes**. For example, Turtle files start with `@prefix` statements, SPARQL queries start with `PREFIX` statements, and JSON-LD data includes a `@context` with prefixes.

For robot we use the JSON-LD format. See `robot-core/src/main/resources/obo_context.jsonld` for the JSON-LD context that is used by default. It includes common, general linked-data prefixes, and prefixes for all the OBO library projects.

If you do not want to use the defaults, you can use the `--noprefixes` option. If you want to replace the defaults, use the `--prefixes` option and specify your JSON-LD file. Whatever your choice, you can add more prefixes using the `--prefix` option, as many times as you like. Finally, you can print or save the current prefixes using the `export-prefixes` command. Here are some examples:

    robot --noprefixes --prefix "foo: http://foo#" \
      export-prefixes --output results/foo.json

    robot --prefixes foo.json -p "bar: http://bar#" -p "baz: http://baz#" \
      export-prefixes

The various prefix options can be used with any command. When chaining commands, you usually want to specify all the prefix options first, so that they are used "globally" by all commands. But you can also use prefix options for single commands. Here's a silly example with a global prefix "foo" and a local prefix "bar". The first export includes both the global and local prefixes, while the second export includes only the global prefix.

    robot --noprefixes --prefix "foo: http://foo#" \
      export-prefixes --prefix "bar: http://bar#" \
      export-prefixes


## Makefile

On Unix platforms (including Mac OS X and Linux) you can use the venerable [Make](https://www.gnu.org/software/make/) tool to string together multiple `robot` commands. Make can also handle dependencies between build tasks.

    TODO make
    TODO make release

When working with Makefiles, be careful with whitespace! Make expects tabs in some places and spaces in others, and mixing them up will lead to unexpected results. Configure your text editor to indicate when tabs or spaces are being used.


## Gradle

[Gradle](http://gradle.org) is similar to Make, but runs on any platform that supports Java.

TODO


## TODO

Here are some other commands we should provide examples for:

- import, update imports
- add metadata
- package for release
- get term hierarchy
- convert formats

