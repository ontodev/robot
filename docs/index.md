---
layout: default
title: ROBOT is an OBO Tool
---

# ROBOT is an OBO Tool

ROBOT is a tool for working with [Open Biomedical Ontologies](http://obofoundry.org). It can be used as a command-line tool or as a library for any language on the Java Virtual Machine.

See our homepage <http://robot.obolibrary.org> for documentation and examples of commands, and our JavaDocs for [`robot-core`](http://www.javadoc.io/doc/org.obolibrary.robot/robot-core/) and [`robot-command`](http://www.javadoc.io/doc/org.obolibrary.robot/robot-command/).


## 1. Command Line Tool

The command-line tool is packaged a Java JAR file and can be run via the `robot` shell script.

1. Make sure that you have [Java 8 or later installed](https://www.java.com/en/download/installed.jsp)
2. Download **two** files: the `robot.jar` file from the [latest release](https://github.com/ontodev/robot/releases/latest), and the right script for your platform:
    - [`robot`](https://github.com/ontodev/robot/raw/master/bin/robot) shell script for Unix, Linux, and Mac OS X
    - [`robot.bat`](https://github.com/ontodev/robot/raw/master/bin/robot.bat) batch script for Windows
3. Put both files on your [system PATH](https://en.wikipedia.org/wiki/PATH_(variable)):
    - on Unix, Linux, and Mac OS X this could be `/usr/local/bin/`
    - on Windows this could be `C:\Windows\`
    - or [update your PATH](https://docs.oracle.com/javase/tutorial/essential/environment/paths.html) to include your chosen directory
    - NOTE: both files must be in the same directory
4. Make sure `robot` is executable:
    - Mac: run the command `chmod u+x robot` (may require `sudo`)
    - Windows: run the command `icacls robot /grant Users:RX /T`
5. Now you should be able to run ROBOT from a command line:

        robot help

## 2. Library

ROBOT is written in Java, and can be used from any language that runs on the Java Virtual Machine. It's available on Maven Central. The code is divided into two parts:

1. [`robot-core`](https://github.com/ontodev/robot/tree/master/robot-core/src/main/java/org/obolibrary/robot) is a library of operations for working with ontologies ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.obolibrary.robot%22%20a%3A%22robot-core%22), [JavaDocs](http://www.javadoc.io/doc/org.obolibrary.robot/robot-core/))
2. [`robot-command`](https://github.com/ontodev/robot/tree/master/robot-command/src/main/java/org/obolibrary/robot) is a command-line interface for using those operations ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.obolibrary.robot%22%20a%3A%22robot-command%22), [JavaDocs](http://www.javadoc.io/doc/org.obolibrary.robot/robot-command/))

You can also download the standalone `robot.jar` file from the [latest release](https://github.com/ontodev/robot/releases/latest) to include in your projects.

The `robot-core` library provides a number of Operation classes for working with ontologies. The `IOHelper` class contains convenient methods for loading and saving ontologies, and for loading sets of term IRIs. Here's an example of extracting a "core" subset from an ontology:

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
