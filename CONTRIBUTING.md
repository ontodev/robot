## Contributing

ROBOT is fully open to contributions from anyone! To get started, check the [issues](https://github.com/ontodev/robot/issues) and look for `good first issue`.

### Getting Started

[Fork](https://help.github.com/articles/fork-a-repo/) the ROBOT repo, and then [clone](https://help.github.com/articles/cloning-a-repository/) it to create a local copy for development. If your desired changes aren't already in the tracker, [create a new issue](https://github.com/ontodev/robot/issues/new).

Before changing anything, make sure the tests pass:

    $ mvn test

### Making Changes

  * [Create a new branch](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging) with your topic name (e.g. `extract-bug-fix`)
  * Make your changes and write any corresponding tests
  * Test your changes (`$ mvn test -Dtest=TestName`)
  * Ensure ROBOT builds correctly (`$ mvn clean package`)
  * Commit and push changes to your fork, then create a [pull request](https://help.github.com/articles/about-pull-requests/)

### Writing Code

The source code consists of two main directories: `robot-command` and `robot-core`. The core files contain code for the bulk of the operation, while the command files add command line functionality. When implementing a new feature, make sure the operation has both `*Operation.java` and `*Command.java` files. The operation methods should not be dependent on the command methods.

ROBOT follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), which is auto-enforced by [google java format](https://github.com/google/google-java-format) during builds. Make sure to add doc comments for any new methods - for specifications see [JavaDocs](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html).

### Documenting Errors

ROBOT implements a custom error-handling system that wraps any Java `Exception`. Each `*Operation.java` and the matching `*Command.java` share a namespace that points to the documentation URL. For example in [`ExtractOperation.java`, line 34](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/ExtractOperation.java#L34):

	/** Namespace for errors. */
	private static final String NS = "extract#";

If the `NS` class variable is not defined, add that variable to match the name of the command or operation followed by `#` (see `extract` example above). Some files may point to the `global#` namespace if they are not connected to a specific ROBOT command, such as `CommandLineHelper.java` and `IOHelper.java`.

Each specific error message is a class variable formatted as a concatenation of `NS` plus the name of the error in all uppercase letters, followed by a brief description:

	private static final String nameOfError = NS + "NAME OF ERROR description of error";

This class variable should be passed when throwing an `Exception`. For example, the following error in [`ExtractOperation.java`, line 121](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/ExtractOperation.java#L121) corresponds with [Extract - Unknown Individuals Error](http://robot.obolibrary.org/extract#unknown-individuals-error):

	throw new IllegalArgumentException(String.format(unknownIndividualsError, individuals));

When writing the documentation for the error in `docs/*.md`, the `NAME OF ERROR` portion should exactly match the header of the error on the command's documentation page. ROBOT uses the `NS` and the `NAME OF ERROR` to create a link to the error documentation that will be printed on the command line, along with the error message itself. For example, the `UNKNOWN INDIVIDUALS ERROR` above displays:

	UNKNOWN INDIVIDUALS ERROR 'x' is not a valid --individuals argument
	For details see: http://robot.obolibrary.org/extract#unknown-individuals-error
	Use the -vvv option to show the stack trace.
	Use the --help option to see usage information.

The text below the header in the documentation should be an extended description of what causes the error and how the user can fix it.

If the documentation page does not yet have an error section, place one at the bottom of the page using the 'Error Messages' header (see [Extract - Error Messages](http://robot.obolibrary.org/extract#error-messages) for an example):

	---

	## Error Messages
