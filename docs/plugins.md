# Plugins

The set of ROBOT commands can be extended locally with plugins. A ROBOT plugin is a Java archive file (`.jar`) providing one or more supplementary commands (hereafter called "pluggable commands").

## Using plugins

ROBOT searches for plugins in the following locations:

* the `.robot/plugins` directory in the current user's home directory;
* the directory specified by the Java system property `robot.pluginsdir`, if such a property is set;
* the directory specified by the environment variable `ROBOT_PLUGINS_DIRECTORY`, if such a variable is set in the environment.

Installing a plugin is therefore simply a matter of either

* placing the Jar file into your `~/.robot/plugins` directory, or
* placing the Jar file into any directory and making sure ROBOT knows to search that directory, by setting the `robot.pluginsdir` system property or the `ROBOT_PLUGINS_DIRECTORY` environment variable accordingly.

Importantly, the basename of the Jar file (without the `.jar` extension) within the directory will become part of the name of any pluggable command provided by the plugin. For example, if the file is named `myplugin.jar` and it provides a command called `mycommand`, that command will be available under the name `myplugin:mycommand`. Because of that:

* the name of the Jar file **must** be in lowercase only;
* the name **should** be kept short and simple.

Once the plugin is installed, any pluggable command it provides is immediately available to ROBOT. You can check by calling `robot` without any argument to get it to print the full list of available commands, which will include the commands provided by installed plugins, if any.

## Creating plugins

A pluggable command, just like any other ROBOT command, is a Java class that implements the `org.obolibrary.robot.Command` interface. A plugin is Java archive file that contains at least:

* the compiled Java code ("bytecode") for at least one class implementing the `org.obolibrary.robot.Command` interface, and
* a `META-INF/services/org.obolibrary.robot.Command` file that list all implementations of that interface available in the archive (one per line).

For example, if the command `mycommand` is implemented in a class named `MyCommand` in the package `org.example.myplugin`, the `META-INF/services/org.obolibrary.robot.Command` file must contain a single line `org.example.myplugin.MyCommand`.

In addition to the class implementing the command itself, the archive must also provide any additional classes that may be required for the command to work. This must include classes from any external dependency, unless that dependency also happens to be a dependency of ROBOT itself (for example, there is no need for the archive to contain a copy of the classes of the OWL API, since they are already present in the standard distribution of ROBOT).

A more detailed walkthrough of how to create a plugin is available [here](https://incenp.org/notes/2023/writing-robot-plugins.html).

## Existing plugins

 * [kgcl-java](https://github.com/gouttegd/kgcl-java) - a plugin that provides an `apply` command to apply ontology changes specified using the [Knowledge Graph Change Language](https://w3id.org/kgcl/)
 * [sssom-java](https://incenp.org/dvlpt/sssom-java/) provides functionality for processing SSSOM files in the context of ontologies.
