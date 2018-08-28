---
layout: default
title: ROBOT is an OBO Tool
---

# ROBOT is an OBO Tool

ROBOT is a tool for working with [Open Biomedical Ontologies](http://obofoundry.org). It can be used as a command-line tool or as a library for any language on the Java Virtual Machine.

Click on the command names in the sidebar for documentation and examples, and visit our JavaDocs for [`robot-core`](http://www.javadoc.io/doc/org.obolibrary.robot/robot-core/) and [`robot-command`](http://www.javadoc.io/doc/org.obolibrary.robot/robot-command/) for technical details.

For a "how-to" covering the major commands and features of ROBOT, visit our tutorial [located here](https://github.com/rctauber/robot-tutorial).


## 1. Getting Started

The command-line tool is packaged a Java JAR file and can be run via the `robot` shell script. Before getting started, make sure you have [Java 8 or later](https://www.java.com/en/download/) installed. Check by entering `java -version` on the command line.

### Mac & Linux

1. Download the `robot.jar` file from the [latest release](https://github.com/ontodev/robot/releases/latest).
2. Save the [ROBOT shell script](https://github.com/ontodev/robot/raw/master/bin/robot).
    - OR enter `curl https://raw.githubusercontent.com/ontodev/robot/master/bin/robot > robot` in the same directory as `robot.jar` to download it from the terminal.
3. Put both files on your [system PATH](https://en.wikipedia.org/wiki/PATH_(variable)) in the same directory.
    - this could be `/usr/local/bin/`
    - OR [update your PATH](https://docs.oracle.com/javase/tutorial/essential/environment/paths.html) to include the new directory. Follow the Solaris/Linux directions for Mac OS, except instead of updating `.bashrc`, you will need to update your `.bash_profile`.
4. Make sure `robot` is executable by running `sudo chmod u+x robot` from the terminal in the same directory. This will require you to enter you password.
5. Now you should be able to run ROBOT from a command line:

        robot help

### Windows

1. Download the `robot.jar` file from the [latest release](https://github.com/ontodev/robot/releases/latest).
2. Save the [ROBOT batch script](https://github.com/ontodev/robot/raw/master/bin/robot.bat).
    - OR enter `echo java -jar %~dp0robot.jar %* > robot.bat` in the same directory as `robot.jar` to create the batch script.
3. Put both files on your [system PATH](https://en.wikipedia.org/wiki/PATH_(variable)) in the same directory.
    - this could be `C:\Windows\`
    - OR [update your PATH](https://docs.oracle.com/javase/tutorial/essential/environment/paths.html) to include the new directory.
4. Make sure `robot.bat` is executable by running `icacls robot.bat /grant Users:RX /T` from the command prompt in the same directory.
5. Now you should be able to run ROBOT from a command line:

        robot help

## 2. Using the Library

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
