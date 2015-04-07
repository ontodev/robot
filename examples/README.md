# OWLTools2 Examples

This document will walk you through some examples of things you can do with OWLTools2. These examples are based on the [OBO tutorial](https://github.com/jamesaoverton/obo-tutorial).


## Editing

Many ontology projects use an "edit" file for development. Editors modify this file to add terms and fix bugs, often using Protégé. When ready, the edit file is processed and packaged for release. OWLTools2 provides a range of commands to help with the release process.

We'll use the `edit.owl` file as our running example. It contains a fragment of [Uberon](http://uberon.github.io), a cross-species anatomy ontology with rich logical axioms. You can use Protégé to look around.

What follows is a series of example commands that can be used to process `edit.owl` in various ways. The expected results are also provided in files that you can inspect. The example commands will create new files in a new directory, but with similar names, so that you can compare the results you get with the expected results. We use this system for testing OWLTools2.


## Comparing Files

To compare two text files and check for differences, you can use the Unix `diff` command on Linux or Mac OS X:

    diff file1.txt file2.txt

or on Windows the `FC` command:

    FC file1.txt file2.txt

Any number of graphical diff tools are also available, for example FileMerge is part of Apple's free XCode tools.

Although OWL ontology files are text, it's possible to have two identical ontologies saved to two files with very different structure. OWLTools2 provides a `diff` command that compares two ontologies while ignoring these differences:

    owltools2 diff --left edit.owl --right release.owl

If an `output` is provided then a report will be written with any differences between the two ontologies:

    owltools2 diff --left edit.owl --right release.owl \
      --output results/release-diff.txt

See `release-diff.txt` for an example.


## Merging

OWL ontologies are often divided into several `.owl` files, with `owl:imports` statements to bring them together. Sometimes you want to take all those imports and merge them into a single ontology with a single `.owl` file.

    TODO owltools2 merge --input edit.owl --output merged.owl

You don't need `owl:import` statements: you can merge any number of ontologies by using multiple `--input` arguments. You can also specify the `OntologyIRI` for the output ontology.

    TODO owltools2 merge --input edit.owl --input foo.owl \
      --output merged.owl --output-iri "http://example.com"


## Extracting

The reuse of ontology terms creates links between data, making the ontology and the data more valuable. But often you want to reuse just a subset of terms from a target ontology, not the whole thing.

    TODO owltools2 extract --input uberon.owl --term-file uberon_module.txt \
      --output uberon_module.owl --output-iri "http://foo"


## Reasoning

One of the main benefits of working with OWL is the availability of powerful automated reasoners. There are several reasoners available, and each has different capabilities and characteristics. For this example we'll be using [ELK](https://code.google.com/p/elk-reasoner/), a very fast reasoner that supports the EL subset of OWL 2.

    TODO owltools2 reason --reasoner ELK --input edit.owl --output reasoned.owl


## Annotating

It's important to add metadata to an ontology before releasing it. We use YAML files to describe the ontology, and merge them into the OWL as annotations.

    TODO owltools2 annotate --input reasoned.owl \
      --annotations annotations.yml \
      --output example.owl


## Converting

Ontologies are shared in different formats. The default format used by OWLTools2 is RDF/XML, but there are other OWL formats, RDF formats, and also the OBO file format.

    TODO owltools2 convert --input example.owl --output example.obo


## Chaining

On Unix platforms it's common to "chain" a series of commands, creating "pipeline" that combines several simple commands to accomplish a complex task. This works because most Unix tools communicate with streams of text. Unfortunately this doesn't work as well for OWL ontologies, because they cannot be streamed between commands, but we can achieve a similar result within OWLTools2.

OWLTools2 allows several commands to be chained by using the output ontology as the input to the next step. Here's an example of a full release pipeline using chained commands:

    TODO owltools2 \
      merge --input edit.owl \
      reason --reasoner ELK \
      annotate --annotations annotations.yml --output example.owl \
      convert --output example.obo

Each command has been put on its own line, for clarity. Only the first command has an explicit `--input` argument. The following commands use the output of the previous command as their input. Also notice that the first two commands do not specify an `--output` file. Their output is not saved to the filesystem, only sent to the next command. But the last two commands both specify `--output` files, and their results are saved to different files.

Chained commands are powerful but can be tedious to write out. Consider putting them in a `Makefile`.


## Makefile

On Unix platforms (including Mac OS X and Linux) you can use the venerable [Make](https://www.gnu.org/software/make/) tool to string together multiple `owltools2` commands. Make can also handle dependencies between build tasks.

    TODO make
    TODO make release

When working with Makefiles, be careful with whitespace! Make expects tabs in some places and spaces in others, and mixing them up will lead to unexpected results. Configure your text editor to indicate when tabs or spaces are being used.


## Gradle

[Gradle](http://gradle.org) is similar to Make, but runs on any platform that supports Java.

TODO


## TODO

Here are some other commands we should provide examples for:

- merge
- import, update imports
- reason
- add metadata
- package for release
- extract module
- diff
- get term hierarchy
- convert formats


