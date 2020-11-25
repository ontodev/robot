## Contributing

ROBOT is fully open to contributions from anyone! To get started, check the [issues](https://github.com/ontodev/robot/issues) and look for `good first issue`.

### Contents

1. [Getting Started](#getting-started)
2. [Making Changes](#making-changes)
3. [Writing Code](#writing-code)
4. [Writing Unit Tests](#writing-unit-tests)
5. [Writing Integration Tests](#writing-integration-tests)
6. [Documenting Errors](#documenting-errors)

### Getting Started

[Fork](https://help.github.com/articles/fork-a-repo/) the ROBOT repo, and then [clone](https://help.github.com/articles/cloning-a-repository/) it to create a local copy for development. If your desired changes aren't already in the tracker, [create a new issue](https://github.com/ontodev/robot/issues/new).

Before changing anything, make sure the tests pass:

    $ mvn test

### Making Changes

* [Create a new branch](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging) with your topic name (e.g. `extract-bug-fix`)
* Make your changes and write any corresponding tests
* Test your changes (`$ mvn test -Dtest=TestName`; make sure you are in the `robot-core` sub-directory before running tests)
* Ensure ROBOT builds correctly (`$ mvn clean package`)
* Commit and push changes to your fork, then create a [pull request](https://help.github.com/articles/about-pull-requests/)

### Writing Code

The source code consists of two main directories: `robot-command` and `robot-core`. The core files contain code for the bulk of the operation, while the command files add command line functionality. When implementing a new feature, make sure the operation has both `*Operation.java` and `*Command.java` files. The operation methods should not be dependent on the command methods.

ROBOT follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), which is auto-enforced by [google java format](https://github.com/google/google-java-format) during builds. Make sure to add doc comments for any new methods - for specifications see [JavaDocs](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html).

Each new operation must have a corresponding unit test file (see [Writing Unit Tests](#writing-unit-tests)). Each new command must have a corresponding Markdown documentation file with embedded examples (see [Writing Integration Tests](#writing-integration-tests)).

### Writing Unit Tests

Each operation has a set of unit tests built with [JUnit](https://junit.org/junit5/) that are executed when Maven builds the project. These are located in [`robot-core/src/main/test/java/org/obolibrary/robot/`](https://github.com/ontodev/robot/tree/master/robot-core/src/test/java/org/obolibrary/robot). The test file name corresponds to the operation name, e.g. `ExtractOperationTest.java`. Each test class extends the `CoreTest` class:

```
public class ExtractOperationTest extends CoreTest {
  ...
}
```

[`CoreTest.java`](https://github.com/ontodev/robot/blob/master/robot-core/src/test/java/org/obolibrary/robot/CoreTest.java) provides ontology loading and comparison methods for the tests. Each test within a test class is a public method annotated with `@Test` (from JUnit):

```
@Test
public void testExtractStar() {
  ...
}
```

Tests can be structured in different ways as long as there is a [JUnit assertion](http://junit.sourceforge.net/javadoc/org/junit/Assert.html) in tests in the method. We recommend the following - the test loads an ontology, runs an operation, and then compares to a known-good output (the expected output):

```
OWLOntology input = loadOntology("/resource.owl");
OWLOntology output = MyOperation.operation(input);
assertIdentical("/known-good.owl", output);
```

This method uses [`DiffOperation.compare(...)`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/DiffOperation.java#L74) functionality to ensure ontologies are identical. If the assertion fails, the test fails. Make sure your output ontology IRI is the same as the known-good ontology's IRI, otherwise the test will fail.

Resource files (e.g., `resource.owl` and `known-good.owl`) are stored in `robot-core/src/test/resources/`. When using the `loadOntology(...)` and `assertIdentical(...)` methods, do not use the full path. Use the name of the file prefixed by a forward slash, as shown above.

### Writing Integration Tests

Each command is documented in its own Markdown file in [`docs/`](https://github.com/ontodev/robot/tree/master/docs). These files are used to generate the [documentation](http://robot.obolibrary.org/). The embedded examples in these Markdown files are parsed and executed as part of our integration tests, with the results compared against a known-good set of outputs. The [`diff`](http://robot.obolibrary.org/diff) functionality is used when comparing ontology files, and a standard `diff` is used for all other file types. This "executable documentation" serves as end-to-end tests of the `robot.jar` file against a number of examples, and also tests that our documented examples do what they should.

The integration tests are executed with `mvn verify`, which is run on all pull requests via Travis CI.

Embedded examples for testing must use Markdown [indented code blocks](https://github.github.com/gfm/#indented-code-blocks), where each line begins with four spaces. To provide code examples that *will not* be tested, use [fenced code blocks](https://github.github.com/gfm/#fenced-code-blocks) instead, beginning and ending with three backticks (\`\`\`).

Each integration test should have at least two corresponding files in the [`docs/examples/`](https://github.com/ontodev/robot/tree/master/docs/examples) directory: one or more input files and a known-good output file. When writing the example in the documentation, the inputs must be just the file names (no directories) and the output should be the known-good file name prefixed by `results/`:

```
robot my-command --input my-file.owl --output results/known-good.owl
```

The `results/known-good.owl` file will be compared to the file with the same name in the `docs/examples/` directory.

### Documenting Errors

ROBOT implements a custom error-handling system that wraps any Java `Exception`. Each `*Operation.java` and the matching `*Command.java` share a namespace that points to the documentation URL. For example in [`ExtractOperation.java`, line 34](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/ExtractOperation.java#L34):

```
/** Namespace for errors. */
private static final String NS = "extract#";
```

If the `NS` class variable is not defined, add that variable to match the name of the command or operation followed by `#` (see `extract` example above). Some files may point to the `global#` namespace if they are not connected to a specific ROBOT command, such as `CommandLineHelper.java` and `IOHelper.java`.

Each specific error message is a class variable formatted as a concatenation of `NS` plus the name of the error in all uppercase letters, followed by a brief description:

```
private static final String nameOfError = NS + "NAME OF ERROR description of error";
```

This class variable should be passed when throwing an `Exception`. For example, the following error in [`ExtractOperation.java`, line 121](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/ExtractOperation.java#L121) corresponds with [Extract - Unknown Individuals Error](http://robot.obolibrary.org/extract#unknown-individuals-error):

```
throw new IllegalArgumentException(String.format(unknownIndividualsError, individuals));
```

When writing the documentation for the error in `docs/*.md`, the `NAME OF ERROR` portion should exactly match the header of the error on the command's documentation page. ROBOT uses the `NS` and the `NAME OF ERROR` to create a link to the error documentation that will be printed on the command line, along with the error message itself. For example, the `UNKNOWN INDIVIDUALS ERROR` above displays:

```
UNKNOWN INDIVIDUALS ERROR 'x' is not a valid --individuals argument
For details see: http://robot.obolibrary.org/extract#unknown-individuals-error
Use the -vvv option to show the stack trace.
Use the --help option to see usage information.
```

The text below the header in the documentation should be an extended description of what causes the error and how the user can fix it.

If the documentation page does not yet have an error section, place one at the bottom of the page using the 'Error Messages' header (see [Extract - Error Messages](http://robot.obolibrary.org/extract#error-messages) for an example):

```
---

## Error Messages
```
