# Python

ROBOT is written in Java, like OWLAPI and Protégé, and runs on the Java Virtual Machine (JVM). Using [Py4J](https://www.py4j.org/) it's possible to control ROBOT from Python. There are two main ways to do this: (1) launch the ROBOT process from Python, or (2) start ROBOT from the command line and then connect to it from Python.

Both approaches require that the Py4j Python module is installed, see <http://py4j.org/install.html>.


### Starting ROBOT from Python

You can start ROBOT from Python using [`py4j.java_gateway.launch_gateway(...)`](https://www.py4j.org/py4j_java_gateway.html#py4j.java_gateway.launch_gateway). You must specify `jarpath={path/to/robot.jar}`, `classpath=org.obolibrary.robot.PythonOperation`, and `port={port}` (e.g., `port=25333`) in the args. All other `launch_gateway` args are optional.

```python
from py4j.java_gateway import launch_gateway

launch_gateway(jarpath='bin/robot.jar',
               classpath='org.obolibrary.robot.PythonOperation',
               port=25333,
               die_on_exit=True)
```

If you don't specify `die_on_exit=True` in the `launch_gateway` args, the JVM will continue running after the Python process has exited.


### Starting ROBOT from the Command Line

If you prefer, you can start ROBOT from the command line with `robot python` and then connect to it from Python. The ROBOT `python` command does not accept any input ontologies and does not produce any outputs. You *cannot* chain this command with other ROBOT commands. To stop the ROBOT process use `Ctrl+C`.

By default, the `python` command starts a Py4J gateway server on port `25333` (the Py4J default). To change this, use the `--port` option:

```
robot python --port 8000
```

To run this command in the background (Unix, Linux, MacOS), use:

```
robot python &
```

When you're done you can stop the gateway server from the command line with Ctrl+C. If you are running it in the background, first run `fg` to bring it back to the foreground.


### Controlling ROBOT from Python

Once ROBOT has started, you can access any ROBOT methods or objects through the Py4j gateway. Objects must start with `gateway.jvm` followed by the package name (e.g., `org.obolibrary.robot`). For example, use the ROBOT `IOHelper` to load an ontology and print basic information:

```python
from py4j.java_gateway import launch_gateway, JavaGateway

# Not required when running `robot python` from the command line:
launch_gateway(jarpath='bin/robot.jar',
               classpath='org.obolibrary.robot.PythonOperation',
               port=25333,
               die_on_exit=True)

gateway = JavaGateway()

io_helper = gateway.jvm.org.obolibrary.robot.IOHelper()

ont = io_helper.loadOntology('docs/examples/annotated.owl')

print(ont.getOntologyID().getVersionIRI())
```

For details on using ROBOT as a Java library, see the [`robot-core` JavaDocs](https://www.javadoc.io/doc/org.obolibrary.robot/robot-core/latest/index.html) and the [OWLAPI v4 JavaDocs](https://owlcs.github.io/owlapi/apidocs_4/index.html). The [`robot-core` unit tests](https://github.com/ontodev/robot/tree/master/robot-core/src/test/java/org/obolibrary/robot) may also be helpful as examples. For details on using Java from Python see the [Py4J](https://www.py4j.org/) documentation.

