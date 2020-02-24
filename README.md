# ROBOT is an OBO Tool

[![Build Status](https://travis-ci.org/ontodev/robot.svg?branch=master)](https://travis-ci.org/ontodev/robot)
[![Javadocs](https://www.javadoc.io/badge/org.obolibrary.robot/robot-core.svg)](https://www.javadoc.io/doc/org.obolibrary.robot/robot-core)

ROBOT is a tool for working with [Open Biomedical Ontologies](http://obofoundry.org).

### Cite ROBOT

R.C. Jackson, J.P. Balhoff, E. Douglass, N.L. Harris, C.J. Mungall, and J.A. Overton. [ROBOT: A tool for automating ontology workflows](https://rdcu.be/bMnHT). BMC Bioinformatics, vol. 20, July 2019.


### Installation and Usage

Please see <http://robot.obolibrary.org>.


## Build

We use [Maven](http://maven.apache.org) as our build tool. Make sure it's [installed](http://maven.apache.org/download.cgi), then run:

    mvn clean package

This will create a self-contained Jar file in `bin/robot.jar`.

Other build options:

- `mvn clean test` runs JUnit tests with reports in `[module]/target/surefire-reports`
- `mvn clean verify` rebuilds the package and runs integration tests against it, with reports in `[module]/target/failsafe-reports`
- `mvn site` generates Javadoc in `target/site` and `[module]/target/site`

Alternatively, you can use [Docker](https://www.docker.com) with the provided [Dockerfile](Dockerfile) to build and run ROBOT from within a container. First build an image with `docker build --tag robot .` then run ROBOT from the container with the usual command-line arguments: `docker run --rm robot --help`.


## Code Style

We use [Google Java Style](https://google.github.io/styleguide/javaguide.html), automatically enforced with [google-java-format](https://github.com/google/google-java-format) and [fmt-maven-plugin](https://github.com/coveo/fmt-maven-plugin). You may want to use the [styleguide configuration file](https://github.com/google/styleguide) for [Eclipse](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml) or [IntelliJ](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml).


## Design

The library provides a set of Operations and a set of Commands. Commands handle the command-line interface and IO tasks, while Operations focus on manipulating ontologies. Sometimes you will have the pair of an Operation and a Command, but there's no necessity for a one-to-one correspondence between them.

Commands implement the Command interface, which requires a `main(String[] args)` method. Each command can be called via `main`, but the CommandLineInterface class provides a single entry point for selecting between all the available commands. While each Command can run independently, there are shared conventions for command-line options such as `--input`, `--prefix`, `--output`, etc. These shared conventions are implemented in the CommandLineHelper utility class. There is also an IOHelper class providing convenient methods for loading and saving ontologies and lists of terms. A simple Command will consist of a few CommandLineHelper calls to determine arguments, a few IOHelper calls to load or save files, and one call to the appropriate Operation.

Operations are currently implemented with static methods and no shared interface. They should not contain IO or CLI code.

The current implementation is modular but not pluggable. In particular, the CommandLineInterface class depends on a hard-coded list of Commands.


## Term Lists

Many Operations require lists of terms. The IOHelper class defines methods for collecting lists of terms from strings and files, and returning a `Set<IRI>`. Our convention is that a term list is a space-separated list of IRIs or CURIEs with optional comments. The "#" character and everything to the end of the line is ignored. Note that a "#" must start the line or be preceded by whitespace -- a "#" inside an IRI does not start a comment.


## Acknowledgments

The initial version of ROBOT was developed by James A. Overton, based on requirements and designs given by Chris Mungall, Heiko Dietze and David Osumi-Sutherland. This initial version was funded by P41 grant 5P41HG002273-09 to the Gene Ontology Consortium. Current support is from NIH grant 1 R24 HG010032-01, “Services to support the OBO foundry standards” to C. Mungall and B. Peters.


## Copyright

The copyright for ROBOT code and documentation belongs to the respective authors. ROBOT code is distributed under a [BSD3 license](https://github.com/ontodev/robot/blob/master/LICENSE.txt). Our `pom.xml` files list a number of software dependencies, each with its own license.
