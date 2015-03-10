# OWLTools2 Examples

This document will walk you through some examples of things you can do with OWLTools2.


## Editing

Many ontology projects use an "edit" file for development. Editors modify this file to add terms and fix bugs, often using Protégé. When ready, the editing version is processed and packaged for release. OWLTools2 provides a range of commands to help with the release process.

We'll use the `edit.owl` file for our running example. It contains a fragment of [Uberon](http://uberon.github.io), a cross-species anatomy ontology with rich logical axioms. You can use Protégé to look around.

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


## Merging Ontologies

TODO


## Reasoning

TODO


## Extracting

TODO


## Makefile

On Unix platforms (including Mac OS X and Linux) you can use the venerable [Make](https://www.gnu.org/software/make/) tool to string together multiple `owltools2` commands. Make can also handle dependencies between build tasks.

TODO


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


