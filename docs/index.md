# ROBOT is an OBO Tool

[![Build Status](https://travis-ci.org/ontodev/robot.svg?branch=master)](https://travis-ci.org/ontodev/robot)

ROBOT is a tool for working with [Open Biomedical Ontologies](http://obofoundry.org). It's still in development!


# Usage

This code can be used as:

1. a command-line tool
2. a library for any JVM language


## 1. Command Line Tool

The command-line tool is packaged a Java JAR file and can be run via the `robot` shell script.

1. Make sure that you have [Java 8 or later installed](https://www.java.com/en/download/installed.jsp)
2. Download **two** files: the [`robot.jar`](https://build.berkeleybop.org/job/robot/lastSuccessfulBuild/artifact/bin/robot.jar) file (about 25MB) and the right script for your platform:
    - [`robot`](https://build.berkeleybop.org/job/robot/lastSuccessfulBuild/artifact/bin/robot) shell script for Unix, Linux, and Mac OS X
    - [`robot.bat`](https://build.berkeleybop.org/job/robot/lastSuccessfulBuild/artifact/bin/robot.bat) batch script for Windows
3. Put both files on your [system PATH](https://en.wikipedia.org/wiki/PATH_(variable)):
    - on Unix, Linux, and Mac OS X this could be `/usr/local/bin/`
    - on Windows this could be `C:\Windows\`
    - or update your PATH to include your chosen directory
    - NOTE: both files must be in the same directory
4. Now you should be able to run ROBOT from a command line:

        robot help

See [examples/README.md](https://github.com/ontodev/robot/tree/master/examples/README.md) for a tutorial with many example commands.


## 2. Library

The core ROBOT operations are written in plain Java code, and can be used from any language that uses the Java Virtual Machine. The command-line tool is both built on top of the library of operations. You can add use these operations in your own code, or add new operations written in any JVM language.

You can download the latest [`robot.jar`](https://build.berkeleybop.org/job/robot/lastSuccessfulBuild/artifact/bin/robot.jar) file (about 25MB) to include in your projects.


### Java

The library provides a number of Operation classes for manipulating ontologies. The `IOHelper` class contains convenient methods for loading and saving ontologies, and for loading sets of term IRIs. Here's an example of extracting a "core" subset from an ontology:

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
