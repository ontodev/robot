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