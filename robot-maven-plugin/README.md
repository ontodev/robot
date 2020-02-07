# ROBOT Maven Plugins

This directory contains additional `mvn` plugins specific to ROBOT.

Before using these plugins, you must install the JAR:
```
mvn install
```

## ROBOT Plugins

### Update Context

This plugin updates the [`obo_context.jsonld`](https://raw.githubusercontent.com/ontodev/robot/master/robot-core/src/main/resources/obo_context.jsonld) file with the most current prefixes from the [OBO Foundry context file](https://raw.githubusercontent.com/OBOFoundry/OBOFoundry.github.io/master/registry/obo_context.jsonld). You must be connected to the internet to run this plugin.

This can be run from either this directory, or the root `robot` directory:
```
mvn robot:UpdateContext -N
```

The `-N` flag is recommended so that the plugin does not run multiple times for each child module.

--- 

## Creating New Plugins

Add new plugins `src/main/java/org.obolibrary.robot` directory following this filename convention: `{plugin-name}Mojo.java`.

These new Java classes must extend the `AbstractMojo` class and have an `execute()` method. A `@Mojo` annotation is also required to specify the name of the plugin.
```
@Mojo(name = "MyNewPlugin")
public class MyNewPluginMojo extends AbstractMojo {
  
  public void execute() {
    // Do stuff
  }
}
```

For more information on plugin development, [see here](https://maven.apache.org/guides/plugin/guide-java-plugin-development.html).

Then, install the new JAR for this module:
```
mvn install
```

Now you can run your plugin from the command line anywhere within this repo:
```
mvn robot:MyNewPlugin -N
```

