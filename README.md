# OWLTools2 Prototype

This repository contains a number of experiments, with the goal of developing a new suite of tools and techniques for Open Biomedical Ontology development.

NOTE: "owltools2" is just a placeholder name.

One part of this experiment is "Documentation-Driven Development": imagine a future where the perfect tool exists, write documentation describing it, then write code to make that future vision a reality. WARNING: Even though this documentation is written in the present tense, many of the features described here are not yet implemented!


# Usage

This code can be used as:

1. a command-line tool -- for single tasks
2. a Gradle plugin -- for multiple tasks
3. a library for any JVM language -- for embedding your own applications


## 1. Command Line Tool

The command-line tool is packaged a Jar file and can be run via the `owltools2` shell script. Currently implemented commands:

    owltools2 help
    owltools2 extract --input examples/test.owl --term-file examples/terms.txt --output examples/out.owl --output-iri "http://example.com"

Other ideas for commands:

    owltools2 merge --input edit.owl --output merged.owl
    owltools2 merge -i first.owl -i second.owl --output merged.owl
    owltools2 reason merged.owl --reasoner elk --output reasoned.owl
    export BASEIRI="https://github.org/ontodev/owltools2-experiment/examples/"
    owltools2 extract --input edit.owl --term-file core.txt \\
      --output core.owl --output-iri "$BASEIRI/core.owl"
    owltools2 convert --input example.owl --output example.obo
    owltools2 diff --left ontology1.owl --right ontology2.owl \\
      --output report.txt
    owltools2 check-consistent -i edit.owl
    owltools2 check-satisfiable -i edit.owl
    owltools2 check-style -i release.owl
    owltools2 remove-redundant -i edit.owl
    owltools2 report --reportdir reports/
    owltools2 add-ontology-metadata --template template.yaml \\
      --input before.owl --output after.owl
    owltools2 add-class-metadata
    owltools2 update --input uberon.owl --sparql update.rq --overwrite
    owltools2 convert --input uberon.owl --output uberon.obo --format obo-basic
    owltools2 strip-axioms --input uberon.owl --output uberon.small.owl \\
      --properties-file props.txt
    owltools2 deploy ???
    owltools2 remove --terms terms.txt --input test.owl --overwrite
    owltools2 qtt ???
    owltools2 mireot ???
    owltools2 relabel --input edit.owl --terms terms.csv --overwrite


## 2. Gradle Plugin

[Gradle](http://gradle.org) is a build tool that takes the best features from [Ant](https://ant.apache.org) and [Maven](http://maven.apache.org), and strives for greater usability and power.

Make sure that Gradle is [installed](https://gradle.org/docs/current/userguide/installation.html). (On Mac OS X, [homebrew](http://brew.sh) is convenient: `brew install gradle`.)

Then define a `build.gradle` file like this one:

TODO: I don't think this example is idiomatic Gradle. Suggestions are welcome! The Gradle Java plugin uses a lot of more convention, but I think that ontology projects are too

    plugins {
      id "owltools2" version "0.0.1"
    }

    Reasoner elk = new Reasoner(name: 'ELK')
    Ontology ontology
    Ontology reasoned

    task reason (dependsOn: update) << {
      ontology = loadOntology('foo.owl')
      reasoned = reasonOntology(ontology, elk)
      saveOntology(reasoned, 'reasoned.owl')
    }

    task core (dependsOn: reason) << {
      Set<String> terms = loadTerms('core.txt')
      Ontology core = extractOntology(reasoned, terms)
      saveOntology(core, 'core.owl')
    }

    task subsets (dependsOn: core)

    task release (dependsOn: [subsets, reports])

Finally, run `gradle build` to execute these commands.


## 3. Library

The core operations are written in plain Java code, and can be called from any language that uses the Java Virtual Machine. The command-line tool and Gradle plugin are both built on top of the library of operations. You can add use these operations in your own code, or add new operations written in any JVM language.

TODO: Examples of using operations and implementing new operations, in multiple languages: Java, Clojure, Groovy, Scala.


### Java

The library provides a number of Operation classes for manipulating ontologies. The `IOHelper` class contains convenient static methods for loading and saving ontologies, and for loading sets of term IRIs. Here's an example of extracting a "core" subset from an ontology:

    IOHelper ioHelper = new IOHelper();
    OWLOntology full = ioHelper.loadOntology("ontology.owl");
    Set<IRI> coreTerms = ioHelper.loadTerms("coreTerms.txt");
    OWLOntology core = ExtractOperation.extract(full, coreTerms);
    ioHelper.saveOntology(core, "core.owl");

Alternatively:

    IOHelper ioHelper = new IOHelper();
    ioHelper.saveOntology(
      ExtractOperation.extract(
        ioHelper.loadOntology("ontology.owl"),
        ioHelper.loadTerms("coreTerms.txt"),
        IRI.create("http://example.com")
      ),
      'core.owl'
    );


# Build

We use [Maven](http://maven.apache.org) as our build tool. Make sure it's [installed](http://maven.apache.org/download.cgi), then run:

    mvn package

This will create a self-contained Jar file in `bin/owltools2.jar`.

Other build options:

- `mvn site` generates reports (including Checkstyle) in `target/site`


# Design

The library provides a set of Operations and a set of Commands. Commands handle the command-line interface and IO tasks, while Operations focus on manipulating ontologies. Sometimes you will have the pair of an Operation and a Command, but there's no necessity for a one-to-one correspondence between them.

Commands implements the Command interface, which requires a `main(String[] args)` method. Each command can be called via `main`, but the CommandLineInterface class provides a single entry point for selecting between all the available commands. While each Command can run independently, there are shared conventions for command-line options such as `--from`, `--terms`, `--output`, etc. These shared conventions are implemented in the CommandLineHelper utility class. There is also an IOHelper class providing convenient methods for loading and saving ontologies and lists of terms. A simple Command will consist of a few CommandLineHelper calls to determine arguments, a few IOHelper calls to load or save files, and one call to the appropriate Operation.

Operations are currently implemented with static methods and no shared interface. They should not contain IO or CLI code.

The current implementation is modular but not pluggable. In particular, the CommandLineInterface class depends on a hard-coded list of Commands.


## Term Lists

Many Operations require lists of terms. The IOHelper class defines methods for collecting lists of terms from strings and files, and returning a `Set<IRI>`. Our convention is that a term list is a space-separated list of IRIs or CURIEs (not yet implemented!) with optional comments. The "#" character and everything to the end of the line is ignored. Note that a "#" must start the line or be preceded by whitespace -- a "#" inside an IRI does not start a comment.


