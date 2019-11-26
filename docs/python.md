# Python

### In Python

You can also launch the Gateway in your script using [`py4j.java_gateway.launch_gateway(...)`](https://www.py4j.org/py4j_java_gateway.html#py4j.java_gateway.launch_gateway). This does not require you to start the Java Gateway from the command line. You must specify `jarpath={path/to/robot.jar}` and `classpath=org.obolibrary.robot.PythonOperation` in the args, all other `launch_gateway` args are optional.

```python
import py4j
from py4j.java_gateway import JavaGateway

py4j.java_gateway.launch_gateway(jarpath='path/to/robot.jar', 
                                 classpath='org.obolibrary.robot.PythonOperation',
                                 die_on_exit=True)

gateway = JavaGateway()

io_helper = gateway.jvm.org.obolibrary.robot.IOHelper()

ont = io_helper.loadOntology('ontology.owl')
```

If you do not specify `die_on_exit=True` in the `launch_gateway` args, the JVM will continue running after the Python process has exited.

### On The Command Line

The `python` command enables access of ROBOT operations and objects in Python using [Py4J](https://www.py4j.org/). This command does not accept any input ontologies and does not produce any outputs. You *cannot* chain this command with other ROBOT commands.

By default, the `python` command starts a Py4J gateway server on port `25333` (the Py4J default). To change this, use the `--port` option:
```
robot python --port 8000
```

To run this command in the background (Unix, Linux, MacOS), use:
```
robot python &
```

Once the server has started, you can dynamically access ROBOT through the gateway in a Python script. You can access any ROBOT objects through the gateway, for example, the `IOHelper` to load an ontology:
```python
from py4j.java_gateway import JavaGateway

gateway = JavaGateway()

io_helper = gateway.jvm.org.obolibrary.robot.IOHelper()

ont = io_helper.loadOntology('ontology.owl')
```

Objects must start with `gateway.jvm` followed by the package name (e.g., `org.obolibrary.robot`).

Once finished, you can stop the gateway server from the command line with Ctrl+C. If you are running it in the background, first run the following command to bring it back to the foreground (Unix, Linux, MacOS):
```
fg
```